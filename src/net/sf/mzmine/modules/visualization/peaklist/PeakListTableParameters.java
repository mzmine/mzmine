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

import java.util.EnumSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.StorableParameterSet;
import net.sf.mzmine.modules.visualization.peaklist.table.CommonColumnType;
import net.sf.mzmine.modules.visualization.peaklist.table.DataFileColumnType;

import org.dom4j.Element;

public class PeakListTableParameters implements StorableParameterSet {

    private static final int DEFAULT_COLUMN_WIDTH = 100;
    private static final int DEFAULT_ROW_HEIGHT = 20;

    private static final String COLUMN_ELEMENT = "column";
    private static final String TYPE_ATTRIBUTE = "type";
    private static final String TYPE_COMMON = "common";
    private static final String TYPE_DATAFILE = "datafile";
    private static final String NAME_ATTRIBUTE = "name";
    private static final String VISIBLE_ELEMENT = "visible";
    private static final String WIDTH_ELEMENT = "width";
    private static final String ROWHEIGHT_ELEMENT = "rowheight";
    private static final String PEAKSHAPENORM_ELEMENT = "peakshapenormalization";

    private Hashtable<CommonColumnType, Boolean> commonColumnsVisibility;
    private Hashtable<DataFileColumnType, Boolean> dataFileColumnsVisibility;
    private Hashtable<CommonColumnType, Integer> commonColumnsWidth;
    private Hashtable<DataFileColumnType, Integer> dataFileColumnsWidth;

    private PeakShapeNormalization peakShapeNormalization;

    private int rowHeight;

    public PeakListTableParameters() {

        commonColumnsVisibility = new Hashtable<CommonColumnType, Boolean>();
        dataFileColumnsVisibility = new Hashtable<DataFileColumnType, Boolean>();
        commonColumnsWidth = new Hashtable<CommonColumnType, Integer>();
        dataFileColumnsWidth = new Hashtable<DataFileColumnType, Integer>();

        peakShapeNormalization = PeakShapeNormalization.PEAKMAX;

        rowHeight = DEFAULT_ROW_HEIGHT;

    }

    /**
     * @see net.sf.mzmine.data.StorableParameterSet#exportValuesToXML(org.dom4j.Element)
     */
    public void exportValuesToXML(Element element) {

        EnumSet<CommonColumnType> commonColumns = EnumSet.noneOf(CommonColumnType.class);
        commonColumns.addAll(commonColumnsVisibility.keySet());
        commonColumns.addAll(commonColumnsWidth.keySet());

        for (CommonColumnType column : commonColumns) {
            Element newElement = element.addElement(COLUMN_ELEMENT);
            newElement.addAttribute(TYPE_ATTRIBUTE, TYPE_COMMON);
            newElement.addAttribute(NAME_ATTRIBUTE, column.toString());
            newElement.addElement(VISIBLE_ELEMENT).setText(
                    String.valueOf(isColumnVisible(column)));
            newElement.addElement(WIDTH_ELEMENT).setText(
                    String.valueOf(getColumnWidth(column)));
        }

        EnumSet<DataFileColumnType> dataFileColumns = EnumSet.noneOf(DataFileColumnType.class);
        dataFileColumns.addAll(dataFileColumnsVisibility.keySet());
        dataFileColumns.addAll(dataFileColumnsWidth.keySet());

        for (DataFileColumnType column : dataFileColumns) {
            Element newElement = element.addElement(COLUMN_ELEMENT);
            newElement.addAttribute(TYPE_ATTRIBUTE, TYPE_DATAFILE);
            newElement.addAttribute(NAME_ATTRIBUTE, column.toString());
            newElement.addElement(VISIBLE_ELEMENT).setText(
                    String.valueOf(isColumnVisible(column)));
            newElement.addElement(WIDTH_ELEMENT).setText(
                    String.valueOf(getColumnWidth(column)));
        }

        element.addElement(ROWHEIGHT_ELEMENT).setText(String.valueOf(rowHeight));
        element.addElement(PEAKSHAPENORM_ELEMENT).setText(peakShapeNormalization.name());

    }

    /**
     * @see net.sf.mzmine.data.StorableParameterSet#importValuesFromXML(org.dom4j.Element)
     */
    public void importValuesFromXML(Element element) {

        List elementsList = element.elements(COLUMN_ELEMENT);
        Iterator elementsIterator = elementsList.iterator();

        while (elementsIterator.hasNext()) {
            Element columnElement = (Element) elementsIterator.next();
            String colType = columnElement.attributeValue(TYPE_ATTRIBUTE);
            String colName = columnElement.attributeValue(NAME_ATTRIBUTE);
            int colWidth = Integer.parseInt(columnElement.elementText(WIDTH_ELEMENT));
            boolean colVisible = Boolean.parseBoolean(columnElement.elementText(VISIBLE_ELEMENT));

            if (colType.equals(TYPE_COMMON)) {
                CommonColumnType column = CommonColumnType.valueOf(colName);
                setColumnWidth(column, colWidth);
                setColumnVisible(column, colVisible);
            }
            if (colType.equals(TYPE_DATAFILE)) {
                DataFileColumnType column = DataFileColumnType.valueOf(colName);
                setColumnWidth(column, colWidth);
                setColumnVisible(column, colVisible);
            }
        }

        String rowHeightStr = element.elementText(ROWHEIGHT_ELEMENT);
        if (rowHeightStr != null)
            rowHeight = Integer.parseInt(rowHeightStr);

        String peakShapeStr = element.elementText(PEAKSHAPENORM_ELEMENT);
        if (peakShapeStr != null)
            peakShapeNormalization = PeakShapeNormalization.valueOf(peakShapeStr);

    }

    public PeakListTableParameters clone() {

        PeakListTableParameters newParameters = new PeakListTableParameters();

        for (CommonColumnType commonColumn : commonColumnsVisibility.keySet()) {
            newParameters.setColumnVisible(commonColumn,
                    commonColumnsVisibility.get(commonColumn));
        }

        for (CommonColumnType commonColumn : commonColumnsWidth.keySet()) {
            newParameters.setColumnWidth(commonColumn,
                    commonColumnsWidth.get(commonColumn));
        }

        for (DataFileColumnType dataFileColumn : dataFileColumnsVisibility.keySet()) {
            newParameters.setColumnVisible(dataFileColumn,
                    dataFileColumnsVisibility.get(dataFileColumn));
        }

        for (DataFileColumnType dataFileColumn : dataFileColumnsWidth.keySet()) {
            newParameters.setColumnWidth(dataFileColumn,
                    dataFileColumnsWidth.get(dataFileColumn));
        }

        newParameters.setRowHeight(rowHeight);
        newParameters.setPeakShapeNormalization(peakShapeNormalization);

        return newParameters;

    }

    public PeakShapeNormalization getPeakShapeNormalization() {
        return peakShapeNormalization;
    }

    public void setPeakShapeNormalization(PeakShapeNormalization max) {
        this.peakShapeNormalization = max;
    }

    public int getRowHeight() {
        return rowHeight;
    }

    public void setRowHeight(int height) {
        this.rowHeight = height;
    }

    public boolean isColumnVisible(CommonColumnType type) {
        Boolean visible = commonColumnsVisibility.get(type);
        if (visible == null)
            return true;
        return visible;
    }

    public void setColumnVisible(CommonColumnType type, boolean visible) {
        commonColumnsVisibility.put(type, visible);
    }

    public int getColumnWidth(CommonColumnType type) {
        Integer width = commonColumnsWidth.get(type);
        if (width == null)
            return DEFAULT_COLUMN_WIDTH;
        return width;
    }

    public void setColumnWidth(CommonColumnType type, int width) {
        commonColumnsWidth.put(type, width);
    }

    public boolean isColumnVisible(DataFileColumnType type) {
        Boolean visible = dataFileColumnsVisibility.get(type);
        if (visible == null)
            return true;
        return visible;
    }

    public void setColumnVisible(DataFileColumnType type, boolean visible) {
        dataFileColumnsVisibility.put(type, visible);
    }

    public int getColumnWidth(DataFileColumnType type) {
        Integer width = dataFileColumnsWidth.get(type);
        if (width == null)
            return DEFAULT_COLUMN_WIDTH;
        return width;
    }

    public void setColumnWidth(DataFileColumnType type, int width) {
        dataFileColumnsWidth.put(type, width);
    }

	public Object getParameterValue(Parameter parameter) {
		return null;
	}

	public Parameter[] getParameters() {
		return null;
	}

}