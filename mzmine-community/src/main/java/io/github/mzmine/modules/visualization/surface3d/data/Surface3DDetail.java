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

import org.jetbrains.annotations.NotNull;

/**
 * View-dependent sampling density. There is no fixed limit on either coordinate axis; the total
 * vertex count across all overlays is bounded so that rotating and zooming stay fluid.
 *
 * @param flat 2D view: at most one sample per screen pixel, like the former 2D plot
 */
public record Surface3DDetail(double width, double height, int samples, boolean flat) {

  // the plot area of the 2D view is smaller than the viewport, cells should cover a pixel
  private static final double FLAT_PLOT_FRACTION = 0.7;

  public static final Surface3DDetail DEFAULT = new Surface3DDetail(1200, 800, 1);

  public Surface3DDetail(final double width, final double height, final int samples) {
    this(width, height, samples, false);
  }

  // decision: beyond ~1 M grid vertices per view, extra vertices are sub-pixel but the GPU
  // upload, JavaFX vertex buffer generation, and frame time keep growing.
  static final long SURFACE_VERTICES = 1_000_000;
  // pixel columns use up to 8 vertices and 10 triangles each
  static final long PIXEL_CELLS = 360_000;

  /**
   * @param nativeX number of distinct measured coordinates along x
   * @param nativeY number of distinct measured coordinates (or useful m/z bins) along y
   */
  public @NotNull GridSize grid(final int nativeX, final int nativeY, final boolean pixels) {
    return grid(nativeX, nativeY, pixels,
        Math.min(256L * 1024 * 1024, Runtime.getRuntime().maxMemory() / 24));
  }

  @NotNull GridSize grid(final int nativeX, final int nativeY, final boolean pixels,
      final long memory) {
    final int viewX;
    final int viewY;
    if (flat) {
      // decision: cells thinner than a pixel are not drawn, so centroids jittering between fine
      // m/z bins would appear as dashed traces. Bins of a pixel keep the maximum of the pixel.
      viewX = (int) Math.ceil(Math.max(400, width * FLAT_PLOT_FRACTION));
      viewY = (int) Math.ceil(Math.max(300, height * FLAT_PLOT_FRACTION));
    } else {
      viewX = (int) Math.ceil(Math.max(400, width));
      // the spectral axis gets more samples than screen pixels, bins retain maxima of narrow peaks
      viewY = (int) Math.ceil(Math.max(600, Math.max(width, height) * 1.6));
    }
    int x = Math.max(1, Math.min(nativeX, viewX));
    int y = Math.max(1, Math.min(nativeY, viewY));
    // Memory guard for data, mesh arrays, JavaFX copies and GPU buffers, plus a render budget.
    final long memoryVertices = memory / (pixels ? 1408 : 256);
    final long renderVertices = pixels ? PIXEL_CELLS : SURFACE_VERTICES;
    final long vertices = Math.max(4096,
        Math.min(memoryVertices, renderVertices) / Math.max(1, samples));
    final boolean reduced = (long) x * y > vertices;
    if (reduced) {
      final double factor = Math.sqrt((double) vertices / ((long) x * y));
      x = Math.max(Math.min(2, nativeX), (int) (x * factor));
      y = Math.max(Math.min(2, nativeY), (int) (y * factor));
    }
    return new GridSize(x, y);
  }

  public record GridSize(int x, int y) {

  }
}
