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
package net.sf.mzmine.methods.rawdata;
import java.io.Serializable;

import org.xml.sax.Attributes;

public class MeanFilterParameters implements FilterParameters, Serializable {

	private static final String myTagName = "MeanFilterParameters";
	private static final String windowLengthAttibuteName = "WindowLength";

	// One-sided window length. Value is in MZ. True window size is two times this (plus-minus)
	public double windowLength = (double)0.1;

	public Class getFilterClass() {
		return MeanFilter.class;
	}

	/**
	 * This method returns a string containing XML-tag with current parameter values
	 */
	public String writeParameterTag() {
		String s = "<";
		s = s.concat(myTagName);
		s = s.concat(" " + windowLengthAttibuteName + "=\"" + windowLength + "\"");
		s = s.concat("/>");
		return s;
	}

	/**
	 * This method returns the name of XML tag used for storing parameters.
	 */
	public String getParameterTagName() { return myTagName; }

	/**
	 * This method sets the current parameter values according to XML Attributes-object
	 */
	public boolean loadXMLAttributes(Attributes atr) {

		try { windowLength = Double.parseDouble(atr.getValue(windowLengthAttibuteName)); } catch (NumberFormatException e) { return false; }
		return true;
	}

}