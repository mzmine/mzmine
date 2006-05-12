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
package net.sf.mzmine.methods.filtering.zoomscan;

import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import net.sf.mzmine.io.RawDataFile;

import net.sf.mzmine.methods.MethodParameters;

/**
 * This class represents parameter for the zoom scan filter method
 */
public class ZoomScanFilterParameters implements MethodParameters {

	private static final String tagName = "ZoomScanFilterParameters";
	private static final String minMZRangeAttibuteName = "MinimumMZRage";

	/**
	 * Minimum required M/Z range width
	 */
	public double minMZRange = (double)100;

	private RawDataFile[] rawDataFiles;


    /**
     * @return parameters in human readable form
     */
    public String toString() {
		return new String("Minimum M/Z range = " + minMZRange + "Da");
	}

    /**
     *
     * @return parameters represented by XML element
     */
    public Element addToXML(Document doc) {

		Element e = doc.createElement(tagName);
		e.setAttribute(minMZRangeAttibuteName, String.valueOf(minMZRange));
		return e;

	}

    /**
     * Reads parameters from XML
     * @param doc XML document containing all available parameters (may not contain tag for this
     */
    public void readFromXML(Element element) {

		String attrValue;
		attrValue = element.getAttribute(minMZRangeAttibuteName);
		try { minMZRange = Double.parseDouble(attrValue); } catch (NumberFormatException nfe) {}
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