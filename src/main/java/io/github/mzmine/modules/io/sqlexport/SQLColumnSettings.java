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

package io.github.mzmine.modules.io.sqlexport;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

public class SQLColumnSettings extends AbstractTableModel {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private final ObservableList<SQLRowObject> list= FXCollections.observableArrayList();;

  @Override
  public int getColumnCount() {
    return 3;
  }

  @Override
  public Class<?> getColumnClass(int col) {
    switch (col) {
      case 0:
      case 2:
        return String.class;
      case 1:
        return SQLExportDataType.class;
    }
    return null;
  }

  @Override
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

  @Override
  public synchronized int getRowCount() {
    return list.size();
  }

  @Override
  public boolean isCellEditable(int row, int col) {
    if ((col == 0) || (col == 1))
      return true;
    SQLExportDataType dataType = list.get(row).getType();
    return dataType.hasAdditionalValue();
  }

  @Override
  public synchronized Object getValueAt(int row, int col) {
    if (row >= list.size())
      return null;
    switch (col) {
      case 0:
        return list.get(row).getName();
      case 1:
        return list.get(row).getType();
      case 2:
        return list.get(row).getValue();
    }
    return null;
  }

  public synchronized void addNewRow() {
  	list.add(new SQLRowObject("", "",SQLExportDataType.CONSTANT));

    int insertedRow = list.size() - 1;
    fireTableRowsInserted(insertedRow, insertedRow);
  }

  public synchronized void removeRow(SQLRowObject row) {
    int i=list.indexOf(row);
    list.remove(row);
    fireTableRowsDeleted(i, i);

  }

  public void setValueAt(Object val, int row, int col) {
    switch (col) {
      case 0:
        list.get(row).setName((String) val);
        break;
      case 1:
        SQLExportDataType dataTypeVal = (SQLExportDataType) val;
        list.get(row).setType(dataTypeVal);
        if (!dataTypeVal.hasAdditionalValue())
        	list.get(row).setValue(dataTypeVal.valueType());
        break;
      case 2:
      	list.get(row).setValue((String) val);
        break;
    }
  }
  public ObservableList<SQLRowObject> getList(){
    return  list;
  }

}
