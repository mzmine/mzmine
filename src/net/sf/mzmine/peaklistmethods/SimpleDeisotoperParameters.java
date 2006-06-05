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

import java.util.HashSet;

import java.io.Serializable;

import org.xml.sax.Attributes;

/**
 * This class represents parameter settings for the simple deisotoper method
 *
 * @version 31 March 2006
 */
public class SimpleDeisotoperParameters implements PeakListProcessorParameters, Serializable {

	// CONSTANTS FOR WRITING PARAMETERS TO XML FILE

	private static final String myTagName = "SimpleDeisotoperParameters";

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
	public static final Integer chargeFour = 4;
	public static final Integer chargeFive = 5;


	// PARAMETERS AND THEIR DEFAULT VALUES

	public double mzTolerance = (double)0.05;
	public double rtTolerance = (double)5;
	public boolean monotonicShape = true;
	public HashSet<Integer> chargeStates;



	/**
	 * Initializes parameter object and sets default parameter values
	 */
	public SimpleDeisotoperParameters() {
		chargeStates = new HashSet<Integer>();
		chargeStates.add(chargeOne);
	}



	/**
	 * Returns the class of the peak list processor which corresponds to this parameter class
	 */
	public Class getPeakListProcessorClass() {
		return SimpleDeisotoper.class;
	}



	/**
	 * Returns a string containing current parameter values in an XML tag
	 */
	public String writeParameterTag() {

		String s = "<";
		s = s.concat(myTagName);
		s = s.concat(" " + mzToleranceAttributeName + "=\"" + mzTolerance + "\"");
		s = s.concat(" " + rtToleranceAttributeName + "=\"" + rtTolerance + "\"");
		if (monotonicShape) 					{ s = s.concat(" " + monotonicShapeAttributeName + "=\"1\""); }
		if (chargeStates.contains(chargeOne))	{ s = s.concat(" " + chargeStateOneAttributeName + "=\"1\""); }
		if (chargeStates.contains(chargeTwo))	{ s = s.concat(" " + chargeStateTwoAttributeName + "=\"1\""); }
		if (chargeStates.contains(chargeThree))	{ s = s.concat(" " + chargeStateThreeAttributeName + "=\"1\""); }
		s = s.concat("/>");
		return s;

	}



	/**
	 * Returns the name of the XML tag for this parameter set
	 */
	public String getParameterTagName() { return myTagName; }



	/**
	 * Sets parameter values according to given XML attributes
	 */
	public boolean loadXMLAttributes(Attributes atr) {

		try { mzTolerance = Double.parseDouble(atr.getValue(mzToleranceAttributeName)); } catch (NumberFormatException e) {	return false; }
		try { rtTolerance = Double.parseDouble(atr.getValue(rtToleranceAttributeName)); } catch (NumberFormatException e) {	return false; }

		if (atr.getValue(monotonicShapeAttributeName)!=null) { monotonicShape = true; }

		chargeStates.clear();
		if (atr.getValue(chargeStateOneAttributeName)!=null) { chargeStates.add(chargeOne); }
		if (atr.getValue(chargeStateTwoAttributeName)!=null) { chargeStates.add(chargeTwo); }
		if (atr.getValue(chargeStateThreeAttributeName)!=null) { chargeStates.add(chargeThree); }


		return true;
	}

}