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
package net.sf.mzmine.methods.filtering;
import java.io.Serializable;

import org.xml.sax.Attributes;

public class ChromatographicMedianFilterParameters implements FilterParameters, Serializable {

	private static final String myTagName = "ChromatographicMedianFilterParameters";
	private static final String mzToleranceAttributeName = "MZTolerance";
	private static final String oneSidedWindowLengthAttributeName = "OneSidedWindowLength";

	public double mzTolerance = (double)0.1;
	public int oneSidedWindowLength = 1;

	public Class getFilterClass() {
		return ChromatographicMedianFilter.class;
	}

	public String writeParameterTag() {

		String s = "<";
		s = s.concat(myTagName);
		s = s.concat(" " + mzToleranceAttributeName + "=\"" + mzTolerance + "\"");
		s = s.concat(" " + oneSidedWindowLengthAttributeName + "=\"" + oneSidedWindowLength + "\"");
		s = s.concat("/>");
		return s;

	}

	public String getParameterTagName() { return myTagName; }

	public boolean loadXMLAttributes(Attributes atr) {

		try { mzTolerance = Double.parseDouble(atr.getValue(mzToleranceAttributeName));	} catch (NumberFormatException e) {	return false; }
		try { oneSidedWindowLength = Integer.parseInt(atr.getValue(oneSidedWindowLengthAttributeName));	} catch (NumberFormatException e) {	return false; }
		return true;
	}

}