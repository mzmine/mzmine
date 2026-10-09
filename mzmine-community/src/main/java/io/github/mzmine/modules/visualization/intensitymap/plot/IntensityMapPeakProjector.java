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

import com.google.common.collect.Range;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapBounds;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapLabel;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapPeak;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapProjection;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapSeries;
import io.github.mzmine.modules.visualization.intensitymap.render.IntensityMapMesh;
import io.github.mzmine.modules.visualization.intensitymap.render.IntensityMapPicker;
import io.github.mzmine.modules.visualization.intensitymap.render.IntensityMapScale;
import io.github.mzmine.modules.visualization.intensitymap.render.IntensityMapScreenGeometry;
import io.github.mzmine.modules.visualization.intensitymap.render.IntensityMapTile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Positions of peaks in the current view, e.g. of feature labels. Created for each placement, so
 * the shown overlays are looked up once and not for every label.
 */
final class IntensityMapPeakProjector {

  private final IntensityMapProjection projection;
  private final Group model;
  private final IntensityMapCamera camera;
  private final IntensityMapTileLayout tileLayout;
  private final Node layer;
  private final IntensityMapScale scale;
  private final double lineScale;
  private final Map<String, IntensityMapSeries> shown = new HashMap<>();
  private final @Nullable IntensityMapLanes lanes;
  // overlaid surfaces can cover each other, side by side only the overlays of one tile
  private final List<IntensityMapPicker.Target> overlaid;

  /**
   * @param layer     node of the screen positions, e.g. the label layer
   * @param visible   overlays in view
   * @param lineScale scale of line widths, follows the zoom
   * @param lanes     separate m/z windows along y, null for one continuous axis
   */
  IntensityMapPeakProjector(@NotNull final IntensityMapProjection projection,
      @NotNull final Group model, @NotNull final IntensityMapCamera camera,
      @NotNull final IntensityMapTileLayout tileLayout, @NotNull final Node layer,
      @NotNull final IntensityMapScale scale, @NotNull final List<IntensityMapSeries> visible,
      final double lineScale, @Nullable final IntensityMapLanes lanes) {
    this.projection = projection;
    this.lanes = lanes;
    this.model = model;
    this.camera = camera;
    this.tileLayout = tileLayout;
    this.layer = layer;
    this.scale = scale;
    this.lineScale = lineScale;
    visible.forEach(value -> shown.put(value.id(), value));
    overlaid = visible.stream().map(value -> new IntensityMapPicker.Target(value, scale)).toList();
  }

  /**
   * @param id overlay of the peak
   * @return screen position of the peak in the layer, null if it is not shown
   */
  @Nullable Point2D position(@NotNull final String id, @NotNull final IntensityMapPeak peak) {
    final IntensityMapSeries value = shown.get(id);
    final IntensityMapTile tile = value == null ? null : tileLayout.tileOf(id);
    final Point3D point = tile == null ? null : localPoint(peak, value);
    if (point == null) {
      return null;
    }
    final Point3D modelPoint = tile.toModel(point);
    return camera.isInFront(model.localToParent(modelPoint)) ? IntensityMapScreenGeometry.project(
        model, modelPoint, layer) : null;
  }

  /**
   * @return true unless a surface lies between the camera and the label; always true in views
   * without depth test, e.g. the flat 2D view
   */
  boolean unoccluded(@NotNull final IntensityMapLabel label, @NotNull final Point2D screen) {
    if (!projection.depthTest()) {
      return true;
    }
    final IntensityMapSeries value = shown.get(label.seriesId());
    final IntensityMapTile tile = value == null ? null : tileLayout.tileOf(value.id());
    final Point3D point = tile == null ? null : localPoint(label.apex(), value);
    if (point == null) {
      return false;
    }
    final IntensityMapPicker.Ray ray = tile.toLocal(camera.ray(screen.getX(), screen.getY()));
    final IntensityMapPicker.Hit hit = IntensityMapPicker.pick(ray,
        tileLayout.tiled().isEmpty() ? overlaid : tileLayout.tileMates(value.id()).stream()
            .map(mate -> new IntensityMapPicker.Target(mate, scale)).toList());
    final Point3D direction = ray.direction();
    final double length = direction.dotProduct(direction);
    if (hit == null || !(length > 0)) {
      return true;
    }
    final double t = point.subtract(ray.origin()).dotProduct(direction) / length;
    // a hit close to the label is the labeled peak itself
    final double tolerance = 6 * lineScale / Math.sqrt(length);
    return hit.t() >= t - tolerance;
  }

  /**
   * @param xRange extent of the peak along x, e.g. its retention time range
   * @param yRange extent of the peak along y
   * @return screen bounds of the peak in the layer, in 3D from the floor to its top; null if it is
   * not shown
   */
  @Nullable Bounds extent(@NotNull final String id, @NotNull final IntensityMapPeak peak,
      @NotNull final Range<Double> xRange, @NotNull final Range<Double> yRange) {
    final IntensityMapSeries value = shown.get(id);
    final IntensityMapTile tile = value == null ? null : tileLayout.tileOf(id);
    final Point3D top = tile == null ? null : localPoint(peak, value);
    if (top == null) {
      return null;
    }
    final IntensityMapBounds range = scale.bounds();
    final double[] screen = IntensityMapExtent.empty();
    final double[] ys = displayed(yRange, peak.y());
    for (final double x : new double[]{xRange.lowerEndpoint(), xRange.upperEndpoint()}) {
      for (final double y : ys) {
        for (final double height : new double[]{0, top.getY()}) {
          final Point3D modelPoint = tile.toModel(
              new Point3D(IntensityMapMesh.localX(range, Math.clamp(x, range.xMin(), range.xMax())),
                  height,
                  IntensityMapMesh.localZ(range, Math.clamp(y, range.yMin(), range.yMax()))));
          final Point2D point = camera.isInFront(model.localToParent(modelPoint))
              ? IntensityMapScreenGeometry.project(model, modelPoint, layer) : null;
          if (point != null) {
            IntensityMapExtent.include(screen, point.getX(), point.getY());
          }
        }
      }
    }
    return IntensityMapExtent.isEmpty(screen) ? null
        : new BoundingBox(screen[0], screen[2], screen[1] - screen[0], screen[3] - screen[2]);
  }

  /**
   * @return tile of the labeled overlay, null if it is not shown
   */
  @Nullable IntensityMapTile tile(@NotNull final IntensityMapLabel label) {
    return shown.containsKey(label.seriesId()) ? tileLayout.tileOf(label.seriesId()) : null;
  }

  /**
   * @return corners of a box around the labeled feature and its surroundings in model coordinates,
   * in 3D also the top of the peak; null if the overlay is not shown
   */
  @Nullable List<Point3D> zoomCorners(@NotNull final IntensityMapLabel label) {
    final IntensityMapSeries value = shown.get(label.seriesId());
    final IntensityMapTile tile = tile(label);
    if (value == null || tile == null) {
      return null;
    }
    final IntensityMapBounds range = scale.bounds();
    final double share = projection.featureZoomShare();
    final double[] x = zoomSpan(label.xRange(), range.xMin(), range.xMax(), share);
    final double[] shown = displayed(label.yRange(), label.y());
    if (Double.isNaN(shown[0]) || Double.isNaN(shown[1])) {
      return null;
    }
    final double[] y = zoomSpan(Range.closed(shown[0], shown[1]), range.yMin(), range.yMax(),
        share);
    final List<Point3D> corners = new ArrayList<>(IntensityMapExtent.corners(tile,
        new double[]{IntensityMapMesh.localX(range, x[0]), IntensityMapMesh.localX(range, x[1]),
            IntensityMapMesh.localZ(range, y[0]), IntensityMapMesh.localZ(range, y[1])}));
    final Point3D top = localPoint(label.apex(), value);
    if (top != null && top.getY() != 0) {
      corners.add(tile.toModel(top));
    }
    return corners;
  }

  /**
   * @param share smallest share of the data range in the view
   * @return lower and upper end of the zoom around a feature extent, within the data range
   */
  static double @NotNull [] zoomSpan(@NotNull final Range<Double> extent, final double min,
      final double max, final double share) {
    final double width = extent.upperEndpoint() - extent.lowerEndpoint();
    // decision: the feature fills about a quarter of the view, and narrow m/z traces or single
    // scans are not magnified to a blur
    final double pad = Math.max(1.5 * width, share / 2 * (max - min));
    return new double[]{Math.max(min, extent.lowerEndpoint() - pad),
        Math.min(max, extent.upperEndpoint() + pad)};
  }

  /**
   * @return the y coordinate as drawn, NaN for m/z between the lanes
   */
  private double displayed(final double y) {
    return lanes == null ? y : lanes.toLane(y);
  }

  /**
   * @param center value inside the range, e.g. the apex, used for range ends between lanes
   * @return lower and upper end of the range as drawn
   */
  private double @NotNull [] displayed(@NotNull final Range<Double> range, final double center) {
    final double fallback = displayed(center);
    final double low = displayed(range.lowerEndpoint());
    final double high = displayed(range.upperEndpoint());
    return new double[]{Double.isNaN(low) ? fallback : low, Double.isNaN(high) ? fallback : high};
  }

  /**
   * @return position of the peak in the local coordinates of its tile, in 3D on top of the drawn
   * surface; null outside the data or below the noise floor
   */
  private @Nullable Point3D localPoint(@NotNull final IntensityMapPeak peak,
      @NotNull final IntensityMapSeries value) {
    final IntensityMapBounds range = scale.bounds();
    final double y = displayed(peak.y());
    // decision: peaks hidden by the noise floor lose their labels as well
    if (!range.contains(peak.x(), y) || scale.belowNoise(value.data(), peak.intensity())) {
      return null;
    }
    final double x = IntensityMapMesh.localX(range, peak.x());
    final double z = IntensityMapMesh.localZ(range, y);
    if (!projection.heights()) {
      return new Point3D(x, 0, z);
    }
    // the drawn bin can be higher than the feature, e.g. with a coeluting ion
    final double feature =
        -Math.min(1, scale.height(value.data(), peak.intensity())) * IntensityMapMesh.HEIGHT;
    final double drawn = IntensityMapPicker.height(new IntensityMapPicker.Target(value, scale), x,
        z);
    return new Point3D(x, Double.isNaN(drawn) ? feature : Math.min(feature, drawn), z);
  }
}
