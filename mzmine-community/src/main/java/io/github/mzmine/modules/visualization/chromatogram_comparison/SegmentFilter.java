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

import io.github.mzmine.modules.dataprocessing.featdet_fastchromatogrambuilder.FastChromatogramBuilder;
import org.jetbrains.annotations.NotNull;

/**
 * The filter parameters of a chromatogram builder. The fast chromatogram builder needs the min
 * height within a segment of min consecutive scans above the min group intensity, ADAP only
 * anywhere in the chromatogram.
 */
public record SegmentFilter(int minConsecutiveScans, double minGroupIntensity, double minHeight) {

  /**
   * @param usedBefore the number of scans the builder of this filter used before each scan, see
   *                   {@link ComparisonScans#usedScansBefore(ComparisonSide)}. Scans the builder
   *                   did not use are skipped, consecutive scans of the builder are consecutive.
   * @return true if a segment of consecutive scans of the builder reaches the min height
   */
  boolean passes(@NotNull ComparedChromatogram chromatogram, int @NotNull [] usedBefore) {
    final int[] scans = chromatogram.scans();
    final int[] builderScans = new int[scans.length];
    final double[] intensities = new double[scans.length];
    int n = 0;
    for (int i = 0; i < scans.length; i++) {
      // the builder used the scan if the count increases after it
      if (usedBefore[scans[i] + 1] > usedBefore[scans[i]]) {
        builderScans[n] = usedBefore[scans[i]];
        intensities[n++] = chromatogram.intensities()[i];
      }
    }
    return FastChromatogramBuilder.passesFilters(builderScans, intensities, n, minConsecutiveScans,
        minGroupIntensity, minHeight);
  }
}
