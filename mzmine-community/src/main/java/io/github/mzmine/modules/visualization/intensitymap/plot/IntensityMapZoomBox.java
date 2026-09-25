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

import io.github.mzmine.modules.visualization.intensitymap.render.IntensityMapTile;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The box dragged on the floor with Ctrl/⌘ to zoom to it (user decision: zooming replaces selecting
 * subsets).
 */
final class IntensityMapZoomBox {

  // a box of a few pixels on screen is a click, not a drag
  private static final double MIN_BOX_PIXELS = 3;
  private final Box box = new Box(1, 0.6, 1);
  private final Group model;
  private @Nullable Point3D start;
  private @Nullable Point3D end;
  private @NotNull IntensityMapTile tile = IntensityMapTile.IDENTITY;

  IntensityMapZoomBox(@NotNull final Group model) {
    this.model = model;
    box.setMaterial(new PhongMaterial(Color.rgb(59, 130, 246, 0.35)));
    box.setVisible(false);
  }

  @NotNull Box node() {
    return box;
  }

  boolean isActive() {
    return start != null;
  }

  /**
   * @return the tile the box is dragged on
   */
  @NotNull IntensityMapTile tile() {
    return tile;
  }

  /**
   * @param point local floor point in the tile, null outside the plot
   */
  void start(@NotNull final IntensityMapTile tile, @Nullable final Point3D point) {
    this.tile = tile;
    start = point;
    end = point;
  }

  /**
   * @param point local floor point in the tile of the start
   */
  void drag(@Nullable final Point3D point) {
    end = point;
  }

  /**
   * @param minimum smallest width, depth, and height, so the box stays visible
   * @param height  height of the box, above the data
   */
  void show(final double minimum, final double height) {
    if (start == null || end == null) {
      box.setVisible(false);
      return;
    }
    box.setWidth(Math.max(minimum, Math.abs(end.getX() - start.getX())));
    box.setDepth(Math.max(minimum, Math.abs(end.getZ() - start.getZ())));
    box.setTranslateX((end.getX() + start.getX()) / 2);
    box.setTranslateZ((end.getZ() + start.getZ()) / 2);
    box.setHeight(height);
    box.setTranslateY(-height / 2);
    box.setVisible(true);
  }

  void cancel() {
    start = null;
    end = null;
    box.setVisible(false);
  }

  /**
   * Ends the drag.
   *
   * @return the corners of the dragged box in model coordinates, null if it was a click
   */
  @Nullable List<Point3D> finish() {
    final Point3D first = start;
    final Point3D last = end;
    cancel();
    if (first == null || last == null
        || screenDistance(first, new Point3D(last.getX(), 0, first.getZ())) < MIN_BOX_PIXELS
        || screenDistance(first, new Point3D(first.getX(), 0, last.getZ())) < MIN_BOX_PIXELS) {
      return null;
    }
    final List<Point3D> corners = new ArrayList<>();
    for (final double x : new double[]{first.getX(), last.getX()}) {
      for (final double z : new double[]{first.getZ(), last.getZ()}) {
        corners.add(tile.toModel(new Point3D(x, 0, z)));
      }
    }
    return corners;
  }

  /**
   * @return screen distance of two local points of the tile
   */
  private double screenDistance(@NotNull final Point3D a, @NotNull final Point3D b) {
    final Point3D sa = model.localToScene(tile.toModel(a), true);
    final Point3D sb = model.localToScene(tile.toModel(b), true);
    return sa == null || sb == null ? 0 : Math.hypot(sa.getX() - sb.getX(), sa.getY() - sb.getY());
  }
}
