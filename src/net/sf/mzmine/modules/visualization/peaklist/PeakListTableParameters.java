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

package net.sf.mzmine.modules.visualization.peaklist;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.StorableParameterSet;
import net.sf.mzmine.modules.visualization.peaklist.table.CommonColumnType;
import net.sf.mzmine.modules.visualization.peaklist.table.DataFileColumnType;

import org.dom4j.Element;

public class PeakListTableParameters implements StorableParameterSet {

    private static final String COLUMN_ELEMENT = "column";

    enum PeakShapeMaximum {
        PEAKMAX, ROWMAX, GLOBALMAX
    };

    private Set<CommonColumnType> selectedCommonColumns;
    private Set<DataFileColumnType> selectedRawDataColumns;

    public PeakListTableParameters() {
        selectedCommonColumns = new TreeSet<CommonColumnType>();
        selectedRawDataColumns = new TreeSet<DataFileColumnType>();

        // selectedCommonColumns.add(CommonColumnType.ROWNUM);
        // selectedCommonColumns.add(CommonColumnType.AVGMZ);
        // selectedCommonColumns.add(CommonColumnType.AVGRT);
        selectedCommonColumns.add(CommonColumnType.COMMENT);
        // selectedRawDataColumns.add(DataFileColumnType.SHAPE);

    }

    /**
     * @see net.sf.mzmine.data.StorableParameterSet#exportValuesToXML(org.dom4j.Element)
     */
    public void exportValuesToXML(Element element) {
        /*
         * for (ColumnType col : selectedCommonColumns) { Element newElement =
         * element.addElement(SELECTED_COLUMN_ELEMENT);
         * newElement.addAttribute("type", "common");
         * newElement.addAttribute("name", col.getColumnName()); } for
         * (ColumnType col : selectedRawDataColumns) { Element newElement =
         * element.addElement(SELECTED_COLUMN_ELEMENT);
         * newElement.addAttribute("type", "rawdata");
         * newElement.addAttribute("name", col.getColumnName()); }
         */
    }

    /**
     * @see net.sf.mzmine.data.StorableParameterSet#importValuesFromXML(org.dom4j.Element)
     */
    public void importValuesFromXML(Element element) {
        List elementsList = element.elements(COLUMN_ELEMENT);
        Iterator elementsIterator = elementsList.iterator();

        while (elementsIterator.hasNext()) {
            Element nextElement = (Element) elementsIterator.next();
            String colType = nextElement.attributeValue("type");
            String colName = nextElement.attributeValue("name");
            if (colType.equals("common")) {
                for (CommonColumnType col : CommonColumnType.values()) {
                    if (col.getColumnName().equals(colName))
                        selectedCommonColumns.add(col);
                }
            }
            if (colType.equals("rawdata")) {
                for (DataFileColumnType col : DataFileColumnType.values()) {
                    if (col.getColumnName().equals(colName))
                        selectedRawDataColumns.add(col);
                }
            }
        }

    }

    public PeakListTableParameters clone() {
        PeakListTableParameters newSelection = new PeakListTableParameters();
        /*
         * for (ColumnType col : selectedCommonColumns)
         * newSelection.setColumnSelected(col, true); for (ColumnType col :
         * selectedRawDataColumns) newSelection.setColumnSelected(col, true);
         */
        return newSelection;
    }
    
    public PeakShapeMaximum getPeakShapeMaximum() {
        return PeakShapeMaximum.PEAKMAX;
    }
    
    void setPeakShapeMaximum(PeakShapeMaximum max) {
        
    }
    
    public int getRowHeight() {
        return 20;
    }
    
    void setRowHeight(int height) {
        
    }

    public boolean isColumnVisible(CommonColumnType type) {
        return true;
    }

    void setColumnVisible(CommonColumnType type, boolean visible) {

    }

    public int getColumnWidth(CommonColumnType type) {
        return 100;
    }

    public boolean isColumnVisible(DataFileColumnType type) {
        return true;
    }

    void setColumnVisible(DataFileColumnType type, boolean visible) {

    }

    public int getColumnWidth(DataFileColumnType type) {
        return 100;
    }

}