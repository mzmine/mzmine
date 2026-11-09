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

import javafx.geometry.Point3D;
import org.jetbrains.annotations.NotNull;

/**
 * Camera path between two views: the camera pulls back while it pans and moves in on the target,
 * the smooth zooming and panning of van Wijk and Nuij (2003) known from map applications. The
 * view is described by its center and the visible height at the depth of the target; the path
 * minimizes the perceived motion between both views.
 */
final class IntensityMapFlight {

  // decision: the default of the paper and of d3, a balanced pull back
  private static final double RHO = Math.sqrt(2);
  private static final double MIN_MILLIS = 700;
  private static final double MAX_MILLIS = 1800;

  private final Point3D start;
  private final Point3D end;
  private final double depth;
  private final double heightPerDistance;
  private final double w0;
  private final double distance;
  private final double r0;
  private final double length;
  // the camera moves on a straight line if a smooth path is undefined
  private final boolean linear;
  // the target lies straight ahead, the camera only moves in or out
  private final boolean zoomOnly;

  /**
   * @param start             camera position at the start
   * @param end               camera position of the target view
   * @param depth             world depth of the target, e.g. the plane of the zoomed box
   * @param heightPerDistance visible height per camera distance, 2 tan(fov / 2)
   */
  IntensityMapFlight(@NotNull final Point3D start, @NotNull final Point3D end, final double depth,
      final double heightPerDistance) {
    this.start = start;
    this.end = end;
    this.depth = depth;
    this.heightPerDistance = heightPerDistance;
    w0 = (depth - start.getZ()) * heightPerDistance;
    final double w1 = (depth - end.getZ()) * heightPerDistance;
    distance = Math.hypot(end.getX() - start.getX(), end.getY() - start.getY());
    // assumption: both views look at the target from its front, otherwise zooming is undefined
    linear = !(w0 > 0) || !(w1 > 0) || !Double.isFinite(w0) || !Double.isFinite(w1);
    zoomOnly = !linear && distance < 1e-9 * Math.max(w0, w1);
    if (linear) {
      r0 = 0;
      length = 1;
    } else if (zoomOnly) {
      // pure zoom: the visible height changes exponentially
      r0 = 0;
      length = Math.log(w1 / w0) / RHO;
    } else {
      final double rho2 = RHO * RHO;
      final double rho4 = rho2 * rho2;
      final double d2 = distance * distance;
      final double b0 = (w1 * w1 - w0 * w0 + rho4 * d2) / (2 * w0 * rho2 * distance);
      final double b1 = (w1 * w1 - w0 * w0 - rho4 * d2) / (2 * w1 * rho2 * distance);
      r0 = Math.log(Math.sqrt(b0 * b0 + 1) - b0);
      final double r1 = Math.log(Math.sqrt(b1 * b1 + 1) - b1);
      length = (r1 - r0) / RHO;
    }
  }

  /**
   * @return duration of the flight, longer for longer paths
   */
  double durationMillis() {
    return linear ? MIN_MILLIS : Math.clamp(Math.abs(length) * 600, MIN_MILLIS, MAX_MILLIS);
  }

  /**
   * @param t progress in [0, 1]
   * @return camera position along the path
   */
  @NotNull Point3D at(final double t) {
    if (t <= 0) {
      return start;
    }
    if (t >= 1) {
      return end;
    }
    if (linear) {
      return start.add(end.subtract(start).multiply(t));
    }
    final double s = t * length;
    final double share;
    final double height;
    if (zoomOnly) {
      share = t;
      height = w0 * Math.exp(RHO * s);
    } else {
      final double coshR0 = Math.cosh(r0);
      share = w0 / (RHO * RHO * distance) * (coshR0 * Math.tanh(RHO * s + r0) - Math.sinh(r0));
      height = w0 * coshR0 / Math.cosh(RHO * s + r0);
    }
    return new Point3D(start.getX() + share * (end.getX() - start.getX()),
        start.getY() + share * (end.getY() - start.getY()), depth - height / heightPerDistance);
  }
}
