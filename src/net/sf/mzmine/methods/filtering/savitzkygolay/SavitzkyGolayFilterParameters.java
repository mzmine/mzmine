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
package net.sf.mzmine.methods.filtering.savitzkygolay;

import java.io.Serializable;

import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import org.xml.sax.Attributes;

import net.sf.mzmine.io.RawDataFile;

import net.sf.mzmine.methods.MethodParameters;

/**
 * This class represents parameter for the Savizky-Golay filter
 */
public class SavitzkyGolayFilterParameters implements MethodParameters {

	/**
     * These Strings are used to access parameter values in an XML element
     */
	private static final String tagName = "SavitzkyGolayFilterParameters";
	private static final String numberOfDataPointsAttributeName = "NumberOfDataPoints";

	/**
	 * Number of datapoints used for fitting
	 */
	public int numberOfDataPoints = 5;

	private RawDataFile[] rawDataFiles;


	public String toString() {
		return new String("Number of datapoints = " + numberOfDataPoints);
	}

    /**
     * Adds parameters to XML document
     */
    public Element addToXML(Document doc) {

		Element e = doc.createElement(tagName);
		e.setAttribute(numberOfDataPointsAttributeName, String.valueOf(numberOfDataPoints));
		return e;

	}

    /**
     * Reads parameters from XML
     * @param doc XML document supposed to contain parameters for the method (may not contain them, though)
     */
    public void readFromXML(Element element) {

		String attrValue;
		attrValue = element.getAttribute(numberOfDataPointsAttributeName);
		try { numberOfDataPoints = Integer.parseInt(attrValue); } catch (NumberFormatException nfe) {}

	}

	public String getTagName() {
		return tagName;
	}

	/**
	 *
	 */
	public RawDataFile[] getSelectedRawDataFiles() {
		return rawDataFiles;
	}

	/**
	 *
	 */
	public void setSelectedRawDataFiles(RawDataFile[] rawDataFiles) {
		this.rawDataFiles = rawDataFiles;
	}

}