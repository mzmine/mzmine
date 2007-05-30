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

import org.dom4j.Element;

import net.sf.mzmine.data.StorableParameterSet;

/**
 * 
 */
class CustomDBSearchParameters implements StorableParameterSet {

    public static final String fieldID = "ID";
    public static final String fieldMZ = "m/z";
    public static final String fieldRT = "Retention time (s)";
    public static final String fieldName = "Name";

    private String dataBaseFile = "";
    private String fieldSeparator = "\\t";
    private String[] fieldOrder = { fieldID, fieldMZ, fieldRT, fieldName };
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
    CustomDBSearchParameters(String dataBaseFile, String fieldSeparator,
            String[] fieldOrder, boolean ignoreFirstLine, double mzTolerance,
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
     * @see net.sf.mzmine.data.StorableParameterSet#exportValuesToXML(org.dom4j.Element)
     */
    public void exportValuesToXML(Element element) {
        // TODO Auto-generated method stub

    }

    /**
     * @see net.sf.mzmine.data.StorableParameterSet#importValuesFromXML(org.dom4j.Element)
     */
    public void importValuesFromXML(Element element) {
        // TODO Auto-generated method stub

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
