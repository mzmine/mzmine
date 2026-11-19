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

package io.github.mzmine.modules.dataprocessing.featdet_massdetection.local_max;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import java.util.Arrays;
import org.jetbrains.annotations.NotNull;

/**
 * Three-way adaptive peak m/z of the {@link AdaptiveLocalMaxMassDetector}:
 * <ol>
 *   <li><b>Detector saturation</b> (apex at or above the saturation threshold): the mode of an
 *   exponentially modified Gaussian fitted to the saturated core and a few flank points, with the
 *   fitted height bounded by the raw apex (see {@link EmgFitter}). Saturated TOF peaks are
 *   distorted without a flat top, so the threshold and not the peak shape decides.</li>
 *   <li><b>Sharp and asymmetric</b> intense peaks: an apex-centered least-squares parabola vertex,
 *   which tracks the mode and avoids the tail bias of the weighted centroid.</li>
 *   <li><b>Otherwise</b>: the weighted centroid of the {@link LocalMaxMassDetector}.</li>
 * </ol>
 * Weak peaks below {@link #ADAPTIVE_MIN_PERCENT_BASE} of the base peak use the weighted centroid
 * (the fits are unstable at low S/N and the bias is small there). A failed fit falls back to the
 * next branch.
 *
 * @param saturationThreshold raw intensity from which on a peak is saturated,
 *                            {@link Double#POSITIVE_INFINITY} to disable the saturation branch
 */
public record AdaptivePeakMz(double saturationThreshold) implements LocalMaxPeakMz {

  // only intense peaks get the parabola treatment; weak peaks keep the weighted centroid
  private static final double ADAPTIVE_MIN_PERCENT_BASE = 1.0;
  // half-width ratio at ASYMMETRY_HEIGHT_FRACTION above which a peak is asymmetric (-> parabola)
  private static final double ASYMMETRY_THRESHOLD = 1.5;
  // decision: 10 % of the apex like the EP asymmetry factor. TOF tails show low on the peak,
  // half height missed tailing saturated frames (benchmark 2026-10-27).
  private static final double ASYMMETRY_HEIGHT_FRACTION = 0.1;
  // number of points (apex +/- (N-1)/2) used for the apex-centered parabola
  public static final int PARABOLA_POINTS = 5;
  // saturation fit window: the saturated core plus this many non-zero points on each flank
  private static final int SATURATION_FLANK_POINTS = 3;
  private static final int SATURATION_MIN_FIT_POINTS = 3;

  @Override
  public double peakMz(final double @NotNull [] mzs, final double @NotNull [] intensities,
      final double spectrumBaseIntensity, final int startIdx, final int endIdx, final int apexIdx,
      final double weightedCentroidMz) {
    final double maxIntensity = intensities[apexIdx];

    // 1. detector saturation -> mode of an EMG fitted to the saturated core and the flanks
    if (maxIntensity >= saturationThreshold) {
      final double saturated = saturatedEmgMode(mzs, intensities, startIdx, endIdx, apexIdx,
          maxIntensity);
      if (!Double.isNaN(saturated)) {
        return saturated;
      }
    }

    final boolean intenseEnough =
        maxIntensity >= spectrumBaseIntensity * (ADAPTIVE_MIN_PERCENT_BASE / 100.0);
    if (!intenseEnough) {
      return weightedCentroidMz;
    }

    // 2. sharp and asymmetric -> apex-centered parabola vertex
    // decision: asymmetry from the peak widths, not from the point counts between the edges. The
    // edges can reach far into the baseline and move with smoothing.
    final double asymmetry = widthAsymmetry(mzs, intensities, startIdx, endIdx, apexIdx);
    final boolean asymmetric = Double.isNaN(asymmetry) || asymmetry >= ASYMMETRY_THRESHOLD;
    if (asymmetric) {
      final double fit = apexParabolaVertexMz(mzs, intensities, apexIdx, startIdx, endIdx,
          PARABOLA_POINTS);
      if (!Double.isNaN(fit)) {
        return fit;
      }
    }

    // 3. otherwise the weighted centroid
    return weightedCentroidMz;
  }

  /**
   * Mode of an EMG fitted to the saturated core of the peak (all points at or above the saturation
   * threshold) and {@link #SATURATION_FLANK_POINTS} non-zero points on each side, limited to the
   * peak edges. All points have the same weight, the fitted height is bounded by the raw apex
   * because saturation only lowers the apex.
   *
   * @return the mode or NaN if both flanks have no unsaturated point, the fit fails or its mode
   * leaves the fit window
   */
  private double saturatedEmgMode(final double @NotNull [] mzs,
      final double @NotNull [] intensities, final int startIdx, final int endIdx,
      final int peakMaxIdx, final double maxIntensity) {
    int coreStart = -1;
    int coreEnd = -1;
    for (int i = startIdx; i < endIdx; i++) {
      if (intensities[i] >= saturationThreshold) {
        if (coreStart < 0) {
          coreStart = i;
        }
        coreEnd = i;
      }
    }
    if (coreStart < 0) {
      return Double.NaN;
    }
    int from = coreStart;
    for (int added = 0; from > startIdx && added < SATURATION_FLANK_POINTS; ) {
      from--;
      if (intensities[from] > 0) {
        added++;
      }
    }
    int to = coreEnd;
    for (int added = 0; to < endIdx - 1 && added < SATURATION_FLANK_POINTS; ) {
      to++;
      if (intensities[to] > 0) {
        added++;
      }
    }

    final DoubleArrayList fitMzs = new DoubleArrayList();
    final DoubleArrayList fitIntensities = new DoubleArrayList();
    double coreMzSum = 0;
    int core = 0;
    int unsaturatedLeft = 0;
    int unsaturatedRight = 0;
    for (int i = from; i <= to; i++) {
      if (intensities[i] <= 0) {
        continue;
      }
      fitMzs.add(mzs[i]);
      fitIntensities.add(intensities[i]);
      if (intensities[i] >= saturationThreshold) {
        coreMzSum += mzs[i];
        core++;
      } else if (i < peakMaxIdx) {
        unsaturatedLeft++;
      } else {
        unsaturatedRight++;
      }
    }
    // sigma and tau need unsaturated points on both flanks
    if (fitMzs.size() < SATURATION_MIN_FIT_POINTS || unsaturatedLeft == 0
        || unsaturatedRight == 0) {
      return Double.NaN;
    }

    final double low = fitMzs.getDouble(0);
    final double high = fitMzs.getDouble(fitMzs.size() - 1);
    final double[] weights = new double[fitMzs.size()];
    // decision: the saturated core gets the same weight as the unsaturated flanks. Saturated TOF
    // peaks are distorted rather than clipped, and with only 3 flank points per side the fit
    // depends on the core points. The raw apex as lower height bound covers the lowered apex.
    Arrays.fill(weights, 1);
    // start at the center of the saturated core, width estimate from the window
    final EmgFitter.Fit fit = EmgFitter.fit(fitMzs.toDoubleArray(), fitIntensities.toDoubleArray(),
        weights, coreMzSum / core, Math.max(1e-6, (high - low) / 6), maxIntensity * 1.2,
        maxIntensity);
    if (fit == null || Double.isNaN(fit.mode()) || fit.mode() < low || fit.mode() > high) {
      return Double.NaN;
    }
    return fit.mode();
  }

  private static double widthAsymmetry(final double @NotNull [] mzs,
      final double @NotNull [] intensities, final int startIdx, final int endIdx,
      final int peakMaxIdx) {
    return widthAsymmetry(mzs, intensities, startIdx, endIdx, peakMaxIdx,
        ASYMMETRY_HEIGHT_FRACTION);
  }

  /**
   * Ratio of the wider to the narrower half-width at a fraction of the apex height (>= 1). The apex
   * position is the vertex of the parabola through the apex and its neighbors, so the sampling
   * position of the apex point does not change the ratio. The crossings of the height level are
   * interpolated linearly. Public for the centroiding benchmark.
   *
   * @param heightFraction height level as fraction of the apex intensity
   * @return the asymmetry or NaN if one side does not fall below the level within the edges
   */
  public static double widthAsymmetry(final double @NotNull [] mzs,
      final double @NotNull [] intensities, final int startIdx, final int endIdx,
      final int peakMaxIdx, final double heightFraction) {
    final double level = intensities[peakMaxIdx] * heightFraction;
    double leftMz = Double.NaN;
    for (int i = peakMaxIdx; i > startIdx; i--) {
      if (intensities[i - 1] < level) {
        leftMz = interpolateMz(mzs[i - 1], intensities[i - 1], mzs[i], intensities[i], level);
        break;
      }
    }
    double rightMz = Double.NaN;
    for (int i = peakMaxIdx; i < endIdx - 1; i++) {
      if (intensities[i + 1] < level) {
        rightMz = interpolateMz(mzs[i], intensities[i], mzs[i + 1], intensities[i + 1], level);
        break;
      }
    }
    final double apexMz = threePointVertexMz(mzs, intensities, peakMaxIdx, startIdx, endIdx);
    final double left = apexMz - leftMz;
    final double right = rightMz - apexMz;
    if (!(left > 0 && right > 0)) {
      return Double.NaN;
    }
    return Math.max(left, right) / Math.min(left, right);
  }

  /**
   * @return m/z where the line between two points reaches the intensity level
   */
  private static double interpolateMz(final double mz1, final double int1, final double mz2,
      final double int2, final double level) {
    return int1 == int2 ? (mz1 + mz2) / 2 : mz1 + (level - int1) / (int2 - int1) * (mz2 - mz1);
  }

  /**
   * @return vertex of the parabola through the apex and its two neighbors, the apex m/z if the apex
   * is at an edge or the vertex leaves the three points
   */
  private static double threePointVertexMz(final double @NotNull [] mzs,
      final double @NotNull [] intensities, final int i, final int startIdx, final int endIdx) {
    if (i <= startIdx || i >= endIdx - 1) {
      return mzs[i];
    }
    final double x0 = mzs[i - 1], x1 = mzs[i], x2 = mzs[i + 1];
    final double y0 = intensities[i - 1], y1 = intensities[i], y2 = intensities[i + 1];
    final double denominator = (x0 - x1) * (x0 - x2) * (x1 - x2);
    if (denominator == 0) {
      return x1;
    }
    final double a = (x2 * (y1 - y0) + x1 * (y0 - y2) + x0 * (y2 - y1)) / denominator;
    final double b =
        (x2 * x2 * (y0 - y1) + x1 * x1 * (y2 - y0) + x0 * x0 * (y1 - y2)) / denominator;
    if (a >= 0) {
      return x1;
    }
    final double vertex = -b / (2 * a);
    return vertex >= x0 && vertex <= x2 ? vertex : x1;
  }

  /**
   * Vertex m/z of the least-squares parabola over the {@code n} points centered on the apex
   * ({@code apex +/- (n-1)/2}), clamped to the peak edges. Apex-centered (not the n highest points)
   * so a tailing flank does not skew the window. Public for the centroiding benchmark.
   *
   * @return the vertex or NaN if there are fewer than 3 points, the top is too flat (degenerate),
   * or the vertex falls outside the peak edges
   */
  public static double apexParabolaVertexMz(final double @NotNull [] mzs,
      final double @NotNull [] intensities, final int peakMaxIdx, final int startIdx,
      final int endIdx, final int n) {
    final int half = (n - 1) / 2;
    final int from = Math.max(startIdx, peakMaxIdx - half);
    final int to = Math.min(endIdx - 1, peakMaxIdx + half);
    final int count = to - from + 1;
    if (count < 3) {
      return Double.NaN;
    }

    // least squares y = a*u^2 + b*u + c with u = mz - apexMz (centering improves conditioning)
    final double xCenter = mzs[peakMaxIdx];
    final double s0 = count;
    double s1 = 0, s2 = 0, s3 = 0, s4 = 0, t0 = 0, t1 = 0, t2 = 0;
    for (int i = from; i <= to; i++) {
      final double u = mzs[i] - xCenter;
      final double y = intensities[i];
      final double u2 = u * u;
      s1 += u;
      s2 += u2;
      s3 += u2 * u;
      s4 += u2 * u2;
      t0 += y;
      t1 += u * y;
      t2 += u2 * y;
    }
    final double det = det3(s4, s3, s2, s3, s2, s1, s2, s1, s0);
    if (Math.abs(det) < 1e-30) {
      return Double.NaN;
    }
    final double a = det3(t2, s3, s2, t1, s2, s1, t0, s1, s0) / det;
    final double b = det3(s4, t2, s2, s3, t1, s1, s2, t0, s0) / det;
    if (Math.abs(a) < 1e-30) {
      return Double.NaN; // too flat -> degenerate vertex
    }
    final double vertex = xCenter - b / (2 * a);
    // reject runaway vertices outside the peak (e.g. nearly-flat noisy tops)
    if (vertex < mzs[startIdx] || vertex > mzs[endIdx - 1]) {
      return Double.NaN;
    }
    return vertex;
  }

  private static double det3(final double a, final double b, final double c, final double d,
      final double e, final double f, final double g, final double h, final double i) {
    return a * (e * i - f * h) - b * (d * i - f * g) + c * (d * h - e * g);
  }
}
