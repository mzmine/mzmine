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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScaleTransform;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapBounds;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapGrid;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapProjection;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapSeries;
import java.util.List;
import javafx.geometry.Point3D;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

/**
 * Ray picking, side by side tiles, and screen outlines.
 */
class IntensityMapPickerTest {

  @Test
  void rayMarchingHitsThePeakBeforeTheFloor() {
    final IntensityMapGrid data = new IntensityMapGrid(21, 21, "X", "Y", 0, 20, 0, 20);
    for (int x = 0; x < 21; x++) {
      data.markColumn(x);
    }
    data.addMaximum(10, 10, 100);
    final IntensityMapScale scale = new IntensityMapScale(IntensityMapBounds.of(List.of(data)),
        false, false);
    final IntensityMapPicker.Target target = new IntensityMapPicker.Target(
        new IntensityMapSeries("peak", data, Color.RED), scale);
    // straight down onto the peak center
    final IntensityMapPicker.Hit peak = IntensityMapPicker.pick(
        new IntensityMapPicker.Ray(new Point3D(0, -1000, 0), new Point3D(0, 1, 0)),
        List.of(target));
    assertNotNull(peak);
    assertSame(target, peak.target());
    assertEquals(-IntensityMapMesh.HEIGHT, peak.y(), 1);
    // far from the peak only the floor is hit
    final IntensityMapPicker.Hit floor = IntensityMapPicker.pick(
        new IntensityMapPicker.Ray(new Point3D(200, -1000, 150), new Point3D(0, 1, 0)),
        List.of(target));
    assertNotNull(floor);
    assertNull(floor.target());
    assertEquals(20, IntensityMapPicker.dataX(scale.bounds(), IntensityMapMesh.WIDTH / 2), 1e-9);
    // rays missing the plot entirely
    assertNull(IntensityMapPicker.pick(
        new IntensityMapPicker.Ray(new Point3D(2000, -1000, 0), new Point3D(0, 1, 0)),
        List.of(target)));
  }

  @Test
  void flatPickingFindsTheTopOverlayWithSignal() {
    final IntensityMapGrid lower = new IntensityMapGrid(21, 21, "X", "Y", 0, 20, 0, 20);
    final IntensityMapGrid upper = new IntensityMapGrid(21, 21, "X", "Y", 0, 20, 0, 20);
    for (int x = 0; x < 21; x++) {
      lower.markColumn(x);
      upper.markColumn(x);
    }
    lower.addMaximum(10, 10, 100);
    lower.addMaximum(0, 0, 100);
    upper.addMaximum(10, 10, 5);
    final IntensityMapScale scale = new IntensityMapScale(
        IntensityMapBounds.of(List.of(lower, upper)), PaintScaleTransform.LINEAR, false, 0, 0,
        IntensityMapProjection.TOP_VIEW);
    final IntensityMapPicker.Target first = new IntensityMapPicker.Target(
        new IntensityMapSeries("lower", lower, Color.RED), scale);
    final IntensityMapPicker.Target last = new IntensityMapPicker.Target(
        new IntensityMapSeries("upper", upper, Color.BLUE), scale);
    // the overlay drawn last covers the others where it has signal
    final IntensityMapPicker.Hit center = IntensityMapPicker.pick(
        new IntensityMapPicker.Ray(new Point3D(0, -1000, 0), new Point3D(0, 1, 0)),
        List.of(first, last));
    assertNotNull(center);
    assertSame(last, center.target());
    assertEquals(0, center.y());
    // only the first overlay has signal in the corner
    final IntensityMapPicker.Hit corner = IntensityMapPicker.pick(new IntensityMapPicker.Ray(
        new Point3D(-IntensityMapMesh.WIDTH / 2 + 1, -1000, -IntensityMapMesh.DEPTH / 2 + 1),
        new Point3D(0, 1, 0)), List.of(first, last));
    assertNotNull(corner);
    assertSame(first, corner.target());
    // no signal: the floor
    final IntensityMapPicker.Hit empty = IntensityMapPicker.pick(
        new IntensityMapPicker.Ray(new Point3D(100, -1000, 100), new Point3D(0, 1, 0)),
        List.of(first, last));
    assertNotNull(empty);
    assertNull(empty.target());
  }

  @Test
  void tilesKeepTheRayParameter() {
    final List<IntensityMapTile> tiles = IntensityMapTile.grid(5);
    assertEquals(5, tiles.size());
    assertEquals(3, IntensityMapTile.columns(5, 0, 0));
    assertEquals(4, IntensityMapTile.columns(9, 4, 0));
    assertEquals(3, IntensityMapTile.columns(9, 0, 3));
    assertEquals(9, IntensityMapTile.grid(9, 4, 0).size());
    final IntensityMapTile tile = tiles.get(4);
    final IntensityMapPicker.Ray ray = new IntensityMapPicker.Ray(new Point3D(10, -500, 20),
        new Point3D(0.1, 1, 0.05));
    final IntensityMapPicker.Ray local = tile.toLocal(ray);
    final double t = 123;
    final Point3D model = ray.at(t);
    final Point3D back = tile.toModel(local.at(t));
    assertEquals(model.getX(), back.getX(), 1e-9);
    assertEquals(model.getY(), back.getY(), 1e-9);
    assertEquals(model.getZ(), back.getZ(), 1e-9);
  }

  @Test
  void hullOverlapUsesTheOutlineNotTheBoundingBox() {
    // diamond around (10, 10)
    final List<javafx.geometry.Point2D> hull = IntensityMapScreenGeometry.convexHull(
        List.of(new javafx.geometry.Point2D(10, 0), new javafx.geometry.Point2D(20, 10),
            new javafx.geometry.Point2D(10, 20), new javafx.geometry.Point2D(0, 10),
            new javafx.geometry.Point2D(10, 10)));
    assertEquals(4, hull.size());
    assertTrue(
        IntensityMapScreenGeometry.intersects(new javafx.geometry.BoundingBox(8, 8, 4, 4), hull));
    // inside the bounding box corner, outside the diamond
    assertTrue(
        !IntensityMapScreenGeometry.intersects(new javafx.geometry.BoundingBox(0, 0, 3, 3), hull));
    assertTrue(
        !IntensityMapScreenGeometry.intersects(new javafx.geometry.BoundingBox(30, 0, 5, 5), hull));
  }
}
