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
package net.sf.mzmine.methods.deisotoping.incompletefilter;

import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import net.sf.mzmine.methods.MethodParameters;


public class IncompleteIsotopePatternFilterParameters implements MethodParameters {

	private static final String tagName = "IncompleteIsotopePatternFilterParameters";

	private static final String minimumNumberOfPeaksAttributeName = "MinNumberOfPeaks";

	// Parameters and their default values

	public int minimumNumberOfPeaks = 2;

    /**
     * @return parameters in human readable form
     */
    public String toString() {
		return new String("Minimum number of peaks = " + minimumNumberOfPeaks);
	}

    /**
     *
     * @return parameters represented by XML element
     */
    public Element addToXML(Document doc) {

		Element e = doc.createElement(tagName);
		e.setAttribute(minimumNumberOfPeaksAttributeName, String.valueOf(minimumNumberOfPeaks));
		return e;

	}


    /**
     * Reads parameters from XML
     * @param doc XML document supposed to contain parameters for the method (may not contain them, though)
     */
    public void readFromXML(Element element) {

		// Find my element
		NodeList n = element.getElementsByTagName(tagName);
		if ((n==null) || (n.getLength()<1)) return;
		Element myElement = (Element)(n.item(0));

		// Set values
		String attrValue;
		attrValue = myElement.getAttribute(minimumNumberOfPeaksAttributeName);
		try { minimumNumberOfPeaks = Integer.parseInt(attrValue); } catch (NumberFormatException nfe) {}
	}

	public IncompleteIsotopePatternFilterParameters clone() {
		IncompleteIsotopePatternFilterParameters myClone = new IncompleteIsotopePatternFilterParameters();
		myClone.minimumNumberOfPeaks = minimumNumberOfPeaks;
		return myClone;
	}



}