/*
 * Copyright 2006 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */
package net.sf.mzmine.userinterface.dialogs.alignmentresultcolumnselection;

import java.util.TreeSet;

public class AlignmentResultColumnSelection {

	// type for common columns

    public enum CommonColumnType {
    	STDCOMPOUND ("STD", Boolean.class),
        ROWNUM ("ID", Integer.class),
        AVGMZ ("Average M/Z", Double.class),
        AVGRT ("Average Retention time", Double.class),
        ISOTOPEID ("Isotope pattern #", Integer.class),
        ISOTOPEPEAK ("Isotope peak #", Integer.class),
        CHARGE ("Charge state", Integer.class);

		private final String columnName;
		private final Class columnClass;
		CommonColumnType(String columnName, Class columnClass) {
			this.columnName = columnName;
			this.columnClass = columnClass;
		}
		public String getColumnName() { return columnName; }
		public Class getColumnClass() { return columnClass; }
    };

	// type for raw data specific columns

    public enum RawDataColumnType {
		MZ ("M/Z", Double.class),
		RT ("Retention time", Double.class),
		HEIGHT ("Height", Double.class),
		AREA ("Area", Double.class);

		private final String columnName;
		private final Class columnClass;
		RawDataColumnType(String columnName, Class columnClass) {
			this.columnName = columnName;
			this.columnClass = columnClass;
		}
		public String getColumnName() { return columnName; }
		public Class getColumnClass() { return columnClass; }
	}

	private TreeSet<CommonColumnType> selectedCommonColumns;
	private TreeSet<RawDataColumnType> selectedRawDataColumns;

	public AlignmentResultColumnSelection() {

		selectedCommonColumns = new TreeSet<CommonColumnType>();
		selectedRawDataColumns = new TreeSet<RawDataColumnType>();
		
	}
	
	public void setAllColumns() {
		
		selectedCommonColumns.add(CommonColumnType.ROWNUM);
		selectedCommonColumns.add(CommonColumnType.STDCOMPOUND);
		selectedCommonColumns.add(CommonColumnType.AVGMZ);
		selectedCommonColumns.add(CommonColumnType.AVGRT);
		selectedCommonColumns.add(CommonColumnType.ISOTOPEID);
		selectedCommonColumns.add(CommonColumnType.ISOTOPEPEAK);
		selectedCommonColumns.add(CommonColumnType.CHARGE);
		
		selectedRawDataColumns.add(RawDataColumnType.MZ);
		selectedRawDataColumns.add(RawDataColumnType.RT);
		selectedRawDataColumns.add(RawDataColumnType.HEIGHT);
		selectedRawDataColumns.add(RawDataColumnType.AREA);		
	}

	public int getNumberOfCommonColumns() {
		return selectedCommonColumns.size();
	}

	public int getNumberOfRawDataColumns() {
		return selectedRawDataColumns.size();
	}

	public CommonColumnType[] getSelectedCommonColumns() {
		return selectedCommonColumns.toArray(new CommonColumnType[0]);
	}

	public CommonColumnType getSelectedCommonColumn(int index) {
		return selectedCommonColumns.toArray(new CommonColumnType[0])[index];
	}

	public RawDataColumnType[] getSelectedRawDataColumns() {
		return selectedRawDataColumns.toArray(new RawDataColumnType[0]);
	}

	public RawDataColumnType getSelectedRawDataColumn(int index) {
		return selectedRawDataColumns.toArray(new RawDataColumnType[0])[index];
	}
	
	public boolean isSelectedCommonColumnType(CommonColumnType c) {
		return selectedCommonColumns.contains(c);
	}
	
	public boolean isSelectedRawDataColumnType(RawDataColumnType c) {
		return selectedRawDataColumns.contains(c);
	}
	
	public void addCommonColumn(CommonColumnType c) {
		selectedCommonColumns.add(c);
	}
	
	public void removeCommonColumn(CommonColumnType c) {
		selectedCommonColumns.remove(c);
	}
	
	public void addRawDataColumn(RawDataColumnType c) {
		selectedRawDataColumns.add(c);
	}
	
	public void removeRawDataColumn(RawDataColumnType c) {
		selectedRawDataColumns.remove(c);
	}



}