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

package net.sf.mzmine.userinterface.mainwindow;

import java.util.Iterator;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.StorableParameterSet;
import net.sf.mzmine.util.NumberFormatter;
import net.sf.mzmine.util.NumberFormatter.FormatterType;

import org.dom4j.Element;

/**
 * 
 */
public class DesktopParameters implements StorableParameterSet {

    public static final String FORMAT_ELEMENT_NAME = "format";
    public static final String FORMAT_TYPE_ATTRIBUTE_NAME = "type";
    public static final String FORMAT_TYPE_ATTRIBUTE_MZ = "m/z";
    public static final String FORMAT_TYPE_ATTRIBUTE_RT = "Retention time";
    public static final String FORMAT_TYPE_ATTRIBUTE_INT = "Intensity";

    NumberFormatter mzFormat, rtFormat, intensityFormat;

    DesktopParameters() {
        mzFormat = new NumberFormatter(FormatterType.NUMBER, "0.000");
        rtFormat = new NumberFormatter(FormatterType.TIME, "m:ss");
        intensityFormat = new NumberFormatter(FormatterType.NUMBER, "0.00E0");
    }

    DesktopParameters(NumberFormatter mzFormat, NumberFormatter rtFormat,
            NumberFormatter intensityFormat) {
        this.mzFormat = mzFormat;
        this.rtFormat = rtFormat;
        this.intensityFormat = intensityFormat;
    }

    /**
     * @return Returns the intensityFormat.
     */
    NumberFormatter getIntensityFormat() {
        return intensityFormat;
    }

    /**
     * @return Returns the mzFormat.
     */
    NumberFormatter getMZFormat() {
        return mzFormat;
    }

    /**
     * @return Returns the rtFormat.
     */
    NumberFormatter getRTFormat() {
        return rtFormat;
    }

    /**
     * @see net.sf.mzmine.data.StorableParameterSet#exportValuesToXML(org.dom4j.Element)
     */
    public void exportValuesToXML(Element element) {
        Element mzFormatElement = element.addElement(FORMAT_ELEMENT_NAME);
        mzFormatElement.addAttribute(FORMAT_TYPE_ATTRIBUTE_NAME,
                FORMAT_TYPE_ATTRIBUTE_MZ);
        mzFormat.exportToXML(mzFormatElement);

        Element rtFormatElement = element.addElement(FORMAT_ELEMENT_NAME);
        rtFormatElement.addAttribute(FORMAT_TYPE_ATTRIBUTE_NAME,
                FORMAT_TYPE_ATTRIBUTE_RT);
        rtFormat.exportToXML(rtFormatElement);

        Element intensityFormatElement = element.addElement(FORMAT_ELEMENT_NAME);
        intensityFormatElement.addAttribute(FORMAT_TYPE_ATTRIBUTE_NAME,
                FORMAT_TYPE_ATTRIBUTE_INT);
        intensityFormat.exportToXML(intensityFormatElement);

    }

    /**
     * @see net.sf.mzmine.data.StorableParameterSet#importValuesFromXML(org.dom4j.Element)
     */
    public void importValuesFromXML(Element element) {
        Iterator i = element.elements(FORMAT_ELEMENT_NAME).iterator();
        while (i.hasNext()) {
            Element formatElement = (Element) i.next();
            if (formatElement.attributeValue(FORMAT_TYPE_ATTRIBUTE_NAME).equals(
                    FORMAT_TYPE_ATTRIBUTE_MZ))
                mzFormat.importFromXML(formatElement);
            if (formatElement.attributeValue(FORMAT_TYPE_ATTRIBUTE_NAME).equals(
                    FORMAT_TYPE_ATTRIBUTE_RT))
                rtFormat.importFromXML(formatElement);
            if (formatElement.attributeValue(FORMAT_TYPE_ATTRIBUTE_NAME).equals(
                    FORMAT_TYPE_ATTRIBUTE_INT))
                intensityFormat.importFromXML(formatElement);

        }

    }

    /**
     * @see net.sf.mzmine.data.ParameterSet#getParameterValue(net.sf.mzmine.data.Parameter)
     */
    public Object getParameterValue(Parameter parameter) {
        return null;
    }

    public DesktopParameters clone() {
        return new DesktopParameters(mzFormat.clone(), rtFormat.clone(),
                intensityFormat.clone());
    }

}
