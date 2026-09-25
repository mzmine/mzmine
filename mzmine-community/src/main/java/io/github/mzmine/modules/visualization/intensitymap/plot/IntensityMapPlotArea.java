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

import io.github.mzmine.modules.visualization.intensitymap.render.IntensityMapPicker;
import io.github.mzmine.modules.visualization.intensitymap.render.IntensityMapTile;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.SubScene;
import javafx.scene.shape.Rectangle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The fixed plot area of the 2D view, like a zoomed chart (user decision): the fitted view places
 * the axes, zooming and panning only change their tick values, and data outside the area are
 * clipped instead of covering the tick labels. The area is stored as margins to the viewport edges,
 * so it follows resizing.
 */
final class IntensityMapPlotArea {

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
   * Stores the margins of the fitted floor of the tile to the viewport edges.
   *
   * @param tile the tile with a fixed plot area, null for none
   */
  void update(@Nullable final IntensityMapTile tile) {
    insets = null;
    if (tile != null) {
      final double[] screen = IntensityMapExtent.empty();
      for (final double x : new double[]{-WIDTH / 2, WIDTH / 2}) {
        for (final double z : new double[]{-DEPTH / 2, DEPTH / 2}) {
          final Point3D sceneCorner = model.localToScene(tile.toModel(new Point3D(x, 0, z)), true);
          if (sceneCorner != null) {
            final Point2D local = scene.sceneToLocal(sceneCorner.getX(), sceneCorner.getY());
            IntensityMapExtent.include(screen, local.getX(), local.getY());
          }
        }
      }
      if (screen[1] > screen[0] && screen[3] > screen[2]) {
        insets = new Insets(screen[2], scene.getWidth() - screen[1], scene.getHeight() - screen[3],
            screen[0]);
      }
    }
    updateClip(tile);
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
    final Rectangle2D area = area(tile);
    if (area == null || tile == null) {
      return null;
    }
    final double[] extent = IntensityMapExtent.empty();
    for (final double x : new double[]{area.getMinX(), area.getMaxX()}) {
      for (final double y : new double[]{area.getMinY(), area.getMaxY()}) {
        final Point3D hit = IntensityMapPicker.floor(tile.toLocal(camera.ray(x, y)), true);
        if (hit == null) {
          return null;
        }
        IntensityMapExtent.include(extent, hit.getX(), hit.getZ());
      }
    }
    return extent;
  }
}
