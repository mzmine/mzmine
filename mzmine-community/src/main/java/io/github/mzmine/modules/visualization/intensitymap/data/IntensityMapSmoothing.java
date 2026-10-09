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

import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import org.jetbrains.annotations.NotNull;

/**
 * Gaussian smoothing over neighboring data points of the grid. Uses normalized convolution, so
 * unmeasured cells neither receive values nor pull their neighbors towards zero.
 *
 * @param radius kernel half width in data points, 0 disables smoothing
 * @param axes   the axes to smooth along, e.g. only retention time to keep m/z separated
 */
public record IntensityMapSmoothing(int radius, @NotNull Axes axes) {

  public static final IntensityMapSmoothing OFF = new IntensityMapSmoothing(0, Axes.BOTH);

  public enum Axes {
    BOTH("Both axes"), X("X axis"), Y("Y axis");

    private final String label;

    Axes(@NotNull final String label) {
      this.label = label;
    }

    @Override
    public @NotNull String toString() {
      return label;
    }
  }

  public IntensityMapSmoothing {
    radius = Math.max(0, radius);
  }

  public boolean active() {
    return radius > 0;
  }

  public @NotNull IntensityMapGrid apply(@NotNull final IntensityMapGrid data,
      @NotNull final BooleanSupplier canceled) {
    if (!active()) {
      return data;
    }
    // decision: sigma of half the radius, the kernel edge weighs about 14 %
    final double sigma = Math.max(0.5, radius / 2d);
    final double[] kernel = new double[radius + 1];
    for (int k = 0; k <= radius; k++) {
      kernel[k] = Math.exp(-k * k / (2 * sigma * sigma));
    }
    final int width = data.width();
    final int height = data.height();
    float[] values = new float[width * height];
    final boolean[] present = new boolean[values.length];
    for (int row = 0; row < height; row++) {
      for (int column = 0; column < width; column++) {
        final int cell = row * width + column;
        present[cell] = data.isPresent(column, row);
        values[cell] = present[cell] ? data.intensity(column, row) : 0;
      }
    }
    if (axes != Axes.Y) {
      values = pass(values, present, width, height, kernel, true, canceled);
    }
    if (axes != Axes.X) {
      values = pass(values, present, width, height, kernel, false, canceled);
    }
    return data.withIntensities(values);
  }

  private static float @NotNull [] pass(final float @NotNull [] values,
      final boolean @NotNull [] present, final int width, final int height,
      final double @NotNull [] kernel, final boolean alongX,
      @NotNull final BooleanSupplier canceled) {
    final float[] result = new float[values.length];
    final int radius = kernel.length - 1;
    for (int row = 0; row < height; row++) {
      if (canceled.getAsBoolean()) {
        throw new CancellationException();
      }
      for (int column = 0; column < width; column++) {
        final int cell = row * width + column;
        if (!present[cell]) {
          continue;
        }
        double sum = 0;
        double weights = 0;
        for (int k = -radius; k <= radius; k++) {
          final int c = alongX ? column + k : column;
          final int r = alongX ? row : row + k;
          if (c < 0 || r < 0 || c >= width || r >= height) {
            continue;
          }
          final int neighbor = r * width + c;
          if (present[neighbor]) {
            final double weight = kernel[Math.abs(k)];
            sum += weight * values[neighbor];
            weights += weight;
          }
        }
        result[cell] = (float) (weights > 0 ? sum / weights : values[cell]);
      }
    }
    return result;
  }
}
