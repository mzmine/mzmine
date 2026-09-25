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

package io.github.mzmine.modules.visualization.surface3d.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.modules.visualization.surface3d.data.Surface3DBounds;
import io.github.mzmine.modules.visualization.surface3d.data.Surface3DData;
import io.github.mzmine.modules.visualization.surface3d.data.Surface3DSeries;
import java.util.List;
import javafx.geometry.Point3D;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

/**
 * Ray picking, side by side tiles, and screen outlines.
 */
class Surface3DPickerTest {

  @Test
  void rayMarchingHitsThePeakBeforeTheFloor() {
    final Surface3DData data = new Surface3DData(21, 21, "X", "Y", 0, 20, 0, 20);
    for (int x = 0; x < 21; x++) {
      data.markColumn(x);
    }
    data.addMaximum(10, 10, 100);
    final Surface3DScale scale = new Surface3DScale(Surface3DBounds.of(List.of(data)), false,
        false);
    final Surface3DPicker.Target target = new Surface3DPicker.Target(
        new Surface3DSeries("peak", data, Color.RED), scale);
    // straight down onto the peak center
    final Surface3DPicker.Hit peak = Surface3DPicker.pick(
        new Surface3DPicker.Ray(new Point3D(0, -1000, 0), new Point3D(0, 1, 0)), List.of(target));
    assertNotNull(peak);
    assertSame(target, peak.target());
    assertEquals(-Surface3DMesh.HEIGHT, peak.y(), 1);
    // far from the peak only the floor is hit
    final Surface3DPicker.Hit floor = Surface3DPicker.pick(
        new Surface3DPicker.Ray(new Point3D(200, -1000, 150), new Point3D(0, 1, 0)),
        List.of(target));
    assertNotNull(floor);
    assertNull(floor.target());
    assertEquals(20, Surface3DPicker.dataX(scale.bounds(), Surface3DMesh.WIDTH / 2), 1e-9);
    // rays missing the plot entirely
    assertNull(Surface3DPicker.pick(
        new Surface3DPicker.Ray(new Point3D(2000, -1000, 0), new Point3D(0, 1, 0)),
        List.of(target)));
  }

  @Test
  void tilesKeepTheRayParameter() {
    final List<Surface3DTile> tiles = Surface3DTile.grid(5);
    assertEquals(5, tiles.size());
    assertEquals(3, Surface3DTile.columns(5, 0, 0));
    assertEquals(4, Surface3DTile.columns(9, 4, 0));
    assertEquals(3, Surface3DTile.columns(9, 0, 3));
    assertEquals(9, Surface3DTile.grid(9, 4, 0).size());
    final Surface3DTile tile = tiles.get(4);
    final Surface3DPicker.Ray ray = new Surface3DPicker.Ray(new Point3D(10, -500, 20),
        new Point3D(0.1, 1, 0.05));
    final Surface3DPicker.Ray local = tile.toLocal(ray);
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
    final List<javafx.geometry.Point2D> hull = Surface3DScreenGeometry.convexHull(List.of(
        new javafx.geometry.Point2D(10, 0), new javafx.geometry.Point2D(20, 10),
        new javafx.geometry.Point2D(10, 20), new javafx.geometry.Point2D(0, 10),
        new javafx.geometry.Point2D(10, 10)));
    assertEquals(4, hull.size());
    assertTrue(Surface3DScreenGeometry.intersects(new javafx.geometry.BoundingBox(8, 8, 4, 4),
        hull));
    // inside the bounding box corner, outside the diamond
    assertTrue(!Surface3DScreenGeometry.intersects(new javafx.geometry.BoundingBox(0, 0, 3, 3),
        hull));
    assertTrue(!Surface3DScreenGeometry.intersects(new javafx.geometry.BoundingBox(30, 0, 5, 5),
        hull));
  }
}
