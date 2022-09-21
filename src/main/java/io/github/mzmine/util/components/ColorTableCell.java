/*
 * Copyright 2006-2022 The MZmine Development Team
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
package io.github.mzmine.util.components;

import io.github.mzmine.util.javafx.FxColorUtil;
import javafx.scene.Node;
import javafx.scene.control.TableCell;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/**
 * A simple color cell type. Either filled or as circles
 *
 * @param <S> table data type
 */
public class ColorTableCell<S> extends TableCell<S, Color> {

  private final Style style;
  private final Node node;

  public ColorTableCell(Style style) {
    this.style = style;
    node = switch (style) {
      case FILL -> null;
      case CIRCLE -> {
        Circle circle = new Circle();
        circle.setRadius(10);
        yield circle;
      }
    };
  }

  @Override
  protected void updateItem(Color item, boolean empty) {
    super.updateItem(item, empty);

    setText(null);
    if (empty) {
      setGraphic(null);
    } else {
      switch (style) {
        case FILL -> this.setStyle("-fx-background-color:" + FxColorUtil.colorToHex(item) + ";");
        case CIRCLE -> {
          ((Circle) node).setFill(item);
          setGraphic(node);
        }
      }
    }
  }

  public enum Style {
    FILL, CIRCLE
  }
}
