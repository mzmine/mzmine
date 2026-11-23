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

package io.github.mzmine.modules.visualization.chromatogram_comparison;

import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The chromatograms of one side of a {@link ChromatogramGroup} and the data points they miss.
 *
 * @param chromatograms      most intense first, empty if only the other side has a chromatogram
 * @param dataPoints         scans with a data point in any of the chromatograms
 * @param missingDataPoints  scans without data point in which the other side has a signal above the
 *                           threshold
 * @param maxMissedIntensity highest intensity of the other side in these scans
 * @param missingPeakTaken   another chromatogram of the same list holds all missing data points and
 *                           the most intense of them in at least the min consecutive scans of the
 *                           builder, the missing peak is in that chromatogram
 * @param holeDataPoints     scans in short gaps between two signals above the threshold, in which
 *                           the mass list has a data point within the tolerance
 * @param missingRegions     consecutive scans without data point of both kinds, ascending
 * @param takenBy            the chromatograms of the same list with the missing and hole data
 *                           points, for an empty side with the signals of the other side, most data
 *                           points first, unused data points last
 */
public record GroupSide(@NotNull ComparisonSide side,
                        @NotNull List<ComparedChromatogram> chromatograms, int dataPoints,
                        int missingDataPoints, double maxMissedIntensity, boolean missingPeakTaken,
                        int holeDataPoints, @NotNull List<MissingRegion> missingRegions,
                        @NotNull List<DataPointOwner> takenBy) {

  public boolean isEmpty() {
    return chromatograms.isEmpty();
  }

  @NotNull GroupSide withTakenBy(@NotNull List<DataPointOwner> takenBy) {
    return new GroupSide(side, chromatograms, dataPoints, missingDataPoints, maxMissedIntensity,
        missingPeakTaken, holeDataPoints, missingRegions, takenBy);
  }

  public @Nullable ComparedChromatogram mostIntense() {
    return chromatograms.isEmpty() ? null : chromatograms.getFirst();
  }

  /**
   * @return m/z of the most intense chromatogram, null if empty
   */
  public @Nullable Double mz() {
    final ComparedChromatogram main = mostIntense();
    return main == null ? null : main.mz();
  }

  /**
   * @return highest intensity of all chromatograms, null if empty
   */
  public @Nullable Double height() {
    final ComparedChromatogram main = mostIntense();
    return main == null ? null : main.height();
  }
}
