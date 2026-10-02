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

import io.github.mzmine.modules.tools.tools_autoparam.estimation.MzToleranceSearchOptions;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * Defines the plottable data types extractable from {@link DataFileStatistics}. Each value provides
 * axis labels, a default bin width for histograms, and an extraction method.
 */
public enum StatisticsPlotType {

  FWHM("Isotope Peak FWHM", "FWHM / min", "Frequency", 0.005), EDGE_INTENSITY(
      "Edge Intensities (Noise)", "Intensity / a.u.", "Frequency", -1), LOWEST_ISOTOPE_HEIGHT(
      "Lowest Isotope Heights", "Height / a.u.", "Frequency",
      -1), // decision: bin width of 2 instead of 1 because the data point counts often alternate between
  // even and odd values, creating a comb pattern with bin width 1 that breaks the Gaussian fit
  ISOTOPE_DATA_POINTS("Data Points per Isotope", "# Data Points", "Frequency",
          2), BEST_TOLERANCE_FREQUENCY("Best m/z Tolerance", "Tolerance", "Count", -1);

  private final String label;
  private final String xAxisLabel;
  private final String yAxisLabel;
  private final double defaultBinWidth;

  StatisticsPlotType(@NotNull String label, @NotNull String xAxisLabel, @NotNull String yAxisLabel,
      double defaultBinWidth) {
    this.label = label;
    this.xAxisLabel = xAxisLabel;
    this.yAxisLabel = yAxisLabel;
    this.defaultBinWidth = defaultBinWidth;
  }

  public @NotNull String label() {
    return label;
  }

  public @NotNull String xAxisLabel() {
    return xAxisLabel;
  }

  public @NotNull String yAxisLabel() {
    return yAxisLabel;
  }

  public double defaultBinWidth() {
    return defaultBinWidth;
  }

  /**
   * @return true for histogram-based types, false for the category bar chart
   * ({@link #BEST_TOLERANCE_FREQUENCY}).
   */
  public boolean isHistogram() {
    return this != BEST_TOLERANCE_FREQUENCY;
  }

  /**
   * @return true if a Gaussian fit should be added to the histogram for this type.
   */
  public boolean hasGaussianFit() {
    return switch (this) {
      case FWHM, ISOTOPE_DATA_POINTS -> true;
      case EDGE_INTENSITY, LOWEST_ISOTOPE_HEIGHT, BEST_TOLERANCE_FREQUENCY -> false;
    };
  }

  /**
   * Extracts the raw data array for histogram plotting from the given statistics. Not applicable
   * for {@link #BEST_TOLERANCE_FREQUENCY} — use {@link #extractToleranceCounts} instead.
   */
  public double @NotNull [] extractData(@NotNull DataFileStatistics stats) {
    return switch (this) {
      case FWHM -> stats.getIsotopePeakFwhms();
      case EDGE_INTENSITY -> stats.getEdgeIntensities();
      // the same values the min height is estimated from, so the marked estimate fits the histogram
      case LOWEST_ISOTOPE_HEIGHT -> stats.getInjectionTimeCorrectedLowestIsotopeHeights();
      case ISOTOPE_DATA_POINTS ->
          Arrays.stream(stats.getNumberOfLowestIsotopeDataPoints()).asDoubleStream().toArray();
      case BEST_TOLERANCE_FREQUENCY -> new double[0];
    };
  }

  /**
   * Counts how often each tolerance in {@link MzToleranceSearchOptions#ALL_TOLERANCE_OPTIONS}
   * was selected as best tolerance. Returns a map from tolerance label to count, preserving the
   * order of the tolerance array.
   */
  public static @NotNull Map<String, Integer> extractToleranceCounts(
      @NotNull DataFileStatistics stats) {
    final MZTolerance[] allTolerances = MzToleranceSearchOptions.ALL_TOLERANCE_OPTIONS;
    final Map<String, Integer> counts = new LinkedHashMap<>();
    for (MZTolerance tol : allTolerances) {
      counts.put(tol.toString(), 0);
    }
    for (MzToMzTolerancePair pair : stats.getBestTolerances()) {
      final String key = pair.tolerance().toString();
      counts.merge(key, 1, Integer::sum);
    }
    return counts;
  }

  @Override
  public String toString() {
    return label;
  }
}
