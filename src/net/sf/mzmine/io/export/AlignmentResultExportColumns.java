/*
 * Copyright 2006-2007 The MZmine Development Team
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

package net.sf.mzmine.io.export;

import java.util.Set;
import java.util.TreeSet;

import net.sf.mzmine.userinterface.dialogs.alignmentresultcolumnselection.ColumnSet;
import net.sf.mzmine.userinterface.dialogs.alignmentresultcolumnselection.ColumnType;

public class AlignmentResultExportColumns implements ColumnSet {

    
    // type for common columns
    public enum CommonColumnType implements ColumnType {
        ROWNUM("ID", Integer.class), 
        AVGMZ("Average M/Z", Double.class), 
        AVGRT("Average Retention time", Double.class),
        COMMENT("Comment", String.class);

        private final String columnName;
        private final Class columnClass;

        CommonColumnType(String columnName, Class columnClass) {
            this.columnName = columnName;
            this.columnClass = columnClass;
        }

        public String getColumnName() {
            return columnName;
        }

        public Class getColumnClass() {
            return columnClass;
        }
    };

    // type for raw data specific columns
    public enum RawDataColumnType implements ColumnType {
        STATUS("Status", String.class),
        MZ("M/Z", Double.class), 
        RT("Retention time", Double.class),
        HEIGHT("Height", Double.class),
        AREA("Area", Double.class);

        private final String columnName;
        private final Class columnClass;

        RawDataColumnType(String columnName, Class columnClass) {
            this.columnName = columnName;
            this.columnClass = columnClass;
        }

        public String getColumnName() {
            return columnName;
        }

        public Class getColumnClass() {
            return columnClass;
        }
    }

    private Set<CommonColumnType> selectedCommonColumns;
    private Set<RawDataColumnType> selectedRawDataColumns;

    AlignmentResultExportColumns() {
        selectedCommonColumns = new TreeSet<CommonColumnType>();
        selectedRawDataColumns = new TreeSet<RawDataColumnType>();
    }

    public int getNumberOfCommonColumns() {
        return CommonColumnType.values().length;
    }

    public int getNumberOfRawDataColumns() {
        return RawDataColumnType.values().length;
    }

    /**
     * @see net.sf.mzmine.userinterface.dialogs.alignmentresultcolumnselection.ColumnSet#getCommonColumns()
     */
    public CommonColumnType[] getCommonColumns() {
        return CommonColumnType.values();
    }

    /**
     * @see net.sf.mzmine.userinterface.dialogs.alignmentresultcolumnselection.ColumnSet#getRawDataColumns()
     */
    public RawDataColumnType[] getRawDataColumns() {
        return RawDataColumnType.values();
    }

    /**
     * @see net.sf.mzmine.userinterface.dialogs.alignmentresultcolumnselection.ColumnSet#getSelectedCommonColumns()
     */
    public CommonColumnType[] getSelectedCommonColumns() {
        return selectedCommonColumns.toArray(new CommonColumnType[0]);
    }

    /**
     * @see net.sf.mzmine.userinterface.dialogs.alignmentresultcolumnselection.ColumnSet#getSelectedRawDataColumns()
     */
    public RawDataColumnType[] getSelectedRawDataColumns() {
        return selectedRawDataColumns.toArray(new RawDataColumnType[0]);
    }

    /**
     * @see net.sf.mzmine.userinterface.dialogs.alignmentresultcolumnselection.ColumnSet#isColumnSelected(net.sf.mzmine.userinterface.dialogs.alignmentresultcolumnselection.ColumnType)
     */
    public boolean isColumnSelected(ColumnType c) {
        if (c instanceof CommonColumnType) return selectedCommonColumns.contains(c);
        if (c instanceof RawDataColumnType) return selectedRawDataColumns.contains(c);
        return false;
    }

    /**
     * @see net.sf.mzmine.userinterface.dialogs.alignmentresultcolumnselection.ColumnSet#setColumnSelected(net.sf.mzmine.userinterface.dialogs.alignmentresultcolumnselection.ColumnType,
     *      boolean)
     */
    public void setColumnSelected(ColumnType c, boolean selected) {
        if (c instanceof CommonColumnType) {
            if (selected)
                selectedCommonColumns.add((CommonColumnType) c);
            else
                selectedCommonColumns.remove((CommonColumnType) c);
        }
        if (c instanceof RawDataColumnType) {
            if (selected)
                selectedRawDataColumns.add((RawDataColumnType) c);
            else
                selectedRawDataColumns.remove((RawDataColumnType) c);
        }
    }


    /**
     * @see net.sf.mzmine.userinterface.dialogs.alignmentresultcolumnselection.ColumnSet#getNumberOfSelectedCommonColumns()
     */
    public int getNumberOfSelectedCommonColumns() {
        return selectedCommonColumns.size();
    }

    /**
     * @see net.sf.mzmine.userinterface.dialogs.alignmentresultcolumnselection.ColumnSet#getNumberOfSelectedRawDataColumns()
     */
    public int getNumberOfSelectedRawDataColumns() {
        return selectedRawDataColumns.size();
    }

}