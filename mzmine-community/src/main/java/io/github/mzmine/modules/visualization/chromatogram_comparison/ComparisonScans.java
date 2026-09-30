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

import java.util.Arrays;
import org.jetbrains.annotations.NotNull;

/**
 * The scans of a comparison: every scan that the builder of either list used, in the order of the
 * raw data file. Chromatograms refer to scans by their index in this list.
 *
 * @param rts     retention time of each scan
 * @param usedByA true if the builder of list A used the scan (its selected scans)
 * @param usedByB true if the builder of list B used the scan
 */
public record ComparisonScans(float @NotNull [] rts, boolean @NotNull [] usedByA,
                              boolean @NotNull [] usedByB) {

  public ComparisonScans {
    if (usedByA.length != rts.length || usedByB.length != rts.length) {
      throw new IllegalArgumentException("All arrays need one value per scan");
    }
  }

  /**
   * Both builders used the same scans
   */
  public static @NotNull ComparisonScans sameScans(float @NotNull [] rts) {
    final boolean[] used = new boolean[rts.length];
    Arrays.fill(used, true);
    return new ComparisonScans(rts, used, used.clone());
  }

  public int size() {
    return rts.length;
  }

  public float rt(int scan) {
    return rts[scan];
  }

  public boolean isUsedBy(@NotNull ComparisonSide side, int scan) {
    return side == ComparisonSide.A ? usedByA[scan] : usedByB[scan];
  }

  /**
   * @return the number of scans used by the side before each index, one more value than scans
   */
  int @NotNull [] usedScansBefore(@NotNull ComparisonSide side) {
    final int[] counts = new int[size() + 1];
    for (int i = 0; i < size(); i++) {
      counts[i + 1] = counts[i] + (isUsedBy(side, i) ? 1 : 0);
    }
    return counts;
  }
}
