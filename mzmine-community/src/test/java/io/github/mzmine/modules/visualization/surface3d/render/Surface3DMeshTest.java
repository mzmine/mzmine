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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScaleTransform;
import io.github.mzmine.modules.visualization.surface3d.data.Surface3DBounds;
import io.github.mzmine.modules.visualization.surface3d.data.Surface3DData;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Mesh geometry and intensity scales.
 */
class Surface3DMeshTest {

  @Test
  void overlaysUsePhysicalPositionsAndSharedIntensity() {
    final Surface3DData a = new Surface3DData(new double[]{1, 2}, new double[]{100, 101}, "RT",
        "m/z", false);
    final Surface3DData b = new Surface3DData(new double[]{2, 4}, new double[]{100, 101}, "RT",
        "m/z", false);
    for (int x = 0; x < 2; x++) {
      a.markColumn(x);
      b.markColumn(x);
      a.addMaximum(x, 0, 10);
      b.addMaximum(x, 0, 20);
    }
    final Surface3DBounds bounds = Surface3DBounds.of(List.of(a, b));
    final Surface3DMesh ma = Surface3DMesh.build(a, bounds, false);
    final Surface3DMesh mb = Surface3DMesh.build(b, bounds, false);
    assertEquals(ma.points()[3], mb.points()[0]); // same RT has exactly the same position
    assertEquals(ma.points()[1] * 2, mb.points()[1]); // absolute intensity ratio retained
    assertEquals(ma.points().length, ma.normals().length); // precomputed smooth normals
    assertNotEquals(ma.texture()[0], ma.texture()[4]); // vertex colors retain the peak
  }

  @Test
  void missingImagePixelsProduceNoGeometry() {
    final Surface3DData data = new Surface3DData(new double[]{1, 2}, new double[]{1, 2}, "X", "Y",
        true);
    data.addMaximum(0, 0, 5);
    final Surface3DMesh mesh = Surface3DMesh.build(data, Surface3DBounds.of(List.of(data)), false);
    assertEquals(8 * 3, mesh.points().length); // one pixel column, not a bridged rectangle
    assertEquals(10 * 9, mesh.faces().length);
  }

  @Test
  void hiddenWallsBetweenEqualPixelsAreSkipped() {
    final Surface3DData data = new Surface3DData(new double[]{1, 2}, new double[]{1}, "X", "Y",
        true);
    data.addMaximum(0, 0, 5);
    data.addMaximum(1, 0, 5);
    final Surface3DMesh mesh = Surface3DMesh.build(data, Surface3DBounds.of(List.of(data)), false);
    // two tops plus three exposed walls per column, the shared wall is hidden
    assertEquals(2 * (2 + 3 * 2), mesh.triangles());
  }

  @Test
  void surfaceKeepsOnlyReferencedVertices() {
    final Surface3DData data = new Surface3DData(4, 4, "RT", "m/z", 0, 3, 0, 3);
    for (int x = 0; x < 4; x++) {
      data.markColumn(x);
    }
    data.addMaximum(0, 0, 10);
    final Surface3DMesh mesh = Surface3DMesh.build(data, Surface3DBounds.of(List.of(data)), false);
    assertEquals(4 * 3, mesh.points().length);
    assertEquals(2, mesh.triangles());
    // normals point upwards (negative y)
    for (int i = 1; i < mesh.normals().length; i += 3) {
      assertTrue(mesh.normals()[i] < 0);
    }
  }

  @Test
  void downsampledPixelsTileWithoutGaps() {
    // 10 x 10 pixels, 10 µm apart, flat intensity
    final Surface3DData data = new Surface3DData(Surface3DData.coordinates(10, 0, 90),
        Surface3DData.coordinates(10, 0, 90), "X", "Y", true);
    data.setPixelSize(10, 10);
    for (int x = 0; x < 10; x++) {
      for (int y = 0; y < 10; y++) {
        data.addMaximum(x, y, 5);
      }
    }
    final Surface3DData small = data.downsample(4, 4);
    assertEquals(4, small.width());
    assertEquals(30, small.pixelWidth(), 1e-9);
    for (int i = 1; i < small.width(); i++) {
      // neighboring blocks touch, spacing equals the block size
      assertEquals(small.pixelWidth(), small.xValue(i) - small.xValue(i - 1), 1e-9);
    }
    // flat blocks: tops plus outer walls only, no walls along inner seams
    final Surface3DMesh mesh = Surface3DMesh.build(small,
        Surface3DBounds.of(List.of(small)), false);
    assertEquals(16 * 2 + 16 * 2, mesh.triangles());
  }

  @Test
  void noiseFloorRemovesQuietGeometry() {
    final Surface3DData data = new Surface3DData(3, 2, "RT", "m/z", 0, 2, 0, 1);
    for (int x = 0; x < 3; x++) {
      data.markColumn(x);
      data.addMaximum(x, 0, 1);
      data.addMaximum(x, 1, 1);
    }
    data.addMaximum(2, 1, 1000);
    final Surface3DBounds bounds = Surface3DBounds.of(List.of(data));
    final int all = Surface3DMesh.build(data, new Surface3DScale(bounds, false, false),
        () -> false).triangles();
    final int filtered = Surface3DMesh.build(data,
        new Surface3DScale(bounds, false, false, 0.01), () -> false).triangles();
    assertEquals(4, all);
    assertEquals(2, filtered);
    assertTrue(new Surface3DScale(bounds, false, false, 0.01).belowNoise(data, 5));
  }

  @Test
  void transformsMapZeroAndMaximumToTheAxisEnds() {
    for (final PaintScaleTransform transform : PaintScaleTransform.values()) {
      assertEquals(0, Surface3DScale.position(transform, 0, 1000), 1e-12);
      assertEquals(1, Surface3DScale.position(transform, 1000, 1000), 1e-12);
    }
    assertEquals(0.5, Surface3DScale.position(PaintScaleTransform.LINEAR, 500, 1000), 1e-12);
    // logarithmic transformations lift small values, log bases are equivalent
    final double log10 = Surface3DScale.position(PaintScaleTransform.LOG10, 10, 1000);
    assertEquals(Math.log1p(10) / Math.log1p(1000), log10, 1e-12);
    assertEquals(log10, Surface3DScale.position(PaintScaleTransform.LOG2, 10, 1000), 1e-12);
    assertTrue(Surface3DScale.position(PaintScaleTransform.SQRT, 10, 1000) > 0.01);
  }

  @Test
  void normalizedScaleUsesEachOverlayMaximum() {
    final Surface3DData a = new Surface3DData(2, 2, "X", "Y", 0, 1, 0, 1);
    final Surface3DData b = new Surface3DData(2, 2, "X", "Y", 0, 1, 0, 1);
    a.addMaximum(0, 0, 10);
    b.addMaximum(0, 0, 1000);
    final Surface3DBounds bounds = Surface3DBounds.of(List.of(a, b));
    assertEquals(0.01, new Surface3DScale(bounds, false, false).height(a, 10), 1e-12);
    assertEquals(1, new Surface3DScale(bounds, false, true).height(a, 10), 1e-12);
    assertTrue(new Surface3DScale(bounds, false, true).sameGeometry(
        new Surface3DScale(Surface3DBounds.of(List.of(a)), false, true), a));
  }

  @Test
  void baselineStartsHeightsAtTheLowestPixel() {
    final Surface3DData image = new Surface3DData(Surface3DData.coordinates(3, 0, 20),
        Surface3DData.coordinates(1, 0, 0), "X", "Y", true);
    image.addMaximum(0, 0, 1e4);
    image.addMaximum(1, 0, 1e5);
    image.addMaximum(2, 0, 1e6);
    assertEquals(1e4, image.minimum());
    final Surface3DBounds bounds = Surface3DBounds.of(List.of(image));
    final Surface3DScale fromZero = new Surface3DScale(bounds, PaintScaleTransform.LOG10, false,
        0, 0);
    final Surface3DScale fromLowest = new Surface3DScale(bounds, PaintScaleTransform.LOG10, false,
        0, image.minimum() / image.maximum());
    // log heights put the weakest pixel at two thirds of the maximum
    assertEquals(4 / 6d, fromZero.height(image, 1e4), 1e-3);
    assertEquals(Surface3DScale.BASE_HEIGHT, fromLowest.height(image, 1e4), 1e-9);
    assertEquals(1, fromLowest.height(image, 1e6), 1e-9);
    assertEquals(0.51, fromLowest.height(image, 1e5), 0.01);
    assertEquals(0, fromLowest.height(image, 0));
  }
}
