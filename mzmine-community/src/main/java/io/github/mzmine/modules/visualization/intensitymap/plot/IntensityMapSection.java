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

import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.util.FxIconUtil;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Collapsible section with a one-line header: arrow, title, and a hint on the right. Unlike a
 * titled pane with a custom header graphic, the width only follows the parent, which avoids a
 * layout feedback that made the section grow on every pass.
 */
final class IntensityMapSection extends VBox {

  private final BooleanProperty expanded = new SimpleBooleanProperty(true);
  private final Label arrow = new Label();
  private final Label title = new Label();
  private final Label hint = FxLabels.newSmallLabel("");
  private @Nullable Node content;

  IntensityMapSection() {
    title.setMinWidth(0);
    hint.setMinWidth(0);
    final Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    // the title may shrink with an ellipsis, the hint gives way first
    HBox.setHgrow(title, Priority.SOMETIMES);
    final HBox header = new HBox(8, arrow, title, spacer, hint);
    header.setAlignment(Pos.CENTER_LEFT);
    header.setPadding(new Insets(4, 10, 4, 8));
    header.setMinWidth(0);
    header.setCursor(Cursor.HAND);
    header.getStyleClass().add("titled-pane-header");
    header.setStyle("-fx-border-color: derive(-fx-base, -15%); -fx-border-width: 1 0 1 0;");
    header.setOnMouseClicked(_ -> expanded.set(!expanded.get()));
    setMinWidth(0);
    setMaxHeight(Double.MAX_VALUE);
    getChildren().setAll(header);
    expanded.addListener((_, _, _) -> update());
    update();
  }

  @NotNull BooleanProperty expandedProperty() {
    return expanded;
  }

  boolean isExpanded() {
    return expanded.get();
  }

  void setContent(@Nullable final Node content) {
    if (this.content != null) {
      getChildren().remove(this.content);
    }
    this.content = content;
    if (content != null) {
      VBox.setVgrow(content, Priority.ALWAYS);
      getChildren().add(content);
    }
    update();
  }

  void setTitle(@NotNull final String text) {
    title.setText(text);
  }

  void setHint(@NotNull final String text) {
    hint.setText(text);
    Tooltip.install(this, text.isBlank() ? null : new Tooltip(text));
  }

  private void update() {
    final boolean show = expanded.get();
    arrow.setGraphic(FxIconUtil.getFontIcon(show ? "bi-chevron-down" : "bi-chevron-right", 12));
    if (content != null) {
      content.setVisible(show);
      content.setManaged(show);
    }
    setMaxHeight(show ? Double.MAX_VALUE : Region.USE_PREF_SIZE);
  }
}
