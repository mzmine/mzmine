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

package io.github.mzmine.parameters.parametertypes.colorpalette;

import io.github.mzmine.util.color.SimpleColorPalette;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
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
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Implementation of ListCell to display color palettes and select between them.
 *
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 */
public class ColorPaletteCell extends ListCell<SimpleColorPalette> {

  private final static int MAX_PREVIEW_COLORS = 15;
  private static final Color BORDER_CLR = Color.DARKGRAY;
  //  private static final Color TEXT_CLR = Color.BLACK;
//  private static final Color STROKE_CLR = Color.BLACK;
  private static final double STROKE_WIDTH = 0.5;

  private static final Logger logger = Logger.getLogger(ColorPaletteCell.class.getName());

  private final double height;
  private final List<Rectangle> rects;
  private final Rectangle positiveRect, negativeRect, neutralRect;
  private final FlowPane clrPane;
  private final Label lblName;
  private final GridPane pane;

  /**
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
//    clrPane.setPrefWidth(MAX_PREVIEW_COLORS * h);

    rects = new ArrayList<Rectangle>();
    positiveRect = makeRect(Color.TRANSPARENT);
    negativeRect = makeRect(Color.TRANSPARENT);
    neutralRect = makeRect(Color.TRANSPARENT);
    lblName = new Label();

    // nasty way to align the palettes in the dropdown menu
    lblName.setMinWidth(80);
    lblName.setMaxWidth(110);
    lblName.setPrefWidth(110);
    lblName.setAlignment(Pos.CENTER_LEFT);

    // palette in the second row...
    pane = new GridPane();
    pane.setBorder(new Border(new BorderStroke(BORDER_CLR, BorderStrokeStyle.SOLID,
        new CornerRadii(2.0), new BorderWidths(1.0))));
    pane.setVgap(3);
    pane.setHgap(5);

    pane.add(lblName, 0, 0);
    pane.add(clrPane, 0, 1, 7, 1);

    Label label = new Label("Pos.:");
    pane.add(label, 1, 0);
    pane.add(positiveRect, 2, 0);

    label = new Label("Neu.:");
    pane.add(label, 3, 0);
    pane.add(neutralRect, 4, 0);

    label = new Label("Neg.:");
    pane.add(label, 5, 0);
    pane.add(negativeRect, 6, 0);
  }

  private void setRectangles(@Nullable SimpleColorPalette palette) {
    rects.clear();

    if (palette == null || palette.isEmpty()) {
      return;
    }

    for (int i = 0; i < palette.size(); i++) {
      Color clr = palette.get(i);
      Rectangle rect = makeRect(clr);
      rects.add(rect);
    }

    positiveRect.setFill(palette.getPositiveColor());
    neutralRect.setFill(palette.getNeutralColor());
    negativeRect.setFill(palette.getNegativeColor());
  }

  @Override
  protected void updateItem(@Nullable SimpleColorPalette palette, boolean empty) {
    super.updateItem(palette, empty);

    if (palette == null || palette.isEmpty() || empty) {
      setGraphic(null);
    } else {
      setRectangles(palette);
      clrPane.getChildren().clear();
      lblName.setText(palette.getName());
      clrPane.getChildren().addAll(rects);
      setGraphic(pane);
    }
  }

  protected Rectangle makeRect(@NotNull Color clr) {
    Rectangle rect = new Rectangle(height - STROKE_WIDTH * 2, height - STROKE_WIDTH * 2);
    rect.setFill(clr);
    rect.setStrokeWidth(STROKE_WIDTH);
    return rect;
  }
};
