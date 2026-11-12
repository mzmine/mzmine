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

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Compares resolved feature lists: duplicates within a list and features found in the other list,
 * both by m/z tolerance and retention time tolerance, and the m/z difference of found features to
 * the closest feature of the other list.
 */
final class ResolvedFeatureMetrics {

  private ResolvedFeatureMetrics() {
  }

  /**
   * @param features                 number of features
   * @param duplicatePairs           pairs of features within the m/z and retention time tolerance
   * @param matched                  features with a feature in the other list within both
   *                                 tolerances
   * @param medianMzDifferencePpm    median absolute m/z difference of matched features to the
   *                                 closest feature of the other list
   * @param p95MzDifferencePpm       95% quantile of this difference
   * @param beyondHalfTolerance      matched features more than half the tolerance from the other
   *                                 feature, e.g., a feature m/z pulled by a shared data point
   */
  record Result(int features, int duplicatePairs, int matched, double medianMzDifferencePpm,
                double p95MzDifferencePpm, int beyondHalfTolerance) {

    double matchedFraction() {
      return features == 0 ? 0 : (double) matched / features;
    }
  }

  @NotNull
  static Result evaluate(@NotNull FeatureList flist, @NotNull FeatureList other,
      @NotNull MZTolerance tolerance, float rtTolerance) {
    final Features features = Features.of(flist);
    final Features otherFeatures = Features.of(other);
    int duplicates = 0;
    for (int i = 0; i < features.size(); i++) {
      final double mz = features.mzs[i];
      final double max = mz + tolerance.getMzToleranceForMass(mz);
      for (int j = i + 1; j < features.size() && features.mzs[j] <= max; j++) {
        if (Math.abs(features.rts[i] - features.rts[j]) <= rtTolerance) {
          duplicates++;
        }
      }
    }
    int matched = 0;
    int beyondHalf = 0;
    final DoubleArrayList differences = new DoubleArrayList();
    for (int i = 0; i < features.size(); i++) {
      final double mz = features.mzs[i];
      final double tol = tolerance.getMzToleranceForMass(mz);
      double closest = Double.POSITIVE_INFINITY;
      for (int j = ChannelConsolidation.lowerBound(otherFeatures.mzs, mz - tol);
          j < otherFeatures.size() && otherFeatures.mzs[j] <= mz + tol; j++) {
        if (Math.abs(features.rts[i] - otherFeatures.rts[j]) <= rtTolerance) {
          closest = Math.min(closest, Math.abs(otherFeatures.mzs[j] - mz));
        }
      }
      if (closest == Double.POSITIVE_INFINITY) {
        continue;
      }
      matched++;
      differences.add(closest / mz * 1E6);
      if (closest > 0.5 * tol) {
        beyondHalf++;
      }
    }
    return new Result(features.size(), duplicates, matched,
        ChromatogramBenchmarkUtils.quantile(differences, 0.5),
        ChromatogramBenchmarkUtils.quantile(differences, 0.95), beyondHalf);
  }

  /**
   * Features sorted by m/z.
   */
  private record Features(@NotNull double[] mzs, @NotNull float[] rts) {

    @NotNull
    static Features of(@NotNull FeatureList flist) {
      final List<FeatureListRow> rows = flist.getRows();
      final int n = rows.size();
      final double[] unsortedMzs = new double[n];
      final float[] unsortedRts = new float[n];
      for (int i = 0; i < n; i++) {
        unsortedMzs[i] = rows.get(i).getAverageMZ();
        unsortedRts[i] = rows.get(i).getAverageRT();
      }
      final int[] order = ChannelConsolidation.identity(n);
      IntArrays.quickSort(order, 0, n, (a, b) -> Double.compare(unsortedMzs[a], unsortedMzs[b]));
      final double[] mzs = new double[n];
      final float[] rts = new float[n];
      for (int i = 0; i < n; i++) {
        mzs[i] = unsortedMzs[order[i]];
        rts[i] = unsortedRts[order[i]];
      }
      return new Features(mzs, rts);
    }

    int size() {
      return mzs.length;
    }
  }
}
