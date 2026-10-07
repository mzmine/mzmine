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

package io.github.mzmine.modules.tools.tools_autoparam.statistics;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.ScanDataType;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.BuildingIonSeries;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.correlation.CorrelationData;
import io.github.mzmine.modules.dataprocessing.featdet_extract_mz_ranges.ExtractMzRangesIonSeriesFunction;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.correlation.FeatureCorrelationUtil.DIA;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.RangeUtils;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @param mzTolerance   mz tolerance for this envelope
 * @param mainFeature   the main feature this envelope was created around
 * @param isotopeTraces the correlated traces
 * @param isotopeCorr   correlation coefficients for the correlated traces
 */
public record FeatureWithIsotopeTraces(double initialMz, @NotNull MZTolerance mzTolerance,
                                       ModularFeature mainFeature,
                                       @NotNull List<ModularFeature> isotopeTraces,
                                       double[] isotopeCorr) {

  private static final double corrCutoff = 0.8;

  @Nullable
  public static FeatureWithIsotopeTraces of(final double initialMz, @NotNull RawDataFile file,
      @NotNull MZTolerance tolerance, @NotNull FeatureWithIsotopeRanges envelope,
      @Nullable MemoryMapStorage storage, @NotNull Task parentTask) {
    final ModularFeature mainFeature = envelope.mostIntenseIsotope();
    final IonTimeSeries<? extends Scan> mainPeak = mainFeature.getFeatureData();

    final ExtractMzRangesIonSeriesFunction extraction = new ExtractMzRangesIonSeriesFunction(file,
        mainPeak.getSpectra(), envelope.isotopeMzsSorted(), ScanDataType.MASS_LIST, parentTask);
    final @NotNull BuildingIonSeries[] ionSeries = extraction.get();

    final double[] mainIntensities = new double[mainPeak.getNumberOfValues()];
    final double[] mainRts = new double[mainPeak.getNumberOfValues()];
    final double[] isotopeIntensities = new double[mainPeak.getNumberOfValues()];
    mainPeak.getIntensityValues(mainIntensities);
    for (int i = 0; i < mainPeak.getNumberOfValues(); i++) {
      mainRts[i] = mainPeak.getRetentionTime(i);
    }

    List<ModularFeature> allIsotopeSeries = new ArrayList<>();
    final DoubleArrayList correlations = new DoubleArrayList();

    for (int i = 0; i < ionSeries.length; i++) {
      final BuildingIonSeries buildingIsotopeSeries = ionSeries[i];
      // use the same scans as the main feature has, so we can reuse the RT array for the correlation
      final IonTimeSeries<? extends Scan> isotopeSeries = buildingIsotopeSeries.toFullIonTimeSeries(
          storage, mainPeak.getSpectra());
      isotopeSeries.getIntensityValues(isotopeIntensities);

      final CorrelationData corrData = DIA.corrFeatureShape(mainRts, mainIntensities, mainRts,
          isotopeIntensities, 5, 2, 0);

      if (corrData != null && corrData.getPearsonR() > corrCutoff) {
        allIsotopeSeries.add(
            new ModularFeature((ModularFeatureList) envelope.mostIntenseIsotope().getFeatureList(),
                file, isotopeSeries, FeatureStatus.DETECTED));
        correlations.add(corrData.getPearsonR());
      } else {
        break;
      }
    }

    if (allIsotopeSeries.isEmpty()) {
      return null;
    }

    return new FeatureWithIsotopeTraces(initialMz, tolerance, mainFeature, allIsotopeSeries,
        correlations.toDoubleArray());
  }

  public static @Nullable FeatureWithIsotopeTraces getBest(
      @NotNull List<@NotNull FeatureWithIsotopeTraces> envelopes) {
    if (envelopes.isEmpty()) {
      return null;
    }

    int bestNum = 0;
    double bestScore = 0;
    FeatureWithIsotopeTraces best = null;

    final int maxIsotopes = envelopes.stream().mapToInt(i -> i.isotopeTraces().size()).max()
        .orElse(0);
    if (maxIsotopes == 0) {
      throw new IllegalArgumentException("No isotopes found.");
    }

    // could think of a better way to score in the future
    for (FeatureWithIsotopeTraces featureWithIsoTraces : envelopes) {
      final double accumulatedCorr = Arrays.stream(featureWithIsoTraces.isotopeCorr()).sum();
      if (accumulatedCorr > bestScore && bestNum < featureWithIsoTraces.isotopeTraces().size()) {
        bestScore = accumulatedCorr;
        best = featureWithIsoTraces;
        bestNum = featureWithIsoTraces.isotopeTraces().size();
      }
    }

    return best;
  }

  public int getNumberOfLowestIsotopeDataPoints() {
    ModularFeature isotope = null;
    Float fwhm = null;
    for (int i = isotopeTraces.size() - 1; i >= 0; i--) {
      isotope = isotopeTraces.get(i);
      fwhm = isotope.getFWHM();
      if (fwhm != null && Float.isFinite(fwhm)) {
        break;
      }
    }
    if (isotope == null || fwhm == null || !Float.isFinite(fwhm)) {
      return 0;
    }

    final Range<Float> range = RangeUtils.rangeAround(isotope.getRT(), (float) 2.5 * fwhm);
    return (int) isotope.getFeatureData().getSpectra().stream()
        .filter(s -> range.contains(s.getRetentionTime())).count();
  }

  public Stream<ModularFeature> streamFeatures() {
    return Stream.concat(Stream.of(mainFeature), isotopeTraces.stream());
  }
}
