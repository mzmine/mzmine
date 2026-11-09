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

package io.github.mzmine.modules.tools.tools_autoparam.runphases;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.collections.BinarySearch;
import io.github.mzmine.util.collections.BinarySearch.DefaultTo;
import io.github.mzmine.util.scans.ScanUtils;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import org.jetbrains.annotations.NotNull;

/**
 * Small numeric helpers for the run phase traces. All medians ignore NaN values.
 */
final class TraceMath {

  private TraceMath() {
  }

  /**
   * @return the mass list if present, otherwise the scan itself
   */
  static @NotNull MassSpectrum spectrum(@NotNull Scan scan) {
    return scan.getMassList() != null ? scan.getMassList() : scan;
  }

  /**
   * @return the highest intensity within the tolerance around mz, 0 if there is none
   */
  static double maxInTolerance(@NotNull MassSpectrum spectrum, double mz,
      @NotNull MZTolerance tolerance) {
    final double tol = tolerance.getMzToleranceForMass(mz);
    final DataPoint basePeak = ScanUtils.findBasePeak(spectrum, mz - tol, mz + tol);
    return basePeak != null ? basePeak.getIntensity() : 0;
  }

  /**
   * decision: not {@link io.github.mzmine.util.MathUtils#calcMedian(double[])}. NaN marks
   * undetected values and must stay NaN for an empty input, where MathUtils returns 0. The upper
   * middle element (no interpolation) is what the run phase thresholds were tuned with, the same
   * applies to {@link #quantile(double[], double)}.
   *
   * @return median ignoring NaN values, NaN if there are no values
   */
  static double median(double @NotNull [] values) {
    final double[] sorted = Arrays.stream(values).filter(x -> !Double.isNaN(x)).sorted().toArray();
    return sorted.length == 0 ? Double.NaN : sorted[sorted.length / 2];
  }

  static double median(@NotNull List<Double> values) {
    return median(values.stream().mapToDouble(Double::doubleValue).toArray());
  }

  /**
   * @return the value at the quantile of the non NaN values, NaN if there are none
   */
  static double quantile(double @NotNull [] values, double quantile) {
    final double[] sorted = Arrays.stream(values).filter(x -> !Double.isNaN(x)).sorted().toArray();
    return sorted.length == 0 ? Double.NaN
        : sorted[Math.min(sorted.length - 1, (int) (sorted.length * quantile))];
  }

  /**
   * Rolling median over a centered window of scans.
   */
  static double @NotNull [] rollingMedian(double @NotNull [] values, int window) {
    final double[] result = new double[values.length];
    for (int i = 0; i < values.length; i++) {
      result[i] = median(Arrays.copyOfRange(values, Math.max(0, i - window / 2),
          Math.min(values.length, i + window / 2 + 1)));
    }
    return result;
  }

  /**
   * Rolling median over a retention time window, robust to a varying scan density.
   *
   * @param rt        ascending retention times
   * @param halfWidth half window width in minutes
   */
  static double @NotNull [] rollingMedianRt(double @NotNull [] rt, double @NotNull [] values,
      double halfWidth) {
    final double[] result = new double[values.length];
    int from = 0;
    int to = 0;
    for (int i = 0; i < values.length; i++) {
      while (rt[from] < rt[i] - halfWidth) {
        from++;
      }
      while (to < values.length && rt[to] <= rt[i] + halfWidth) {
        to++;
      }
      result[i] = median(Arrays.copyOfRange(values, from, to));
    }
    return result;
  }

  /**
   * Spearman rank correlation of the values against their index. Ties (e.g. scans where an ion is
   * missing) get their average rank.
   *
   * @return the correlation, 0 for fewer than 3 values or constant values
   */
  static double spearman(double @NotNull [] values) {
    if (values.length < 3) {
      return 0;
    }
    final double[] index = new double[values.length];
    for (int i = 0; i < index.length; i++) {
      index[i] = i;
    }
    final double correlation = new SpearmansCorrelation().correlation(index, values);
    // constant values have no rank variance
    return Double.isNaN(correlation) ? 0 : correlation;
  }

  /**
   * @return log10(value + 1) of each value
   */
  static double @NotNull [] log10(double @NotNull [] values) {
    return Arrays.stream(values).map(v -> Math.log10(v + 1)).toArray();
  }

  /**
   * @return the first index with rt >= time, the last index if there is none
   */
  static int indexAtRt(double @NotNull [] rt, double time) {
    int i = BinarySearch.binarySearch(rt, time, DefaultTo.GREATER_EQUALS);
    if (i == -1) {
      return rt.length - 1;
    }
    // an exact match may be any of several equal retention times
    while (i > 0 && rt[i - 1] >= time) {
      i--;
    }
    return i;
  }

  static int argMax(double @NotNull [] values, int from, int to) {
    int best = from;
    for (int i = from; i < to; i++) {
      if (values[i] > values[best]) {
        best = i;
      }
    }
    return best;
  }

  static double max(double @NotNull [] values) {
    return Arrays.stream(values).max().orElse(Double.NaN);
  }

  static double min(double @NotNull [] values) {
    return Arrays.stream(values).min().orElse(Double.NaN);
  }
}
