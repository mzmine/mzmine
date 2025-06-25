/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.javafx.components.factories;

import com.google.common.collect.Range;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Function;
import java.util.logging.Logger;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Options to manipulate table views and maybe treetableviews
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class TableColumns {

  private static final Logger logger = Logger.getLogger(TableColumns.class.getName());

  public static void autoFitLastColumn(TableView<?> table) {
    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
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
    return createColumn(name, 0, valueFactory);
  }

  /**
   * @param name         column name
   * @param valueFactory defines the value of the cell
   * @param <MODEL>      the table row data model
   * @param <V>          the value type of the property
   * @return a new TableColumn
   */
  @NotNull
  public static <MODEL, V> TableColumn<MODEL, V> createColumn(@NotNull String name, double minWidth,
      @NotNull Function<MODEL, ObservableValue<V>> valueFactory) {
    return createColumn(name, minWidth, 0, valueFactory);
  }

  /**
   * @param name         column name
   * @param valueFactory defines the value of the cell
   * @param <MODEL>      the table row data model
   * @param <V>          the value type of the property
   * @return a new TableColumn
   */
  @NotNull
  public static <MODEL, V> TableColumn<MODEL, V> createColumn(@NotNull String name, double minWidth,
      double maxWidth, @NotNull Function<MODEL, ObservableValue<V>> valueFactory) {
    return createColumn(name, minWidth, maxWidth, null, null, valueFactory);
  }

  /**
   * @param name         column name
   * @param valueFactory defines the value of the cell
   * @param sorter       comparator for value class or null for default
   * @param alignment    alignment of content or null for default
   * @param <MODEL>      the table row data model
   * @param <V>          the value type of the property
   * @return a new TableColumn
   */
  @NotNull
  public static <MODEL, V> TableColumn<MODEL, V> createColumn(@NotNull String name, double minWidth,
      double maxWidth, @Nullable ColumnAlignment alignment, final Comparator<V> sorter,
      @NotNull Function<MODEL, ObservableValue<V>> valueFactory) {
    TableColumn<MODEL, V> column = new TableColumn<>(name);
    column.setCellValueFactory(row -> valueFactory.apply(row.getValue()));
    if (minWidth > 0) {
      column.setMinWidth(minWidth);
    }
    if (maxWidth > 0) {
      column.setMaxWidth(maxWidth);
    }
    if (alignment != null) {
      setAlignment(alignment, column);
    }
    if (sorter != null) {
      column.setComparator(sorter);
    }
    return column;
  }

  // #################################################
  // Number columns
  // #################################################

  /**
   * @param name         column name
   * @param valueFactory defines the value of the cell
   * @param alignment    the column alignment. uses the default if null.
   * @param <MODEL>      the table row data model
   * @param <V>          the value type of the property
   * @return a new TableColumn
   */
  @NotNull
  public static <MODEL, V extends Number> TableColumn<MODEL, V> createColumn(@NotNull String name,
      double minWidth, NumberFormat format, @Nullable ColumnAlignment alignment,
      @NotNull Function<MODEL, ObservableValue<V>> valueFactory) {
    return createColumn(name, minWidth, 0, format, alignment, valueFactory);
  }

  /**
   * @param name         column name
   * @param valueFactory defines the value of the cell
   * @param alignment    the column alignment. uses the default if null.
   * @param <MODEL>      the table row data model
   * @param <V>          the value type of the property
   * @return a new TableColumn
   */
  @NotNull
  public static <MODEL, V extends Number> TableColumn<MODEL, V> createColumn(@NotNull String name,
      double minWidth, double maxWidth, NumberFormat format, @Nullable ColumnAlignment alignment,
      @NotNull Function<MODEL, ObservableValue<V>> valueFactory) {
    return createColumn(name, minWidth, maxWidth, format, alignment, null, valueFactory);
  }

  /**
   * @param name         column name
   * @param valueFactory defines the value of the cell
   * @param alignment    the column alignment. uses the default if null.
   * @param sorter       comparator to sort elements
   * @param <MODEL>      the table row data model
   * @param <V>          the value type of the property
   * @return a new TableColumn
   */
  @NotNull
  public static <MODEL, V extends Number> TableColumn<MODEL, V> createColumn(@NotNull String name,
      double minWidth, NumberFormat format, @Nullable ColumnAlignment alignment,
      final Comparator<V> sorter, @NotNull Function<MODEL, ObservableValue<V>> valueFactory) {
    return createColumn(name, minWidth, 0, format, alignment, sorter, valueFactory);
  }

  /**
   * @param name         column name
   * @param valueFactory defines the value of the cell
   * @param alignment    the column alignment. uses the default if null.
   * @param sorter       comparator to sort elements
   * @param <MODEL>      the table row data model
   * @param <V>          the value type of the property
   * @return a new TableColumn
   */
  @NotNull
  public static <MODEL, V extends Number> TableColumn<MODEL, V> createColumn(@NotNull String name,
      double minWidth, double maxWidth, NumberFormat format, @Nullable ColumnAlignment alignment,
      final Comparator<V> sorter, @NotNull Function<MODEL, ObservableValue<V>> valueFactory) {
    var column = createColumn(name, minWidth, maxWidth, alignment, sorter, valueFactory);
    setFormattedCellFactory(column, format);
    return column;
  }

  public static <MODEL, V> TableColumn<MODEL, V> setAlignment(ColumnAlignment alignment,
      TableColumn<MODEL, V> column) {
    alignment.setToColumn(column);
    return column;
  }

  public enum ColumnAlignment {
    LEFT, CENTER, RIGHT;

    public void setToColumn(TableColumn<?, ?> column) {
      column.getStyleClass().removeIf(styleClass -> Arrays.stream(ColumnAlignment.values())
          .anyMatch(align -> align.getStyleClass().equals(styleClass)));
      column.getStyleClass().add(getStyleClass());
    }

    String getStyleClass() {
      return switch (this) {
        case RIGHT -> "align-right-column";
        case LEFT -> "align-left-column";
        case CENTER -> "align-center-column";
      };
    }
  }
}
