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

package io.github.mzmine.gui.colorpicker;

import java.util.function.Consumer;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.paint.Color;

/**
 * Custom ColorPicker controller.
 */
public class ColorPicker {

  @FXML
  private ColorSwatch colorSwatch;
  @FXML
  private ColorMixer colorMixer;

  private Consumer<Color> onColorSelected = (color) -> {
  };

  public void setOnColorSelected(Consumer<Color> onColorSelected) {
    this.onColorSelected = onColorSelected;
    colorSwatch.onColorSelected = onColorSelected;
  }

  public void initialize() {
    colorSwatch.onColorSelected = onColorSelected;
  }

  @FXML
  private void onCustomColorConfirm(ActionEvent actionEvent) {
    onColorSelected.accept(colorMixer.selectedColor.get());
  }
}
