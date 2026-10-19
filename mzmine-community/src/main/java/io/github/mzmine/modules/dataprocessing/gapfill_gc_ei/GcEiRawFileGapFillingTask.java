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

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.ScanDataType;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.BuildingIonSeries;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.dataprocessing.featdet_extract_mz_ranges.ExtractMzRangesIonSeriesFunction;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.FeatureSmoothingOptions;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.SmoothingAlgorithm;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.collections.BinarySearch;
import io.github.mzmine.util.collections.IndexRange;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fills the gaps of one raw data file in a GC-EI feature list. For each gap, the quantifier and top
 * signals of the row are extracted in the retention time window of the row. The quantifier feature
 * is added to the row if all signals have a peak apex that co-elutes with the quantifier apex, which
 * itself needs to be within the retention time tolerance of the row.
 */
class GcEiRawFileGapFillingTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(GcEiRawFileGapFillingTask.class.getName());

  // decision: additional scans around the window so that smoothing has data at the window edges
  private static final int PADDING_SCANS = 10;

  private final @NotNull ModularFeatureList flist;
  private final @NotNull RawDataFile file;
  private final @NotNull List<GcEiGapFillTarget> gaps;
  private final @NotNull GcEiFeatureFindingSettings settings;
  private final @NotNull RTTolerance sampleToSampleRtTolerance;
  private final @NotNull RTTolerance intraSampleCoelutionTolerance;
  private final @NotNull MZTolerance mzTolerance;
  private final int minConsecutiveScans;
  private final @Nullable SmoothingAlgorithm smoother;
  private final @NotNull Runnable onFinished;
  private final AtomicInteger processedGaps = new AtomicInteger(0);
  private int filledGaps;

  /**
   * @param flist                         the feature list to add the gap-filled features to
   * @param file                          the raw data file to fill gaps in
   * @param gaps                          the rows without feature in this raw data file
   * @param settings                      the original feature detection settings
   * @param sampleToSampleRtTolerance     maximum distance of the quantifier apex to the row RT
   * @param intraSampleCoelutionTolerance maximum distance of the top signal apices to the
   *                                      quantifier apex
   * @param onFinished                    called once all gaps were processed
   */
  GcEiRawFileGapFillingTask(@NotNull final ModularFeatureList flist,
      @NotNull final RawDataFile file, @NotNull final List<GcEiGapFillTarget> gaps,
      @NotNull final GcEiFeatureFindingSettings settings,
      @NotNull final RTTolerance sampleToSampleRtTolerance,
      @NotNull final RTTolerance intraSampleCoelutionTolerance, @NotNull final Runnable onFinished,
      @NotNull final Instant moduleCallDate) {
    // final features are stored in the storage of the feature list
    super(null, moduleCallDate);
    this.flist = flist;
    this.file = file;
    this.gaps = gaps;
    this.settings = settings;
    this.sampleToSampleRtTolerance = sampleToSampleRtTolerance;
    this.intraSampleCoelutionTolerance = intraSampleCoelutionTolerance;
    this.onFinished = onFinished;
    mzTolerance = settings.mzTolerance();
    minConsecutiveScans = settings.minConsecutiveScans();
    // smoothers are created per file as they may keep buffers
    smoother = settings.smoothingParameters() != null ? FeatureSmoothingOptions.createSmoother(
        settings.smoothingParameters()) : null;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    final List<? extends Scan> scans = getScans();
    if (scans.isEmpty()) {
      logger.warning(
          () -> "No scans selected for %s in feature list %s. Skipping gap filling.".formatted(
              file.getName(), flist.getName()));
    } else {
      for (final GcEiGapFillTarget gap : gaps) {
        if (isCanceled()) {
          return;
        }
        if (fillGap(gap, scans)) {
          filledGaps++;
        }
        processedGaps.incrementAndGet();
      }
    }
    onFinished.run();
    setStatus(TaskStatus.FINISHED);
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

  /**
   * @return true if the quantifier feature was added to the row
   */
  private boolean fillGap(@NotNull final GcEiGapFillTarget gap,
      @NotNull final List<? extends Scan> allScans) {
    final Range<Float> quantifierRange = sampleToSampleRtTolerance.getToleranceRange(gap.rt());
    // other signals may peak within the co-elution tolerance around the quantifier apex
    final Range<Float> searchRange = Range.closed(
        intraSampleCoelutionTolerance.getToleranceRange(quantifierRange.lowerEndpoint())
            .lowerEndpoint(),
        intraSampleCoelutionTolerance.getToleranceRange(quantifierRange.upperEndpoint())
            .upperEndpoint());
    final IndexRange window = BinarySearch.indexRange(searchRange.span(gap.detectedRange()),
        allScans, Scan::getRetentionTime);
    if (window.isEmpty()) {
      return false;
    }
    final List<? extends Scan> scans = allScans.subList(
        Math.max(0, window.min() - PADDING_SCANS),
        Math.min(allScans.size(), window.maxExclusive() + PADDING_SCANS));

    // first signal is the quantifier
    final double[] mzs = gap.signalsToConfirm(mzTolerance);
    final List<IonTimeSeries<? extends Scan>> chromatograms = extractChromatograms(mzs, scans);
    if (chromatograms.isEmpty()) {
      return false;
    }

    final IonTimeSeries<? extends Scan> quantifier = chromatograms.getFirst();
    final GcEiSignalApex quantifierApex = findApex(quantifier, quantifierRange);
    if (quantifierApex == null) {
      return false;
    }
    final Range<Float> coelutionRange = intraSampleCoelutionTolerance.getToleranceRange(
        quantifierApex.rt());
    for (int i = 1; i < chromatograms.size(); i++) {
      if (findApex(chromatograms.get(i), coelutionRange) == null) {
        return false;
      }
    }

    // decision: integrate the typical peak width of the row around the apex found in this sample
    final IonTimeSeries<? extends Scan> featureData = quantifier.subSeries(
        flist.getMemoryMapStorage(), quantifierApex.rt() - gap.leftWidth(),
        quantifierApex.rt() + gap.rightWidth());
    if (featureData.getNumberOfValues() == 0) {
      return false;
    }
    final ModularFeature feature = new ModularFeature(flist, file, featureData,
        FeatureStatus.ESTIMATED);
    // row bindings are applied once after all files were processed
    gap.row().addFeature(file, feature, false);
    return true;
  }

  /**
   * Extracts the most intense data point per scan within the m/z tolerance of each m/z and applies
   * the smoothing of the original processing.
   *
   * @return one chromatogram per m/z in the input order, including zero intensities. Empty if
   * canceled
   */
  private @NotNull List<IonTimeSeries<? extends Scan>> extractChromatograms(
      final double @NotNull [] mzs, @NotNull final List<? extends Scan> scans) {
    // the extraction requires m/z ranges sorted ascending
    final int[] sortedIndices = IntStream.range(0, mzs.length).boxed()
        .sorted(Comparator.comparingDouble(i -> mzs[i])).mapToInt(Integer::intValue).toArray();
    final List<Range<Double>> sortedRanges = new ArrayList<>(mzs.length);
    for (final int index : sortedIndices) {
      sortedRanges.add(mzTolerance.getToleranceRange(mzs[index]));
    }
    final BuildingIonSeries[] extracted = new ExtractMzRangesIonSeriesFunction(file, scans,
        sortedRanges, ScanDataType.MASS_LIST, this).get();
    if (extracted.length != mzs.length) {
      return List.of();
    }

    final List<IonTimeSeries<? extends Scan>> chromatograms = new ArrayList<>(mzs.length);
    for (int i = 0; i < mzs.length; i++) {
      chromatograms.add(null);
    }
    for (int sorted = 0; sorted < sortedIndices.length; sorted++) {
      chromatograms.set(sortedIndices[sorted], smooth(extracted[sorted].toFullIonTimeSeries(null,
          scans)));
    }
    return chromatograms;
  }

  private @NotNull IonTimeSeries<? extends Scan> smooth(
      @NotNull final IonTimeSeries<? extends Scan> chromatogram) {
    if (smoother == null) {
      return chromatogram;
    }
    // null if retention time smoothing is disabled
    final double[] smoothed = smoother.smoothRt(chromatogram);
    return smoothed == null ? chromatogram : chromatogram.copyAndReplace(null, smoothed);
  }

  /**
   * A peak apex is the highest local maximum within the range with at least the minimum number of
   * consecutive scans of the chromatogram builder.
   *
   * @return the apex or null if there is no peak apex within the range
   */
  private @Nullable GcEiSignalApex findApex(@NotNull final IonTimeSeries<? extends Scan> series,
      @NotNull final Range<Float> range) {
    GcEiSignalApex best = null;
    // decision: the first and last data points cannot be a confirmed apex
    for (int i = 1; i < series.getNumberOfValues() - 1; i++) {
      final float rt = series.getRetentionTime(i);
      final double intensity = series.getIntensity(i);
      if (!range.contains(rt) || intensity <= 0 || intensity <= series.getIntensity(i - 1)
          || intensity < series.getIntensity(i + 1)) {
        continue;
      }
      if ((best == null || intensity > best.height())
          && countConsecutiveScans(series, i) >= minConsecutiveScans) {
        best = new GcEiSignalApex(i, rt, intensity);
      }
    }
    return best;
  }

  /**
   * @return the number of consecutive data points with an intensity above zero around the index
   */
  private static int countConsecutiveScans(@NotNull final IonTimeSeries<? extends Scan> series,
      final int index) {
    int start = index;
    while (start > 0 && series.getIntensity(start - 1) > 0) {
      start--;
    }
    int end = index;
    while (end < series.getNumberOfValues() - 1 && series.getIntensity(end + 1) > 0) {
      end++;
    }
    return end - start + 1;
  }

  int getFilledGaps() {
    return filledGaps;
  }

  @Override
  public String getTaskDescription() {
    return "GC-EI gap filling of %s (%d/%d gaps)".formatted(file.getName(), processedGaps.get(),
        gaps.size());
  }

  @Override
  public double getFinishedPercentage() {
    return gaps.isEmpty() ? 1 : processedGaps.get() / (double) gaps.size();
  }
}
