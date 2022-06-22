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

package io.github.mzmine.util.javafx;

import javafx.beans.binding.DoubleExpression;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/**
 * Options to manipulate table views and maybe treetableviews
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class TableViewUitls {

  public static void autoFitLastColumn(TableView<?> table) {
    autoFitLastColumn(table, table.widthProperty().subtract(10));
  }

  public static void autoFitLastColumn(TableView<?> table, DoubleExpression tableWidth) {
    var cols = table.getColumns();
    if (cols.size() < 2) {
      throw new IllegalArgumentException("Table must contain 2 or more columns");
    }
    // target column to resize
    TableColumn<?, ?> lastCol = cols.get(cols.size() - 1);
    // subtract all widths from the table width
    DoubleExpression remainingWidth = tableWidth;
    for (var col : cols) {
      if (col != lastCol) {
        remainingWidth = remainingWidth.subtract(col.widthProperty());
      }
    }
    lastCol.prefWidthProperty().bind(remainingWidth);
  }
}
