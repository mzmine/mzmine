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

import org.jetbrains.annotations.NotNull;

/**
 * Consecutive scans without data point in the chromatograms of one side of a group.
 *
 * @param side               the side that misses the data points
 * @param firstScan          first scan index without data point
 * @param lastScan           last scan index without data point, inclusive
 * @param rtFirst            retention time of the first scan
 * @param rtLast             retention time of the last scan
 * @param rtStart            half way between the first scan and the scan before
 * @param rtEnd              half way between the last scan and the scan after
 * @param otherMaxIntensity  the highest intensity of the other side in these scans, 0 if it has no
 *                           data point either
 * @param otherSideHasSignal true if the other side has a data point above the signal threshold in
 *                           at least one of these scans, false for a hole between two signals
 */
public record MissingRegion(@NotNull ComparisonSide side, int firstScan, int lastScan,
                            float rtFirst, float rtLast, float rtStart, float rtEnd,
                            double otherMaxIntensity, boolean otherSideHasSignal) {

  public int numScans() {
    return lastScan - firstScan + 1;
  }

  public boolean isSingleScan() {
    return firstScan == lastScan;
  }
}
