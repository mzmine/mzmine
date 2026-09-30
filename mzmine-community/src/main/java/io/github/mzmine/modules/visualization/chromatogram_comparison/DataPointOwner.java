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

import org.jetbrains.annotations.Nullable;

/**
 * A chromatogram of the same list that took data points that one side of a group misses. The data
 * points of a side are missing if they are signals of the other side, data points of the mass lists
 * in holes, or all signals of the other side if the side has no chromatogram.
 *
 * @param chromatogram the chromatogram with the data points, null if no chromatogram of the list
 *                     used them
 * @param groupId      the group of the chromatogram, 0 without chromatogram
 * @param count        number of data points
 */
public record DataPointOwner(@Nullable ComparedChromatogram chromatogram, int groupId, int count) {

  public boolean isUnused() {
    return chromatogram == null;
  }
}
