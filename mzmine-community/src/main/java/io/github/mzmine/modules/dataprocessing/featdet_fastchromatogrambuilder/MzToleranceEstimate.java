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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Result of the {@link MzToleranceEstimation}.
 *
 * @param tolerance                 the estimated scan to scan tolerance, a safety factor times the
 *                                  larger of both requirements, see {@link MzToleranceEstimation}
 * @param scatterTolerance          required by the scatter of the same signal in consecutive scans
 * @param spreadTolerance           required by the deviations from the chromatogram centers in the
 *                                  test build, null if the test build found too few data points
 * @param numSampleFiles            number of sample files
 * @param numPairs                  mutual nearest neighbors in consecutive scans
 * @param numScanPairs              used pairs of consecutive scans
 * @param scatterPpm                scatter of the consecutive differences, in ppm
 * @param numChromatogramDataPoints data points of the chromatograms of the test build
 * @param spreadPpm                 scatter of the deviations from the chromatogram centers, in ppm
 * @param intensityFloor            only data points of at least this intensity were used, 0 for
 *                                  all
 */
public record MzToleranceEstimate(@NotNull MZTolerance tolerance,
                                  @NotNull MZTolerance scatterTolerance,
                                  @Nullable MZTolerance spreadTolerance, int numSampleFiles,
                                  int numPairs, int numScanPairs, double scatterPpm,
                                  int numChromatogramDataPoints, double spreadPpm,
                                  double intensityFloor) {

  @Override
  public @NotNull String toString() {
    return """
        %s (consecutive scans: %s from %d pairs in %d scan pairs, scatter %.2f ppm; \
        test build: %s from %d data points, scatter %.2f ppm; %d sample files%s)""".formatted(
        format(tolerance), format(scatterTolerance), numPairs, numScanPairs, scatterPpm,
        spreadTolerance == null ? "none" : format(spreadTolerance), numChromatogramDataPoints,
        spreadPpm, numSampleFiles,
        intensityFloor > 0 ? "; data points of at least %.3g".formatted(intensityFloor) : "");
  }

  @NotNull
  private static String format(@NotNull MZTolerance tolerance) {
    return "%.4f m/z or %.1f ppm".formatted(tolerance.getMzTolerance(),
        tolerance.getPpmTolerance());
  }
}
