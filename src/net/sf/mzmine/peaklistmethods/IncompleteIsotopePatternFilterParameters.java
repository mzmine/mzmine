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
package net.sf.mzmine.peaklistmethods;
import net.sf.mzmine.alignmentresultmethods.*;
import net.sf.mzmine.alignmentresultvisualizers.*;
import net.sf.mzmine.datastructures.*;
import net.sf.mzmine.distributionframework.*;
import net.sf.mzmine.miscellaneous.*;
import net.sf.mzmine.peaklistmethods.*;
import net.sf.mzmine.rawdatamethods.*;
import net.sf.mzmine.rawdatavisualizers.*;
import net.sf.mzmine.userinterface.*;


import java.io.Serializable;
import org.xml.sax.Attributes;

public class IncompleteIsotopePatternFilterParameters implements PeakListProcessorParameters, Serializable {

	private static final String myTagName = "IncompleteIsotopePatternFilterParameters";

	private static final String minimumNumberOfPeaksAttributeName = "MinNumberOfPeaks";

	// Parameters and their default values

	public int minimumNumberOfPeaks = 2;

	public Class getPeakListProcessorClass() {
		return IncompleteIsotopePatternFilter.class;
	}

	public String writeParameterTag() {

		String s = "<";
		s = s.concat(myTagName);
		s = s.concat(" " + minimumNumberOfPeaksAttributeName + "=\"" + minimumNumberOfPeaks + "\"");
		s = s.concat("/>");
		return s;

	}

	public String getParameterTagName() { return myTagName; }

	public boolean loadXMLAttributes(Attributes atr) {

		try { minimumNumberOfPeaks = Integer.parseInt(atr.getValue(minimumNumberOfPeaksAttributeName)); } catch (NumberFormatException e) {	return false; }

		return true;
	}

}