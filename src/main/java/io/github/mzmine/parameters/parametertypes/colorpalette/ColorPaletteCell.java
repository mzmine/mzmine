/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.parameters.parametertypes.colorpalette;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ListCell;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * Implementation of ListCell to display color palettes and select between them.
 * 
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 */
public class ColorPaletteCell extends ListCell<SimpleColorPalette> {

  private final static int MAX_PREVIEW_COLORS = 10;

  private final double height;
  private final List<Rectangle> rects;
  private final FlowPane pane;

  /**
   * 
   * @param w The width of the combo box.
   * @param h The height of the combo box.
   */
  public ColorPaletteCell(double h) {
    super();
    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
    pane = new FlowPane();
    pane.setMaxSize((MAX_PREVIEW_COLORS + 1) * h, h);
    height = h;
    rects = new ArrayList<Rectangle>();
    pane.setMaxWidth(h * MAX_PREVIEW_COLORS);
  }

  private void setRectangles(@Nullable SimpleColorPalette palette) {
    rects.clear();

    if (palette == null || palette.isEmpty())
      return;

    for (int i = 0; i < palette.size(); i++) {
      Color clr = palette.get(i);
      Rectangle rect = new Rectangle(height, height);
      rect.setFill(clr);
      rects.add(rect);
    }

  }

  @Override
  protected void updateItem(@Nullable SimpleColorPalette palette, boolean empty) {
    super.updateItem(palette, empty);

    if (palette == null || palette.isEmpty() || empty) {
      setGraphic(null);
    } else {
      setRectangles(palette);
      pane.getChildren().clear();
      pane.getChildren().addAll(rects);
      setGraphic(pane);
    }
  }
};


/*
 * ComboBox<Color> cmb = new ComboBox<>(); cmb.getItems().addAll( Color.RED, Color.GREEN,
 * Color.BLUE);
 * 
 * cmb.setCellFactory(p -> { return new ListCell<>() { private final Rectangle rectangle; {
 * setContentDisplay(ContentDisplay.GRAPHIC_ONLY); rectangle = new Rectangle(10, 10); }
 * 
 * @Override protected void updateItem(Color item, boolean empty) { super.updateItem(item, empty);
 * 
 * if (item == null || empty) { setGraphic(null); } else { rectangle.setFill(item);
 * setGraphic(rectangle); } } }; });
 */
