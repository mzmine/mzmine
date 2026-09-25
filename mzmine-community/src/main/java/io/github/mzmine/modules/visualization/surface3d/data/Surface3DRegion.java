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

package io.github.mzmine.modules.visualization.surface3d.data;

import com.google.common.collect.Range;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Coordinate window of the displayed axes. Null ranges select the full data extent.
 */
public record Surface3DRegion(@Nullable Range<Double> x, @Nullable Range<Double> y) {

  public static final Surface3DRegion FULL = new Surface3DRegion(null, null);

  public boolean isFull() {
    return x == null && y == null;
  }

  public static boolean contains(@Nullable final Range<Double> range, final double value) {
    return range == null || range.contains(value);
  }

  /**
   * @return the intersection, the other range if one is null, or null if they do not overlap
   */
  public static @Nullable Range<Double> intersect(@NotNull final Range<Double> range,
      @Nullable final Range<Double> window) {
    if (window == null) {
      return range;
    }
    if (!range.isConnected(window)) {
      return null;
    }
    final Range<Double> result = range.intersection(window);
    return result.isEmpty() ? null : result;
  }
}
