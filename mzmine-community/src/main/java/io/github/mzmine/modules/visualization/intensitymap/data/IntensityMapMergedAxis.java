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

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleToIntFunction;
import java.util.function.IntToDoubleFunction;
import org.jetbrains.annotations.NotNull;

/**
 * One axis of a grid merged from coarse base cells and finer window cells, see
 * {@link IntensityMapGrid#merge(IntensityMapGrid, IntensityMapGrid)}.
 *
 * @param window index of the window cell per merged cell, -1 for base cells
 * @param base   index of the base cell containing each merged cell, -1 if none
 */
record IntensityMapMergedAxis(double @NotNull [] centers, double @NotNull [] lows,
                              double @NotNull [] highs, int @NotNull [] window,
                              int @NotNull [] base) {

  // clipped base cells narrower than this fraction of their size are dropped
  private static final double MIN_FRACTION = 1e-6;

  static @NotNull IntensityMapMergedAxis of(final double @NotNull [] baseCenters,
      @NotNull final IntToDoubleFunction baseLow, @NotNull final IntToDoubleFunction baseHigh,
      final double @NotNull [] windowCenters, @NotNull final IntToDoubleFunction windowLow,
      @NotNull final IntToDoubleFunction windowHigh, @NotNull final DoubleToIntFunction baseBin) {
    final int n = baseCenters.length;
    final double outerLow = baseLow.applyAsDouble(0);
    final double outerHigh = baseHigh.applyAsDouble(n - 1);
    // the window replaces the base between its outer boundaries
    final double from = Math.max(outerLow, windowLow.applyAsDouble(0));
    final double to = Math.min(outerHigh, windowHigh.applyAsDouble(windowCenters.length - 1));
    final List<double[]> cells = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      final double low = baseLow.applyAsDouble(i);
      final double high = baseHigh.applyAsDouble(i);
      if (low < from) {
        addClipped(cells, baseCenters[i], low, Math.min(high, from), i);
      }
    }
    for (int j = 0; j < windowCenters.length; j++) {
      final double low = Math.max(from, windowLow.applyAsDouble(j));
      final double high = Math.min(to, windowHigh.applyAsDouble(j));
      if (high > low || windowCenters.length == 1) {
        cells.add(
            new double[]{windowCenters[j], low, high, j, baseBin.applyAsInt(windowCenters[j])});
      }
    }
    for (int i = 0; i < n; i++) {
      final double low = baseLow.applyAsDouble(i);
      final double high = baseHigh.applyAsDouble(i);
      if (high > to) {
        addClipped(cells, baseCenters[i], Math.max(low, to), high, i);
      }
    }
    final int size = cells.size();
    final double[] centers = new double[size];
    final double[] lows = new double[size];
    final double[] highs = new double[size];
    final int[] window = new int[size];
    final int[] base = new int[size];
    for (int k = 0; k < size; k++) {
      final double[] cell = cells.get(k);
      centers[k] = cell[0];
      lows[k] = cell[1];
      highs[k] = cell[2];
      window[k] = cell.length == 5 ? (int) cell[3] : -1;
      base[k] = (int) cell[cell.length - 1];
    }
    return new IntensityMapMergedAxis(centers, lows, highs, window, base);
  }

  /**
   * Adds a base cell, clipped to the window border. The center moves into the clipped part.
   */
  private static void addClipped(@NotNull final List<double[]> cells, final double center,
      final double low, final double high, final int index) {
    if (!(high - low > MIN_FRACTION * Math.max(Math.abs(high), 1))) {
      return;
    }
    final double inside = center >= low && center <= high ? center : (low + high) / 2;
    cells.add(new double[]{inside, low, high, index});
  }
}
