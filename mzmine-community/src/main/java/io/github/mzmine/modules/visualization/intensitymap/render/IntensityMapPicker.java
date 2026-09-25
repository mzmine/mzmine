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

import static io.github.mzmine.modules.visualization.intensitymap.render.IntensityMapMesh.DEPTH;
import static io.github.mzmine.modules.visualization.intensitymap.render.IntensityMapMesh.HEIGHT;
import static io.github.mzmine.modules.visualization.intensitymap.render.IntensityMapMesh.WIDTH;

import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapBounds;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapGrid;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapPerspective;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapSeries;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapTopView;
import java.util.List;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Analytic picking on the sampled height fields. JavaFX picking tests every triangle of a mesh on
 * each mouse event, which stalls rotation and hovering for large surfaces, so meshes are mouse
 * transparent and rays are marched through the data grids instead.
 */
public final class IntensityMapPicker {

  private static final int STEPS = 480;
  private static final int REFINEMENTS = 8;

  private IntensityMapPicker() {
  }

  /**
   * @param series visible series with their data
   */
  public record Target(@NotNull IntensityMapSeries series, @NotNull IntensityMapScale scale) {

  }

  /**
   * @param x      local model coordinate
   * @param y      local model coordinate, negative above the floor
   * @param z      local model coordinate
   * @param target the surface that was hit, null for the floor
   * @param t      ray parameter of the hit, compares distances of hits along one ray
   */
  public record Hit(double x, double y, double z, @Nullable Target target, double t) {

  }

  public record Ray(@NotNull Point3D origin, @NotNull Point3D direction) {

    public @NotNull Point3D at(final double t) {
      return origin.add(direction.multiply(t));
    }
  }

  /**
   * @param sceneX x in the sub scene coordinate system
   * @param sceneY y in the sub scene coordinate system
   * @return the ray in local coordinates of the model node
   */
  public static @NotNull Ray ray(@NotNull final PerspectiveCamera camera, final double width,
      final double height, final double sceneX, final double sceneY, @NotNull final Node model) {
    final double tangent = Math.tan(Math.toRadians(camera.getFieldOfView() / 2));
    final double aspect = width / height;
    final double ndcX = 2 * sceneX / width - 1;
    final double ndcY = 2 * sceneY / height - 1;
    final double dx = camera.isVerticalFieldOfView() ? ndcX * tangent * aspect : ndcX * tangent;
    final double dy = camera.isVerticalFieldOfView() ? ndcY * tangent : ndcY * tangent / aspect;
    // the camera is translated but never rotated
    final Point3D origin = new Point3D(camera.getTranslateX(), camera.getTranslateY(),
        camera.getTranslateZ());
    final Point3D localOrigin = model.parentToLocal(origin);
    final Point3D localTarget = model.parentToLocal(origin.add(dx, dy, 1));
    return new Ray(localOrigin, localTarget.subtract(localOrigin));
  }

  /**
   * @return the intersection with the floor plane inside the plot area
   */
  public static @Nullable Point3D floor(@NotNull final Ray ray, final boolean clampToPlot) {
    if (Math.abs(ray.direction().getY()) < 1e-12) {
      return null;
    }
    final double t = -ray.origin().getY() / ray.direction().getY();
    if (t <= 0) {
      return null;
    }
    final Point3D point = ray.at(t);
    if (clampToPlot) {
      return new Point3D(Math.clamp(point.getX(), -WIDTH / 2, WIDTH / 2), 0,
          Math.clamp(point.getZ(), -DEPTH / 2, DEPTH / 2));
    }
    return Math.abs(point.getX()) <= WIDTH / 2 && Math.abs(point.getZ()) <= DEPTH / 2 ? point
        : null;
  }

  public static @Nullable Hit pick(@NotNull final Ray ray, @NotNull final List<Target> targets) {
    if (targets.isEmpty()) {
      return march(ray, targets);
    }
    return switch (targets.getFirst().scale().projection()) {
      case IntensityMapTopView _ -> pickPlane(ray, targets);
      case IntensityMapPerspective _ -> march(ray, targets);
    };
  }

  /**
   * Marches the ray through the height fields and refines the first hit by bisection.
   */
  private static @Nullable Hit march(@NotNull final Ray ray, @NotNull final List<Target> targets) {
    final double[] range = {0, Double.MAX_VALUE};
    if (!slab(ray.origin().getX(), ray.direction().getX(), -WIDTH / 2, WIDTH / 2, range) || !slab(
        ray.origin().getY(), ray.direction().getY(), -HEIGHT * 1.05, 0.5, range) || !slab(
        ray.origin().getZ(), ray.direction().getZ(), -DEPTH / 2, DEPTH / 2, range)) {
      return null;
    }
    final double step = (range[1] - range[0]) / STEPS;
    double previous = range[0];
    for (int i = 0; i <= STEPS; i++) {
      final double t = range[0] + i * step;
      final Point3D point = ray.at(t);
      for (final Target target : targets) {
        final double surface = height(target, point.getX(), point.getZ());
        if (surface < 0 && point.getY() >= surface) {
          // bisect between the last point above and the first point below the surface
          double above = previous;
          double below = t;
          for (int k = 0; k < REFINEMENTS; k++) {
            final double middle = (above + below) / 2;
            final Point3D probe = ray.at(middle);
            final double h = height(target, probe.getX(), probe.getZ());
            if (h < 0 && probe.getY() >= h) {
              below = middle;
            } else {
              above = middle;
            }
          }
          final Point3D hit = ray.at(below);
          return new Hit(hit.getX(), height(target, hit.getX(), hit.getZ()), hit.getZ(), target,
              below);
        }
      }
      previous = t;
    }
    final Point3D floor = floor(ray, false);
    return floor == null ? null : new Hit(floor.getX(), 0, floor.getZ(), null,
        -ray.origin().getY() / ray.direction().getY());
  }

  /**
   * The 2D view has no heights: the hit is the floor point, its target the overlay drawn last with
   * signal there, which is the one on top.
   */
  private static @Nullable Hit pickPlane(@NotNull final Ray ray,
      @NotNull final List<Target> targets) {
    final Point3D floor = floor(ray, false);
    if (floor == null) {
      return null;
    }
    final double t = -ray.origin().getY() / ray.direction().getY();
    for (final Target target : targets.reversed()) {
      final double value = value(target, floor.getX(), floor.getZ());
      if (value > 0) {
        return new Hit(floor.getX(), 0, floor.getZ(), target, t);
      }
    }
    return new Hit(floor.getX(), 0, floor.getZ(), null, t);
  }

  /**
   * @return local height of the target at the local position, NaN if not measured there
   */
  static double height(@NotNull final Target target, final double x, final double z) {
    final double value = value(target, x, z);
    return Double.isNaN(value) ? Double.NaN
        : -target.scale().height(target.series().data(), value) * HEIGHT;
  }

  /**
   * @return intensity of the target at the local position, NaN if not measured there or below the
   * noise floor
   */
  private static double value(@NotNull final Target target, final double x, final double z) {
    final IntensityMapGrid data = target.series().data();
    final IntensityMapBounds bounds = target.scale().bounds();
    final double dataX = dataX(bounds, x);
    final double dataY = dataY(bounds, z);
    final int column = data.binX(dataX);
    final int row = data.binY(dataY);
    if (column < 0 || row < 0 || !data.isPresent(column, row)) {
      return Double.NaN;
    }
    if (data.pixels() && (dataX < data.xLow(column) || dataX > data.xHigh(column)
        || dataY < data.yLow(row) || dataY > data.yHigh(row))) {
      return Double.NaN;
    }
    final float value = data.intensity(column, row);
    return target.scale().belowNoise(data, value) ? Double.NaN : value;
  }

  public static double dataX(@NotNull final IntensityMapBounds bounds, final double localX) {
    return bounds.xMin() + (localX / WIDTH + 0.5) * (bounds.xMax() - bounds.xMin());
  }

  public static double dataY(@NotNull final IntensityMapBounds bounds, final double localZ) {
    return bounds.yMin() + (localZ / DEPTH + 0.5) * (bounds.yMax() - bounds.yMin());
  }

  private static boolean slab(final double origin, final double direction, final double min,
      final double max, final double @NotNull [] range) {
    if (Math.abs(direction) < 1e-12) {
      return origin >= min && origin <= max;
    }
    double t0 = (min - origin) / direction;
    double t1 = (max - origin) / direction;
    if (t0 > t1) {
      final double swap = t0;
      t0 = t1;
      t1 = swap;
    }
    range[0] = Math.max(range[0], t0);
    range[1] = Math.min(range[1], t1);
    return range[0] <= range[1];
  }
}
