/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.gapfill_gc_ei;

import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.data_access.FeatureDataAccess;
import io.github.mzmine.datamodel.featuredata.FeatureDataUtils;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.FeatureDataType;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.GeneralResolverParameters;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.Resolver;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.FeatureSmoothingOptions;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.SmoothingAlgorithm;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.ZeroHandlingType;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.taskcontrol.Task;
import it.unimi.dsi.fastutil.doubles.Double2IntOpenHashMap;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fills the gaps of one raw data file in a GC-EI feature list. For each gap, the quantifier m/z and
 * the top signals of the row are re-detected with the original chromatogram builder, smoothing, and
 * resolver settings. The resolved quantifier feature is added to the row if its apex is within the
 * retention time tolerance of the row and all top signals were resolved as co-eluting features.
 */
class GcEiRawFileGapFiller {

  private static final Logger logger = Logger.getLogger(GcEiRawFileGapFiller.class.getName());

  // decision: chromatograms span the full retention time range (the resolver thresholds depend on
  // the whole chromatogram), so the number of chromatograms per pass is limited to bound memory
  private static final int MAX_CHROMATOGRAMS_PER_PASS = 250;

  private final @NotNull ModularFeatureList flist;
  private final @NotNull RawDataFile file;
  private final @NotNull GcEiFeatureFindingSettings settings;
  private final @NotNull RTTolerance rtTolerance;
  private final @NotNull RTTolerance coelutionTolerance;
  private final @NotNull MZTolerance mzTolerance;
  private final @NotNull GcEiTargetedChromatogramBuilder chromatogramBuilder;
  private final @Nullable SmoothingAlgorithm smoother;

  /**
   * @param flist              the feature list to add the gap-filled features to
   * @param file               the raw data file to fill gaps in
   * @param settings           the original feature detection settings
   * @param rtTolerance        maximum distance of the quantifier apex to the row retention time
   * @param coelutionTolerance maximum distance of the top signal apices to the quantifier apex
   */
  GcEiRawFileGapFiller(@NotNull final ModularFeatureList flist, @NotNull final RawDataFile file,
      @NotNull final GcEiFeatureFindingSettings settings, @NotNull final RTTolerance rtTolerance,
      @NotNull final RTTolerance coelutionTolerance) {
    this.flist = flist;
    this.file = file;
    this.settings = settings;
    this.rtTolerance = rtTolerance;
    this.coelutionTolerance = coelutionTolerance;
    mzTolerance = settings.mzTolerance();
    chromatogramBuilder = new GcEiTargetedChromatogramBuilder(settings);
    // smoothers are created per file as they may keep buffers
    smoother = settings.smoothingParameters() != null ? FeatureSmoothingOptions.createSmoother(
        settings.smoothingParameters()) : null;
  }

  /**
   * @param gaps       the rows without feature in this raw data file
   * @param parentTask checked for cancellation
   * @return the number of filled gaps
   */
  int fillGaps(@NotNull final List<GcEiGapFillTarget> gaps, @NotNull final Task parentTask) {
    final List<? extends Scan> scans = getScans();
    if (scans.isEmpty()) {
      logger.warning(
          () -> "No scans selected for %s in feature list %s. Skipping gap filling.".formatted(
              file.getName(), flist.getName()));
      return 0;
    }

    int filled = 0;
    final List<GcEiGapFillTarget> chunk = new ArrayList<>();
    final DoubleArrayList chunkMzs = new DoubleArrayList();
    // maps each distinct m/z of the chunk to its chromatogram index
    final Double2IntOpenHashMap mzIndex = new Double2IntOpenHashMap();

    for (final GcEiGapFillTarget gap : gaps) {
      if (parentTask.isCanceled()) {
        return filled;
      }
      if (chunkMzs.size() + gap.topMzs().length + 1 > MAX_CHROMATOGRAMS_PER_PASS
          && !chunk.isEmpty()) {
        filled += fillChunk(chunk, chunkMzs, mzIndex, scans);
        chunk.clear();
        chunkMzs.clear();
        mzIndex.clear();
      }

      chunk.add(gap);
      addMz(gap.quantifierMz(), chunkMzs, mzIndex);
      for (final double mz : gap.topMzs()) {
        addMz(mz, chunkMzs, mzIndex);
      }
    }

    if (!chunk.isEmpty() && !parentTask.isCanceled()) {
      filled += fillChunk(chunk, chunkMzs, mzIndex, scans);
    }
    return filled;
  }

  private static void addMz(final double mz, @NotNull final DoubleArrayList mzs,
      @NotNull final Double2IntOpenHashMap mzIndex) {
    if (!mzIndex.containsKey(mz)) {
      mzIndex.put(mz, mzs.size());
      mzs.add(mz);
    }
  }

  /**
   * @return the scans used to build the original chromatograms
   */
  private @NotNull List<? extends Scan> getScans() {
    final List<? extends Scan> selected = flist.getSeletedScans(file);
    if (selected != null) {
      return selected;
    }
    // assumption: without selected scans, the scan selection of the chromatogram builder applies
    return List.of(settings.scanSelection().getMatchingScans(file));
  }

  private int fillChunk(@NotNull final List<GcEiGapFillTarget> chunk,
      @NotNull final DoubleArrayList mzs, @NotNull final Double2IntOpenHashMap mzIndex,
      @NotNull final List<? extends Scan> scans) {
    final List<@Nullable IonTimeSeries<Scan>> chromatograms = chromatogramBuilder.buildChromatograms(
        file, scans, mzs.toDoubleArray());
    final List<List<GcEiResolvedPeak>> peaksPerMz = resolve(chromatograms, scans);

    int filled = 0;
    for (final GcEiGapFillTarget gap : chunk) {
      if (tryFillGap(gap, peaksPerMz, mzIndex)) {
        filled++;
      }
    }
    return filled;
  }

  /**
   * Smooths and resolves the chromatograms with the same code as the original processing. A
   * temporary single-file feature list is used, as smoothing and resolving operate on
   * {@link FeatureDataAccess} that includes zeros for all selected scans.
   *
   * @return the resolved peaks for each chromatogram index, empty lists for missing chromatograms
   */
  private @NotNull List<List<GcEiResolvedPeak>> resolve(
      @NotNull final List<@Nullable IonTimeSeries<Scan>> chromatograms,
      @NotNull final List<? extends Scan> scans) {
    final List<List<GcEiResolvedPeak>> peaksPerMz = new ArrayList<>(
        Collections.nCopies(chromatograms.size(), List.of()));

    final ModularFeatureList tempList = new ModularFeatureList("GC-EI gap filling " + file.getName(),
        null, chromatograms.size(), chromatograms.size(), file);
    tempList.setSelectedScans(file, scans);
    for (int i = 0; i < chromatograms.size(); i++) {
      final IonTimeSeries<Scan> chromatogram = chromatograms.get(i);
      if (chromatogram == null) {
        continue;
      }
      final ModularFeature feature = new ModularFeature(tempList, file, chromatogram,
          FeatureStatus.DETECTED);
      // row ID links the feature back to the chromatogram index
      tempList.addRow(new ModularFeatureListRow(tempList, i + 1, feature));
    }
    if (tempList.getNumberOfRows() == 0) {
      return peaksPerMz;
    }

    if (smoother != null) {
      // same as the smoothing task
      final FeatureDataAccess access = EfficientDataAccess.of(tempList,
          EfficientDataAccess.FeatureDataType.INCLUDE_ZEROS, file);
      while (access.hasNextFeature()) {
        final ModularFeature feature = (ModularFeature) access.nextFeature();
        feature.set(FeatureDataType.class,
            smoother.smoothFeature(null, access, feature, ZeroHandlingType.KEEP));
        FeatureDataUtils.recalculateIonSeriesDependingTypes(feature);
      }
    }

    // resolvers keep buffers, therefore one instance per feature list
    final GeneralResolverParameters resolverParameters = settings.resolverParameters();
    final Resolver resolver = resolverParameters.getResolver(resolverParameters, tempList);
    if (resolver == null) {
      throw new IllegalStateException("Resolver could not be initialised.");
    }

    // same as the feature resolver task
    final FeatureDataAccess access = EfficientDataAccess.of(tempList,
        EfficientDataAccess.FeatureDataType.INCLUDE_ZEROS, file);
    while (access.hasNextFeature()) {
      final Feature feature = access.nextFeature();
      final List<IonTimeSeries<? extends Scan>> resolved = resolver.resolve(access, null);
      final List<GcEiResolvedPeak> peaks = resolved.stream()
          .filter(series -> series.getNumberOfValues() > 0).map(GcEiResolvedPeak::of).toList();
      peaksPerMz.set(feature.getRow().getID() - 1, peaks);
    }
    return peaksPerMz;
  }

  /**
   * @return true if the quantifier feature was added to the row
   */
  private boolean tryFillGap(@NotNull final GcEiGapFillTarget gap,
      @NotNull final List<List<GcEiResolvedPeak>> peaksPerMz,
      @NotNull final Double2IntOpenHashMap mzIndex) {
    final GcEiResolvedPeak quantifier = GcEiResolvedPeak.findClosestApex(
        peaksPerMz.get(mzIndex.get(gap.quantifierMz())), gap.rt(), rtTolerance);
    if (quantifier == null) {
      return false;
    }

    for (final double mz : gap.topMzs()) {
      // decision: a top signal within the m/z tolerance of the quantifier is confirmed already
      if (mzTolerance.checkWithinTolerance(mz, gap.quantifierMz())) {
        continue;
      }
      final GcEiResolvedPeak coeluting = GcEiResolvedPeak.findClosestApex(
          peaksPerMz.get(mzIndex.get(mz)), quantifier.apexRt(), coelutionTolerance);
      if (coeluting == null) {
        return false;
      }
    }

    // resolved data is in RAM, store it with the feature list
    final IonTimeSeries<? extends Scan> series = quantifier.series();
    final double[] mzs = series.getMzValues(new double[series.getNumberOfValues()]);
    final double[] intensities = series.getIntensityValues(
        new double[series.getNumberOfValues()]);
    final ModularFeature feature = new ModularFeature(flist, file,
        series.copyAndReplace(flist.getMemoryMapStorage(), mzs, intensities),
        FeatureStatus.ESTIMATED);
    // row bindings are applied once after all files were processed
    gap.row().addFeature(file, feature, false);
    return true;
  }
}
