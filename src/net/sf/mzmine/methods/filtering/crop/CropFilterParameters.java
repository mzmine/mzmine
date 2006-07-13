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
package net.sf.mzmine.methods.filtering.crop;

import net.sf.mzmine.methods.MethodParameters;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This class represents parameter for the crop filter method
 */
class CropFilterParameters implements MethodParameters {

    private static final String tagName = "CropFilterParameters";
    private static final String minMZAttibuteName = "MinimumMZ";
    private static final String maxMZAttibuteName = "MaximumMZ";
    private static final String minRTAttibuteName = "MinimumRT";
    private static final String maxRTAttibuteName = "MaximumRT";

    /**
     * Boundaries of cropped raw data region
     */
    double minMZ = (double) 100;
    double maxMZ = (double) 1000;
    double minRT = (double) 10;
    double maxRT = (double) 600;

    /**
     * @return parameters in human readable form
     */
    public String toString() {
        String s = new String();
        s += "Minimum M/Z = " + minMZ + ", ";
        s += "Maximum M/Z = " + maxMZ + ", ";
        s += "Minimum RT = " + minRT + ", ";
        s += "Maximum RT = " + maxRT;

        return s;
    }

    /**
     * 
     * @return parameters represented by XML element
     */
    public Element addToXML(Document doc) {

        Element e = doc.createElement(tagName);
        e.setAttribute(minMZAttibuteName, String.valueOf(minMZ));
        e.setAttribute(maxMZAttibuteName, String.valueOf(maxMZ));
        e.setAttribute(minRTAttibuteName, String.valueOf(minRT));
        e.setAttribute(maxRTAttibuteName, String.valueOf(maxRT));
        return e;

    }

    /**
     * Reads parameters from XML
     * 
     * @param doc
     *            XML document containing all available parameters (may not
     *            contain tag for this
     */
    public void readFromXML(Element element) {

        String attrValue;
        attrValue = element.getAttribute(minMZAttibuteName);
        try {
            minMZ = Double.parseDouble(attrValue);
        } catch (NumberFormatException nfe) {
        }

        attrValue = element.getAttribute(maxMZAttibuteName);
        try {
            maxMZ = Double.parseDouble(attrValue);
        } catch (NumberFormatException nfe) {
        }

        attrValue = element.getAttribute(minRTAttibuteName);
        try {
            minRT = Double.parseDouble(attrValue);
        } catch (NumberFormatException nfe) {
        }

        attrValue = element.getAttribute(maxRTAttibuteName);
        try {
            maxRT = Double.parseDouble(attrValue);
        } catch (NumberFormatException nfe) {
        }

    }

}