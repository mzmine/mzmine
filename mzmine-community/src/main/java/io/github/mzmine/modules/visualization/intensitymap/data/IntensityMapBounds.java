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

import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Shared physical axes and intensity scale for every overlaid sample.
 *
 * @param invertedY y grows towards the front, images put their origin at the top left as in the
 *                  image viewer
 */
public record IntensityMapBounds(double xMin, double xMax, double yMin, double yMax, double maximum,
                                 boolean invertedY) {

  public IntensityMapBounds(final double xMin, final double xMax, final double yMin,
      final double yMax, final double maximum) {
    this(xMin, xMax, yMin, yMax, maximum, false);
  }

  public static @NotNull IntensityMapBounds of(@NotNull final List<IntensityMapGrid> data) {
    if (data.isEmpty()) {
      throw new IllegalArgumentException("No surface data");
    }
    return new IntensityMapBounds(
        data.stream().mapToDouble(d -> d.pixels() ? d.xLow(0) : d.xMin()).min().orElseThrow(),
        data.stream().mapToDouble(d -> d.pixels() ? d.xHigh(d.width() - 1) : d.xMax()).max()
            .orElseThrow(),
        data.stream().mapToDouble(d -> d.pixels() ? d.yLow(0) : d.yMin()).min().orElseThrow(),
        data.stream().mapToDouble(d -> d.pixels() ? d.yHigh(d.height() - 1) : d.yMax()).max()
            .orElseThrow(),
        data.stream().mapToDouble(IntensityMapGrid::maximum).max().orElseThrow(),
        data.stream().allMatch(IntensityMapGrid::pixels));
  }

  public boolean containsX(final double x) {
    return x >= xMin && x <= xMax;
  }

  public boolean containsY(final double y) {
    return y >= yMin && y <= yMax;
  }

  public boolean contains(final double x, final double y) {
    return containsX(x) && containsY(y);
  }

  public double normalizeX(final double value) {
    return xMin == xMax ? 0.5 : (value - xMin) / (xMax - xMin);
  }

  /**
   * @return share of the value between the y ends, from the back for inverted y
   */
  public double normalizeY(final double value) {
    if (yMin == yMax) {
      return 0.5;
    }
    final double share = (value - yMin) / (yMax - yMin);
    return invertedY ? 1 - share : share;
  }

  /**
   * @param share share of the depth from the back, the inverse of {@link #normalizeY(double)}
   */
  public double denormalizeY(final double share) {
    return yMin + (invertedY ? 1 - share : share) * (yMax - yMin);
  }
}
