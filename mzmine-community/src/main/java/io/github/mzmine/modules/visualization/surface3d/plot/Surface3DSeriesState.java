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

import io.github.mzmine.modules.visualization.surface3d.data.Surface3DData;
import io.github.mzmine.modules.visualization.surface3d.render.Surface3DMesh;
import io.github.mzmine.modules.visualization.surface3d.render.Surface3DScale;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
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
 * visibility, opacity, and color are kept.
 */
final class Surface3DSeriesState {

  private final BooleanProperty visible = new SimpleBooleanProperty(true);
  private final DoubleProperty opacity = new SimpleDoubleProperty(1);
  private final ObjectProperty<Color> color = new SimpleObjectProperty<>();
  private final PhongMaterial material = new PhongMaterial();
  private final MeshView view = new MeshView();
  private @Nullable Surface3DData meshData;
  private @Nullable Surface3DScale meshScale;
  private int triangles;
  private float @NotNull [] envelope = new float[0];

  Surface3DSeriesState(@NotNull final Color color, final double opacity) {
    this.color.set(color);
    this.opacity.set(opacity);
    view.setMaterial(material);
    view.setCullFace(CullFace.NONE);
    // picking is done analytically, see Surface3DPicker
    view.setMouseTransparent(true);
    view.visibleProperty().bind(visible);
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

  boolean needsMesh(@NotNull final Surface3DData data, @NotNull final Surface3DScale scale) {
    return meshData != data || meshScale == null || !meshScale.sameGeometry(scale, data);
  }

  /**
   * Uploads new geometry. Must run on the FX thread.
   */
  void setMesh(@NotNull final Surface3DData data, @NotNull final Surface3DScale scale,
      @NotNull final Surface3DMesh geometry, final float @NotNull [] envelope) {
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
  void updateScale(@NotNull final Surface3DScale scale) {
    meshScale = scale;
  }
}
