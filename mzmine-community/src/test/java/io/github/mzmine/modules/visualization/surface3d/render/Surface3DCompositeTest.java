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

import io.github.mzmine.modules.visualization.surface3d.data.Surface3DBounds;
import io.github.mzmine.modules.visualization.surface3d.data.Surface3DData;
import io.github.mzmine.modules.visualization.surface3d.data.Surface3DSeries;
import java.util.List;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

/**
 * Composite surfaces for color blending.
 */
class Surface3DCompositeTest {

  @Test
  void compositeKeepsMaximumAndItsOwner() {
    final Surface3DData a = data(new double[]{10, 1});
    final Surface3DData b = data(new double[]{5, 7});
    final List<Surface3DSeries> sources = List.of(new Surface3DSeries("a", a, Color.RED),
        new Surface3DSeries("b", b, Color.BLUE));
    final Surface3DScale scale = new Surface3DScale(Surface3DBounds.of(List.of(a, b)), false,
        false);
    final Surface3DComposite composite = Surface3DComposite.build(sources, scale, () -> false);
    final Surface3DData data = composite.data();
    assertEquals(10, data.intensity(0, 0));
    assertEquals(7, data.intensity(1, 0));
    assertEquals(0, composite.owners().cells()[0]);
    assertEquals(1, composite.owners().cells()[1]);
    // flat owner colors: one texture coordinate per overlay
    final Surface3DMesh mesh = Surface3DMesh.build(data, composite.scale(scale), () -> false,
        composite.owners());
    assertEquals(2 * 2, mesh.texture().length);
  }

  @Test
  void normalizedCompositeComparesRelativeIntensities() {
    final Surface3DData a = data(new double[]{100, 50});
    final Surface3DData b = data(new double[]{1, 2});
    final Surface3DScale scale = new Surface3DScale(Surface3DBounds.of(List.of(a, b)), false,
        true);
    final Surface3DComposite composite = Surface3DComposite.build(
        List.of(new Surface3DSeries("a", a, Color.RED), new Surface3DSeries("b", b, Color.BLUE)),
        scale, () -> false);
    // b reaches its own maximum in the second column, a only half of it
    assertEquals(1, composite.owners().cells()[1]);
    assertEquals(1, composite.data().intensity(1, 0), 1e-6);
  }

  @Test
  void blendFractionsFollowIntensityRatios() {
    final Surface3DData a = data(new double[]{30, 0});
    final Surface3DData b = data(new double[]{10, 5});
    final Surface3DScale scale = new Surface3DScale(Surface3DBounds.of(List.of(a, b)), false,
        false);
    final Surface3DComposite composite = Surface3DComposite.build(
        List.of(new Surface3DSeries("a", a, Color.RED), new Surface3DSeries("b", b, Color.BLUE)),
        scale, () -> false, true);
    final float[] mix = composite.mix();
    assertEquals(0.75, mix[0], 1e-6);
    assertEquals(0.25, mix[1], 1e-6);
    assertEquals(0, mix[2], 1e-6);
    assertEquals(1, mix[3], 1e-6);
    assertEquals(List.of(Color.MAGENTA, Color.LIME), Surface3DBlend.mixingColors(2));
    // additive mixing at full brightness: equal red and green give yellow
    final Color yellow = Surface3DBlend.mix(Color.RED, Color.LIME, Color.BLUE, 0.5, 0.5, 0);
    assertEquals(yellow.getRed(), yellow.getGreen(), 1e-9);
    assertEquals(0, yellow.getBlue(), 1e-9);
    final Surface3DMesh mesh = Surface3DMesh.build(composite.data(), composite.scale(scale),
        () -> false, composite.owners(), mix);
    assertEquals(Surface3DMesh.mixCoordinate(0.75f), mesh.texture()[0], 1e-6);
  }

  private static Surface3DData data(final double[] values) {
    final Surface3DData data = new Surface3DData(values.length, 1, "X", "Y", 0,
        values.length - 1, 0, 0);
    for (int i = 0; i < values.length; i++) {
      data.markColumn(i);
      data.addMaximum(i, 0, values[i]);
    }
    return data;
  }
}
