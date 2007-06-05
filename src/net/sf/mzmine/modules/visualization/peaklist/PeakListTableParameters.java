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

import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import net.sf.mzmine.data.StorableParameterSet;
import net.sf.mzmine.modules.visualization.peaklist.table.CommonColumnType;
import net.sf.mzmine.modules.visualization.peaklist.table.DataFileColumnType;

import org.dom4j.Element;

public class PeakListTableParameters implements StorableParameterSet {

    private static final int DEFAULT_COLUMN_WIDTH = 100;
    private static final int DEFAULT_ROW_HEIGHT = 20;
    
    private static final String COLUMN_ELEMENT = "column";



    private Hashtable<CommonColumnType, Boolean> commonColumnsVisibility;
    private Hashtable<DataFileColumnType, Boolean> dataFileColumnsVisibility;
    private Hashtable<CommonColumnType, Integer> commonColumnsWidth;
    private Hashtable<DataFileColumnType, Integer> dataFileColumnsWidth;
    
    private PeakShapeMaximum peakShapeMaximum;
    
    private int rowHeight;
    
    public PeakListTableParameters() {
        
        commonColumnsVisibility = new Hashtable<CommonColumnType, Boolean>();
        dataFileColumnsVisibility = new Hashtable<DataFileColumnType, Boolean>();
        commonColumnsWidth = new Hashtable<CommonColumnType, Integer>();
        dataFileColumnsWidth = new Hashtable<DataFileColumnType, Integer>();
        
        peakShapeMaximum = PeakShapeMaximum.PEAKMAX;
        
        rowHeight = DEFAULT_ROW_HEIGHT;
        
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
                }
            }
            if (colType.equals("rawdata")) {
                for (DataFileColumnType col : DataFileColumnType.values()) {
                }
            }
        }

    }

    public PeakListTableParameters clone() {
        
        PeakListTableParameters newParameters = new PeakListTableParameters();
        
        for (CommonColumnType commonColumn : commonColumnsVisibility.keySet()) {
            newParameters.setColumnVisible(commonColumn, commonColumnsVisibility.get(commonColumn));
        }
        
        for (CommonColumnType commonColumn : commonColumnsWidth.keySet()) {
            newParameters.setColumnWidth(commonColumn, commonColumnsWidth.get(commonColumn));
        }
        
        for (DataFileColumnType dataFileColumn : dataFileColumnsVisibility.keySet()) {
            newParameters.setColumnVisible(dataFileColumn, dataFileColumnsVisibility.get(dataFileColumn));
        }
        
        for (DataFileColumnType dataFileColumn : dataFileColumnsWidth.keySet()) {
            newParameters.setColumnWidth(dataFileColumn, dataFileColumnsWidth.get(dataFileColumn));
        }
        
        newParameters.setRowHeight(rowHeight);
        newParameters.setPeakShapeMaximum(peakShapeMaximum);
        
        return newParameters;
        
    }
    
    public PeakShapeMaximum getPeakShapeMaximum() {
        return peakShapeMaximum;
    }
    
    void setPeakShapeMaximum(PeakShapeMaximum max) {
        this.peakShapeMaximum = max;
    }
    
    public int getRowHeight() {
        return rowHeight;
    }
    
    void setRowHeight(int height) {
        this.rowHeight = height;
    }

    public boolean isColumnVisible(CommonColumnType type) {
        Boolean visible = commonColumnsVisibility.get(type);
        if (visible == null) return true;
        return visible;
    }

    void setColumnVisible(CommonColumnType type, boolean visible) {
        commonColumnsVisibility.put(type, visible);
    }

    public int getColumnWidth(CommonColumnType type) {
        Integer width = commonColumnsWidth.get(type);
        if (width == null) return DEFAULT_COLUMN_WIDTH;
        return width;
    }
    
    void setColumnWidth(CommonColumnType type, int width) {
        commonColumnsWidth.put(type, width);
    }
    
    public boolean isColumnVisible(DataFileColumnType type) {
        Boolean visible = dataFileColumnsVisibility.get(type);
        if (visible == null) return true;
        return visible;
    }

    void setColumnVisible(DataFileColumnType type, boolean visible) {
        dataFileColumnsVisibility.put(type, visible);
    }

    public int getColumnWidth(DataFileColumnType type) {
        Integer width = dataFileColumnsWidth.get(type);
        if (width == null) return DEFAULT_COLUMN_WIDTH;
        return width;
    }

    void setColumnWidth(DataFileColumnType type, int width) {
        dataFileColumnsWidth.put(type, width);
    }

}