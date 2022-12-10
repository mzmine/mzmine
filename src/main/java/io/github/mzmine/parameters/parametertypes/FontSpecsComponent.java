/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.parameters.parametertypes;

import org.jetbrains.annotations.NotNull;
import org.controlsfx.dialog.FontSelectorDialog;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class FontSpecsComponent extends FlowPane {

  private final Label fontLabel;
  private final Button fontSelectButton;
  private final ColorPicker colorPicker;

  private @NotNull Font currentFont = Font.font("Arial");

  public FontSpecsComponent() {

    fontLabel = new Label();

    fontSelectButton = new Button("Select font");
    fontSelectButton.setOnAction(e -> {
      var dialog = new FontSelectorDialog(currentFont);
      var result = dialog.showAndWait();
      if (result.isPresent())
        setFont(result.get());
    });

    colorPicker = new ColorPicker(Color.BLACK);

    getChildren().addAll(fontLabel, fontSelectButton, colorPicker);
  }

  public void setFont(@NotNull Font font) {
    assert font != null;
    this.currentFont = font;
    updateLabel();
  }

  public Font getFont() {
    return currentFont;
  }


  public void setColor(@NotNull Color color) {
    colorPicker.setValue(color);
    updateLabel();
  }

  public Color getColor() {
    return colorPicker.getValue();
  }

  private void updateLabel() {
    fontLabel.setText(currentFont.getName());
    fontLabel.setFont(currentFont);
    fontLabel.setTextFill(getColor());
  }


}
