/*
    Copyright 2005-2006 VTT Biotechnology

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
package net.sf.mzmine.methods.deisotoping.simplegrouper;

import java.util.HashSet;

import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import net.sf.mzmine.methods.MethodParameters;


/**
 * This class represents parameter settings for the simple isotopic peaks grouper method
 *
 * @version 31 March 2006
 */
public class SimpleIsotopicPeaksGrouperParameters implements MethodParameters {

	// CONSTANTS FOR WRITING PARAMETERS TO XML FILE

	private static final String tagName = "SimpleIsotopicPeaksGrouperParameters";

	private static final String mzToleranceAttributeName = "MZTolerance";
	private static final String rtToleranceAttributeName = "RTTolerance";
	private static final String monotonicShapeAttributeName = "MonotonicShape";
	private static final String chargeStateOneAttributeName = "ChargeOne";
	private static final String chargeStateTwoAttributeName = "ChargeTwo";
	private static final String chargeStateThreeAttributeName = "ChargeThree";


	// CONSTANTS FOR REPRESENTING DIFFERENT CHARGES

	public static final Integer chargeOne = 1;
	public static final Integer chargeTwo = 2;
	public static final Integer chargeThree = 3;


	// PARAMETERS AND THEIR DEFAULT VALUES

	public double mzTolerance = (double)0.05;
	public double rtTolerance = (double)5;
	public boolean monotonicShape = true;
	public HashSet<Integer> chargeStates;



	/**
	 * Initializes parameter object and sets default parameter values
	 */
	public SimpleIsotopicPeaksGrouperParameters() {
		chargeStates = new HashSet<Integer>();
		chargeStates.add(chargeOne);
	}

    /**
     * @return parameters in human readable form
     */
    public String toString() {
		// TODO
		return new String();
	}

    /**
     *
     * @return parameters represented by XML element
     */
    public Element addToXML(Document doc) {

		Element e = doc.createElement(tagName);
		// TODO
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
		// TODO
	}

	public SimpleIsotopicPeaksGrouperParameters clone() {
		SimpleIsotopicPeaksGrouperParameters myClone = new SimpleIsotopicPeaksGrouperParameters();
		// TODO
		return myClone;
	}





}