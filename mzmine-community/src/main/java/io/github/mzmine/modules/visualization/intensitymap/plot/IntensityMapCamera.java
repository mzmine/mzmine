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

import static io.github.mzmine.modules.visualization.intensitymap.render.IntensityMapMesh.DEPTH;
import static io.github.mzmine.modules.visualization.intensitymap.render.IntensityMapMesh.WIDTH;

import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapProjection;
import io.github.mzmine.modules.visualization.intensitymap.render.IntensityMapMesh;
import io.github.mzmine.modules.visualization.intensitymap.render.IntensityMapPicker;
import io.github.mzmine.modules.visualization.intensitymap.render.IntensityMapTile;
import java.util.List;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SubScene;
import javafx.scene.layout.Region;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Camera and navigation of the plot: rotation and pan move the model, zooming moves the camera.
 * Also finds the part of the floor that is visible in the viewport.
 */
final class IntensityMapCamera {

  // closest and farthest camera distance to the viewed point: fitted views are ~1250 away, so
  // zooming in reaches more than 10000 times (user request)
  private static final double MIN_DISTANCE = 0.05;
  private static final double MAX_DISTANCE = 4000;
  // sample points per axis to find the visible data window
  private static final int VISIBLE_STEPS = 24;

  private final PerspectiveCamera camera = new PerspectiveCamera(true);
  private final Rotate tilt = new Rotate(0, Rotate.X_AXIS);
  private final Rotate turn = new Rotate(0, Rotate.Y_AXIS);
  private final Scale heightScale = new Scale(1, 1, 1, 0, 0, 0);
  private final Group model;
  private final SubScene scene;
  private final Region viewport;
  private final IntensityMapProjection projection;
  // the camera follows the data until the user navigates
  private boolean autoFit = true;

  IntensityMapCamera(@NotNull final Group model, @NotNull final SubScene scene,
      @NotNull final Region viewport, @NotNull final IntensityMapProjection projection) {
    this.model = model;
    this.scene = scene;
    this.viewport = viewport;
    this.projection = projection;
    model.getTransforms().addAll(tilt, turn, heightScale);
    resetAngles();
    camera.setNearClip(10);
    camera.setFarClip(10000);
    camera.setTranslateZ(-1250);
    scene.setCamera(camera);
  }

  boolean isAutoFit() {
    return autoFit;
  }

  void setAutoFit(final boolean autoFit) {
    this.autoFit = autoFit;
  }

  /**
   * @param value vertical exaggeration of the model
   */
  void setHeightScale(final double value) {
    heightScale.setY(value);
  }

  /**
   * @return true if the view looks at the intensity axis from the side, not from the top
   */
  boolean showsIntensity() {
    return tilt.getAngle() < 75;
  }

  @NotNull IntensityMapPicker.Ray ray(final double x, final double y) {
    return IntensityMapPicker.ray(camera, scene.getWidth(), scene.getHeight(), x, y, model);
  }

  /**
   * @param tiltAngle rotation around x, 90 looks from the top
   * @param turnAngle rotation around the vertical axis
   */
  void setAngles(final double tiltAngle, final double turnAngle) {
    tilt.setAngle(tiltAngle);
    turn.setAngle(turnAngle);
  }

  /**
   * The default angles of the projection.
   */
  void resetAngles() {
    setAngles(projection.defaultTilt(), projection.defaultTurn());
  }

  void resetPan() {
    model.setTranslateX(0);
    model.setTranslateY(0);
  }

  /**
   * Moves the model by screen pixels.
   */
  void pan(final double dx, final double dy) {
    autoFit = false;
    final double factor = -camera.getTranslateZ() / 1050;
    model.setTranslateX(model.getTranslateX() + dx * factor);
    model.setTranslateY(model.getTranslateY() + dy * factor);
  }

  /**
   * Mouse drag: rotates, or pans in the 2D view and with the pan modifier.
   */
  void drag(final double dx, final double dy, final boolean pan) {
    // the 2D view cannot rotate, dragging pans
    if (!projection.rotatable() || pan) {
      pan(dx, dy);
      return;
    }
    autoFit = false;
    turn.setAngle(turn.getAngle() + dx * 0.35);
    tilt.setAngle(Math.clamp(tilt.getAngle() + dy * 0.35, 0, 90));
  }

  void rotate(final double turnDelta, final double tiltDelta) {
    autoFit = false;
    turn.setAngle(turn.getAngle() + turnDelta);
    tilt.setAngle(Math.clamp(tilt.getAngle() + tiltDelta, 0, 90));
  }

  /**
   * Moves the camera along the ray to the target, which stays in place on screen.
   *
   * @param target world point under the cursor
   * @return false if the camera did not move
   */
  boolean zoomTowards(final double factor, @NotNull final Point3D target) {
    if (!(factor > 0) || !Double.isFinite(factor)) {
      return false;
    }
    final Point3D position = new Point3D(camera.getTranslateX(), camera.getTranslateY(),
        camera.getTranslateZ());
    final double distance = Math.clamp((target.getZ() - position.getZ()) / factor, MIN_DISTANCE,
        MAX_DISTANCE);
    final double z = target.getZ() - distance;
    final double effective = (position.getZ() - target.getZ()) / (z - target.getZ());
    if (!(effective > 0) || !Double.isFinite(effective)) {
      return false;
    }
    final Point3D moved = target.add(position.subtract(target).multiply(1 / effective));
    autoFit = false;
    camera.setTranslateX(moved.getX());
    camera.setTranslateY(moved.getY());
    camera.setTranslateZ(moved.getZ());
    return true;
  }

  /**
   * Zooms by the camera distance only, if no point lies under the viewport center.
   */
  void zoomDistance(final double factor) {
    autoFit = false;
    camera.setTranslateZ(-Math.clamp(-camera.getTranslateZ() / factor, MIN_DISTANCE, MAX_DISTANCE));
  }

  /**
   * @return the point under the screen position at the depth of the model center, null behind the
   * camera
   */
  @Nullable Point3D pointAtModelDepth(final double x, final double y) {
    final double tangent = Math.tan(Math.toRadians(camera.getFieldOfView() / 2));
    final double aspect = scene.getWidth() / scene.getHeight();
    final Point3D direction = new Point3D((2 * x / scene.getWidth() - 1) * tangent * aspect,
        (2 * y / scene.getHeight() - 1) * tangent, 1);
    final Point3D origin = new Point3D(camera.getTranslateX(), camera.getTranslateY(),
        camera.getTranslateZ());
    final double depth = model.localToParent(Point3D.ZERO).getZ();
    final double t = (depth - origin.getZ()) / direction.getZ();
    return t > 0 ? origin.add(direction.multiply(t)) : null;
  }

  /**
   * Near and far clipping planes follow the distance to the viewed point, so that close-ups are not
   * clipped and the depth buffer keeps its precision.
   *
   * @param center world point under the viewport center, null if none
   */
  void updateClipping(@Nullable final Point3D center) {
    final double distance = center == null ? -camera.getTranslateZ()
        : Math.max(MIN_DISTANCE, center.getZ() - camera.getTranslateZ());
    camera.setNearClip(Math.clamp(distance * 0.01, MIN_DISTANCE * 0.01, 10));
    camera.setFarClip(Math.max(10000, distance * 100));
  }

  /**
   * Places the camera so that all points are visible.
   *
   * @param corners points in camera coordinates
   */
  void fit(@NotNull final List<Point3D> corners) {
    if (corners.isEmpty()) {
      return;
    }
    double minX = Double.MAX_VALUE;
    double maxX = -Double.MAX_VALUE;
    double minY = Double.MAX_VALUE;
    double maxY = -Double.MAX_VALUE;
    for (final Point3D point : corners) {
      minX = Math.min(minX, point.getX());
      maxX = Math.max(maxX, point.getX());
      minY = Math.min(minY, point.getY());
      maxY = Math.max(maxY, point.getY());
    }
    final double cx = (minX + maxX) / 2;
    final double cy = (minY + maxY) / 2;
    final double tangent = Math.tan(Math.toRadians(camera.getFieldOfView() / 2)) * 0.84;
    final double aspect = viewport.getWidth() / viewport.getHeight();
    double z = Double.MAX_VALUE;
    for (final Point3D point : corners) {
      final double fitted = Math.max(Math.abs(point.getY() - cy) / tangent,
          Math.abs(point.getX() - cx) / (tangent * aspect));
      z = Math.min(z, point.getZ() - Math.max(MIN_DISTANCE, fitted));
    }
    camera.setTranslateX(cx);
    camera.setTranslateY(cy);
    camera.setTranslateZ(z);
  }

  /**
   * Moves the camera of the top view so that the fitted box fills the fixed plot area of the 2D
   * view instead of the viewport.
   *
   * @param area         screen area of the plot
   * @param corners      box corners in camera coordinates, already fitted to the viewport
   * @param modelCorners the same corners in model coordinates
   */
  void fitToArea(@NotNull final Rectangle2D area, @NotNull final List<Point3D> corners,
      @NotNull final List<Point3D> modelCorners) {
    final double[] screen = IntensityMapExtent.empty();
    for (final Point3D corner : modelCorners) {
      final Point3D sceneCorner = model.localToScene(corner, true);
      if (sceneCorner == null) {
        return;
      }
      final Point2D local = scene.sceneToLocal(sceneCorner.getX(), sceneCorner.getY());
      IntensityMapExtent.include(screen, local.getX(), local.getY());
    }
    double minX = Double.MAX_VALUE;
    double maxX = -Double.MAX_VALUE;
    double depth = 0;
    for (final Point3D corner : corners) {
      minX = Math.min(minX, corner.getX());
      maxX = Math.max(maxX, corner.getX());
      depth += corner.getZ() / corners.size();
    }
    final double screenWidth = screen[1] - screen[0];
    final double screenHeight = screen[3] - screen[2];
    if (!(screenWidth > 0) || !(screenHeight > 0) || !(maxX > minX)) {
      return;
    }
    // assumption: the flat top view projects both axes with the same pixels per unit
    final double factor = Math.min(area.getWidth() / screenWidth, area.getHeight() / screenHeight);
    final double distance = Math.max(MIN_DISTANCE, (depth - camera.getTranslateZ()) / factor);
    final double pixelsPerUnit =
        screenWidth / (maxX - minX) * (depth - camera.getTranslateZ()) / distance;
    final double boxX = (screen[0] + screen[1]) / 2;
    final double boxY = (screen[2] + screen[3]) / 2;
    camera.setTranslateX(
        camera.getTranslateX() - (area.getMinX() + area.getWidth() / 2 - boxX) / pixelsPerUnit);
    camera.setTranslateY(
        camera.getTranslateY() - (area.getMinY() + area.getHeight() / 2 - boxY) / pixelsPerUnit);
    camera.setTranslateZ(depth - distance);
  }

  /**
   * @return the visible part of the floor in local coordinates {x0, x1, z0, z1}, null if unknown.
   * Rays through the viewport corners, edge centers, and center are exact for the top view and for
   * zoomed tiles. If a ray misses, e.g. towards the horizon of a tilted view, floor and top points
   * of every tile are projected instead.
   */
  double @Nullable [] visibleFloor(@NotNull final List<IntensityMapTile> tiles) {
    if (scene.getWidth() <= 0 || scene.getHeight() <= 0) {
      return null;
    }
    final double[] extent = IntensityMapExtent.empty();
    boolean all = true;
    for (int i = 0; i <= 2 && all; i++) {
      for (int j = 0; j <= 2 && all; j++) {
        final IntensityMapPicker.Ray ray = ray(scene.getWidth() * i / 2, scene.getHeight() * j / 2);
        Point3D hit = null;
        for (final IntensityMapTile tile : tiles) {
          hit = IntensityMapPicker.floor(tile.toLocal(ray), false);
          if (hit != null) {
            break;
          }
        }
        if (hit == null) {
          all = false;
        } else {
          IntensityMapExtent.include(extent, hit.getX(), hit.getZ());
        }
      }
    }
    if (!all) {
      final double[] projected = IntensityMapExtent.empty();
      projectFloor(tiles, projected);
      return IntensityMapExtent.isEmpty(projected) ? null : projected;
    }
    return IntensityMapExtent.isEmpty(extent) ? null : extent;
  }

  /**
   * Adds the floor and top sample points of the tiles that are visible in the viewport.
   */
  private void projectFloor(@NotNull final List<IntensityMapTile> tiles,
      final double @NotNull [] extent) {
    final Bounds view = viewport.localToScene(viewport.getLayoutBounds());
    final int steps = VISIBLE_STEPS;
    for (final IntensityMapTile tile : tiles) {
      for (int i = 0; i <= steps; i++) {
        final double x = (i / (double) steps - 0.5) * WIDTH;
        for (int j = 0; j <= steps; j++) {
          final double z = (j / (double) steps - 0.5) * DEPTH;
          for (final double y : projection.heights() ? new double[]{0, -IntensityMapMesh.HEIGHT}
              : new double[]{0}) {
            final Point3D local = tile.toModel(new Point3D(x, y, z));
            if (model.localToParent(local).getZ()
                <= camera.getTranslateZ() + camera.getNearClip()) {
              continue;
            }
            final Point3D screen = model.localToScene(local, true);
            if (screen != null && view.contains(screen.getX(), screen.getY())) {
              IntensityMapExtent.include(extent, x, z);
            }
          }
        }
      }
    }
    if (!IntensityMapExtent.isEmpty(extent)) {
      // points between the samples may be visible too
      extent[0] = Math.max(-WIDTH / 2, extent[0] - WIDTH / steps);
      extent[1] = Math.min(WIDTH / 2, extent[1] + WIDTH / steps);
      extent[2] = Math.max(-DEPTH / 2, extent[2] - DEPTH / steps);
      extent[3] = Math.min(DEPTH / 2, extent[3] + DEPTH / steps);
    }
  }
}
