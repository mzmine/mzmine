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

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.util.List;
import java.util.function.Consumer;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;

/**
 * Grid of buttons for selecting color.
 */
public class ColorSwatch extends GridPane {

  private static final double MIN_TILE_SIZE = 18;
  private static final double PREF_TILE_SIZE = 24;
  private static final List<Color> BASIC_COLORS = List.of(
      Color.CYAN, Color.TEAL, Color.BLUE, Color.NAVY, Color.MAGENTA, Color.PURPLE, Color.RED,
      Color.MAROON, Color.YELLOW, Color.OLIVE, Color.GREEN, Color.LIME);

  private final int nColumns;
  private final int nRows;
  Consumer<Color> onColorSelected = (clr) -> {
  };

  /**
   * No arguments constructor. Needed for FXML usage.
   */
  public ColorSwatch() {
    this(new Color[]{});
  }

  public ColorSwatch(Color... extraColors) {
    getStylesheets().add(getClass().getResource("ColorSwatch.css").toExternalForm());
    getStyleClass().add("color-grid");

    SimpleColorPalette palette = null;
    try {
     palette = MZmineCore.getConfiguration().getDefaultColorPalette();
    } catch (Exception e) {

    }
    List<Color> colors = palette != null ? palette : BASIC_COLORS;

    nColumns = colors.size();
    nRows = (colors.size() + extraColors.length) / nColumns;

    // create button controls for color selection.
    for (int i = 0; i < colors.size(); i++) {
      addColorButton(colors.get(i), 0, i);
    }
    for (int i = 0; i < extraColors.length; i++) {
      addColorButton(extraColors[i], (i / nColumns) + 1, i % nColumns);
    }
  }

  private void addColorButton(Color color, int row, int column) {
    final Button colorChoice = new Button();
    colorChoice.setUserData(color);

    colorChoice.setMinSize(MIN_TILE_SIZE, MIN_TILE_SIZE);
    colorChoice.setPrefSize(PREF_TILE_SIZE, PREF_TILE_SIZE);
    colorChoice.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

    var colorHex = String.format(
        "#%02x%02x%02x",
        (int) Math.round(color.getRed() * 255),
        (int) Math.round(color.getGreen() * 255),
        (int) Math.round(color.getBlue() * 255));
    final String backgroundStyle = "-fx-background-color: " + colorHex + ";";
    colorChoice.setStyle(backgroundStyle);
    colorChoice.getStyleClass().add("color-choice");

    // choose the color when the button is clicked.
    colorChoice.setOnAction(actionEvent -> {
      onColorSelected.accept((Color) colorChoice.getUserData());
    });

    add(colorChoice, column, row);
  }

  @Override
  protected void layoutChildren() {
    final double tileSize = Math.min(getWidth() / nColumns, getHeight() / nRows);
    for (var child : getChildren()) {
      if (child instanceof Button btn) {
        btn.setPrefSize(tileSize, tileSize);
      }
    }
    setPrefWidth(nColumns * tileSize);
    setMaxWidth(nColumns * tileSize);
    setPrefHeight(nRows * tileSize);
    setMaxHeight(nRows * tileSize);
    super.layoutChildren();
  }
}
