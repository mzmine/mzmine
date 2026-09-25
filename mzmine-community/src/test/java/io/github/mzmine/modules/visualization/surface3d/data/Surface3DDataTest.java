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

package io.github.mzmine.modules.visualization.surface3d.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Grid data: coordinates, binning, transposition, downsampling, and smoothing.
 */
class Surface3DDataTest {

  @Test
  void retainsIrregularAcquisitionCoordinates() {
    final Surface3DData data = new Surface3DData(new double[]{1, 1.1, 5}, new double[]{100, 200},
        "RT", "m/z", false);
    assertEquals(1.1, data.xValue(1));
    assertEquals(1, data.binX(1.15));
    assertEquals(2, data.binX(5));
    assertEquals(-1, data.binX(5.1));
    data.addMaximum(1, 1, 3);
    data.addMaximum(1, 1, 8);
    data.addMaximum(1, 1, 4);
    assertEquals(8, data.intensity(1, 1));
  }

  @Test
  void distinguishesMissingImagingPixelsFromMeasuredZero() {
    final Surface3DData data = new Surface3DData(3, 3, "X", "Y", 0, 2, 0, 2);
    assertTrue(data.isEmpty());
    data.addSum(1, 1, 0);
    assertFalse(data.isEmpty());
    assertTrue(data.isPresent(1, 1));
    assertFalse(data.isPresent(0, 1));
  }

  @Test
  void detailGrowsWithViewportAndHasNo300Limit() {
    final var small = new Surface3DDetail(500, 400, 1).grid(2000, 2000, false,
        256L * 1024 * 1024);
    final var large = new Surface3DDetail(1000, 700, 1).grid(2000, 2000, false,
        256L * 1024 * 1024);
    assertTrue(large.y() > 300);
    assertTrue(large.x() > small.x());
    assertEquals(20, new Surface3DDetail(1000, 700, 1).grid(20, 10000, false,
        256L * 1024 * 1024).x());
  }

  @Test
  void flatDetailHasNoBinsFinerThanScreenPixels() {
    // 20000 useful m/z bins, a 1000 x 700 pixel view
    final var surface = new Surface3DDetail(1000, 700, 1).grid(200, 20000, false,
        256L * 1024 * 1024);
    final var flat = new Surface3DDetail(1000, 700, 1, true).grid(200, 20000, false,
        256L * 1024 * 1024);
    assertTrue(surface.y() > 700);
    assertTrue(flat.y() <= 700);
  }

  @Test
  void mergedWindowReplacesBaseCellsAndKeepsTheRange() {
    // coarse base 0..100 and a fine window 40..60
    final Surface3DData base = new Surface3DData(Surface3DData.coordinates(11, 0, 100),
        Surface3DData.coordinates(11, 0, 100), "X", "Y", false);
    final Surface3DData window = new Surface3DData(Surface3DData.coordinates(21, 40, 60),
        Surface3DData.coordinates(21, 40, 60), "X", "Y", false);
    for (int x = 0; x < 11; x++) {
      for (int y = 0; y < 11; y++) {
        base.addMaximum(x, y, 1);
      }
    }
    for (int x = 0; x < 21; x++) {
      for (int y = 0; y < 21; y++) {
        window.addMaximum(x, y, 2);
      }
    }
    final Surface3DData merged = Surface3DData.merge(base, window);
    assertEquals(0, merged.xMin(), 1e-9);
    assertEquals(100, merged.xMax(), 1e-9);
    // cells tile the axis without gaps or overlaps
    for (int i = 1; i < merged.width(); i++) {
      assertEquals(merged.xHigh(i - 1), merged.xLow(i), 1e-9);
    }
    assertEquals(2, merged.intensity(merged.binX(50), merged.binY(50)));
    assertEquals(1, merged.intensity(merged.binX(10), merged.binY(50)));
    assertEquals(1, merged.intensity(merged.binX(50), merged.binY(90)));
  }

  @Test
  void mergedPixelBlocksAreClippedAtTheWindow() {
    // 20 native pixels of 10, merged to blocks of 4 in the base, native in the window 80..120
    final Surface3DData base = new Surface3DData(
        Surface3DData.blockCoordinates(Surface3DData.coordinates(20, 0, 190), 4, 10),
        new double[]{0}, "X", "Y", true);
    base.setPixelSize(40, 10);
    final Surface3DData window = new Surface3DData(Surface3DData.coordinates(5, 80, 120),
        new double[]{0}, "X", "Y", true);
    window.setPixelSize(10, 10);
    for (int x = 0; x < base.width(); x++) {
      base.addMaximum(x, 0, 1);
    }
    for (int x = 0; x < window.width(); x++) {
      window.addMaximum(x, 0, 2);
    }
    final Surface3DData merged = Surface3DData.merge(base, window);
    assertEquals(base.xLow(0), merged.xLow(0), 1e-9);
    assertEquals(base.xHigh(base.width() - 1), merged.xHigh(merged.width() - 1), 1e-9);
    for (int i = 1; i < merged.width(); i++) {
      assertTrue(merged.xLow(i) >= merged.xHigh(i - 1) - 1e-9);
    }
    assertEquals(2, merged.intensity(merged.binX(100), 0));
    assertEquals(1, merged.intensity(merged.binX(20), 0));
    // the window border cuts a base block, which keeps its part outside
    assertEquals(1, merged.intensity(merged.binX(71), 0));
  }

  @Test
  void transposeSwapsAxesAndValues() {
    final Surface3DData data = new Surface3DData(new double[]{1, 2, 3}, new double[]{10, 20},
        "Mobility", "m/z", false);
    data.addMaximum(2, 1, 7);
    final Surface3DData swapped = data.transpose();
    assertEquals("m/z", swapped.xLabel());
    assertEquals("Mobility", swapped.yLabel());
    assertEquals(2, swapped.width());
    assertEquals(3, swapped.height());
    assertEquals(7, swapped.intensity(1, 2));
    assertTrue(swapped.isPresent(1, 2));
    assertFalse(swapped.isPresent(0, 2));
  }

  @Test
  void downsampleRetainsMaxima() {
    final Surface3DData data = new Surface3DData(100, 100, "RT", "m/z", 0, 99, 0, 99);
    for (int x = 0; x < 100; x++) {
      data.markColumn(x);
    }
    data.addMaximum(37, 61, 1000);
    final Surface3DData small = data.downsample(10, 10);
    assertEquals(10, small.width());
    assertEquals(10, small.height());
    assertEquals(1000, small.maximum());
    assertEquals(1000, small.intensity(small.binX(37), small.binY(61)));
    assertTrue(small.isPresent(0, 0));
    assertTrue(data.downsample(200, 200) == data);
  }

  @Test
  void mergedPixelBlocksKeepEdgePixels() {
    final Surface3DData image = new Surface3DData(Surface3DData.coordinates(10, 0, 90),
        Surface3DData.coordinates(10, 0, 90), "X", "Y", true);
    image.setPixelSize(10, 10);
    image.addMaximum(0, 0, 5);
    image.addMaximum(9, 9, 7);
    // factor 2: block centers 5..85 lie inside the outermost pixels at 0 and 90
    final Surface3DData merged = image.downsample(5, 5);
    assertEquals(5, merged.width());
    assertEquals(5, merged.intensity(0, 0));
    assertEquals(7, merged.intensity(4, 4));
  }

  @Test
  void uniformAxesBinLikeIrregularAxes() {
    final Surface3DData uniform = new Surface3DData(11, 2, "RT", "m/z", 0, 10, 0, 1);
    final Surface3DData irregular = new Surface3DData(
        new double[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10.5}, new double[]{0, 1}, "RT", "m/z", false);
    for (final double value : new double[]{0, 0.49, 0.51, 4.5001, 9.9, 10}) {
      assertEquals(Math.round(value), uniform.binX(value));
    }
    assertEquals(-1, uniform.binX(10.01));
    assertEquals(-1, uniform.binX(Double.NaN));
    assertEquals(10, irregular.binX(10.4));
  }

  @Test
  void smoothingSpreadsSignalButKeepsMissingCells() {
    final Surface3DData data = new Surface3DData(new double[]{0, 1, 2, 3, 4},
        new double[]{0, 1, 2}, "X", "Y", true);
    for (int x = 0; x < 5; x++) {
      for (int y = 0; y < 3; y++) {
        if (x != 4 || y != 1) {
          data.addMaximum(x, y, 0);
        }
      }
    }
    data.addMaximum(2, 1, 100);
    final Surface3DData both = new Surface3DSmoothing(1, Surface3DSmoothing.Axes.BOTH).apply(data,
        () -> false);
    assertTrue(both.intensity(2, 1) < 100);
    assertTrue(both.intensity(1, 1) > 0);
    assertTrue(both.intensity(2, 0) > 0);
    // missing cells stay missing and do not count as zero
    assertTrue(!both.isPresent(4, 1));
    final Surface3DData alongX = new Surface3DSmoothing(1, Surface3DSmoothing.Axes.X).apply(data,
        () -> false);
    assertTrue(alongX.intensity(1, 1) > 0);
    assertEquals(0, alongX.intensity(2, 0));
    assertTrue(alongX.pixels());
    assertTrue(Surface3DSmoothing.OFF.apply(data, () -> false) == data);
  }
}
