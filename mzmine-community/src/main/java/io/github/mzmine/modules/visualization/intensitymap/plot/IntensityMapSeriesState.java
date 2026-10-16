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

import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapGrid;
import io.github.mzmine.modules.visualization.intensitymap.render.IntensityMapMesh;
import io.github.mzmine.modules.visualization.intensitymap.render.IntensityMapScale;
import io.github.mzmine.util.MathUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleUnaryOperator;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * User controlled appearance of one overlay and its uploaded mesh. Survives resampling so that
 * visibility, opacity, color, and color range are kept.
 */
final class IntensityMapSeriesState {

  private final BooleanProperty visible = new SimpleBooleanProperty(true);
  private final DoubleProperty opacity = new SimpleDoubleProperty(1);
  private final ObjectProperty<Color> color = new SimpleObjectProperty<>();
  // color range as positions of the color scale in [0, 1]
  private final DoubleProperty colorLow = new SimpleDoubleProperty(0);
  private final DoubleProperty colorHigh = new SimpleDoubleProperty(1);
  // intensity at a position of the color scale, for the range labels; null if unknown
  private final ObjectProperty<DoubleUnaryOperator> colorIntensity = new SimpleObjectProperty<>();
  // unclipped colors of the overlay, null if its colors do not show intensity
  private final ObjectProperty<Image> colorBar = new SimpleObjectProperty<>();
  private @Nullable Image clipSource;
  private double clipLow = 0;
  private double clipHigh = 1;
  private @Nullable Image clipped;
  private final PhongMaterial material = new PhongMaterial();
  private final MeshView view = new MeshView();
  private @Nullable IntensityMapGrid meshData;
  private @Nullable IntensityMapScale meshScale;
  private int triangles;
  private float @NotNull [] envelope = new float[0];
  // true once the color range was taken from the data; afterwards it is only changed by the user
  private boolean colorRangeFromData;

  IntensityMapSeriesState(@NotNull final Color color, final double opacity) {
    this.color.set(color);
    this.opacity.set(opacity);
    view.setMaterial(material);
    view.setCullFace(CullFace.NONE);
    // picking is done analytically, see IntensityMapPicker
    view.setMouseTransparent(true);
    view.visibleProperty().bind(visible);
  }

  /**
   * Starts the color range at quantiles of the measured intensities, as the image viewer does for
   * images. Only the first data set the range, a range chosen later is kept.
   *
   * @param quantiles lower and upper quantile in [0, 1]
   */
  void startColorRange(@NotNull final IntensityMapGrid data, @NotNull final IntensityMapScale scale,
      final double @NotNull [] quantiles) {
    if (colorRangeFromData) {
      return;
    }
    colorRangeFromData = true;
    final List<Double> measured = new ArrayList<>();
    for (int row = 0; row < data.height(); row++) {
      for (int column = 0; column < data.width(); column++) {
        if (data.isPresent(column, row)) {
          measured.add((double) data.intensity(column, row));
        }
      }
    }
    final double[] values = MathUtils.calcQuantile(
        measured.stream().mapToDouble(Double::doubleValue).toArray(), quantiles);
    final double low = scale.color(data, values[0]);
    final double high = scale.color(data, values[1]);
    // a flat image keeps the full range
    if (high - low >= IntensityMapColorRange.MIN_SPAN) {
      colorLow.set(low);
      colorHigh.set(high);
    }
  }

  @NotNull BooleanProperty visibleProperty() {
    return visible;
  }

  boolean isVisible() {
    return visible.get();
  }

  @NotNull DoubleProperty opacityProperty() {
    return opacity;
  }

  @NotNull ObjectProperty<Color> colorProperty() {
    return color;
  }

  @NotNull DoubleProperty colorLowProperty() {
    return colorLow;
  }

  @NotNull DoubleProperty colorHighProperty() {
    return colorHigh;
  }

  @NotNull ObjectProperty<DoubleUnaryOperator> colorIntensityProperty() {
    return colorIntensity;
  }

  @NotNull ObjectProperty<Image> colorBarProperty() {
    return colorBar;
  }

  /**
   * @return the texture clipped to the color range; the same instance while nothing changes, so the
   * texture is not uploaded again
   */
  @NotNull Image clippedTexture(@NotNull final Image texture) {
    final double low = colorLow.get();
    final double high = colorHigh.get();
    if (clipped == null || clipSource != texture || clipLow != low || clipHigh != high) {
      clipSource = texture;
      clipLow = low;
      clipHigh = high;
      clipped = IntensityMapColors.clip(texture, low, high);
    }
    return clipped;
  }

  @NotNull PhongMaterial material() {
    return material;
  }

  @NotNull MeshView view() {
    return view;
  }

  int triangles() {
    return triangles;
  }

  float @NotNull [] envelope() {
    return envelope;
  }

  boolean needsMesh(@NotNull final IntensityMapGrid data, @NotNull final IntensityMapScale scale) {
    return meshData != data || meshScale == null || !meshScale.sameGeometry(scale, data);
  }

  /**
   * Uploads new geometry. Must run on the FX thread.
   */
  void setMesh(@NotNull final IntensityMapGrid data, @NotNull final IntensityMapScale scale,
      @NotNull final IntensityMapMesh geometry, final float @NotNull [] envelope) {
    meshData = data;
    this.envelope = envelope;
    meshScale = scale;
    triangles = geometry.triangles();
    if (geometry.faces().length == 0) {
      view.setMesh(null);
      return;
    }
    final TriangleMesh mesh = new TriangleMesh(VertexFormat.POINT_NORMAL_TEXCOORD);
    mesh.getPoints().setAll(geometry.points());
    mesh.getNormals().setAll(geometry.normals());
    mesh.getTexCoords().setAll(geometry.texture());
    mesh.getFaces().setAll(geometry.faces());
    view.setMesh(mesh);
  }

  /**
   * Geometry depends on a scale that is still valid, only the scale identity changed.
   */
  void updateScale(@NotNull final IntensityMapScale scale) {
    meshScale = scale;
  }
}
