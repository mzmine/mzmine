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

import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.modules.visualization.intensitymap.render.IntensityMapPicker;
import io.github.mzmine.modules.visualization.intensitymap.render.IntensityMapTile;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntToDoubleFunction;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Circle;
import org.jetbrains.annotations.NotNull;

/**
 * What hovering shows: the readout of all visible overlays, a cursor on the data, and the slices a
 * click would select.
 */
final class IntensityMapHover {

  private static final int MAX_ROWS = 8;
  private final VBox readout = new VBox(3);
  private final Box dropLine = new Box(0.9, 1, 0.9);
  private final Box crossX = new Box(WIDTH, 0.35, 0.35);
  private final Box crossZ = new Box(0.35, 0.35, DEPTH);
  private final Group slices = new Group();
  private final PhongMaterial sliceMaterial = new PhongMaterial(
      IntensityMapPlot.ACCENT.deriveColor(0, 1, 1, 0.06));
  private final PhongMaterial accentMaterial = new PhongMaterial(IntensityMapPlot.ACCENT);
  private boolean dark;
  // cursor and slices lie in the data plane, for views without depth test
  private boolean coplanar;

  IntensityMapHover() {
    dropLine.setMaterial(accentMaterial);
    crossX.setMaterial(accentMaterial);
    crossZ.setMaterial(accentMaterial);
    // decision: a faint unlit plane plus a crisp floor line, specular highlights washed it out
    sliceMaterial.setSpecularColor(Color.TRANSPARENT);
    slices.setMouseTransparent(true);
    readout.setManaged(false);
    readout.setVisible(false);
    for (final Node node : cursor()) {
      node.setVisible(false);
    }
  }

  /**
   * @return the readout, placed in an overlay above the view
   */
  @NotNull VBox readout() {
    return readout;
  }

  /**
   * @return the slices, in the model after the data
   */
  @NotNull Group slices() {
    return slices;
  }

  /**
   * @return the cursor on the data, in the markers of the model
   */
  @NotNull List<Node> cursor() {
    return List.of(dropLine, crossX, crossZ);
  }

  /**
   * @param coplanar draw the cursor and slices as flat lines in the data plane, for views without
   *                 heights and depth test. Planes that reach up to the data would project into
   *                 wide bands when the camera is close in a deep zoom.
   */
  void setCoplanar(final boolean coplanar) {
    this.coplanar = coplanar;
  }

  /**
   * @param style style of the readout bubble
   */
  void setStyle(final boolean dark, @NotNull final String style) {
    this.dark = dark;
    readout.setStyle(style);
  }

  void hide() {
    slices.getChildren().clear();
    readout.setVisible(false);
    for (final Node node : cursor()) {
      node.setVisible(false);
    }
  }

  void clearSlices() {
    slices.getChildren().clear();
  }

  /**
   * A drop line from the hit surface to the floor and a crosshair on the floor.
   *
   * @param lineScale line width, follows the zoom
   */
  void showCursor(@NotNull final IntensityMapPicker.Hit hit, final double lineScale) {
    final double t = lineScale;
    dropLine.setVisible(hit.target() != null && hit.y() < -0.5 * t);
    dropLine.setWidth(0.9 * t);
    dropLine.setDepth(0.9 * t);
    crossX.setHeight(0.35 * t);
    crossX.setDepth(0.35 * t);
    crossZ.setWidth(0.35 * t);
    crossZ.setHeight(0.35 * t);
    if (dropLine.isVisible()) {
      dropLine.setHeight(-hit.y());
      dropLine.setTranslateX(hit.x());
      dropLine.setTranslateY(hit.y() / 2);
      dropLine.setTranslateZ(hit.z());
    }
    // lines on the floor, below the surface in 3D
    final double floor = coplanar ? 0 : 0.6;
    crossX.setTranslateZ(hit.z());
    crossX.setTranslateY(floor);
    crossZ.setTranslateX(hit.x());
    crossZ.setTranslateY(floor);
    crossX.setVisible(true);
    crossZ.setVisible(true);
  }

  /**
   * Slice planes through the position in every tile. Their tops reach the highest data point of the
   * tile and form a crosshair above the data (user request), visible even over dense pixel
   * columns.
   *
   * @param x         local x of the position
   * @param z         local z of the position
   * @param top       height of the slices per tile index
   * @param lineScale line width, follows the zoom
   */
  void showSlices(@NotNull final List<IntensityMapTile> tiles, final double x, final double z,
      @NotNull final IntensityMapSliceMode mode, @NotNull final IntToDoubleFunction top,
      final double lineScale) {
    slices.getChildren().clear();
    for (int i = 0; i < tiles.size(); i++) {
      final Group marker = new Group();
      marker.getTransforms().setAll(tiles.get(i).transforms());
      slices.getChildren().add(marker);
      if (coplanar) {
        if (mode != IntensityMapSliceMode.Y) {
          marker.getChildren().add(sliceLine(0.7 * lineScale, DEPTH, x, 0, 0, lineScale));
        }
        if (mode != IntensityMapSliceMode.X) {
          marker.getChildren().add(sliceLine(WIDTH, 0.7 * lineScale, 0, z, 0, lineScale));
        }
        continue;
      }
      final double height = top.applyAsDouble(i);
      if (mode != IntensityMapSliceMode.Y) {
        marker.getChildren().addAll(slicePlane(0.6 * lineScale, height, DEPTH, x, 0),
            sliceLine(0.7 * lineScale, DEPTH, x, 0, -height, lineScale),
            sliceLine(0.5 * lineScale, DEPTH, x, 0, -0.3 * lineScale, lineScale));
      }
      if (mode != IntensityMapSliceMode.X) {
        marker.getChildren().addAll(slicePlane(WIDTH, height, 0.6 * lineScale, 0, z),
            sliceLine(WIDTH, 0.7 * lineScale, 0, z, -height, lineScale),
            sliceLine(WIDTH, 0.5 * lineScale, 0, z, -0.3 * lineScale, lineScale));
      }
    }
  }

  private @NotNull Box slicePlane(final double width, final double height, final double depth,
      final double x, final double z) {
    final Box plane = new Box(width, height, depth);
    plane.setTranslateX(x);
    plane.setTranslateZ(z);
    plane.setTranslateY(-height / 2);
    plane.setMaterial(sliceMaterial);
    return plane;
  }

  private @NotNull Box sliceLine(final double width, final double depth, final double x,
      final double z, final double y, final double lineScale) {
    final Box line = new Box(width, 0.5 * lineScale, depth);
    line.setTranslateX(x);
    line.setTranslateZ(z);
    line.setTranslateY(y);
    line.setMaterial(accentMaterial);
    return line;
  }

  /**
   * Shows the readout next to the cursor, inside the viewport.
   *
   * @param header position of the cursor
   * @param rows   visible overlays, more than eight are summarized
   */
  void showReadout(@NotNull final String header, @NotNull final List<IntensityMapHoverRow> rows,
      final double x, final double y, final double width, final double height) {
    final List<Node> children = new ArrayList<>();
    children.add(label(header, true));
    int shown = 0;
    for (final IntensityMapHoverRow row : rows) {
      if (shown == MAX_ROWS) {
        children.add(label("+ " + (rows.size() - shown) + " more", false));
        break;
      }
      children.add(FxLayout.newHBox(Pos.CENTER_LEFT, Insets.EMPTY, 6, new Circle(4, row.color()),
          label(row.text(), row.emphasized())));
      shown++;
    }
    readout.getChildren().setAll(children);
    readout.setVisible(true);
    // new labels need CSS before they can be measured, otherwise they collapse to an ellipsis
    readout.applyCss();
    readout.autosize();
    final double left = x + 18 + readout.getWidth() > width ? x - 12 - readout.getWidth() : x + 18;
    final double top =
        y + 18 + readout.getHeight() > height ? y - 12 - readout.getHeight() : y + 18;
    readout.relocate(Math.max(4, left), Math.max(4, top));
  }

  private @NotNull Label label(@NotNull final String text, final boolean bold) {
    final Label label = new Label(text);
    label.setStyle((dark ? "-fx-text-fill: #e2e8f0;" : "-fx-text-fill: #1e293b;") + (bold
        ? "-fx-font-weight: bold;" : ""));
    return label;
  }
}
