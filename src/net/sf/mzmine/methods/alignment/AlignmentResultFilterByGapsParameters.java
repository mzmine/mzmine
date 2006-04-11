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

package net.sf.mzmine.methods.alignment;

import net.sf.mzmine.alignmentresultmethods.*;
import net.sf.mzmine.alignmentresultvisualizers.*;
import net.sf.mzmine.datastructures.*;
import net.sf.mzmine.obsoletedistributionframework.*;
import net.sf.mzmine.peaklistmethods.*;
import net.sf.mzmine.rawdatamethods.*;
import net.sf.mzmine.rawdatavisualizers.*;
import net.sf.mzmine.userinterface.*;
import net.sf.mzmine.util.*;

import org.xml.sax.Attributes;

public class AlignmentResultFilterByGapsParameters implements AlignmentResultProcessorParameters {


	private static final String myTagName = "AlignmentResultFilterByGapsParameters";
	private static final String requiredNumOfPresentAttributeName = "RequiredNumOfPresent";

	public int paramRequiredNumOfPresent = 1;

	public Class getAlignmentResultProcessorClass() {
		return AlignmentResultFilterByGaps.class;
	}

	public String writeParameterTag() {
		String s = "<";
		s = s.concat(myTagName);
		s = s.concat(" " + requiredNumOfPresentAttributeName + "=\"" + paramRequiredNumOfPresent + "\"");
		s = s.concat("/>");
		return s;
	}

	public String getParameterTagName() { return myTagName; }

	public boolean loadXMLAttributes(Attributes atr) {
		try { paramRequiredNumOfPresent = Integer.parseInt(atr.getValue(requiredNumOfPresentAttributeName));	} catch (NumberFormatException e) {	return false; }
		return true;
	}

}