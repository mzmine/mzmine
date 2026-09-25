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

package io.github.mzmine.modules.visualization.surface3d.plot;

import static io.github.mzmine.modules.visualization.surface3d.render.Surface3DMesh.DEPTH;
import static io.github.mzmine.modules.visualization.surface3d.render.Surface3DMesh.HEIGHT;
import static io.github.mzmine.modules.visualization.surface3d.render.Surface3DMesh.WIDTH;

import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScaleTransform;
import io.github.mzmine.modules.visualization.surface3d.data.Surface3DBounds;
import io.github.mzmine.modules.visualization.surface3d.render.Surface3DMesh;
import io.github.mzmine.modules.visualization.surface3d.render.Surface3DScale;
import io.github.mzmine.modules.visualization.surface3d.render.Surface3DScreenGeometry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 3D grid with screen-facing, projected axis labels and calibrated tick marks. Tick counts follow
 * the projected axis length, so zooming adds or removes ticks.
 */
final class Surface3DAxes {

  private static final double MIN_AXIS_PIXELS = 60;
  // axes foreshortened below this fraction of the least foreshortened axis hide their labels
  private static final double MIN_AXIS_RATIO = 0.3;
  // assumption: typical tick labels need this much room along their axis
  private static final double X_TICK_PIXELS = 60;
  private static final double Y_TICK_PIXELS = 45;
  private static final double INTENSITY_TICK_PIXELS = 32;
  // screen distance between an axis line and its tick labels
  private static final double TICK_GAP = 5;
  private final Group geometry = new Group();
  private final Pane labels = new Pane();
  private final Group intensityAxis = new Group();
  private final List<Anchor> anchors = new ArrayList<>();
  private final Set<Label> sized = Collections.newSetFromMap(new IdentityHashMap<>());
  private final PhongMaterial grid = new PhongMaterial();
  private final PhongMaterial axis = new PhongMaterial();
  private final PhongMaterial floor = new PhongMaterial();
  private String textColor = "#334155";
  private boolean intensityVisible = true;
  private @Nullable Surface3DAxesSpec spec;
  private int xTicks = 6;
  private int yTicks = 6;
  private int intensityTicks = 5;

  Surface3DAxes() {
    labels.setMouseTransparent(true);
    labels.setPickOnBounds(false);
    geometry.setMouseTransparent(true);
    setDark(false);
  }

  @NotNull Group geometry() {
    return geometry;
  }

  @NotNull Pane labels() {
    return labels;
  }

  void setDark(final boolean dark) {
    grid.setDiffuseColor(Color.web(dark ? "#3a4452" : "#c5d1df"));
    axis.setDiffuseColor(Color.web(dark ? "#8b98aa" : "#64748b"));
    floor.setDiffuseColor(Color.web(dark ? "#262c35" : "#f0f4f9"));
    textColor = dark ? "#d5dbe3" : "#334155";
    for (final Anchor anchor : anchors) {
      style(anchor.label(), anchor.title());
    }
  }

  void clear() {
    spec = null;
    clearContent();
  }

  private void clearContent() {
    geometry.getChildren().clear();
    labels.getChildren().clear();
    anchors.clear();
    sized.clear();
  }

  void rebuild(@NotNull final Surface3DAxesSpec spec) {
    this.spec = spec;
    build(spec);
  }

  private void build(@NotNull final Surface3DAxesSpec spec) {
    final Surface3DBounds bounds = spec.bounds();
    final PaintScaleTransform transform = spec.transform();
    final Surface3DFormat format = spec.format();
    clearContent();
    intensityAxis.getChildren().clear();
    geometry.getChildren().add(intensityAxis);
    final Box plane = new Box(WIDTH, 0.25, DEPTH);
    plane.setTranslateY(1.5);
    plane.setMaterial(floor);
    geometry.getChildren().add(plane);
    line(WIDTH, 0.8, 0.8, 0, 0, -DEPTH / 2, axis);
    line(0.8, 0.8, DEPTH, -WIDTH / 2, 0, 0, axis);
    // the vertical axis stands in the back corner, clear of the y tick labels and the data
    intensityLine(0.8, HEIGHT, 0.8, -WIDTH / 2, -HEIGHT / 2, DEPTH / 2);

    // decision: titles first, so the overlap pass keeps them in favor of single ticks
    label(spec.xLabel(), 0, 8, -DEPTH / 2 - 44, Axis.X, true);
    label(spec.yLabel(), -WIDTH / 2 - 66, 8, 0, Axis.Y, true);
    label(transform == PaintScaleTransform.LINEAR ? spec.intensityLabel()
            : spec.intensityLabel() + " · " + transform.name().toLowerCase(Locale.ROOT),
        -WIDTH / 2 - 20, -HEIGHT - 24, DEPTH / 2, Axis.INTENSITY, true);

    final double xStep = Surface3DTicks.step(bounds.xMin(), bounds.xMax(), xTicks);
    for (final double value : Surface3DTicks.ticks(bounds.xMin(), bounds.xMax(), xTicks)) {
      final double x = Surface3DMesh.localX(bounds, value);
      line(0.45, 0.45, DEPTH, x, 0.8, 0, grid);
      line(0.7, 0.7, 5, x, 0, -DEPTH / 2 - 2.5, axis);
      tick(format.value(value, spec.xKind(), xStep), new Point3D(x, 0, -DEPTH / 2),
          new Point3D(x, 4, -DEPTH / 2 - 14), Axis.X);
    }
    final double yStep = Surface3DTicks.step(bounds.yMin(), bounds.yMax(), yTicks);
    for (final double value : Surface3DTicks.ticks(bounds.yMin(), bounds.yMax(), yTicks)) {
      final double z = Surface3DMesh.localZ(bounds, value);
      line(WIDTH, 0.45, 0.45, 0, 0.8, z, grid);
      line(5, 0.7, 0.7, -WIDTH / 2 - 2.5, 0, z, axis);
      tick(format.value(value, spec.yKind(), yStep), new Point3D(-WIDTH / 2, 0, z),
          new Point3D(-WIDTH / 2 - 22, 4, z), Axis.Y);
    }
    final double maximum = spec.intensityMaximum();
    if (maximum > 0) {
      // heights may start at a baseline instead of zero
      final double lowest = spec.intensityBaseline() * maximum;
      final double[] ticks = Surface3DScale.isLogarithmic(transform) ? Surface3DTicks.logTicks(
          maximum, intensityTicks) : Surface3DTicks.ticks(lowest, maximum, intensityTicks);
      for (final double value : ticks) {
        if (value <= 0 || value < lowest) {
          continue;
        }
        // same mapping as the surface, so ticks stay calibrated for every transformation
        final double t = Surface3DScale.height(transform, value, maximum,
            spec.intensityBaseline());
        intensityLine(6, 0.7, 0.7, -WIDTH / 2 - 3, -HEIGHT * t, DEPTH / 2);
        tick(format.intensity(value), new Point3D(-WIDTH / 2, -HEIGHT * t, DEPTH / 2),
            new Point3D(-WIDTH / 2 - 35, -HEIGHT * t, DEPTH / 2 + 2), Axis.INTENSITY);
      }
    }
    setIntensityVisible(intensityVisible);
  }

  private void intensityLine(final double width, final double height, final double depth,
      final double x, final double y, final double z) {
    line(width, height, depth, x, y, z, axis);
    final var node = geometry.getChildren().removeLast();
    intensityAxis.getChildren().add(node);
  }

  @NotNull List<Point3D> fittingPoints() {
    return anchors.stream()
        .filter(anchor -> anchor.axis() != Axis.INTENSITY || intensityAxis.isVisible())
        .map(Anchor::fit).toList();
  }

  void setIntensityVisible(final boolean visible) {
    intensityVisible = visible;
    intensityAxis.setVisible(visible);
  }

  private void line(final double width, final double height, final double depth, final double x,
      final double y, final double z, @NotNull final PhongMaterial material) {
    final Box line = new Box(width, height, depth);
    line.setTranslateX(x);
    line.setTranslateY(y);
    line.setTranslateZ(z);
    line.setMaterial(material);
    geometry.getChildren().add(line);
  }

  private void label(@NotNull final String text, final double x, final double y, final double z,
      @NotNull final Axis axis, final boolean title) {
    final Point3D position = new Point3D(x, y, z);
    addAnchor(text, position, position, axis, title);
  }

  /**
   * @param onAxis tick position on the axis line; the label is moved outwards in screen space
   * @param fit    position that reserves room for the text when fitting the camera
   */
  private void tick(@NotNull final String text, @NotNull final Point3D onAxis,
      @NotNull final Point3D fit, @NotNull final Axis axis) {
    addAnchor(text, onAxis, fit, axis, false);
  }

  private void addAnchor(@NotNull final String text, @NotNull final Point3D position,
      @NotNull final Point3D fit, @NotNull final Axis axis, final boolean title) {
    final Label label = new Label(text);
    label.setManaged(false);
    style(label, title);
    labels.getChildren().add(label);
    anchors.add(new Anchor(label, position, fit, axis, title));
  }

  private void style(@NotNull final Label label, final boolean title) {
    // no padding: label bounds must match the text for the overlap tests
    label.setStyle("-fx-text-fill: " + textColor + "; -fx-font-size: " + (title ? "13" : "11")
        + "; -fx-font-weight: " + (title ? "bold" : "normal") + "; -fx-padding: 0;");
    sized.remove(label);
  }

  void project() {
    if (geometry.getScene() == null) {
      return;
    }
    // hide the labels of axes that point towards the viewer, they would overlap each other
    final double x = scale(new Point3D(-WIDTH / 2, 0, -DEPTH / 2),
        new Point3D(WIDTH / 2, 0, -DEPTH / 2), WIDTH);
    final double y = scale(new Point3D(-WIDTH / 2, 0, -DEPTH / 2),
        new Point3D(-WIDTH / 2, 0, DEPTH / 2), DEPTH);
    final double intensity = intensityVisible ? scale(new Point3D(-WIDTH / 2, 0, DEPTH / 2),
        new Point3D(-WIDTH / 2, -HEIGHT, DEPTH / 2), HEIGHT) : 0;
    updateTickCounts(x * WIDTH, y * DEPTH, intensity * HEIGHT);
    final double reference = Math.max(x, Math.max(y, intensity));
    final boolean xReadable = readable(x, WIDTH, reference);
    final boolean yReadable = readable(y, DEPTH, reference);
    final boolean intensityReadable = readable(intensity, HEIGHT, reference);
    final Point2D xNormal = outwardNormal(new Point3D(-WIDTH / 2, 0, -DEPTH / 2),
        new Point3D(WIDTH / 2, 0, -DEPTH / 2));
    final Point2D yNormal = outwardNormal(new Point3D(-WIDTH / 2, 0, -DEPTH / 2),
        new Point3D(-WIDTH / 2, 0, DEPTH / 2));
    final Point2D intensityNormal = outwardNormal(new Point3D(-WIDTH / 2, 0, DEPTH / 2),
        new Point3D(-WIDTH / 2, -HEIGHT, DEPTH / 2));
    for (final Anchor anchor : anchors) {
      final Label label = anchor.label();
      final boolean visible = switch (anchor.axis()) {
        case X -> xReadable;
        case Y -> yReadable;
        case INTENSITY -> intensityReadable;
      };
      label.setVisible(visible);
      if (!visible) {
        continue;
      }
      final Point3D scene = geometry.localToScene(anchor.position(), true);
      if (scene == null) {
        continue;
      }
      final Point2D position = labels.sceneToLocal(scene.getX(), scene.getY());
      // text measurement is costly, labels only need it once CSS has been applied
      if (!sized.contains(label)) {
        label.applyCss();
        label.autosize();
        if (label.getSkin() != null) {
          sized.add(label);
        }
      }
      Point2D center = position;
      if (!anchor.title()) {
        // decision: a fixed screen distance outside the axis line, independent of tile size,
        // so tick labels never reach into their own plot
        final Point2D normal = switch (anchor.axis()) {
          case X -> xNormal;
          case Y -> yNormal;
          case INTENSITY -> intensityNormal;
        };
        if (normal != null) {
          center = position.add(normal.multiply(extent(label, normal) + TICK_GAP));
        }
      }
      label.relocate(center.getX() - label.getWidth() / 2, center.getY() - label.getHeight() / 2);
    }
    placeTitle(Axis.X, new Point3D(0, 0, -DEPTH / 2), xNormal);
    placeTitle(Axis.Y, new Point3D(-WIDTH / 2, 0, 0), yNormal);
  }

  /**
   * @return screen normal of the axis line pointing away from the plot center, null if the axis
   * collapses to a point
   */
  private @Nullable Point2D outwardNormal(@NotNull final Point3D start,
      @NotNull final Point3D end) {
    final Point2D a = screen(start);
    final Point2D b = screen(end);
    final Point2D center = screen(new Point3D(0, 0, 0));
    if (a == null || b == null || center == null) {
      return null;
    }
    final Point2D direction = b.subtract(a);
    if (direction.magnitude() < 1e-6) {
      return null;
    }
    Point2D normal = new Point2D(-direction.getY(), direction.getX()).normalize();
    final Point2D middle = a.midpoint(b);
    if (normal.dotProduct(middle.subtract(center)) < 0) {
      normal = normal.multiply(-1);
    }
    return normal;
  }

  /**
   * Moves the axis title outwards in screen space, beyond the tick labels, so that titles never
   * compete with ticks regardless of zoom and tile size.
   *
   * @param middle local position of the axis center
   */
  private void placeTitle(@NotNull final Axis axis, @NotNull final Point3D middle,
      @Nullable final Point2D axisNormal) {
    final Anchor title = anchors.stream().filter(a -> a.axis() == axis && a.title()).findFirst()
        .orElse(null);
    if (title == null || !title.label().isVisible()) {
      return;
    }
    final Point2D axisMiddle = screen(middle);
    if (axisMiddle == null || axisNormal == null) {
      return;
    }
    final Point2D normal = axisNormal;
    // outermost extent of the tick labels along the outward direction
    double distance = 0;
    for (final Anchor anchor : anchors) {
      if (anchor.axis() != axis || anchor.title() || !anchor.label().isVisible()) {
        continue;
      }
      final Label label = anchor.label();
      final Point2D labelCenter = new Point2D(label.getLayoutX() + label.getWidth() / 2,
          label.getLayoutY() + label.getHeight() / 2);
      distance = Math.max(distance, labelCenter.subtract(axisMiddle).dotProduct(normal)
          + extent(label, normal));
    }
    final Label label = title.label();
    final double offset = Math.max(distance, 6) + 6 + extent(label, normal);
    final Point2D position = axisMiddle.add(normal.multiply(offset));
    label.relocate(position.getX() - label.getWidth() / 2,
        position.getY() - label.getHeight() / 2);
  }

  /**
   * @return the laid out text box; bounds in parent also contain text rendering margins, which
   * would report overlaps between labels that do not touch
   */
  static @NotNull Bounds layoutBounds(@NotNull final Label label) {
    return new javafx.geometry.BoundingBox(label.getLayoutX(), label.getLayoutY(),
        label.getWidth(), label.getHeight());
  }

  private static double extent(@NotNull final Label label, @NotNull final Point2D direction) {
    return Math.abs(direction.getX()) * label.getWidth() / 2
        + Math.abs(direction.getY()) * label.getHeight() / 2;
  }

  private @Nullable Point2D screen(@NotNull final Point3D local) {
    final Point3D scene = geometry.localToScene(local, true);
    return scene == null ? null : labels.sceneToLocal(scene.getX(), scene.getY());
  }

  /**
   * Regenerates ticks when the projected axis length calls for a different number of them.
   */
  private void updateTickCounts(final double xPixels, final double yPixels,
      final double intensityPixels) {
    if (spec == null) {
      return;
    }
    final int x = Math.clamp((int) (xPixels / X_TICK_PIXELS), 2, 10);
    final int y = Math.clamp((int) (yPixels / Y_TICK_PIXELS), 2, 10);
    final int intensity = intensityVisible ? Math.clamp(
        (int) (intensityPixels / INTENSITY_TICK_PIXELS), 1, 6) : intensityTicks;
    if (x != xTicks || y != yTicks || intensity != intensityTicks) {
      xTicks = x;
      yTicks = y;
      intensityTicks = intensity;
      build(spec);
    }
  }

  /**
   * Hides labels that would overlap an already placed label or a forbidden outline. Titles come
   * first and are kept preferentially.
   *
   * @param occupied  label bounds placed so far, in the coordinates of the shared label layer;
   *                  visible labels of this axes are added
   * @param obstacles outlines that labels must not cover, for example other tiles
   */
  void hideOverlaps(@NotNull final List<Bounds> occupied,
      @NotNull final List<List<Point2D>> obstacles, @NotNull final Bounds area) {
    for (final Anchor anchor : anchors) {
      final Label label = anchor.label();
      if (!label.isVisible()) {
        continue;
      }
      final Bounds bounds = layoutBounds(label);
      // partially visible labels at the view border are cut, hide them instead
      boolean free = area.contains(bounds);
      for (final Bounds other : free ? occupied : List.<Bounds>of()) {
        if (bounds.intersects(other)) {
          free = false;
          break;
        }
      }
      if (free) {
        for (final List<Point2D> obstacle : obstacles) {
          if (Surface3DScreenGeometry.intersects(bounds, obstacle)) {
            free = false;
            break;
          }
        }
      }
      label.setVisible(free);
      if (free) {
        occupied.add(bounds);
      }
    }
  }

  /**
   * @return screen pixels per local unit along the axis
   */
  private double scale(@NotNull final Point3D start, @NotNull final Point3D end,
      final double length) {
    final Point3D a = geometry.localToScene(start, true);
    final Point3D b = geometry.localToScene(end, true);
    return a == null || b == null ? 0
        : Math.hypot(a.getX() - b.getX(), a.getY() - b.getY()) / length;
  }

  private static boolean readable(final double scale, final double length,
      final double reference) {
    return scale * length >= MIN_AXIS_PIXELS && scale >= reference * MIN_AXIS_RATIO;
  }

  static @NotNull String format(final double value) {
    return Surface3DTicks.format(value, Math.abs(value) >= 100 ? 0.01 : 0.0001);
  }

  private enum Axis {
    X, Y, INTENSITY
  }

  /**
   * @param position where the label is anchored, on the axis line for ticks
   * @param fit      position that reserves room for the label when fitting the camera
   */
  private record Anchor(@NotNull Label label, @NotNull Point3D position, @NotNull Point3D fit,
                        @NotNull Axis axis, boolean title) {

  }
}
