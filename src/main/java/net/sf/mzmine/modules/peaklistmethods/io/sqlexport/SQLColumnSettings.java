/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.io.sqlexport;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

public class SQLColumnSettings extends AbstractTableModel {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    List<String> columnNames = new ArrayList<String>();
    List<SQLExportDataType> columnTypes = new ArrayList<SQLExportDataType>();
    List<String> columnValues = new ArrayList<String>();

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
	return columnNames.size();
    }

    @Override
    public boolean isCellEditable(int row, int col) {
	if ((col == 0) || (col == 1))
	    return true;
	SQLExportDataType dataType = columnTypes.get(row);
	return dataType.hasAdditionalValue();
    }

    @Override
    public synchronized Object getValueAt(int row, int col) {
	if (row >= columnNames.size())
	    return null;
	switch (col) {
	case 0:
	    return columnNames.get(row);
	case 1:
	    return columnTypes.get(row);
	case 2:
	    return columnValues.get(row);
	}
	return null;
    }

    public synchronized void addNewRow() {
	columnNames.add("");
	columnTypes.add(SQLExportDataType.CONSTANT);
	columnValues.add("");
	int insertedRow = columnNames.size() - 1;
	fireTableRowsInserted(insertedRow, insertedRow);
    }

    public synchronized void removeRow(int row) {
	columnNames.remove(row);
	columnTypes.remove(row);
	columnValues.remove(row);
	fireTableRowsDeleted(row, row);
    }

    public void setValueAt(Object val, int row, int col) {
	switch (col) {
	case 0:
	    columnNames.set(row, (String) val);
	    break;
	case 1:
	    SQLExportDataType dataTypeVal = (SQLExportDataType) val;
	    columnTypes.set(row, dataTypeVal);
	    if (!dataTypeVal.hasAdditionalValue())
		columnValues.set(row, dataTypeVal.valueType());
	    break;
	case 2:
	    columnValues.set(row, (String) val);
	    break;
	}
    }

}
