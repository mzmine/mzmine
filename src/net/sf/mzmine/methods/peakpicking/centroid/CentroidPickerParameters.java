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

package net.sf.mzmine.methods.peakpicking.centroid;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.methods.MethodParameters;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

class CentroidPickerParameters implements MethodParameters {

    private static final String tagName = "CentroidPickerParameters";

    private static final String binSizeAttributeName = "BinSize";
    private static final String chromatographicThresholdLevelAttributeName = "ChromatographicThresholdLevel";
    private static final String noiseLevelAttributeName = "NoiseLevel";
    private static final String minimumPeakHeightAttributeName = "MinimumPeakHeight";
    private static final String minimumPeakDurationAttributeName = "MinimumPeakDuration";
    private static final String mzToleranceAttributeName = "MZTolerance";
    private static final String intToleranceAttributeName = "IntTolerance";

    // Parameters and their default values

    public double binSize = 0.25;
    public double chromatographicThresholdLevel = 0.0;
    public double noiseLevel = 4.0;
    public double minimumPeakHeight = 15.0;
    public double minimumPeakDuration = 3.0;
    public double mzTolerance = 0.050;
    public double intTolerance = 0.20;

    public String toString() {
        String s = new String();
        s += "" + "M/Z bin size=" + binSize + " Da";
        s += ", " + "Chromatographic threshold level="
                + chromatographicThresholdLevel * 100.0 + "%";
        s += ", " + "Noise level=" + noiseLevel;
        s += ", " + "Minimum peak height=" + minimumPeakHeight;
        s += ", " + "Minimum peak duration=" + minimumPeakDuration + " seconds";
        s += ", " + "M/Z tolerance=" + mzTolerance + " Da";
        s += ", " + "Intensity tolerance=" + intTolerance * 100.0 + "%";
        return s;

    }

    /**
     * Adds parameters to XML document
     */
    public Element addToXML(Document doc) {

        Element e = doc.createElement(tagName);
        e.setAttribute(binSizeAttributeName, String.valueOf(binSize));
        e.setAttribute(chromatographicThresholdLevelAttributeName,
                String.valueOf(chromatographicThresholdLevel));
        e.setAttribute(noiseLevelAttributeName, String.valueOf(noiseLevel));
        e.setAttribute(minimumPeakHeightAttributeName,
                String.valueOf(minimumPeakHeight));
        e.setAttribute(minimumPeakDurationAttributeName,
                String.valueOf(minimumPeakDuration));
        e.setAttribute(mzToleranceAttributeName, String.valueOf(mzTolerance));
        e.setAttribute(intToleranceAttributeName, String.valueOf(intTolerance));

        return e;
    }

    /**
     * Reads parameters from XML
     * 
     * @param doc
     *            XML document supposed to contain parameters for the method
     *            (may not contain them, though)
     */
    public void readFromXML(Element element) {
        // Find my element
        NodeList n = element.getElementsByTagName(tagName);
        if ((n == null) || (n.getLength() < 1))
            return;
        Element myElement = (Element) (n.item(0));

        // Set values
        String attrValue;
        attrValue = myElement.getAttribute(binSizeAttributeName);
        try {
            binSize = Double.parseDouble(attrValue);
        } catch (NumberFormatException nfe) {
        }

        attrValue = myElement.getAttribute(chromatographicThresholdLevelAttributeName);
        try {
            chromatographicThresholdLevel = Double.parseDouble(attrValue);
        } catch (NumberFormatException nfe) {
        }

        attrValue = myElement.getAttribute(noiseLevelAttributeName);
        try {
            noiseLevel = Double.parseDouble(attrValue);
        } catch (NumberFormatException nfe) {
        }

        attrValue = myElement.getAttribute(minimumPeakHeightAttributeName);
        try {
            minimumPeakHeight = Double.parseDouble(attrValue);
        } catch (NumberFormatException nfe) {
        }

        attrValue = myElement.getAttribute(minimumPeakDurationAttributeName);
        try {
            minimumPeakDuration = Double.parseDouble(attrValue);
        } catch (NumberFormatException nfe) {
        }

        attrValue = myElement.getAttribute(mzToleranceAttributeName);
        try {
            mzTolerance = Double.parseDouble(attrValue);
        } catch (NumberFormatException nfe) {
        }

        attrValue = myElement.getAttribute(intToleranceAttributeName);
        try {
            intTolerance = Double.parseDouble(attrValue);
        } catch (NumberFormatException nfe) {
        }

    }

	public Parameter[] getParameters() {
		// TODO Auto-generated method stub
		return null;
	}

}