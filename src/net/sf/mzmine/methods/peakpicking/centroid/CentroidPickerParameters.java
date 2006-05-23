/*
    Copyright 2005 VTT Biotechnology

    This file is part of MZmine.

    MZmine is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    MZmine is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with MZmine; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/
package net.sf.mzmine.methods.peakpicking.centroid;

import java.io.Serializable;

import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import org.xml.sax.Attributes;

import net.sf.mzmine.methods.MethodParameters;

public class CentroidPickerParameters implements MethodParameters {

	private static final String myTagName = "CentroidPickerParameters";

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
		// TODO
		return null;
	}

    /**
     * Adds parameters to XML document
     */
    public Element addToXML(Document doc) {
		// TODO
		return null;
	}

    /**
     * Reads parameters from XML
     * @param doc XML document supposed to contain parameters for the method (may not contain them, though)
     */
    public void readFromXML(Element element) {
		// TODO
	}

	public MethodParameters clone() {
		// TODO
		return null;
	}

}