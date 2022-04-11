/*
 * Copyright 2006-2021 The MZmine Development Team
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

package io.github.mzmine.modules.io.export_features_sql;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class SQLColumnSettings {

  private final ObservableList<SQLRowObject> tableData = FXCollections.observableArrayList();

  public String getColumnName(int col) {
    switch (col) {
      case 0:
        return "Table column";
      case 1:
        return "Export data type";
      case 2:
        return "Export value";
    }
    return null;
  }

  public synchronized int getRowCount() {
    return tableData.size();
  }

  public synchronized Object getValueAt(int row, int col) {
    if (row >= tableData.size())
      return null;
    switch (col) {
      case 0:
        return tableData.get(row).getName();
      case 1:
        return tableData.get(row).getType();
      case 2:
        return tableData.get(row).getValue();
    }
    return null;
  }

  public synchronized void addNewRow() {   tableData.add(new SQLRowObject("",SQLExportDataType.CONSTANT, ""));  }

  public synchronized void removeRow(SQLRowObject row) { tableData.remove(row); }

  public void setValueAt(Object val, int row, int col) {
    switch (col) {
      case 0:
        tableData.get(row).setName((String) val);
        break;
      case 1:
        SQLExportDataType dataTypeVal = (SQLExportDataType) val;
        tableData.get(row).setType(dataTypeVal);
        if (!dataTypeVal.hasAdditionalValue())
        	tableData.get(row).setValue(dataTypeVal.valueType());
        break;
      case 2:
      	tableData.get(row).setValue((String) val);
        break;
    }
  }

  public ObservableList<SQLRowObject> getTableData(){ return tableData;  }


}
