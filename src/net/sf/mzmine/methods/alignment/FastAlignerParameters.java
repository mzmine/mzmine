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

import java.io.Serializable;
import org.xml.sax.Attributes;

public class FastAlignerParameters implements PeakListAlignerParameters, Serializable {

	private static final String myTagName = "FastAlignerParameters";
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


	public Class getPeakListAlignerClass() {
		return FastAligner.class;
	}

	public String writeParameterTag() {

		String s = "<";
		s = s.concat(myTagName);
		s = s.concat(" " + paramMZvsRTBalanceAttributeName + "=\"" + paramMZvsRTBalance + "\"");
		s = s.concat(" " + paramMZToleranceAttributeName + "=\"" + paramMZTolerance + "\"");
		s = s.concat(" " + paramRTToleranceUseAbsAttributeName + "=\"" + paramRTToleranceUseAbs + "\"");
		s = s.concat(" " + paramRTToleranceAbsAttributeName + "=\"" + paramRTToleranceAbs + "\"");
		s = s.concat(" " + paramRTTolerancePercentAttributeName + "=\"" + paramRTTolerancePercent + "\"");
		s = s.concat("/>");
		return s;

	}

	public String getParameterTagName() { return myTagName; }

	public boolean loadXMLAttributes(Attributes atr) {

		try { paramMZvsRTBalance = Double.parseDouble(atr.getValue(paramMZvsRTBalanceAttributeName));	} catch (NumberFormatException e) {	return false; }
		try { paramMZTolerance = Double.parseDouble(atr.getValue(paramMZToleranceAttributeName)); } catch (NumberFormatException e) {	return false; }
		try { paramRTToleranceUseAbs = Boolean.parseBoolean(atr.getValue(paramRTToleranceUseAbsAttributeName)); } catch (NumberFormatException e) {	return false; }
		try { paramRTToleranceAbs = Double.parseDouble(atr.getValue(paramRTToleranceAbsAttributeName)); } catch (NumberFormatException e) {	return false; }
		try { paramRTTolerancePercent = Double.parseDouble(atr.getValue(paramRTTolerancePercentAttributeName)); } catch (NumberFormatException e) {	return false; }

		return true;
	}
}