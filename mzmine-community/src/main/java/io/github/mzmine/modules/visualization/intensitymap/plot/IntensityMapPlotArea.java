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


import io.github.mzmine.modules.visualization.intensitymap.render.IntensityMapPicker;
import io.github.mzmine.modules.visualization.intensitymap.render.IntensityMapTile;
import javafx.geometry.Insets;
import javafx.geometry.Point3D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.SubScene;
import javafx.scene.shape.Rectangle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The fixed plot area of the 2D view, as in a zoomed chart: the fitted view places the axes,
 * zooming and panning only change their tick values, and data outside the area are clipped instead
 * of covering the tick labels. The area is stored as margins to the viewport edges, so it follows
 * resizing.
 */
final class IntensityMapPlotArea {

  // pixels a tile may miss at the plot area border and still cover it
  private static final double COVER_TOLERANCE = 1;

  private final IntensityMapCamera camera;
  private final Group model;
  private final SubScene scene;
  private @Nullable Insets insets;

  IntensityMapPlotArea(@NotNull final IntensityMapCamera camera, @NotNull final Group model,
      @NotNull final SubScene scene) {
    this.camera = camera;
    this.model = model;
    this.scene = scene;
  }

  /**
   * Stores the margins of the fitted floor of the tile to the viewport edges and updates the clip.
   *
   * @param tile the tile with a fixed plot area, null for none
   */
  void update(@Nullable final IntensityMapTile tile) {
    insets = null;
    final double[] screen = tile == null ? null : IntensityMapExtent.screen(model,
        IntensityMapExtent.corners(tile, IntensityMapExtent.full()), scene);
    if (screen != null) {
      if (screen[1] > screen[0] && screen[3] > screen[2]) {
        insets = new Insets(screen[2], scene.getWidth() - screen[1], scene.getHeight() - screen[3],
            screen[0]);
      }
    }
    updateClip(tile);
  }

  /**
   * @param low      screen start of the data along one axis
   * @param high     screen end of the data
   * @param areaLow  start of the plot area
   * @param areaHigh end of the plot area
   * @return screen shift that brings the data edges to the plot area edges, or that centers data
   * narrower than the plot area; 0 if the data already cover the plot area
   */
  static double correction(final double low, final double high, final double areaLow,
      final double areaHigh) {
    if (high - low <= areaHigh - areaLow) {
      return (areaLow + areaHigh - low - high) / 2;
    }
    if (low > areaLow) {
      return areaLow - low;
    }
    if (high < areaHigh) {
      return areaHigh - high;
    }
    return 0;
  }

  /**
   * @param tile the tile with a fixed plot area, null for none
   * @return screen area of the plot in the current viewport, null if there is none
   */
  @Nullable Rectangle2D area(@Nullable final IntensityMapTile tile) {
    final Insets current = insets;
    if (current == null || tile == null) {
      return null;
    }
    final double width = scene.getWidth() - current.getLeft() - current.getRight();
    final double height = scene.getHeight() - current.getTop() - current.getBottom();
    return width > 0 && height > 0 ? new Rectangle2D(current.getLeft(), current.getTop(), width,
        height) : null;
  }

  /**
   * Clips the view to the plot area, so zoomed data do not cover the tick labels.
   */
  void updateClip(@Nullable final IntensityMapTile tile) {
    final Rectangle2D area = area(tile);
    // a small margin keeps the axis lines at the border
    scene.setClip(area == null ? null
        : new Rectangle(area.getMinX() - 2, area.getMinY() - 2, area.getWidth() + 4,
            area.getHeight() + 4));
  }

  /**
   * @return local floor {x0, x1, z0, z1} shown in the plot area, null without a plot area
   */
  double @Nullable [] floor(@Nullable final IntensityMapTile tile) {
    return tile == null ? null : floorAtCorners(tile, true, 0);
  }

  /**
   * @return true if the floor of the tile reaches all corners of its plot area
   */
  boolean coveredBy(@NotNull final IntensityMapTile tile) {
    // a tile fitted to the plot area ends exactly at its corners
    return floorAtCorners(tile, false, COVER_TOLERANCE) != null;
  }

  /**
   * @param clamp clamps points beyond the floor edges to the floor, otherwise they do not count
   * @param inset pixels the corners are moved into the plot area
   * @return floor extent {x0, x1, z0, z1} under the corners of the plot area, null if a corner
   * misses the floor or there is no plot area
   */
  private double @Nullable [] floorAtCorners(@NotNull final IntensityMapTile tile,
      final boolean clamp, final double inset) {
    final Rectangle2D area = area(tile);
    if (area == null) {
      return null;
    }
    final double[] extent = IntensityMapExtent.empty();
    for (final double x : new double[]{area.getMinX() + inset, area.getMaxX() - inset}) {
      for (final double y : new double[]{area.getMinY() + inset, area.getMaxY() - inset}) {
        final Point3D hit = IntensityMapPicker.floor(tile.toLocal(camera.ray(x, y)), clamp);
        if (hit == null) {
          return null;
        }
        IntensityMapExtent.include(extent, hit.getX(), hit.getZ());
      }
    }
    return extent;
  }
}
