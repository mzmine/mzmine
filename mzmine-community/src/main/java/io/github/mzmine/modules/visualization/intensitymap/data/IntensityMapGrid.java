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

import java.util.Arrays;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Numeric coordinates and nonnegative intensity, with missing values represented explicitly.
 */
public final class IntensityMapGrid {

  private final double[] x;
  private final double[] y;
  private final String xLabel;
  private final String yLabel;
  private final float[] intensity;
  private final boolean[] present;
  private final boolean pixels;
  // 0 if the axis is irregular, otherwise the constant coordinate spacing for O(1) binning
  private final double xStep;
  private final double yStep;
  private double pixelWidth = 1;
  private double pixelHeight = 1;
  private double maximum;
  // lowest positive intensity, computed on first use
  private double minimum = Double.NaN;
  // explicit cell extents of merged grids, whose cells differ in size; null derives them
  private double @Nullable [] xLows;
  private double @Nullable [] xHighs;
  private double @Nullable [] yLows;
  private double @Nullable [] yHighs;
  private IntensityMapAxisKind xKind = IntensityMapAxisKind.OTHER;
  private IntensityMapAxisKind yKind = IntensityMapAxisKind.OTHER;

  public IntensityMapGrid(final int width, final int height, @NotNull final String xLabel,
      @NotNull final String yLabel, final double xMin, final double xMax, final double yMin,
      final double yMax) {
    this(coordinates(width, xMin, xMax), coordinates(height, yMin, yMax), xLabel, yLabel, false);
  }

  public IntensityMapGrid(final double @NotNull [] x, final double @NotNull [] y,
      @NotNull final String xLabel, @NotNull final String yLabel, final boolean pixels) {
    validate(x);
    validate(y);
    this.x = x.clone();
    this.y = y.clone();
    this.xLabel = xLabel;
    this.yLabel = yLabel;
    this.pixels = pixels;
    xStep = uniformStep(this.x);
    yStep = uniformStep(this.y);
    intensity = new float[Math.multiplyExact(x.length, y.length)];
    present = new boolean[intensity.length];
  }

  private static void validate(final double @NotNull [] values) {
    if (values.length == 0) {
      throw new IllegalArgumentException("An axis must contain at least one coordinate");
    }
    for (int i = 0; i < values.length; i++) {
      if (!Double.isFinite(values[i]) || (i > 0 && values[i] < values[i - 1])) {
        throw new IllegalArgumentException("Axis coordinates must be finite and sorted");
      }
    }
  }

  private static double uniformStep(final double @NotNull [] values) {
    if (values.length < 2) {
      return 0;
    }
    final double step = (values[values.length - 1] - values[0]) / (values.length - 1);
    if (!(step > 0)) {
      return 0;
    }
    for (int i = 1; i < values.length - 1; i++) {
      if (Math.abs(values[i] - (values[0] + i * step)) > step * 1e-6) {
        return 0;
      }
    }
    return step;
  }

  public static double @NotNull [] coordinates(final int count, final double min,
      final double max) {
    if (count < 1 || !Double.isFinite(min) || !Double.isFinite(max) || min > max) {
      throw new IllegalArgumentException("Invalid axis range");
    }
    final double[] values = new double[count];
    for (int i = 0; i < count; i++) {
      values[i] = count == 1 ? min : min + (max - min) * i / (count - 1);
    }
    return values;
  }

  /**
   * Evenly picks existing coordinates so that every reduced coordinate is still a measured one.
   */
  public static double @NotNull [] reduceCoordinates(final double @NotNull [] values,
      final int count) {
    if (values.length <= count) {
      return values;
    }
    final int size = Math.max(2, count);
    final double[] result = new double[size];
    for (int i = 0; i < size; i++) {
      result[i] = values[(int) Math.round((double) i * (values.length - 1) / (size - 1))];
    }
    return result;
  }

  /**
   * @param step distance between neighboring pixels
   * @return how many pixels per axis are merged so that at most maxCount blocks remain
   */
  public static int blockFactor(final double @NotNull [] values, final int maxCount,
      final double step) {
    if (values.length < 2 || !(step > 0)) {
      return 1;
    }
    final long span = Math.round((values[values.length - 1] - values[0]) / step) + 1;
    return (int) Math.max(1, Math.ceil(span / (double) Math.max(1, maxCount)));
  }

  /**
   * Regular block centers for merging pixels. Unlike picking every n-th coordinate, whole blocks
   * tile the image without gaps, which otherwise show as grid lines between pixels.
   *
   * @return uniformly spaced coordinates with a spacing of factor * step
   */
  public static double @NotNull [] blockCoordinates(final double @NotNull [] values,
      final int factor, final double step) {
    if (factor <= 1 || values.length < 2) {
      return values;
    }
    final double first = values[0];
    final long span = Math.round((values[values.length - 1] - first) / step) + 1;
    final int blocks = (int) Math.ceil(span / (double) factor);
    final double[] result = new double[blocks];
    for (int i = 0; i < blocks; i++) {
      result[i] = first + (i * factor + (factor - 1) / 2d) * step;
    }
    return result;
  }

  public void addMaximum(final int column, final int row, final double value) {
    if (column < 0 || column >= width() || row < 0 || row >= height() || !Double.isFinite(value)
        || value < 0) {
      return;
    }
    final int index = row * width() + column;
    minimum = Double.NaN;
    final float stored = (float) Math.min(Float.MAX_VALUE, Math.max(intensity[index], value));
    intensity[index] = stored;
    present[index] = true;
    if (stored > maximum) {
      maximum = stored;
    }
  }

  public void addSum(final int column, final int row, final double value) {
    if (column < 0 || column >= width() || row < 0 || row >= height() || !Double.isFinite(value)
        || value < 0) {
      return;
    }
    addMaximum(column, row, intensity(column, row) + value);
  }

  public void markColumn(final int column) {
    if (column < 0 || column >= width()) {
      return;
    }
    for (int row = 0; row < height(); row++) {
      present[row * width() + column] = true;
    }
  }

  public void markPresent(final int column, final int row) {
    if (column >= 0 && column < width() && row >= 0 && row < height()) {
      present[row * width() + column] = true;
    }
  }

  public int binX(final double value) {
    return xLows == null ? nearest(value, x, xStep, pixels) : cell(value, x, xLows, xHighs, pixels);
  }

  public int binY(final double value) {
    return yLows == null ? nearest(value, y, yStep, pixels) : cell(value, y, yLows, yHighs, pixels);
  }

  /**
   * @return the cell with explicit extents that contains the value, for other data the nearest one
   * within the coordinate range
   */
  private static int cell(final double value, final double @NotNull [] centers,
      final double @NotNull [] lows, final double @Nullable [] highs, final boolean pixels) {
    if (!(value >= (pixels ? lows[0] : centers[0])) || !(value <= (pixels ? highs[highs.length - 1]
        : centers[centers.length - 1]))) {
      return -1;
    }
    int index = Arrays.binarySearch(lows, value);
    index = index >= 0 ? index : Math.max(0, -index - 2);
    if (value <= highs[index]) {
      return index;
    }
    // between separated cells
    if (pixels) {
      return -1;
    }
    return index + 1 < centers.length && centers[index + 1] - value < value - centers[index] ? index
        + 1 : index;
  }

  /**
   * @return lower boundary of the cell of the x coordinate
   */
  public double xLow(final int index) {
    return xLows != null ? xLows[index] : low(x, index, pixels, pixelWidth);
  }

  public double xHigh(final int index) {
    return xHighs != null ? xHighs[index] : high(x, index, pixels, pixelWidth);
  }

  public double yLow(final int index) {
    return yLows != null ? yLows[index] : low(y, index, pixels, pixelHeight);
  }

  public double yHigh(final int index) {
    return yHighs != null ? yHighs[index] : high(y, index, pixels, pixelHeight);
  }

  /**
   * Pixels span their size around the center. Other cells reach halfway to their neighbors, and as
   * far beyond the first and last coordinate as the spacing to their neighbor.
   */
  private static double low(final double @NotNull [] values, final int i, final boolean pixels,
      final double size) {
    if (pixels) {
      return values[i] - size / 2;
    }
    if (values.length == 1) {
      return values[0] - singleHalfWidth(values[0]);
    }
    return i == 0 ? values[0] - (values[1] - values[0]) / 2 : (values[i - 1] + values[i]) / 2;
  }

  private static double high(final double @NotNull [] values, final int i, final boolean pixels,
      final double size) {
    if (pixels) {
      return values[i] + size / 2;
    }
    final int n = values.length;
    if (n == 1) {
      return values[0] + singleHalfWidth(values[0]);
    }
    return i == n - 1 ? values[n - 1] + (values[n - 1] - values[n - 2]) / 2
        : (values[i] + values[i + 1]) / 2;
  }

  // assumption: a single row or column gets a narrow band of 1 % of its coordinate
  private static double singleHalfWidth(final double value) {
    return Math.max(Math.abs(value) * 0.005, 0.5e-3);
  }

  /**
   * Combines coarse data of the complete range with finer data of a window, e.g. after zooming in:
   * window cells replace the base cells they cover, base cells at the window border are clipped to
   * it. The result keeps the coordinate range of the base, so the view does not move, and panning
   * beyond the window shows the coarse data instead of nothing.
   *
   * @param window data of the same overlay sampled inside a window of the base range
   */
  public static @NotNull IntensityMapGrid merge(@NotNull final IntensityMapGrid base,
      @NotNull final IntensityMapGrid window) {
    final IntensityMapMergedAxis xs = IntensityMapMergedAxis.of(base.x, base::xLow, base::xHigh,
        window.x, window::xLow, window::xHigh, base::binX);
    final IntensityMapMergedAxis ys = IntensityMapMergedAxis.of(base.y, base::yLow, base::yHigh,
        window.y, window::yLow, window::yHigh, base::binY);
    final IntensityMapGrid result = new IntensityMapGrid(xs.centers(), ys.centers(), base.xLabel,
        base.yLabel, base.pixels);
    result.xLows = xs.lows();
    result.xHighs = xs.highs();
    result.yLows = ys.lows();
    result.yHighs = ys.highs();
    result.pixelWidth = base.pixelWidth;
    result.pixelHeight = base.pixelHeight;
    result.xKind = base.xKind;
    result.yKind = base.yKind;
    for (int row = 0; row < result.height(); row++) {
      for (int column = 0; column < result.width(); column++) {
        final int wx = xs.window()[column];
        final int wy = ys.window()[row];
        final boolean inWindow = wx >= 0 && wy >= 0;
        final IntensityMapGrid source = inWindow ? window : base;
        final int sx = inWindow ? wx : xs.base()[column];
        final int sy = inWindow ? wy : ys.base()[row];
        if (sx < 0 || sy < 0 || !source.isPresent(sx, sy)) {
          continue;
        }
        result.present[row * result.width() + column] = true;
        result.addMaximum(column, row, source.intensity(sx, sy));
      }
    }
    return result;
  }

  /**
   * @param pixels pixel cells extend half a step beyond their centers: merged pixel blocks are
   *               centered inside their outermost pixels, which would otherwise be dropped
   */
  private static int nearest(final double value, final double @NotNull [] values, final double step,
      final boolean pixels) {
    final double margin = pixels && step > 0 ? step / 2 : 0;
    if (!(value >= values[0] - margin) || !(value <= values[values.length - 1] + margin)) {
      return -1;
    }
    if (step > 0) {
      return Math.clamp(Math.round((value - values[0]) / step), 0, values.length - 1);
    }
    final int found = Arrays.binarySearch(values, value);
    if (found >= 0) {
      return found;
    }
    final int upper = -found - 1;
    return upper == 0 ? 0 : upper == values.length ? upper - 1
        : value - values[upper - 1] <= values[upper] - value ? upper - 1 : upper;
  }

  /**
   * @return a copy with swapped axes, for example to show m/z horizontally.
   */
  public @NotNull IntensityMapGrid transpose() {
    final IntensityMapGrid result = new IntensityMapGrid(y, x, yLabel, xLabel, pixels);
    result.xLows = yLows;
    result.xHighs = yHighs;
    result.yLows = xLows;
    result.yHighs = xHighs;
    for (int row = 0; row < height(); row++) {
      for (int column = 0; column < width(); column++) {
        final int source = row * width() + column;
        final int target = column * result.width() + row;
        result.intensity[target] = intensity[source];
        result.present[target] = present[source];
      }
    }
    result.maximum = maximum;
    result.pixelWidth = pixelHeight;
    result.pixelHeight = pixelWidth;
    result.xKind = yKind;
    result.yKind = xKind;
    return result;
  }

  /**
   * Max-pools in memory, so a new overlay can share the render budget without re-reading raw data.
   *
   * @return this instance if it already fits
   */
  public @NotNull IntensityMapGrid downsample(final int maxWidth, final int maxHeight) {
    if (width() <= maxWidth && height() <= maxHeight) {
      return this;
    }
    // pixels merge in whole blocks, other data keep measured coordinates
    final int factorX = pixels ? blockFactor(x, maxWidth, pixelWidth) : 1;
    final int factorY = pixels ? blockFactor(y, maxHeight, pixelHeight) : 1;
    final IntensityMapGrid result =
        pixels ? new IntensityMapGrid(blockCoordinates(x, factorX, pixelWidth),
            blockCoordinates(y, factorY, pixelHeight), xLabel, yLabel, true)
            : new IntensityMapGrid(reduceCoordinates(x, maxWidth), reduceCoordinates(y, maxHeight),
                xLabel, yLabel, false);
    for (int row = 0; row < height(); row++) {
      final int targetRow = result.binY(y[row]);
      for (int column = 0; column < width(); column++) {
        final int index = row * width() + column;
        if (!present[index]) {
          continue;
        }
        final int targetColumn = result.binX(x[column]);
        result.markPresent(targetColumn, targetRow);
        result.addMaximum(targetColumn, targetRow, intensity[index]);
      }
    }
    if (pixels) {
      result.setPixelSize(pixelWidth * factorX, pixelHeight * factorY);
    }
    result.xKind = xKind;
    result.yKind = yKind;
    return result;
  }

  /**
   * @param values intensity per cell (row * width + column), for example smoothed values
   * @return a copy with the same coordinates and measured cells
   */
  @NotNull IntensityMapGrid withIntensities(final float @NotNull [] values) {
    if (values.length != intensity.length) {
      throw new IllegalArgumentException("One value per cell required");
    }
    final IntensityMapGrid result = new IntensityMapGrid(x, y, xLabel, yLabel, pixels);
    result.xLows = xLows;
    result.xHighs = xHighs;
    result.yLows = yLows;
    result.yHighs = yHighs;
    for (int i = 0; i < values.length; i++) {
      if (present[i]) {
        result.present[i] = true;
        result.addMaximum(i % width(), i / width(), values[i]);
      }
    }
    result.pixelWidth = pixelWidth;
    result.pixelHeight = pixelHeight;
    result.xKind = xKind;
    result.yKind = yKind;
    return result;
  }

  public @NotNull IntensityMapAxisKind xKind() {
    return xKind;
  }

  public @NotNull IntensityMapAxisKind yKind() {
    return yKind;
  }

  /**
   * Selects the number formats of the axes.
   */
  public void setAxisKinds(@NotNull final IntensityMapAxisKind x,
      @NotNull final IntensityMapAxisKind y) {
    xKind = x;
    yKind = y;
  }

  public int width() {
    return x.length;
  }

  public int height() {
    return y.length;
  }

  public @NotNull String xLabel() {
    return xLabel;
  }

  public @NotNull String yLabel() {
    return yLabel;
  }

  public double xMin() {
    return x[0];
  }

  public double xMax() {
    return x[x.length - 1];
  }

  public double yMin() {
    return y[0];
  }

  public double yMax() {
    return y[y.length - 1];
  }

  public double xValue(final int index) {
    return x[index];
  }

  public double yValue(final int index) {
    return y[index];
  }

  public double maximum() {
    return maximum;
  }

  /**
   * @return the lowest positive intensity of a measured cell, 0 without signal
   */
  public double minimum() {
    if (Double.isNaN(minimum)) {
      double lowest = Double.POSITIVE_INFINITY;
      for (int i = 0; i < intensity.length; i++) {
        if (present[i] && intensity[i] > 0 && intensity[i] < lowest) {
          lowest = intensity[i];
        }
      }
      minimum = Double.isFinite(lowest) ? lowest : 0;
    }
    return minimum;
  }

  public boolean pixels() {
    return pixels;
  }

  public double pixelWidth() {
    return pixelWidth;
  }

  public double pixelHeight() {
    return pixelHeight;
  }

  public void setPixelSize(final double width, final double height) {
    if (!(width > 0) || !(height > 0) || !Double.isFinite(width) || !Double.isFinite(height)) {
      throw new IllegalArgumentException("Pixel dimensions must be finite and positive");
    }
    pixelWidth = width;
    pixelHeight = height;
  }

  public boolean isPresent(final int column, final int row) {
    return present[row * width() + column];
  }

  public float intensity(final int column, final int row) {
    return intensity[row * width() + column];
  }

  public boolean isEmpty() {
    for (final boolean value : present) {
      if (value) {
        return false;
      }
    }
    return true;
  }
}
