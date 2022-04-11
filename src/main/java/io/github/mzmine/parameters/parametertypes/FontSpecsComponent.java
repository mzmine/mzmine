/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
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
