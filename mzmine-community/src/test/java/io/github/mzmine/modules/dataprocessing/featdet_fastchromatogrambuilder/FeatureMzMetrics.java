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

package io.github.mzmine.modules.dataprocessing.featdet_fastchromatogrambuilder;

import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * m/z quality of resolved features on real data without ground truth: the scatter of the intense
 * data points of a feature around their center and intense data points farther than the tolerance
 * from the feature m/z, e.g., a neighboring ion or a shared centroid of two ions. Weak data points
 * scatter more on all instruments and do not count.
 */
final class FeatureMzMetrics {

  private FeatureMzMetrics() {
  }

  /**
   * @param features         resolved features
   * @param medianSpreadPpm  median of the intensity weighted standard deviation of the intense data
   *                         points of a feature around their weighted mean, features with at least
   *                         3 intense data points
   * @param p95SpreadPpm     95% quantile of this spread
   * @param foreignFeatures  features with an intense data point farther than the tolerance from the
   *                         feature m/z
   */
  record Result(int features, double medianSpreadPpm, double p95SpreadPpm, int foreignFeatures) {

  }

  /**
   * @param resolved   detected data points of the resolved features
   * @param featureMzs m/z of each feature in the feature list, same order
   */
  @NotNull
  static Result evaluate(@NotNull List<EvaluatedChromatogram> resolved,
      @NotNull double[] featureMzs, @NotNull MZTolerance tolerance) {
    final DoubleArrayList spreads = new DoubleArrayList();
    int foreign = 0;
    for (int f = 0; f < resolved.size(); f++) {
      final EvaluatedChromatogram feature = resolved.get(f);
      final int apex = feature.apexIndex();
      if (apex < 0) {
        continue;
      }
      final double minIntense =
          FeatureHoleMetrics.INTENSE_FRACTION * feature.intensities()[apex];
      final double featureMz = featureMzs[f];
      final double tol = tolerance.getMzToleranceForMass(featureMz);
      double weights = 0;
      double sum = 0;
      int count = 0;
      boolean hasForeign = false;
      for (int i = 0; i < feature.size(); i++) {
        final double intensity = feature.intensities()[i];
        if (intensity < minIntense) {
          continue;
        }
        final double mz = feature.mzs()[i];
        weights += intensity;
        sum += intensity * mz;
        count++;
        if (Math.abs(mz - featureMz) > tol) {
          hasForeign = true;
        }
      }
      if (hasForeign) {
        foreign++;
      }
      if (count < 3) {
        continue;
      }
      final double mean = sum / weights;
      double variance = 0;
      for (int i = 0; i < feature.size(); i++) {
        final double intensity = feature.intensities()[i];
        if (intensity >= minIntense) {
          final double ppm = (feature.mzs()[i] - mean) / mean * 1E6;
          variance += intensity * ppm * ppm;
        }
      }
      spreads.add(Math.sqrt(variance / weights));
    }
    return new Result(resolved.size(), ChromatogramBenchmarkUtils.quantile(spreads, 0.5),
        ChromatogramBenchmarkUtils.quantile(spreads, 0.95), foreign);
  }
}
