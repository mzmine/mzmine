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

package io.github.mzmine.util.javafx;

import com.google.common.collect.Range;
import java.text.NumberFormat;
import java.util.logging.Logger;
import javafx.beans.binding.DoubleExpression;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/**
 * Options to manipulate table views and maybe treetableviews
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class TableViewUtils {

  private static final Logger logger = Logger.getLogger(TableViewUtils.class.getName());

  public static void autoFitLastColumn(TableView<?> table) {
    autoFitLastColumn(table, table.widthProperty().subtract(10));
  }

  public static void autoFitLastColumn(TableView<?> table, DoubleExpression tableWidth) {
    var cols = table.getColumns();
    if (cols.size() < 1) {
      throw new IllegalArgumentException("Table must contain 1 or more columns");
    }
    // target column to resize
    TableColumn<?, ?> lastCol = cols.get(cols.size() - 1);
    // subtract all widths from the table width
    DoubleExpression remainingWidth = tableWidth;
    for (int i = 0; i < cols.size()-1; i++) {
      var col = cols.get(i);
      if (col != lastCol) {
        remainingWidth = remainingWidth.subtract(col.widthProperty());
      }
    }
    lastCol.prefWidthProperty().bind(remainingWidth);
  }

  /**
   * Use a numberformat to format the content of cells
   *
   * @param col    the formatted column
   * @param format the number format
   * @param <T>    type of the table data
   * @param <S>    type of the column data (numbers)
   */
  public static <T, S extends Number> void setFormattedCellFactory(TableColumn<T, S> col,
      NumberFormat format) {
    col.setCellFactory(column -> new TableCell<>() {

      @Override
      public void updateItem(S value, boolean empty) {
        super.updateItem(value, empty);
        if (empty || value == null) {
          setText(null);
          return;
        }
        try {
          setText(format.format(value));
        } catch (Exception ex) {
          logger.warning("Cannot format number " + value);
        }
      }
    });
  }

  /**
   * Use a numberformat to format the content of cells
   *
   * @param col    the formatted column
   * @param format the number format
   * @param <T>    type of the table data
   * @param <S>    type of the column data (numbers)
   */
  public static <T, S extends Number & Comparable> void setFormattedRangeCellFactory(
      TableColumn<T, Range<S>> col, NumberFormat format) {
    col.setCellFactory(column -> new TableCell<>() {

      @Override
      public void updateItem(Range<S> value, boolean empty) {
        super.updateItem(value, empty);
        if (empty || value == null) {
          setText(null);
          return;
        }
        try {
          setText(
              format.format(value.lowerEndpoint()) + "-" + format.format(value.upperEndpoint()));
        } catch (Exception ex) {
          logger.warning("Cannot format number " + value);
        }
      }
    });
  }
}
