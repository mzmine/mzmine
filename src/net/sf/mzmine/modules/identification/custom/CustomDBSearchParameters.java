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

package net.sf.mzmine.modules.identification.custom;

import java.util.List;

import net.sf.mzmine.data.StorableParameterSet;

import org.dom4j.Element;

/**
 * 
 */
class CustomDBSearchParameters implements StorableParameterSet {

    public static final String DATABASEFILE_ELEMENT = "databasefile";
    public static final String FIELDSEPARATOR_ELEMENT = "fieldseparator";
    public static final String FIELDORDER_ELEMENT = "fieldorder";
    public static final String FIELD_ELEMENT = "field";
    public static final String IGNOREFIRSTLINE_ELEMENT = "ignorefirstline";
    public static final String MZTOLERANCE_ELEMENT = "mztolerance";
    public static final String RTTOLERANCE_ELEMENT = "rttolerance";
    public static final String UPDATEROWCOMMENT_ELEMENT = "updaterowcomment";

    public static final String fieldID = "ID";
    public static final String fieldMZ = "m/z";
    public static final String fieldRT = "Retention time (s)";
    public static final String fieldName = "Name";
    public static final String fieldFormula = "Formula";

    private String dataBaseFile = "";
    private char fieldSeparator = '\t';
    private Object[] fieldOrder = { fieldID, fieldMZ, fieldRT, fieldName, fieldFormula };
    private boolean ignoreFirstLine = false;
    private double mzTolerance = 1;
    private double rtTolerance = 60;
    private boolean updateRowComment = true;

    /**
     * 
     */
    CustomDBSearchParameters() {
    }

    /**
     * @param dataBaseFile
     * @param fieldSeparator
     * @param fieldOrder
     * @param ignoreFirstLine
     * @param mzTolerance
     * @param rtTolerance
     * @param updateRowComment
     */
    CustomDBSearchParameters(String dataBaseFile, char fieldSeparator,
            Object[] fieldOrder, boolean ignoreFirstLine, double mzTolerance,
            double rtTolerance, boolean updateRowComment) {
        this.dataBaseFile = dataBaseFile;
        this.fieldSeparator = fieldSeparator;
        this.fieldOrder = fieldOrder;
        this.ignoreFirstLine = ignoreFirstLine;
        this.mzTolerance = mzTolerance;
        this.rtTolerance = rtTolerance;
        this.updateRowComment = updateRowComment;
    }

    
    
    
    /**
     * @return Returns the dataBaseFile.
     */
    String getDataBaseFile() {
        return dataBaseFile;
    }

    
    /**
     * @param dataBaseFile The dataBaseFile to set.
     */
    void setDataBaseFile(String dataBaseFile) {
        this.dataBaseFile = dataBaseFile;
    }

    
    /**
     * @return Returns the fieldOrder.
     */
    Object[] getFieldOrder() {
        return fieldOrder;
    }

    
    /**
     * @param fieldOrder The fieldOrder to set.
     */
    void setFieldOrder(Object[] fieldOrder) {
        this.fieldOrder = fieldOrder;
    }

    
    /**
     * @return Returns the fieldSeparator.
     */
    char getFieldSeparator() {
        return fieldSeparator;
    }

    
    /**
     * @param fieldSeparator The fieldSeparator to set.
     */
    void setFieldSeparator(char fieldSeparator) {
        this.fieldSeparator = fieldSeparator;
    }

    
    /**
     * @return Returns the ignoreFirstLine.
     */
    boolean isIgnoreFirstLine() {
        return ignoreFirstLine;
    }

    
    /**
     * @param ignoreFirstLine The ignoreFirstLine to set.
     */
    void setIgnoreFirstLine(boolean ignoreFirstLine) {
        this.ignoreFirstLine = ignoreFirstLine;
    }

    
    /**
     * @return Returns the mzTolerance.
     */
    double getMzTolerance() {
        return mzTolerance;
    }

    
    /**
     * @param mzTolerance The mzTolerance to set.
     */
    void setMzTolerance(double mzTolerance) {
        this.mzTolerance = mzTolerance;
    }

    
    /**
     * @return Returns the rtTolerance.
     */
    double getRtTolerance() {
        return rtTolerance;
    }

    
    /**
     * @param rtTolerance The rtTolerance to set.
     */
    void setRtTolerance(double rtTolerance) {
        this.rtTolerance = rtTolerance;
    }

    
    /**
     * @return Returns the updateRowComment.
     */
    boolean isUpdateRowComment() {
        return updateRowComment;
    }

    
    /**
     * @param updateRowComment The updateRowComment to set.
     */
    void setUpdateRowComment(boolean updateRowComment) {
        this.updateRowComment = updateRowComment;
    }

    /**
     * @see net.sf.mzmine.data.StorableParameterSet#exportValuesToXML(org.dom4j.Element)
     */
    public void exportValuesToXML(Element element) {

        element.addElement(DATABASEFILE_ELEMENT).setText(dataBaseFile);
        element.addElement(FIELDSEPARATOR_ELEMENT).setText(String.valueOf(fieldSeparator));
        Element orderElement = element.addElement(FIELDORDER_ELEMENT);
        for (Object field : fieldOrder)
            orderElement.addElement(FIELD_ELEMENT).setText((String) field);
        element.addElement(IGNOREFIRSTLINE_ELEMENT).setText(
                String.valueOf(ignoreFirstLine));
        element.addElement(MZTOLERANCE_ELEMENT).setText(
                String.valueOf(mzTolerance));
        element.addElement(RTTOLERANCE_ELEMENT).setText(
                String.valueOf(rtTolerance));
        element.addElement(UPDATEROWCOMMENT_ELEMENT).setText(
                String.valueOf(updateRowComment));

    }

    /**
     * @see net.sf.mzmine.data.StorableParameterSet#importValuesFromXML(org.dom4j.Element)
     */
    public void importValuesFromXML(Element element) {

        dataBaseFile = element.elementText(DATABASEFILE_ELEMENT);
        fieldSeparator = element.elementText(FIELDSEPARATOR_ELEMENT).charAt(0);
        Element orderElement = element.element(FIELDORDER_ELEMENT);
        List fields = orderElement.elements(FIELD_ELEMENT);
        if (fields.size() == fieldOrder.length) {
            for (int i = 0; i < fieldOrder.length; i++) {
                Element fel = (Element) fields.get(i);
                fieldOrder[i] = fel.getText();
            }
        }

        ignoreFirstLine = Boolean.parseBoolean(element.elementText(IGNOREFIRSTLINE_ELEMENT));
        mzTolerance = Double.parseDouble(element.elementText(MZTOLERANCE_ELEMENT));
        rtTolerance = Double.parseDouble(element.elementText(RTTOLERANCE_ELEMENT));
        updateRowComment = Boolean.parseBoolean(element.elementText(UPDATEROWCOMMENT_ELEMENT));

    }

    public CustomDBSearchParameters clone() {
        return new CustomDBSearchParameters(dataBaseFile, fieldSeparator,
                fieldOrder, ignoreFirstLine, mzTolerance, rtTolerance,
                updateRowComment);
    }

    public String toString() {

        StringBuffer paramString = new StringBuffer();

        paramString.append("Database file: " + dataBaseFile + "\n");
        paramString.append("Field separator: " + fieldSeparator + "\n");
        paramString.append("Field order: " + fieldOrder + "\n");
        paramString.append("Ignore first line: " + ignoreFirstLine + "\n");
        paramString.append("m/z tolerance: " + mzTolerance + "\n");
        paramString.append("Retention time tolerance: " + rtTolerance + "\n");
        paramString.append("Update row comment: " + updateRowComment + "\n");

        return paramString.toString();
    }

}
