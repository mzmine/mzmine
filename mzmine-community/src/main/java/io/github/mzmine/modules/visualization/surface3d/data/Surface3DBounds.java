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

import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Shared physical axes and intensity scale for every overlaid sample.
 */
public record Surface3DBounds(double xMin, double xMax, double yMin, double yMax, double maximum) {

  public static @NotNull Surface3DBounds of(@NotNull final List<Surface3DData> data) {
    if (data.isEmpty()) {
      throw new IllegalArgumentException("No surface data");
    }
    return new Surface3DBounds(
        data.stream().mapToDouble(d -> d.xMin() - (d.pixels() ? d.pixelWidth() / 2 : 0)).min()
            .orElseThrow(),
        data.stream().mapToDouble(d -> d.xMax() + (d.pixels() ? d.pixelWidth() / 2 : 0)).max()
            .orElseThrow(),
        data.stream().mapToDouble(d -> d.yMin() - (d.pixels() ? d.pixelHeight() / 2 : 0)).min()
            .orElseThrow(),
        data.stream().mapToDouble(d -> d.yMax() + (d.pixels() ? d.pixelHeight() / 2 : 0)).max()
            .orElseThrow(), data.stream().mapToDouble(Surface3DData::maximum).max().orElseThrow());
  }

  public double normalizeX(final double value) {
    return xMin == xMax ? 0.5 : (value - xMin) / (xMax - xMin);
  }

  public double normalizeY(final double value) {
    return yMin == yMax ? 0.5 : (value - yMin) / (yMax - yMin);
  }
}
