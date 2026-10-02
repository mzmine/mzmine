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

package io.github.mzmine.modules.tools.tools_autoparam;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.SimpleRange.SimpleFloatRange;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.MzToleranceSearchOptions;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @param effectiveRtRange retention time range of the run that contains the separation, without
 *                         dead volume, calibrant plugs and re-equilibration, in minutes. Null if
 *                         the file has too few MS1 scans or no run phase was detected
 * @param referenceInjectionTime median MS1 injection time in ms within the effective RT range,
 *                               see
 *                               {@link RawDataParameterEstimation#estimateReferenceInjectionTime}.
 *                               Null if there is no injection time or it is ion mobility data
 */
public record DataFileStatistics(RawDataFile file, List<FeatureStatistics> featureStatistics,
                                 @Nullable SimpleFloatRange effectiveRtRange,
                                 @Nullable Double referenceInjectionTime) {

  public double[] getEdgeIntensities() {
    return featureStatistics.stream()
        .flatMapToDouble(stats -> Arrays.stream(stats.getNonZeroIsotopeEdgeIntensities()))
        .toArray();
  }

  public double[] getIsotopePeakFwhms() {
    return featureStatistics.stream()
        .flatMapToDouble(stats -> Arrays.stream(stats.getBestIsotopesFWHMs())).toArray();
  }

  public int[] getNumberOfLowestIsotopeDataPoints() {
    return featureStatistics.stream()
        .mapToInt(fs -> fs.getBestEnvelope().getNumberOfLowestIsotopeDataPoints()).toArray();
  }

  public MzToMzTolerancePair[] getBestTolerances() {
    return featureStatistics.stream()
        .map(stats -> new MzToMzTolerancePair(stats.getMz(), stats.getBestTolerance()))
        .toArray(MzToMzTolerancePair[]::new);
  }

  /**
   * Counts how often each tolerance in {@link MzToleranceSearchOptions#ALL_TOLERANCE_OPTIONS}
   * was selected as best tolerance. Returns a map from tolerance label to count, preserving the
   * order of the tolerance array.
   */
  public @NotNull Map<MZTolerance, Integer> extractToleranceCounts() {
    final MZTolerance[] allTolerances = MzToleranceSearchOptions.ALL_TOLERANCE_OPTIONS;
    final Map<MZTolerance, Integer> counts = new LinkedHashMap<>();
    for (MZTolerance tol : allTolerances) {
      counts.put(tol, 0);
    }
    for (MzToMzTolerancePair pair : getBestTolerances()) {
      final MZTolerance key = pair.tolerance();
      counts.merge(key, 1, Integer::sum);
    }
    return counts;
  }

  public MZTolerance getMzToleranceForIsotopes() {
    MZTolerance harmonizedTolerance = null;
    for (FeatureStatistics stats : featureStatistics) {
      harmonizedTolerance = MZTolerance.enlargeByDominatingTolerance(harmonizedTolerance,
          stats.getBestEnvelope().isotopeTraces().getLast());
    }
    return harmonizedTolerance;
  }

  public double[] getLowestIsotopeHeights() {
    return featureStatistics().stream().map(FeatureStatistics::getBestEnvelope)
        .map(fwi -> fwi.isotopeTraces().getLast()).mapToDouble(Feature::getHeight).toArray();
  }

  /**
   * Lowest isotope heights as they would be in a scan with the {@link #referenceInjectionTime}.
   * Height times injection time is proportional to the charges in the trap, and the detection limit
   * is constant in charges. The seed features are intense, so their apexes are in short-IT scans
   * with a raised noise floor, and their raw heights overestimate the minimum height.
   *
   * @return the converted heights, or the raw heights if this file is not injection-time normalized
   */
  public double @NotNull [] getInjectionTimeCorrectedLowestIsotopeHeights() {
    if (referenceInjectionTime == null) {
      return getLowestIsotopeHeights();
    }
    final DoubleArrayList heights = new DoubleArrayList();
    for (final FeatureStatistics stats : featureStatistics) {
      final Feature lowest = stats.getBestEnvelope().isotopeTraces().getLast();
      final Scan apex = lowest.getRepresentativeScan();
      final Float apexInjectionTime = apex == null ? null : apex.getInjectionTime();
      final Float height = lowest.getHeight();
      // assumption: a height without the apex injection time cannot be converted and is left out
      if (apexInjectionTime == null || apexInjectionTime <= 0 || height == null) {
        continue;
      }
      heights.add(height * apexInjectionTime / referenceInjectionTime);
    }
    return heights.toDoubleArray();
  }
}
