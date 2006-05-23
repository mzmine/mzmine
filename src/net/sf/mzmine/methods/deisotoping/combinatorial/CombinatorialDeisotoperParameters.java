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
package net.sf.mzmine.methods.deisotoping.combinatorial;

import net.sf.mzmine.methods.MethodParameters;

import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;


/**
 * Parameters for combinatorial deisotoper.
 * Since this method is only a stub currently, this is an empty class.
 */
public class CombinatorialDeisotoperParameters implements MethodParameters {

	private static final String tagName = "CombinatorialDeisotoperParameters";

    public String toString() {
		return new String();
	}

    /**
     * Adds parameters to XML document
     */
    public Element addToXML(Document doc) {
		Element e = doc.createElement(tagName);
		return e;
	}

    /**
     * Reads parameters from XML
     * @param doc XML document supposed to contain parameters for the method (may not contain them, though)
     */
    public void readFromXML(Element element) {
		// no parameters => do nothing
	}

	public MethodParameters clone() {
		CombinatorialDeisotoperParameters myClone = new CombinatorialDeisotoperParameters();
		return myClone;
	}

}