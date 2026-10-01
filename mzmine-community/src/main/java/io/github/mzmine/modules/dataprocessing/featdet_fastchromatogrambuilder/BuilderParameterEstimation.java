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

import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ChromatogramBuilderSettings;
import io.github.mzmine.modules.dataprocessing.featdet_fastchromatogrambuilder.BuilderParameterEstimate.Timings;
import io.github.mzmine.modules.dataprocessing.featdet_fastchromatogrambuilder.MzToleranceEstimation.ClearScatter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.List;
import java.util.function.BooleanSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Determines all parameters of the {@link FastChromatogramBuilder} from a few sample files for a
 * {@link ChromatogramBuilderSensitivity}:
 * <ol>
 *   <li>The noise level, from which half of the data points continue in the next scan, and the
 *   signal level, from which nearly all do, see {@link SignalPersistenceProfile}. Its near window
 *   is the scatter of the same signal in consecutive scans, the first step of the
 *   {@link MzToleranceEstimation}.</li>
 *   <li>The min intensity for consecutive scans and the min height from these levels, they grow
 *   from sensitive to abundant.</li>
 *   <li>The m/z tolerance, estimated with these intensities, see
 *   {@link MzToleranceEstimation}.</li>
 *   <li>The min consecutive scans, a fraction of the peak width in scans of the clear peaks of the
 *   test build of the tolerance estimation.</li>
 * </ol>
 */
final class BuilderParameterEstimation {

  // decision: at the noise level half of the data points are signals that continue in the next scan
  static final double NOISE_SIGNAL_FRACTION = 0.5;
  // decision: at the signal level nearly all data points continue. On TOF data with a noise filter
  // this is about twice the noise level of the mass detection, the min intensity of the wizard.
  static final double SIGNAL_FRACTION = 0.95;
  // the near window covers the pair difference of two data points, sqrt(2) times their scatter
  static final double NEAR_WINDOW_FACTOR = Math.sqrt(2d);
  private static final double SAFETY_FACTOR = MzToleranceEstimation.SAFETY_FACTOR;
  // intensity quantiles for the scatter of consecutive signals if all data points show no clear
  // signal, e.g., without noise filter in the mass detection
  static final double[] SCATTER_FLOOR_QUANTILES = {0.5, 0.75, 0.9, 0.97, 0.99};
  // without a noise level in the profile, e.g., very sparse data
  static final double FALLBACK_NOISE_QUANTILE = 0.5;
  // without a signal level, the typical ratio of filtered data
  static final double FALLBACK_SIGNAL_TO_NOISE = 2d;
  // min consecutive scans of the test build of the tolerance estimation
  static final int TEST_BUILD_MIN_CONSECUTIVE = 4;
  // decision: clear peaks for the width, independent of the sensitivity. Lower apexes included
  // noise spikes, 3 instead of 5 scans on unfiltered ZenoTOF data.
  static final double WIDTH_MIN_APEX_TO_SIGNAL = 10d;
  static final int MIN_WIDTH_PEAKS = 20;
  static final int MIN_CONSECUTIVE = 3;
  static final int MAX_CONSECUTIVE = 20;

  private BuilderParameterEstimation() {
  }

  /**
   * @param samples           scans of the sample files, each in retention time order
   * @param fallbackTolerance used if the data do not allow a tolerance estimate
   * @param isCanceled        checked between the steps, may be null
   * @return the estimate, null if canceled
   */
  @Nullable
  static BuilderParameterEstimate estimate(@NotNull List<? extends MzIntensityScans> samples,
      @NotNull ChromatogramBuilderSensitivity sensitivity, @NotNull MZTolerance fallbackTolerance,
      @Nullable BooleanSupplier isCanceled) {
    long start = System.nanoTime();
    final IntensityHistogram histogram = new IntensityHistogram();
    for (final MzIntensityScans scans : samples) {
      histogram.addScans(scans, MzToleranceEstimation.scanPairStride(scans));
      if (canceled(isCanceled)) {
        return null;
      }
    }

    final long histogramMs = elapsedMs(start);
    start = System.nanoTime();
    // the near window of the profile from the scatter of consecutive signals, all data points
    // first, then the data points above intensity quantiles
    final double[] floors = new double[SCATTER_FLOOR_QUANTILES.length + 1];
    for (int i = 0; i < SCATTER_FLOOR_QUANTILES.length; i++) {
      floors[i + 1] = histogram.quantile(SCATTER_FLOOR_QUANTILES[i]);
    }
    MZTolerance nearWindow = nearWindow(samples, floors, isCanceled);
    if (canceled(isCanceled)) {
      return null;
    }
    if (nearWindow == null) {
      nearWindow = new MZTolerance(
          NEAR_WINDOW_FACTOR * fallbackTolerance.getMzTolerance() / SAFETY_FACTOR,
          NEAR_WINDOW_FACTOR * fallbackTolerance.getPpmTolerance() / SAFETY_FACTOR);
    }
    final long scatterMs = elapsedMs(start);
    start = System.nanoTime();
    SignalPersistenceProfile profile = profile(samples, nearWindow, isCanceled);
    if (profile == null) {
      return null;
    }
    // decision: once more with the data points of at least the noise level, which are mostly
    // signals. On unfiltered LC-QTOF MSe data the quantile floors still had mostly noise pairs and
    // a near window of 306 ppm.
    final long profileMs = elapsedMs(start);
    start = System.nanoTime();
    final double firstNoise = profile.intensityAtSignalFraction(NOISE_SIGNAL_FRACTION);
    if (Double.isFinite(firstNoise) && firstNoise > histogram.getMinIntensity()) {
      final MZTolerance refined = nearWindow(samples, new double[]{firstNoise}, isCanceled);
      if (canceled(isCanceled)) {
        return null;
      }
      if (refined != null && !refined.equals(nearWindow)) {
        nearWindow = refined;
        profile = profile(samples, nearWindow, isCanceled);
        if (profile == null) {
          return null;
        }
      }
    }

    final long refinementMs = elapsedMs(start);
    final double profileNoise = profile.intensityAtSignalFraction(NOISE_SIGNAL_FRACTION);
    final double profileSignal = profile.intensityAtSignalFraction(SIGNAL_FRACTION);
    final boolean levelsFound = Double.isFinite(profileNoise) && Double.isFinite(profileSignal);
    final double noiseLevel = Double.isFinite(profileNoise) ? profileNoise
        : histogram.quantile(FALLBACK_NOISE_QUANTILE);
    final double signalLevel = Double.isFinite(profileSignal) ? Math.max(noiseLevel, profileSignal)
        : FALLBACK_SIGNAL_TO_NOISE * noiseLevel;

    final double minGroup = roundSignificant(minGroupIntensity(sensitivity, noiseLevel,
        signalLevel));
    final double minHeight = Math.max(minGroup,
        roundSignificant(minHeight(sensitivity, signalLevel)));

    final PeakWidthCollector widths = new PeakWidthCollector(
        WIDTH_MIN_APEX_TO_SIGNAL * signalLevel);
    start = System.nanoTime();
    final MzToleranceEstimate toleranceEstimate = MzToleranceEstimation.estimate(samples,
        TEST_BUILD_MIN_CONSECUTIVE, minGroup, minHeight, isCanceled, widths);
    if (canceled(isCanceled)) {
      return null;
    }
    final long toleranceMs = elapsedMs(start);
    final MZTolerance tolerance =
        toleranceEstimate == null ? fallbackTolerance : toleranceEstimate.tolerance();

    final double peakWidth =
        widths.getNumPeaks() >= MIN_WIDTH_PEAKS ? widths.quantile(0.5) : Double.NaN;
    final int minConsecutive = minConsecutiveScans(sensitivity, peakWidth);

    final ChromatogramBuilderSettings settings = new ChromatogramBuilderSettings(minConsecutive,
        minGroup, minHeight, tolerance);
    return new BuilderParameterEstimate(settings, sensitivity, noiseLevel, signalLevel,
        levelsFound, histogram.getMinIntensity(), nearWindow, profile.bins(), peakWidth,
        widths.getNumPeaks(), toleranceEstimate,
        new Timings(histogramMs, scatterMs, profileMs, refinementMs, toleranceMs));
  }

  /**
   * @param floors the intensity floors in the order they are tried
   * @return the near window of the profile from the first floor with a clear scatter of
   * consecutive signals, null if none or if canceled
   */
  @Nullable
  private static MZTolerance nearWindow(@NotNull List<? extends MzIntensityScans> samples,
      @NotNull double[] floors, @Nullable BooleanSupplier isCanceled) {
    final ClearScatter scatter = MzToleranceEstimation.findScatter(samples, floors, isCanceled);
    final MZTolerance pointTolerance =
        scatter == null ? null : scatter.scatter().tolerance(MzToleranceEstimation.COVERAGE);
    if (pointTolerance == null) {
      return null;
    }
    return new MZTolerance(NEAR_WINDOW_FACTOR * pointTolerance.getMzTolerance(),
        NEAR_WINDOW_FACTOR * pointTolerance.getPpmTolerance());
  }

  /**
   * @return the profile of all data points of the samples, null if canceled
   */
  @Nullable
  private static SignalPersistenceProfile profile(@NotNull List<? extends MzIntensityScans> samples,
      @NotNull MZTolerance nearWindow, @Nullable BooleanSupplier isCanceled) {
    final SignalPersistenceProfile profile = new SignalPersistenceProfile(nearWindow);
    for (final MzIntensityScans scans : samples) {
      profile.addScans(scans, MzToleranceEstimation.scanPairStride(scans));
      if (canceled(isCanceled)) {
        return null;
      }
    }
    return profile;
  }

  /**
   * @param noiseLevel  half of the data points are signals
   * @param signalLevel nearly all data points are signals
   * @return the min intensity for consecutive scans
   */
  static double minGroupIntensity(@NotNull ChromatogramBuilderSensitivity sensitivity,
      double noiseLevel, double signalLevel) {
    // decision: medium gives the results of the batch wizard defaults, abundant those of the
    // workshop settings, see the implementation note
    return switch (sensitivity) {
      case SENSITIVE -> 0.5d * noiseLevel;
      case MEDIUM -> noiseLevel;
      case ABUNDANT -> 2d * signalLevel;
    };
  }

  /**
   * @param signalLevel nearly all data points are signals
   * @return the min height
   */
  static double minHeight(@NotNull ChromatogramBuilderSensitivity sensitivity,
      double signalLevel) {
    return switch (sensitivity) {
      case SENSITIVE -> 0.5d * signalLevel;
      case MEDIUM -> signalLevel;
      case ABUNDANT -> 10d * signalLevel;
    };
  }

  /**
   * @param peakWidthScans median full width at half maximum in scans, NaN if unknown
   */
  static int minConsecutiveScans(@NotNull ChromatogramBuilderSensitivity sensitivity,
      double peakWidthScans) {
    if (!Double.isFinite(peakWidthScans)) {
      // the values of the batch wizard
      return switch (sensitivity) {
        case SENSITIVE -> 3;
        case MEDIUM -> 4;
        case ABUNDANT -> 6;
      };
    }
    final double fraction = switch (sensitivity) {
      case SENSITIVE -> 0.3d;
      case MEDIUM -> 0.5d;
      case ABUNDANT -> 0.8d;
    };
    return Math.clamp(Math.round(fraction * peakWidthScans), MIN_CONSECUTIVE, MAX_CONSECUTIVE);
  }

  /**
   * @return the value rounded to two significant digits, readable values in the log and the
   * parameters
   */
  static double roundSignificant(double value) {
    if (!(value > 0d) || !Double.isFinite(value)) {
      return 0d;
    }
    final double scale = Math.pow(10d, Math.floor(Math.log10(value)) - 1d);
    return Math.round(value / scale) * scale;
  }

  private static long elapsedMs(long startNanos) {
    return (System.nanoTime() - startNanos) / 1_000_000;
  }

  private static boolean canceled(@Nullable BooleanSupplier isCanceled) {
    return isCanceled != null && isCanceled.getAsBoolean();
  }
}
