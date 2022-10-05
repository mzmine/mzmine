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
