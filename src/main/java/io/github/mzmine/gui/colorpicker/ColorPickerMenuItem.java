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

import java.io.IOException;
import java.net.URL;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

/**
 * MenuItem wrapping custom ColorPicker.
 */
public class ColorPickerMenuItem extends CustomMenuItem {

  private final ObjectProperty<Color> selectedColor = new SimpleObjectProperty<>();

  public ColorPickerMenuItem() throws IOException {
    URL url = getClass().getResource("ColorPicker.fxml");
    assert url != null;
    FXMLLoader loader = new FXMLLoader(url);
    Pane pane = loader.load();
    // has to be retrieved after the fxml is loaded
    ColorPicker controller = loader.getController();

    setContent(pane);
    setHideOnClick(false);
    getStyleClass().add("set-color-menu-item");
    controller.setOnColorSelected((color) -> {
      selectedColor.set(color);
      // hide menu
      var m = getParentMenu();
      // find root menu
      while (m.getParentMenu() != null) {
        m = m.getParentMenu();
      }
      m.hide();
    });
  }

  public Color getSelectedColor() {
    return selectedColor.get();
  }

  public void setSelectedColor(Color selectedColor) {
    this.selectedColor.set(selectedColor);
  }

  public ObjectProperty<Color> selectedColorProperty() {
    return selectedColor;
  }

}
