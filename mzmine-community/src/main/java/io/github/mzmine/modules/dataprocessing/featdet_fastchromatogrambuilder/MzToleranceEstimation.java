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
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Estimates the scan to scan m/z tolerance of the {@link FastChromatogramBuilder} from a few sample
 * files in two steps, both fitted with a {@link MzScatterModel}:
 * <ol>
 *   <li>The m/z scatter of the same signal in consecutive scans, see
 *   {@link ConsecutiveSignalPairs}. This is the precision of the instrument.</li>
 *   <li>A test build of the chromatograms with twice this tolerance. The m/z deviations of the
 *   data points from the center of their chromatogram also contain the drift over the run and
 *   ions of slightly different m/z that share one chromatogram. The builder compares traces and
 *   loose data points with this center.</li>
 * </ol>
 * The estimate is {@link #SAFETY_FACTOR} times the larger requirement of both steps. If noise
 * dominates the data, e.g., without noise filter in the mass detection, both steps use only data
 * points of at least an intensity floor, see {@link #intensityFloors(double)}.
 */
final class MzToleranceEstimation {

  // decision: the requirement contains 99.5% of the gaussian scatter of the data points
  static final double COVERAGE = 0.995;
  // decision: the gaussian scatter misses rare larger shifts of intense signals, e.g., space charge
  // in the injection front of Orbitrap data. In the tolerance sweep, tolerances above the
  // requirement lose few features, tolerances below lose more. 1.5 times the requirement was the
  // best compromise over all data sets.
  static final double SAFETY_FACTOR = 1.5d;
  // the test build uses a wider tolerance than the final estimate
  static final double TEST_BUILD_FACTOR = 2d;
  // decision: enough pairs for a stable fit while the estimation stays well below a second
  static final int MAX_SCAN_PAIRS_PER_FILE = 1000;
  static final double MIN_SPACING_TO_SCATTER = 10d;

  private MzToleranceEstimation() {
  }

  /**
   * @param samples             scans of the sample files, each in retention time order
   * @param minConsecutiveScans builder parameter for the test build
   * @param minGroupIntensity   builder parameter for the test build
   * @param minHeight           builder parameter for the test build
   * @param isCanceled          checked between the steps, may be null
   * @return the estimate or null if the data do not allow an estimate or if canceled
   */
  @Nullable
  static MzToleranceEstimate estimate(@NotNull List<? extends MzIntensityScans> samples,
      int minConsecutiveScans, double minGroupIntensity, double minHeight,
      @Nullable BooleanSupplier isCanceled) {
    for (final double floor : intensityFloors(minGroupIntensity)) {
      final List<? extends MzIntensityScans> signals =
          floor > 0d ? samples.stream().map(scans -> new IntensityFloorScans(scans, floor)).toList()
              : samples;
      final ConsecutiveSignalPairs pairs = new ConsecutiveSignalPairs();
      for (final MzIntensityScans scans : signals) {
        final int stride = Math.max(1, scans.getNumberOfScans() / MAX_SCAN_PAIRS_PER_FILE);
        pairs.addScans(scans, stride);
        if (isCanceled != null && isCanceled.getAsBoolean()) {
          return null;
        }
      }
      final MzScatterModel scatter = pairs.fitScatter();
      // decision: the same signal scatters much less between consecutive scans than neighboring
      // signals of one scan are apart, otherwise the pairs are unrelated signals, e.g., only noise
      if (scatter != null
          && scatter.sigmaPpm() * MIN_SPACING_TO_SCATTER <= pairs.medianSpacingPpm()) {
        return estimate(signals, pairs, scatter, floor, minConsecutiveScans, minGroupIntensity,
            minHeight, isCanceled);
      }
    }
    return null;
  }

  /**
   * Noise data points dominate data without noise filter in the mass detection: the pairs and the
   * spacing of neighbors, and the nearest neighbors of noise look like a broad gaussian that the
   * fit accepts, 250 ppm on unfiltered GC-QTOF data.
   *
   * @return the intensity floors that are tried in this order until the pairs show a clear signal:
   * all data points first, so data with noise filter keep their estimate, then half the min group
   * intensity and the min group intensity
   */
  @NotNull
  static double[] intensityFloors(double minGroupIntensity) {
    // decision: the builder counts data points below the min group intensity as noise. Half of it
    // is the noise level of the batch wizard for absolute noise levels, which uses 2 x the noise
    // level as min group intensity.
    if (!(minGroupIntensity > 0d)) {
      return new double[]{0d};
    }
    return new double[]{0d, minGroupIntensity / 2d, minGroupIntensity};
  }

  /**
   * Second step, the test build, after the scatter of consecutive signals was found.
   *
   * @param signals the sample scans with the intensity floor
   */
  @Nullable
  private static MzToleranceEstimate estimate(@NotNull List<? extends MzIntensityScans> signals,
      @NotNull ConsecutiveSignalPairs pairs, @NotNull MzScatterModel scatter, double floor,
      int minConsecutiveScans, double minGroupIntensity, double minHeight,
      @Nullable BooleanSupplier isCanceled) {
    final MZTolerance scatterTolerance = scatter.tolerance(COVERAGE);
    if (scatterTolerance == null) {
      return null;
    }

    final MZTolerance testTolerance = new MZTolerance(
        TEST_BUILD_FACTOR * scatterTolerance.getMzTolerance(),
        TEST_BUILD_FACTOR * scatterTolerance.getPpmTolerance());
    final FloatArrayList deviations = new FloatArrayList();
    final FloatArrayList mzs = new FloatArrayList();
    final FloatArrayList intensities = new FloatArrayList();
    // decision: without the coalesced fills, a shared centroid of two ions is no scatter of one ion
    // and changed the estimate of GC-EI-QTOF data by 2 ppm
    final FastChromatogramBuilderOptions options = FastChromatogramBuilderOptions.DEFAULT.withCoalescedMaxHoleScans(
        0);
    for (final MzIntensityScans scans : signals) {
      final FastChromatogramBuilder builder = new FastChromatogramBuilder(testTolerance,
          minConsecutiveScans, minGroupIntensity, minHeight, options);
      final List<BuiltChromatogram> chromatograms = builder.build(scans, isCanceled, null);
      if (chromatograms == null) {
        return null;
      }
      for (final BuiltChromatogram chromatogram : chromatograms) {
        addDeviations(chromatogram, deviations, mzs, intensities);
      }
    }
    // decision: the test tolerance limits the deviations, loose noise data points are uniform
    // within it, so it is the window of the fit
    final MzScatterModel spread = MzScatterModel.fit(deviations.elements(), mzs.elements(),
        intensities.elements(), deviations.size(), 1d,
        mz -> testTolerance.getMzToleranceForMass(mz) / mz * 1E6);
    final MZTolerance spreadTolerance = spread == null ? null : spread.tolerance(COVERAGE);

    double absolute = scatterTolerance.getMzTolerance();
    double ppm = scatterTolerance.getPpmTolerance();
    if (spreadTolerance != null) {
      absolute = Math.max(absolute, spreadTolerance.getMzTolerance());
      ppm = Math.max(ppm, spreadTolerance.getPpmTolerance());
    }
    final MZTolerance tolerance = roundUp(
        new MZTolerance(SAFETY_FACTOR * absolute, SAFETY_FACTOR * ppm));
    return new MzToleranceEstimate(tolerance, scatterTolerance, spreadTolerance, signals.size(),
        pairs.getNumPairs(), pairs.getNumScanPairs(), scatter.sigmaPpm(), deviations.size(),
        spread == null ? Double.NaN : spread.sigmaPpm(), floor);
  }

  /**
   * Deviations of all data points from the intensity weighted center of their chromatogram.
   */
  private static void addDeviations(@NotNull BuiltChromatogram chromatogram,
      @NotNull FloatArrayList deviations, @NotNull FloatArrayList mzs,
      @NotNull FloatArrayList intensities) {
    final int n = chromatogram.getNumberOfDataPoints();
    if (n < 2) {
      return;
    }
    double sumMzIntensity = 0d;
    double sumIntensity = 0d;
    for (int i = 0; i < n; i++) {
      sumMzIntensity += chromatogram.getMz(i) * chromatogram.getIntensity(i);
      sumIntensity += chromatogram.getIntensity(i);
    }
    final double center = sumMzIntensity / sumIntensity;
    for (int i = 0; i < n; i++) {
      final double mz = chromatogram.getMz(i);
      deviations.add((float) ((mz - center) / center * 1E6));
      mzs.add((float) mz);
      intensities.add((float) chromatogram.getIntensity(i));
    }
  }

  /**
   * Rounds up to 0.1 mDa and 0.1 ppm, which keeps the coverage and gives readable values.
   */
  @NotNull
  static MZTolerance roundUp(@NotNull MZTolerance tolerance) {
    final double absolute = Math.ceil(tolerance.getMzTolerance() * 1E4 - 1E-9) / 1E4;
    final double ppm = Math.ceil(tolerance.getPpmTolerance() * 10d - 1E-9) / 10d;
    return new MZTolerance(Math.max(0d, absolute), Math.max(0d, ppm));
  }
}
