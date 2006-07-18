/*
 * Copyright 2006 The MZmine Development Team This file is part of MZmine.
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. MZmine is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with MZmine; if not, write to the Free Software Foundation, Inc., 51
 * Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */
package net.sf.mzmine.methods.filtering.chromatographicmedian;

import net.sf.mzmine.methods.MethodParameters;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * This class represents parameter for the chromatographic median filter method
 */
class ChromatographicMedianFilterParameters implements MethodParameters {

    /**
     * These Strings are used to access parameter values in an XML element
     */
    private static final String tagName = "ChromatographicMedianFilterParameters";
    private static final String mzToleranceAttributeName = "MZTolerance";
    private static final String oneSidedWindowLengthAttributeName = "OneSidedWindowLength";

    /**
     * Maximum tolerance in M/Z direction ("stripe-width")
     */
    public double mzTolerance = (double) 0.1;

    /**
     * Median filter window length (one-sided, in scans)
     */
    public int oneSidedWindowLength = 1;

    /**
     * @return parameters in human readable form
     */
    public String toString() {
        return new String("One-sided window length = " + oneSidedWindowLength
                + "scans, M/Z tolerance = " + mzTolerance);
    }

    /**
     * @return parameters represented by XML element
     */
    public Element addToXML(Document doc) {

        Element e = doc.createElement(tagName);
        e.setAttribute(mzToleranceAttributeName, String.valueOf(mzTolerance));
        e.setAttribute(oneSidedWindowLengthAttributeName,
                String.valueOf(oneSidedWindowLength));
        return e;

    }

    /**
     * Reads parameters from XML
     * 
     * @param doc XML document containing all available parameters (may not
     *            contain tag for this
     */
    public void readFromXML(Element element) {

        // Find my element
        NodeList n = element.getElementsByTagName(tagName);
        if ((n == null) || (n.getLength() < 1))
            return;
        Element myElement = (Element) (n.item(0));

        // Set values
        String attrValue;
        attrValue = myElement.getAttribute(mzToleranceAttributeName);
        try {
            mzTolerance = Double.parseDouble(attrValue);
        } catch (NumberFormatException nfe) {
        }
        attrValue = myElement.getAttribute(oneSidedWindowLengthAttributeName);
        try {
            oneSidedWindowLength = Integer.parseInt(attrValue);
        } catch (NumberFormatException nfe) {
        }
    }

}