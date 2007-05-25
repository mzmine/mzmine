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

package net.sf.mzmine.modules.visualization.alignmentresult;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JComponent;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.StorableParameterSet;
import net.sf.mzmine.userinterface.dialogs.alignmentresultcolumnselection.ColumnSet;
import net.sf.mzmine.userinterface.dialogs.alignmentresultcolumnselection.ColumnType;

import org.dom4j.Element;

public class AlignmentResultTableColumns implements ColumnSet,
        StorableParameterSet {

    private static final String SELECTED_COLUMN_ELEMENT = "selectedcolumn";
    
    // type for common columns
    public enum CommonColumnType implements ColumnType {
        ROWNUM("ID", Integer.class), 
        AVGMZ("m/z", Double.class), 
        AVGRT("RT", Double.class),
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
        STATUS("Status", JComponent.class),
        SHAPE("Peak shape", JComponent.class),
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

    public AlignmentResultTableColumns() {
        selectedCommonColumns = new TreeSet<CommonColumnType>();
        selectedRawDataColumns = new TreeSet<RawDataColumnType>();
        
        selectedCommonColumns.add(CommonColumnType.ROWNUM);
        selectedCommonColumns.add(CommonColumnType.AVGMZ);
        selectedCommonColumns.add(CommonColumnType.AVGRT);
        selectedCommonColumns.add(CommonColumnType.COMMENT);
        selectedRawDataColumns.add(RawDataColumnType.SHAPE);
        
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
     * @see net.sf.mzmine.data.StorableParameterSet#exportValuesToXML(org.dom4j.Element)
     */
    public void exportValuesToXML(Element element) {
        for (ColumnType col : selectedCommonColumns) {
            Element newElement = element.addElement(SELECTED_COLUMN_ELEMENT);
            newElement.addAttribute("type", "common");
            newElement.addAttribute("name", col.getColumnName());
        }
        for (ColumnType col : selectedRawDataColumns) {
            Element newElement = element.addElement(SELECTED_COLUMN_ELEMENT);
            newElement.addAttribute("type", "rawdata");
            newElement.addAttribute("name", col.getColumnName());
        }

    }

    /**
     * @see net.sf.mzmine.data.StorableParameterSet#importValuesFromXML(org.dom4j.Element)
     */
    public void importValuesFromXML(Element element) {
        List elementsList = element.elements(SELECTED_COLUMN_ELEMENT);
        Iterator elementsIterator = elementsList.iterator();

        while (elementsIterator.hasNext()) {
            Element nextElement = (Element) elementsIterator.next();
            String colType = nextElement.attributeValue("type");
            String colName = nextElement.attributeValue("name");
            if (colType.equals("common")) {
                for (CommonColumnType col : CommonColumnType.values()) {
                    if (col.getColumnName().equals(colName)) selectedCommonColumns.add(col);
                }
            }
            if (colType.equals("rawdata")) {
                for (RawDataColumnType col : RawDataColumnType.values()) {
                    if (col.getColumnName().equals(colName)) selectedRawDataColumns.add(col);
                }
            }
        }

    }

    /**
     * @see net.sf.mzmine.data.ParameterSet#getParameterValue(net.sf.mzmine.data.Parameter)
     */
    public Object getParameterValue(Parameter parameter) {
        return null;
    }

    public AlignmentResultTableColumns clone() {
        AlignmentResultTableColumns newSelection = new AlignmentResultTableColumns();
        for (ColumnType col : selectedCommonColumns)
            newSelection.setColumnSelected(col, true);
        for (ColumnType col : selectedRawDataColumns)
            newSelection.setColumnSelected(col, true);
        return newSelection;
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