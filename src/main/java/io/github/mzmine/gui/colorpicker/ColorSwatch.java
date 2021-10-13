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

    SimpleColorPalette palette = MZmineCore.getConfiguration().getDefaultColorPalette();
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
