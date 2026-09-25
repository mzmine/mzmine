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

package io.github.mzmine.modules.visualization.intensitymap.plot;

import static io.github.mzmine.modules.visualization.intensitymap.render.IntensityMapMesh.DEPTH;
import static io.github.mzmine.modules.visualization.intensitymap.render.IntensityMapMesh.WIDTH;

import com.google.common.collect.Range;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapBounds;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapRegion;
import io.github.mzmine.modules.visualization.intensitymap.render.IntensityMapPicker;
import org.jetbrains.annotations.NotNull;

/**
 * Rectangular extents {x0, x1, z0, z1} on the plot floor or on screen.
 */
final class IntensityMapExtent {

  private IntensityMapExtent() {
  }

  /**
   * @return an extent that contains nothing, to be grown by {@link #include}
   */
  static double @NotNull [] empty() {
    return new double[]{Double.MAX_VALUE, -Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE};
  }

  static void include(final double @NotNull [] extent, final double x, final double z) {
    extent[0] = Math.min(extent[0], x);
    extent[1] = Math.max(extent[1], x);
    extent[2] = Math.min(extent[2], z);
    extent[3] = Math.max(extent[3], z);
  }

  static boolean isEmpty(final double @NotNull [] extent) {
    return extent[0] > extent[1];
  }

  /**
   * @return true if the floor extent covers at least 90 % of both axes
   */
  static boolean nearlyAll(final double @NotNull [] floor) {
    return floor[1] - floor[0] >= WIDTH * 0.9 && floor[3] - floor[2] >= DEPTH * 0.9;
  }

  /**
   * @return true if the floor extent covers the complete floor
   */
  static boolean complete(final double @NotNull [] floor) {
    return floor[0] <= -WIDTH / 2 + 1e-9 && floor[1] >= WIDTH / 2 - 1e-9
        && floor[2] <= -DEPTH / 2 + 1e-9 && floor[3] >= DEPTH / 2 - 1e-9;
  }

  /**
   * @return the data window of a local floor extent
   */
  static @NotNull IntensityMapRegion toData(@NotNull final IntensityMapBounds bounds,
      final double @NotNull [] floor) {
    return new IntensityMapRegion(Range.closed(IntensityMapPicker.dataX(bounds, floor[0]),
        IntensityMapPicker.dataX(bounds, floor[1])),
        Range.closed(IntensityMapPicker.dataY(bounds, floor[2]),
            IntensityMapPicker.dataY(bounds, floor[3])));
  }
}
