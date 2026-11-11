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

package io.github.mzmine.modules.visualization.intensitymap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapDetail;
import com.google.common.collect.Range;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapProjection;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapRegion;
import io.github.mzmine.modules.visualization.intensitymap.plot.IntensityMapOverlayView;
import org.junit.jupiter.api.Test;

class IntensityMapDetailLoaderTest {

  // dense data, the render budget limits the bins
  private static final int NATIVE = 100_000;
  // the memory of a typical desktop, independent of the test JVM
  private static final long MEMORY = 256L * 1024 * 1024;
  private static final double ENLARGED = 1 + 2 * IntensityMapDetailLoader.margin();

  @Test
  void flatZoomWindowsKeepOneBinPerPixel() {
    // a zoomed window must be as fine as the zoomed out view, one bin per pixel
    final IntensityMapDetail view = new IntensityMapDetail(900, 600, 1,
        IntensityMapProjection.TOP_VIEW);
    final IntensityMapDetail window = IntensityMapDetailLoader.windowDetail(view, overlay(900, 600),
        ENLARGED, ENLARGED, 1);
    final double margin = 1 + 2 * IntensityMapDetailLoader.margin();
    final var zoomedOut = view.grid(NATIVE, NATIVE, false, MEMORY);
    final var zoomed = window.grid(NATIVE, NATIVE, false, MEMORY);
    assertEquals(zoomedOut.x(), zoomed.x() / margin, 1);
    assertEquals(zoomedOut.y(), zoomed.y() / margin, 1);
  }

  @Test
  void flatZoomWindowsOfLargePlotsKeepOneBinPerPixel() {
    final IntensityMapDetail view = new IntensityMapDetail(1300, 800, 1,
        IntensityMapProjection.TOP_VIEW);
    final var zoomed = IntensityMapDetailLoader.windowDetail(view, overlay(1300, 800), ENLARGED,
        ENLARGED, 1).grid(NATIVE, NATIVE, false, MEMORY);
    final double margin = 1 + 2 * IntensityMapDetailLoader.margin();
    // flat cells only become geometry where data are measured, the budget counts bins
    assertEquals(1300, zoomed.x() / margin, 1);
    assertEquals(800, zoomed.y() / margin, 1);
  }

  @Test
  void flatWindowsFollowTheScreenSizeOfTheirTile() {
    // side by side, a tile covers only part of the plot area
    final IntensityMapDetail plotDetail = new IntensityMapDetail(1200, 800, 4,
        IntensityMapProjection.TOP_VIEW);
    final IntensityMapDetail window = IntensityMapDetailLoader.windowDetail(plotDetail,
        overlay(300, 200), ENLARGED, ENLARGED, 4);
    final double margin = 1 + 2 * IntensityMapDetailLoader.margin();
    assertEquals(300 * margin, window.width(), 1e-9);
    assertEquals(200 * margin, window.height(), 1e-9);
  }

  @Test
  void panningReadsAhead() {
    final IntensityMapRegion before = region(4.0, 4.5, 500, 600);
    // panned to later retention times
    final double[] right = IntensityMapDetailLoader.margins(before, region(4.2, 4.7, 500, 600));
    assertTrue(right[1] > right[0], "more margin ahead");
    assertEquals(right[2], right[3], 1e-12);
    // panned to lower m/z
    final double[] down = IntensityMapDetailLoader.margins(before, region(4.0, 4.5, 450, 550));
    assertTrue(down[2] > down[3], "more margin ahead");
    assertEquals(down[0], down[1], 1e-12);
  }

  @Test
  void standingStillKeepsEvenMargins() {
    final IntensityMapRegion view = region(4.0, 4.5, 500, 600);
    final double[] shares = IntensityMapDetailLoader.margins(view, view);
    assertEquals(IntensityMapDetailLoader.margin(), shares[0], 1e-12);
    assertEquals(shares[0], shares[1], 1e-12);
    assertEquals(shares[2], shares[3], 1e-12);
    assertEquals(IntensityMapDetailLoader.margin(), IntensityMapDetailLoader.margins(null, view)[1],
        1e-12);
  }

  private static IntensityMapRegion region(final double x0, final double x1, final double y0,
      final double y1) {
    return new IntensityMapRegion(Range.closed(x0, x1), Range.closed(y0, y1));
  }

  private static IntensityMapOverlayView overlay(final double width, final double height) {
    return new IntensityMapOverlayView(
        new IntensityMapRegion(Range.closed(1d, 2d), Range.closed(100d, 200d)), false, width,
        height);
  }

  @Test
  void surfacesKeepTheirVertexBudget() {
    final var surface = new IntensityMapDetail(1300, 800, 1,
        IntensityMapProjection.PERSPECTIVE).grid(NATIVE, NATIVE, false, MEMORY);
    assertTrue((long) surface.x() * surface.y() <= 1_000_000);
  }
}
