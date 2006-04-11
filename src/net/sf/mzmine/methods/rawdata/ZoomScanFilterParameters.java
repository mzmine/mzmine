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
import net.sf.mzmine.alignmentresultmethods.*;
import net.sf.mzmine.alignmentresultvisualizers.*;
import net.sf.mzmine.datastructures.*;
import net.sf.mzmine.obsoletedistributionframework.*;
import net.sf.mzmine.peaklistmethods.*;
import net.sf.mzmine.rawdatamethods.*;
import net.sf.mzmine.rawdatavisualizers.*;
import net.sf.mzmine.userinterface.*;
import net.sf.mzmine.util.*;


import java.io.Serializable;
import org.xml.sax.Attributes;

public class ZoomScanFilterParameters implements FilterParameters, Serializable {

	private static final String myTagName = "ZoomScanFilterParameters";
	private static final String minMZRangeAttibuteName = "MinimumMZRage";

	// One-sided window length. Value is in MZ. True window size is two times this (plus-minus)
	public double minMZRange = (double)100;

	public Class getFilterClass() {
		return ZoomScanFilter.class;
	}

	/**
	 * This method returns a string containing XML-tag with current parameter values
	 */
	public String writeParameterTag() {
		String s = "<";
		s = s.concat(myTagName);
		s = s.concat(" " + minMZRangeAttibuteName + "=\"" + minMZRange + "\"");
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

		try { minMZRange = Double.parseDouble(atr.getValue(minMZRangeAttibuteName)); } catch (NumberFormatException e) { return false; }
		return true;

	}

}