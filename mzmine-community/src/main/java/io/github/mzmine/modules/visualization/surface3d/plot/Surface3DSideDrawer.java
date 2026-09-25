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

import io.github.mzmine.javafx.util.FxIconUtil;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;

/**
 * Side panel that collapses sideways like an accordion section. A narrow strip with the rotated
 * title stays visible and toggles the content.
 */
final class Surface3DSideDrawer extends HBox {

  private final BooleanProperty expanded = new SimpleBooleanProperty(true);
  private final Node content;
  private final Button strip = new Button();
  private final Label chevron = new Label();

  Surface3DSideDrawer(@NotNull final String title, @NotNull final Node content) {
    this.content = content;
    final Label text = new Label(title);
    text.setRotate(-90);
    text.setStyle("-fx-font-weight: bold;");
    final VBox graphic = new VBox(6, chevron, new Group(text));
    graphic.setAlignment(Pos.TOP_CENTER);
    strip.setGraphic(graphic);
    strip.setMaxHeight(Double.MAX_VALUE);
    strip.setAlignment(Pos.TOP_CENTER);
    strip.setPadding(new Insets(8, 3, 8, 3));
    strip.setFocusTraversable(false);
    strip.setStyle("-fx-background-radius: 0; -fx-background-insets: 0;");
    strip.setOnAction(_ -> expanded.set(!expanded.get()));
    expanded.addListener((_, _, _) -> update());
    getChildren().setAll(strip, content);
    update();
  }

  @NotNull BooleanProperty expandedProperty() {
    return expanded;
  }

  private void update() {
    final boolean show = expanded.get();
    content.setVisible(show);
    content.setManaged(show);
    // chevron points in the direction the panel will move
    chevron.setGraphic(FxIconUtil.getFontIcon(show ? "bi-chevron-right" : "bi-chevron-left", 14));
    strip.setTooltip(new Tooltip(show ? "Collapse the panel" : "Expand the panel"));
  }
}
