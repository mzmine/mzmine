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

package io.github.mzmine.modules.visualization.intensitymap.render;

import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScaleTransform;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapBounds;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapGrid;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapProjection;
import org.jetbrains.annotations.NotNull;

/**
 * Maps intensities to surface height and color. With a shared scale all overlays are comparable;
 * normalized overlays each use their own maximum, for example to compare ions of different
 * abundance.
 *
 * @param transform  the paint scale transformation of the imaging preferences, applied to the
 *                   heights, or to the colors of projections without heights
 * @param noiseFloor fraction of the maximum below which no geometry is created
 * @param baseline   fraction of the maximum whose height is (almost) zero, so that heights show the
 *                   differences above the lowest pixels. 0 starts heights at zero intensity.
 * @param projection the view; without heights all values lie in the plane and intensity is only
 *                   shown by color
 */
public record IntensityMapScale(@NotNull IntensityMapBounds bounds,
                                @NotNull PaintScaleTransform transform, boolean normalized,
                                double noiseFloor, double baseline,
                                @NotNull IntensityMapProjection projection) {

  /**
   * Height of values at or below the baseline: columns without height are not drawn, so the lowest
   * pixels remain thin tiles.
   */
  public static final double BASE_HEIGHT = 0.02;

  public IntensityMapScale(@NotNull final IntensityMapBounds bounds,
      @NotNull final PaintScaleTransform transform, final boolean normalized,
      final double noiseFloor, final double baseline) {
    this(bounds, transform, normalized, noiseFloor, baseline, IntensityMapProjection.PERSPECTIVE);
  }

  public IntensityMapScale(@NotNull final IntensityMapBounds bounds, final boolean logarithmic,
      final boolean normalized) {
    this(bounds, logarithmic ? PaintScaleTransform.LOG10 : PaintScaleTransform.LINEAR, normalized,
        0, 0);
  }

  public IntensityMapScale(@NotNull final IntensityMapBounds bounds, final boolean logarithmic,
      final boolean normalized, final double noiseFloor) {
    this(bounds, logarithmic ? PaintScaleTransform.LOG10 : PaintScaleTransform.LINEAR, normalized,
        noiseFloor, 0);
  }

  /**
   * @return true for logarithmic transformations, which use decade ticks
   */
  public boolean logarithmic() {
    return isLogarithmic(transform);
  }

  public static boolean isLogarithmic(@NotNull final PaintScaleTransform transform) {
    return switch (transform) {
      case LOG10, LOG2 -> true;
      case LINEAR, SQRT -> false;
    };
  }

  /**
   * @return true if the value is below the noise floor, a fraction of the maximum. Such values
   * produce no geometry.
   */
  public boolean belowNoise(@NotNull final IntensityMapGrid data, final double value) {
    final double maximum = maximum(data);
    return noiseFloor > 0 && maximum > 0 && value / maximum < noiseFloor;
  }

  public double maximum(@NotNull final IntensityMapGrid data) {
    return normalized ? data.maximum() : bounds.maximum();
  }

  /**
   * @return height in [0, 1]
   */
  public double height(@NotNull final IntensityMapGrid data, final double value) {
    if (!projection.heights()) {
      // decision (user request): the 2D view is a plane, it draws in scene order without depth
      // test, so data cover the floor without any height
      return 0;
    }
    return height(transform, value, maximum(data), baseline);
  }

  /**
   * The mapping of surface heights, also used for the intensity axis.
   *
   * @param baseline fraction of the maximum at the floor, 0 for heights from zero
   * @return height in [0, 1]
   */
  public static double height(@NotNull final PaintScaleTransform transform, final double value,
      final double maximum, final double baseline) {
    final double position = position(transform, value, maximum);
    if (!(baseline > 0) || !(position > 0)) {
      return position;
    }
    final double floor = position(transform, baseline * maximum, maximum);
    if (!(floor < 1)) {
      return position;
    }
    return BASE_HEIGHT + (1 - BASE_HEIGHT) * Math.clamp((position - floor) / (1 - floor), 0, 1);
  }

  /**
   * @return paint scale position in [0, 1]. With heights, the transformation only changes the
   * height, so that colors keep their linear meaning (user decision). Without heights, colors are
   * the only cue and follow the transformation (user request). The scale starts at the noise floor
   * (user request), so the whole paint scale covers the shown values.
   */
  public double color(@NotNull final IntensityMapGrid data, final double value) {
    final double maximum = maximum(data);
    if (!(maximum > 0)) {
      return 0;
    }
    final double start = colorPosition(noiseFloor * maximum, maximum);
    final double span = 1 - start;
    return span > 0 ? Math.clamp((colorPosition(value, maximum) - start) / span, 0, 1) : 0;
  }

  /**
   * @param color paint scale position in [0, 1]
   * @return intensity at the paint scale position, the inverse of {@link #color}
   */
  public double intensityAtColor(@NotNull final IntensityMapGrid data, final double color) {
    final double maximum = maximum(data);
    if (!(maximum > 0)) {
      return 0;
    }
    final double start = colorPosition(noiseFloor * maximum, maximum);
    final double position = start + Math.clamp(color, 0, 1) * (1 - start);
    if (!projection.transformsColors()) {
      return position * maximum;
    }
    final double zero = transform.transform(1);
    final double range = transform.transform(maximum + 1) - zero;
    return Math.max(0, transform.revertTransform(zero + position * range) - 1);
  }

  /**
   * @return position of the value between zero and the maximum, transformed if colors follow the
   * transformation
   */
  private double colorPosition(final double value, final double maximum) {
    if (projection.transformsColors()) {
      return Math.min(1, position(transform, value, maximum));
    }
    return value > 0 ? Math.min(1, value / maximum) : 0;
  }

  /**
   * Offsets by one so that zero intensity maps to zero for logarithmic transformations too.
   *
   * @return relative position of the value between zero and the maximum
   */
  public static double position(@NotNull final PaintScaleTransform transform, final double value,
      final double maximum) {
    if (!(maximum > 0) || !(value > 0)) {
      return 0;
    }
    final double zero = transform.transform(1);
    final double range = transform.transform(maximum + 1) - zero;
    return range > 0 ? (transform.transform(value + 1) - zero) / range : 0;
  }

  /**
   * @return true if a mesh built with this scale is identical for the data under the other scale
   */
  public boolean sameGeometry(@NotNull final IntensityMapScale other,
      @NotNull final IntensityMapGrid data) {
    return transform == other.transform && normalized == other.normalized
        && noiseFloor == other.noiseFloor && baseline == other.baseline && projection.equals(
        other.projection) && bounds.xMin() == other.bounds.xMin()
        && bounds.xMax() == other.bounds.xMax() && bounds.yMin() == other.bounds.yMin()
        && bounds.yMax() == other.bounds.yMax() && maximum(data) == other.maximum(data);
  }
}
