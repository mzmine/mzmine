/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.tools.tools_autoparam.optimizer.gui;

import java.text.NumberFormat;
import java.util.Objects;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;
import org.moeaframework.core.Solution;

/**
 * Shows a number with a bar relative to the largest value in the column. The bar scales with the
 * column width and is empty for values at or below 0.
 */
public class BarTableCell extends TableCell<Solution, Number> {

  private static final double BAR_HEIGHT = 16;

  private final DoubleProperty widthFraction = new SimpleDoubleProperty(0d);

  public BarTableCell(@NotNull Color color, @NotNull NumberFormat formatter) {
    final Label label = new Label();
    final Region bar = new Region();
    bar.setBackground(new Background(
        new BackgroundFill(new Color(color.getRed(), color.getGreen(), color.getBlue(), 0.5),
            CornerRadii.EMPTY, Insets.EMPTY)));
    // decision: no preferred width, so the bar follows the column width and never widens it
    bar.setMinWidth(0);
    bar.setPrefWidth(0);
    bar.setMinHeight(BAR_HEIGHT);
    bar.setMaxHeight(BAR_HEIGHT);

    final StackPane content = new StackPane(bar, label);
    StackPane.setAlignment(bar, Pos.CENTER_LEFT);
    StackPane.setAlignment(label, Pos.CENTER_RIGHT);
    bar.maxWidthProperty().bind(content.widthProperty().multiply(widthFraction));

    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
    setMinWidth(USE_PREF_SIZE);

    itemProperty().subscribe(item -> {
      if (item == null) {
        setGraphic(null);
        return;
      }
      setGraphic(content);
      label.setText(formatter.format(item));
      final ObservableList<Solution> items = getTableColumn().getTableView().getItems();
      double max = 0d;
      for (int i = 0; i < items.size(); i++) {
        max = Math.max(
            Objects.requireNonNullElse(getTableColumn().getCellData(i), 0d).doubleValue(), max);
      }
      widthFraction.set(fraction(item.doubleValue(), max));
    });
  }

  /**
   * @return the value relative to the maximum within 0 to 1, 0 if the maximum is not positive
   */
  private static double fraction(double value, double max) {
    if (!(max > 0) || !Double.isFinite(value)) {
      return 0;
    }
    return Math.clamp(value / max, 0d, 1d);
  }
}
