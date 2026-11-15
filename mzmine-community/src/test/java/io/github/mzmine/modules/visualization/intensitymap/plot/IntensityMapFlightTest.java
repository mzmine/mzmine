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
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.geometry.Point3D;
import org.junit.jupiter.api.Test;

class IntensityMapFlightTest {

  private static final double DEPTH = 0;
  private static final double HEIGHT_PER_DISTANCE = 0.5;

  @Test
  void flightStartsAndEndsAtTheViews() {
    final Point3D start = new Point3D(0, 0, -1000);
    final Point3D end = new Point3D(300, -50, -40);
    final IntensityMapFlight flight = new IntensityMapFlight(start, end, DEPTH,
        HEIGHT_PER_DISTANCE);
    assertEquals(start, flight.at(0));
    assertEquals(end, flight.at(1));
    assertClose(end, flight.at(1 - 1e-9));
    assertClose(start, flight.at(1e-9));
  }

  @Test
  void pureZoomChangesTheDistanceExponentially() {
    final IntensityMapFlight flight = new IntensityMapFlight(new Point3D(10, 20, -1000),
        new Point3D(10, 20, -10), DEPTH, HEIGHT_PER_DISTANCE);
    final Point3D middle = flight.at(0.5);
    assertEquals(-100, middle.getZ(), 1e-6);
    assertEquals(10, middle.getX(), 1e-9);
  }

  @Test
  void panningPullsTheCameraBack() {
    final IntensityMapFlight flight = new IntensityMapFlight(new Point3D(0, 0, -100),
        new Point3D(1000, 0, -100), DEPTH, HEIGHT_PER_DISTANCE);
    final Point3D middle = flight.at(0.5);
    assertTrue(middle.getZ() < -100, "camera pulls back, z " + middle.getZ());
    assertEquals(500, middle.getX(), 1e-6);
  }

  @Test
  void cameraMovesSteadilyTowardsTheTarget() {
    final IntensityMapFlight flight = new IntensityMapFlight(new Point3D(0, 0, -1000),
        new Point3D(400, 100, -20), DEPTH, HEIGHT_PER_DISTANCE);
    double previous = -1;
    for (int i = 0; i <= 50; i++) {
      final double x = flight.at(i / 50d).getX();
      assertTrue(x >= previous, "x " + x + " after " + previous);
      previous = x;
    }
    assertTrue(flight.durationMillis() >= 700 && flight.durationMillis() <= 1800);
  }

  @Test
  void targetsBehindTheCameraMoveLinearly() {
    final IntensityMapFlight flight = new IntensityMapFlight(new Point3D(0, 0, 10),
        new Point3D(100, 0, -10), DEPTH, HEIGHT_PER_DISTANCE);
    assertClose(new Point3D(50, 0, 0), flight.at(0.5));
  }

  private static void assertClose(final Point3D expected, final Point3D actual) {
    assertTrue(expected.distance(actual) < 1e-3, expected + " != " + actual);
  }
}
