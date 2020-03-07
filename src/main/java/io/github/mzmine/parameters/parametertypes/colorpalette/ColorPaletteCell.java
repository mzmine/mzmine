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
import java.util.logging.Logger;
import javax.annotation.Nullable;
import io.github.mzmine.util.color.SimpleColorPalette;
import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * Implementation of ListCell to display color palettes and select between them.
 * 
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 */
public class ColorPaletteCell extends ListCell<SimpleColorPalette> {

  private final static int MAX_PREVIEW_COLORS = 15;
  private static final Color BORDER_CLR = Color.DARKGRAY;
  private static final Color TEXT_CLR = Color.BLACK;

  private static final Logger logger = Logger.getLogger(ColorPaletteCell.class.getName());

  private final double height;
  private final List<Rectangle> rects;
  private final FlowPane clrPane;
  private final Label label;
  private final GridPane pane;

  /**
   * 
   * @param w The width of the combo box.
   * @param h The height of the combo box.
   */
  public ColorPaletteCell(double h) {
    super();

    height = h;
    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

    clrPane = new FlowPane();
    clrPane.setAlignment(Pos.CENTER_LEFT);
    clrPane.setMaxHeight(h);
    clrPane.setMaxWidth(MAX_PREVIEW_COLORS * h);
    clrPane.setPrefHeight(h);
    clrPane.setPrefWidth(MAX_PREVIEW_COLORS * h);

    rects = new ArrayList<Rectangle>();
    label = new Label();
    label.setTextFill(TEXT_CLR);

    // nasty way to align the palettes in the dropdown menu
    label.setMinWidth(80);
    label.setMaxWidth(80);
    label.setPrefWidth(80);
    label.setAlignment(Pos.CENTER_LEFT);

    // palette in the second row...
    pane = new GridPane();
    pane.setBorder(new Border(new BorderStroke(BORDER_CLR, BorderStrokeStyle.SOLID,
        new CornerRadii(2.0), new BorderWidths(1.0))));
    pane.add(clrPane, 1, 0);
    pane.add(label, 0, 0);
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
      clrPane.getChildren().clear();
      label.setText(palette.getName());
      clrPane.getChildren().addAll(rects);
      setGraphic(pane);
    }
  }
};
