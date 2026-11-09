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

package io.github.mzmine.modules.visualization.intensitymap.data;

import com.google.common.collect.Range;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Coordinate window of the displayed axes, e.g. the visible part of a zoomed view. Null ranges
 * select the full data extent.
 */
public record IntensityMapRegion(@Nullable Range<Double> x, @Nullable Range<Double> y) {

  public static final IntensityMapRegion FULL = new IntensityMapRegion(null, null);

  public boolean isFull() {
    return x == null && y == null;
  }

  /**
   * @param left  share of the width added below the x range
   * @param right share of the width added above the x range
   * @param below share of the height added below the y range
   * @param above share of the height added above the y range
   * @return a larger window; null ranges stay null
   */
  public @NotNull IntensityMapRegion expand(final double left, final double right,
      final double below, final double above) {
    return new IntensityMapRegion(expand(x, left, right), expand(y, below, above));
  }

  private static @Nullable Range<Double> expand(@Nullable final Range<Double> range,
      final double lower, final double upper) {
    if (range == null) {
      return null;
    }
    final double size = range.upperEndpoint() - range.lowerEndpoint();
    return Range.closed(range.lowerEndpoint() - size * lower, range.upperEndpoint() + size * upper);
  }

  /**
   * @return true if both windows share some data, a null range overlaps everything
   */
  public boolean overlaps(@NotNull final IntensityMapRegion other) {
    return overlaps(x, other.x) && overlaps(y, other.y);
  }

  private static boolean overlaps(@Nullable final Range<Double> range,
      @Nullable final Range<Double> other) {
    return range == null || other == null || range.isConnected(other);
  }

  /**
   * @return true if this window contains the other one, a null range contains everything
   */
  public boolean encloses(@NotNull final IntensityMapRegion other) {
    return encloses(x, other.x) && encloses(y, other.y);
  }

  private static boolean encloses(@Nullable final Range<Double> range,
      @Nullable final Range<Double> other) {
    return range == null || (other != null && range.encloses(other));
  }

  /**
   * @return extent along x, infinite for a full axis
   */
  public double width() {
    return x == null ? Double.POSITIVE_INFINITY : x.upperEndpoint() - x.lowerEndpoint();
  }

  public double height() {
    return y == null ? Double.POSITIVE_INFINITY : y.upperEndpoint() - y.lowerEndpoint();
  }

  public static boolean contains(@Nullable final Range<Double> range, final double value) {
    return range == null || range.contains(value);
  }

  /**
   * @return the intersection, the range itself if the window is null, or null if they do not
   * overlap
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
