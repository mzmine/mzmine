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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Range;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapAxisKind;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapGrid;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapRegion;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapSeries;
import java.util.List;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

class IntensityMapLanesTest {

  @Test
  void separateWindowsGetStackedLanes() {
    final IntensityMapLanes lanes = IntensityMapLanes.of(
        List.of(overlay(876.79, 876.81), overlay(850.77, 850.79), overlay(876.79, 876.81)));

    assertNotNull(lanes);
    assertEquals(2, lanes.lanes().size());
    // lanes are sorted by m/z, the lowest at the bottom
    assertEquals(0, lanes.toLane(850.77), 1e-9);
    assertEquals(1 + IntensityMapLanes.HEIGHT / 2, lanes.toLane(876.80), 1e-9);
    assertEquals(876.80, lanes.toMz(1 + IntensityMapLanes.HEIGHT / 2), 1e-9);
    assertTrue(Double.isNaN(lanes.toLane(860)));
    assertTrue(Double.isNaN(lanes.toMz(0.95)));
    assertEquals(850.79, lanes.nearestMz(0.95), 1e-9);
  }

  @Test
  void oneContinuousRangeHasNoLanes() {
    assertNull(IntensityMapLanes.of(List.of(overlay(100, 900), overlay(850.77, 850.79))));
    assertNull(IntensityMapLanes.of(List.of(overlay(850.77, 850.79))));
  }

  @Test
  void dataMoveIntoTheirLane() {
    final IntensityMapGrid high = grid(876.79, 876.81);
    high.addMaximum(1, 2, 5);
    final IntensityMapLanes lanes = IntensityMapLanes.of(
        List.of(new IntensityMapSeries("h", high, Color.BLACK), overlay(850.77, 850.79)));
    assertNotNull(lanes);

    final IntensityMapGrid laned = lanes.toLanes(high);

    assertEquals(1, laned.yMin(), 1e-9);
    assertEquals(1 + IntensityMapLanes.HEIGHT, laned.yMax(), 1e-9);
    assertEquals(5, laned.intensity(1, 2));
  }

  @Test
  void visiblePartOfALaneIsReadInMz() {
    final IntensityMapLanes lanes = IntensityMapLanes.of(
        List.of(overlay(850.77, 850.79), overlay(876.79, 876.81)));
    assertNotNull(lanes);
    final IntensityMapGrid laned = lanes.toLanes(grid(876.79, 876.81));
    final IntensityMapOverlayView view = new IntensityMapOverlayView(
        new IntensityMapRegion(Range.closed(1d, 2d),
            Range.closed(0.5, 1 + IntensityMapLanes.HEIGHT)), false, 400, 300);

    final IntensityMapOverlayView inLane = lanes.toMz(view, laned);

    assertNotNull(inLane);
    assertEquals(876.79, inLane.region().y().lowerEndpoint(), 1e-9);
    assertEquals(876.81, inLane.region().y().upperEndpoint(), 1e-9);
    // the lane covers 0.85 of the 1.35 visible lane units
    assertEquals(300 * IntensityMapLanes.HEIGHT / 1.35, inLane.height(), 1e-6);
    assertNull(lanes.toMz(
        new IntensityMapOverlayView(new IntensityMapRegion(null, Range.closed(0.0, 0.5)), false,
            400, 300), laned));
  }

  private static @NotNull IntensityMapSeries overlay(final double low, final double high) {
    return new IntensityMapSeries("m/z " + low, grid(low, high), Color.BLACK);
  }

  private static @NotNull IntensityMapGrid grid(final double low, final double high) {
    final IntensityMapGrid grid = new IntensityMapGrid(3, 3, "RT", "m/z", 1, 2, low, high);
    grid.setAxisKinds(IntensityMapAxisKind.RETENTION_TIME, IntensityMapAxisKind.MZ);
    return grid;
  }
}
