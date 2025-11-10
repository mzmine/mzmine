/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.javafx.components.skins;

import javafx.beans.binding.Bindings;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.control.skin.TextFieldSkin;
import javafx.scene.text.Text;

/**
 * Automatically scales TextField prefWidth by the actual length of text
 */
public class DynamicTextFieldSkin extends TextFieldSkin {

  private final Text measurer = new Text();
  private double minWidth;
  private double maxWidth;

  /**
   * @param minColumnCount -1 to deactivate
   * @param maxColumnCount -1 to deactivate
   */
  public DynamicTextFieldSkin(TextField field, int minColumnCount, int maxColumnCount) {
    super(field);
    // share the editor’s font to measure the text correctly
    measurer.styleProperty().bind(field.styleProperty());
    Bindings.bindContent(measurer.getStyleClass(), field.getStyleClass());
    measurer.fontProperty().bind(field.fontProperty());
    measurer.applyCss();

    // Set reasonable defaults (will be recalculated when scene/stylesheets are available)
    // These defaults ensure computePrefWidth() works correctly before scene is set
    if (minColumnCount == -1) {
      minWidth = 30; // Fixed default, no recalculation needed
    } else {
      // Rough estimate: ~8 pixels per character (will be recalculated with CSS)
      minWidth = minColumnCount * 8.0;
    }
    if (maxColumnCount == -1) {
      maxWidth = Double.MAX_VALUE; // Fixed default, no recalculation needed
    } else {
      // Rough estimate: ~8 pixels per character (will be recalculated with CSS)
      maxWidth = Math.max(minWidth, maxColumnCount * 8.0);
    }

    // needs dummy scene to apply css
    final Scene scene = new Scene(new Group(measurer));
    field.sceneProperty().subscribe((_, ns) -> {
      if (ns == null) {
        return;
      }
      scene.getStylesheets().setAll(ns.getStylesheets());
      measurer.applyCss();

      // Recalculate min/max width with actual stylesheets applied
      // Only recalculate if we actually measured based on column counts (not fixed defaults)
      if (minColumnCount != -1) {
        measurer.setText("W".repeat(Math.max(0, minColumnCount)));
        measurer.applyCss();
        minWidth = measurer.getLayoutBounds().getWidth();
      }
      if (maxColumnCount != -1) {
        measurer.setText("W".repeat(Math.max(0, maxColumnCount)));
        measurer.applyCss();
        maxWidth = Math.max(minWidth, measurer.getLayoutBounds().getWidth());
      }
    });
  }

  @Override
  protected double computePrefWidth(double height, double topInset, double rightInset,
      double bottomInset, double leftInset) {
    String txt = getField().getText();
    measurer.setText(txt == null || txt.isBlank() ? getField().getPromptText() : txt);

    double textW = measurer.getLayoutBounds().getWidth();

    // default arrow glyph + padding
    // total = insets + text + arrow area
    return Math.min(Math.max(leftInset + textW + 5 + rightInset, minWidth), maxWidth);
  }

  TextField getField() {
    return getSkinnable();
  }

}
