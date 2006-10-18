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

package net.sf.mzmine.methods.alignment.join;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.methods.MethodParameters;

import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;


public class JoinAlignerParameters implements MethodParameters {

	private static final String tagName = "JoinAlignerParameters";
	private static final String paramMZvsRTBalanceAttributeName = "MZvsRTBalance";
	private static final String paramMZToleranceAttributeName = "MZTolerance";
	private static final String paramRTToleranceUseAbsAttributeName = "RTToleranceUseAbs";
	private static final String paramRTToleranceAbsAttributeName = "RTToleranceAbs";
	private static final String paramRTTolerancePercentAttributeName = "RTTolerancePercent";

	public double paramMZvsRTBalance = (double)10;		// These are the default parameter values for alignment
	public double paramMZTolerance = (double)0.2;
	public boolean paramRTToleranceUseAbs = false;
	public double paramRTToleranceAbs = (double)15;
	public double paramRTTolerancePercent = 0.01;



    public String toString() {
		String s = new String();

		s += ""   + "M/Z vs. RT balance = " + paramMZvsRTBalance;
		s += ", " + "M/Z tolerance = " + paramMZTolerance;
		s += ", " + "Use absolute RT tolerance = " + paramRTToleranceUseAbs;
		s += ", " + "Absolute RT tolerance = " + paramRTToleranceUseAbs;
		s += ", " + "Relative RT tolerance = " + paramRTTolerancePercent;

		return s;
	}

    /**
     * Adds parameters to XML document
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

	public MethodParameters clone() {
		JoinAlignerParameters myClone = new JoinAlignerParameters();

		myClone.paramMZvsRTBalance = paramMZvsRTBalance;
		myClone.paramMZTolerance = paramMZTolerance;
		myClone.paramRTToleranceUseAbs = paramRTToleranceUseAbs;
		myClone.paramRTToleranceAbs = paramRTToleranceAbs;
		myClone.paramRTTolerancePercent = paramRTTolerancePercent;

		return myClone;
	}

	public Parameter[] getParameters() {
		// TODO Auto-generated method stub
		return null;
	}

}