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

import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapFeatureTrace;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapPeak;
import java.util.List;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Draws the stored data of features over the raw data of the 3D view: a dashed outline of their
 * ranges on the floor, which shows the part of each feature in the shared window, and their stored
 * data points on top of the surface, connected along the trace. Drawn in screen space like the peak
 * labels, so it follows tiles, lanes, and zoom.
 */
final class IntensityMapTraceLayer {

  private static final double POINT_RADIUS = 1.8;
  // narrow ranges would collapse the outline onto the trace
  private static final double MIN_OUTLINE_PIXELS = 10;
  private final Pane layer = new Pane();
  private List<IntensityMapFeatureTrace> traces = List.of();

  IntensityMapTraceLayer() {
    layer.setMouseTransparent(true);
    layer.setPickOnBounds(false);
  }

  @NotNull Pane layer() {
    return layer;
  }

  void setTraces(@NotNull final List<IntensityMapFeatureTrace> traces) {
    this.traces = List.copyOf(traces);
  }

  boolean isEmpty() {
    return traces.isEmpty();
  }

  /**
   * @param peaks   screen positions in the current view, null removes the drawing
   * @param area    screen area of the plot; the drawing is clipped to it
   * @param visible false removes the drawing
   */
  void update(@Nullable final IntensityMapPeakProjector peaks, @NotNull final Bounds area,
      final boolean visible) {
    layer.getChildren().clear();
    if (!visible || peaks == null || traces.isEmpty()) {
      return;
    }
    layer.setClip(new Rectangle(area.getMinX(), area.getMinY(), area.getWidth(), area.getHeight()));
    final Color accent = IntensityMapPlot.ACCENT;
    for (final IntensityMapFeatureTrace trace : traces) {
      final List<Point2D> outline = peaks.outline(trace.seriesId(), trace.xRange(), trace.yRange());
      if (outline != null) {
        final Polygon box = new Polygon();
        widened(outline).forEach(point -> box.getPoints().addAll(point.getX(), point.getY()));
        box.setFill(accent.deriveColor(0, 1, 1, 0.08));
        box.setStroke(accent);
        box.setStrokeWidth(1.2);
        box.getStrokeDashArray().setAll(5d, 4d);
        layer.getChildren().add(box);
      }
      // a point outside the view or below the noise floor ends a segment of the line
      Polyline line = null;
      for (final IntensityMapPeak point : trace.points()) {
        final Point2D screen = peaks.position(trace.seriesId(), point);
        if (screen == null) {
          line = null;
          continue;
        }
        if (line == null) {
          line = newLine(accent);
          layer.getChildren().add(line);
        }
        line.getPoints().addAll(screen.getX(), screen.getY());
        final Circle dot = new Circle(screen.getX(), screen.getY(), POINT_RADIUS, accent);
        layer.getChildren().add(dot);
      }
    }
  }

  /**
   * @return the outline, stretched around its center to at least {@link #MIN_OUTLINE_PIXELS} in
   * both screen directions
   */
  static @NotNull List<Point2D> widened(@NotNull final List<Point2D> outline) {
    double minX = Double.POSITIVE_INFINITY;
    double maxX = Double.NEGATIVE_INFINITY;
    double minY = Double.POSITIVE_INFINITY;
    double maxY = Double.NEGATIVE_INFINITY;
    for (final Point2D point : outline) {
      minX = Math.min(minX, point.getX());
      maxX = Math.max(maxX, point.getX());
      minY = Math.min(minY, point.getY());
      maxY = Math.max(maxY, point.getY());
    }
    final double centerX = (minX + maxX) / 2;
    final double centerY = (minY + maxY) / 2;
    final double scaleX = maxX - minX < MIN_OUTLINE_PIXELS ? scale(maxX - minX) : 1;
    final double scaleY = maxY - minY < MIN_OUTLINE_PIXELS ? scale(maxY - minY) : 1;
    if (scaleX == 1 && scaleY == 1) {
      return outline;
    }
    return outline.stream().map(point -> new Point2D(centerX + (point.getX() - centerX) * scaleX,
        centerY + (point.getY() - centerY) * scaleY)).toList();
  }

  private static double scale(final double extent) {
    // a degenerate outline is a point or a line, it cannot be stretched
    return extent > 1e-6 ? MIN_OUTLINE_PIXELS / extent : 1;
  }

  private static @NotNull Polyline newLine(@NotNull final Color color) {
    final Polyline line = new Polyline();
    line.setStroke(color);
    line.setStrokeWidth(1.4);
    line.setStrokeLineJoin(StrokeLineJoin.ROUND);
    line.setStrokeLineCap(StrokeLineCap.ROUND);
    return line;
  }
}
