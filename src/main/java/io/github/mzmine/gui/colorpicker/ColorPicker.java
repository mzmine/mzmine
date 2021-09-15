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
