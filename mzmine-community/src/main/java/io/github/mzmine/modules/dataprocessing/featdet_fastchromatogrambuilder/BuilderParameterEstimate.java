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
import io.github.mzmine.modules.dataprocessing.featdet_fastchromatogrambuilder.SignalPersistenceProfile.IntensityBin;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Result of the {@link BuilderParameterEstimation}.
 *
 * @param settings          the determined builder parameters
 * @param sensitivity       the sensitivity they were determined for
 * @param noiseLevel        lowest intensity from which half of the data points continue in the next
 *                          scan, see {@link SignalPersistenceProfile}
 * @param signalLevel       lowest intensity from which nearly all data points continue
 * @param levelsFound       false if the profile did not reach a level and a fallback was used
 * @param minIntensity      lowest intensity of the sampled data points
 * @param nearWindow        the window of the same signal in consecutive scans of the profile
 * @param profile           signal fractions by intensity
 * @param peakWidthScans    median full width at half maximum of clear peaks in scans, NaN if
 *                          there were none
 * @param numWidthPeaks     number of the measured peaks
 * @param toleranceEstimate the m/z tolerance estimate, null if the data did not allow one and the
 *                          fallback was used
 * @param timings           time of the steps
 */
public record BuilderParameterEstimate(@NotNull ChromatogramBuilderSettings settings,
                                       @NotNull ChromatogramBuilderSensitivity sensitivity,
                                       double noiseLevel, double signalLevel,
                                       boolean levelsFound, double minIntensity,
                                       @NotNull MZTolerance nearWindow,
                                       @NotNull List<IntensityBin> profile, double peakWidthScans,
                                       int numWidthPeaks,
                                       @Nullable MzToleranceEstimate toleranceEstimate,
                                       @NotNull Timings timings) {

  @Override
  public @NotNull String toString() {
    return """
        %s (noise level %.3G, signal level %.3G%s, lowest intensity %.3G; peak width %s; \
        m/z tolerance %s; %s)""".formatted(settings, noiseLevel, signalLevel,
        levelsFound ? "" : " (fallback, the data did not reach a level)", minIntensity,
        Double.isFinite(peakWidthScans) ? "%.0f scans from %d peaks".formatted(peakWidthScans,
            numWidthPeaks) : "unknown, too few clear peaks",
        toleranceEstimate == null ? "fallback, too few signals" : toleranceEstimate, timings);
  }

  /**
   * Time of the estimation steps in ms. The sampled scan pairs: intensity histogram, scatter of
   * consecutive signals for the near window (one pass per tried floor), the persistence profile and
   * its refinement. The tolerance includes its own scatter and the test build on all scans of the
   * sample files, which also measures the peak widths.
   */
  public record Timings(long histogramMs, long scatterMs, long profileMs, long refinementMs,
                        long toleranceMs) {

    public long totalMs() {
      return histogramMs + scatterMs + profileMs + refinementMs + toleranceMs;
    }

    @Override
    public @NotNull String toString() {
      return "%d ms: histogram %d, scatter %d, profile %d, refinement %d, tolerance and test build %d".formatted(
          totalMs(), histogramMs, scatterMs, profileMs, refinementMs, toleranceMs);
    }
  }
}
