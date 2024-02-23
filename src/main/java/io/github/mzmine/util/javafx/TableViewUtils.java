/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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
import java.util.function.Function;
import java.util.logging.Logger;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.jetbrains.annotations.NotNull;

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
    if (cols.isEmpty()) {
      throw new IllegalArgumentException("Table must contain 1 or more columns");
    }
    // target column to resize
    TableColumn<?, ?> lastCol = cols.getLast();
    // subtract all widths from the table width
    DoubleExpression remainingWidth = tableWidth;
    for (int i = 0; i < cols.size() - 1; i++) {
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
    col.setCellFactory(__ -> new TableCell<>() {

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
  public static <T, S extends Number & Comparable<?>> void setFormattedRangeCellFactory(
      TableColumn<T, Range<S>> col, NumberFormat format) {
    col.setCellFactory(_ -> new TableCell<>() {

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


  /**
   * @param name         column name
   * @param valueFactory defines the value of the cell
   * @param <MODEL>      the table row data model
   * @param <V>          the value type of the property
   * @return a new TableColumn
   */
  @NotNull
  public static <MODEL, V> TableColumn<MODEL, V> createColumn(@NotNull String name,
      @NotNull Function<MODEL, ObservableValue<V>> valueFactory) {
    TableColumn<MODEL, V> column = new TableColumn<>(name);
    column.setCellValueFactory(row -> valueFactory.apply(row.getValue()));
    return column;
  }

  /**
   *
   * @param name column name
   * @param valueFactory defines the value of the cell
   * @return a new TableColumn
   * @param <MODEL> the table row data model
   * @param <V> the value type of the property
   */
  @NotNull
  public static <MODEL, V> TableColumn<MODEL, V> createColumn(@NotNull String name, double minWidth,
      @NotNull Function<MODEL, ObservableValue<V>> valueFactory) {
    var column = createColumn(name, valueFactory);
    column.setMinWidth(minWidth);
    return column;
  }

  /**
   *
   * @param name column name
   * @param valueFactory defines the value of the cell
   * @return a new TableColumn
   * @param <MODEL> the table row data model
   * @param <V> the value type of the property
   */
  @NotNull
  public static <MODEL, V> TableColumn<MODEL, V> createColumn(@NotNull String name, double minWidth,
      double maxWidth, @NotNull Function<MODEL, ObservableValue<V>> valueFactory) {
    var column = createColumn(name, minWidth, valueFactory);
    column.setMaxWidth(maxWidth);
    return column;
  }

}
