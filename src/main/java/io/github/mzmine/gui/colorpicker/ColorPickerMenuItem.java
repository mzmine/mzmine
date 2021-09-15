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
