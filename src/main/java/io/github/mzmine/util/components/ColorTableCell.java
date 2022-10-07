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
