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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import org.jetbrains.annotations.NotNull;

/**
 * Screen-space outlines of projected tiles, used to place labels only where they cover nothing.
 */
public final class IntensityMapScreenGeometry {

  private IntensityMapScreenGeometry() {
  }

  /**
   * @return counter-clockwise convex hull (monotone chain)
   */
  public static @NotNull List<Point2D> convexHull(@NotNull final List<Point2D> points) {
    final List<Point2D> sorted = new ArrayList<>(points);
    sorted.sort(Comparator.comparingDouble(Point2D::getX).thenComparingDouble(Point2D::getY));
    if (sorted.size() < 3) {
      return sorted;
    }
    final List<Point2D> hull = new ArrayList<>();
    for (int pass = 0; pass < 2; pass++) {
      final int start = hull.size();
      for (final Point2D point : sorted) {
        while (hull.size() >= start + 2
            && cross(hull.get(hull.size() - 2), hull.getLast(), point) <= 0) {
          hull.removeLast();
        }
        hull.add(point);
      }
      hull.removeLast();
      sorted.sort(Comparator.comparingDouble(Point2D::getX).thenComparingDouble(Point2D::getY)
          .reversed());
    }
    return hull;
  }

  private static double cross(@NotNull final Point2D o, @NotNull final Point2D a,
      @NotNull final Point2D b) {
    return (a.getX() - o.getX()) * (b.getY() - o.getY())
        - (a.getY() - o.getY()) * (b.getX() - o.getX());
  }

  public static @NotNull Bounds bounds(@NotNull final List<Point2D> hull) {
    double minX = Double.MAX_VALUE;
    double minY = Double.MAX_VALUE;
    double maxX = -Double.MAX_VALUE;
    double maxY = -Double.MAX_VALUE;
    for (final Point2D point : hull) {
      minX = Math.min(minX, point.getX());
      minY = Math.min(minY, point.getY());
      maxX = Math.max(maxX, point.getX());
      maxY = Math.max(maxY, point.getY());
    }
    return new BoundingBox(minX, minY, Math.max(0, maxX - minX), Math.max(0, maxY - minY));
  }

  /**
   * Separating axis test of a rectangle and a convex polygon.
   */
  public static boolean intersects(@NotNull final Bounds rect, @NotNull final List<Point2D> hull) {
    if (hull.isEmpty() || !rect.intersects(bounds(hull))) {
      return false;
    }
    if (hull.size() < 3) {
      return true;
    }
    final Point2D[] corners = {new Point2D(rect.getMinX(), rect.getMinY()),
        new Point2D(rect.getMaxX(), rect.getMinY()), new Point2D(rect.getMaxX(), rect.getMaxY()),
        new Point2D(rect.getMinX(), rect.getMaxY())};
    for (int i = 0; i < hull.size(); i++) {
      final Point2D a = hull.get(i);
      final Point2D b = hull.get((i + 1) % hull.size());
      // outward normal of a counter-clockwise edge in y-down screen coordinates
      final double nx = b.getY() - a.getY();
      final double ny = a.getX() - b.getX();
      final double edge = nx * a.getX() + ny * a.getY();
      boolean separated = true;
      for (final Point2D corner : corners) {
        if (nx * corner.getX() + ny * corner.getY() < edge) {
          separated = false;
          break;
        }
      }
      if (separated) {
        return false;
      }
    }
    return true;
  }
}
