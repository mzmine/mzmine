/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.restrictions.elements;

import net.sf.mzmine.parameters.ParameterSet;

import org.openscience.cdk.interfaces.IIsotope;
import org.openscience.cdk.interfaces.IMolecularFormula;

public class ElementalHeuristicChecker {

    public static boolean checkFormula(IMolecularFormula formula,
	    ParameterSet parameters) {

	double eH = 0, eC = 0, eN = 0, eO = 0, eP = 0, eS = 0;
	for (IIsotope isotope : formula.isotopes()) {
	    if (isotope.getSymbol().equals("C"))
		eC += formula.getIsotopeCount(isotope);
	    if (isotope.getSymbol().equals("N"))
		eN += formula.getIsotopeCount(isotope);
	    if (isotope.getSymbol().equals("O"))
		eO += formula.getIsotopeCount(isotope);
	    if (isotope.getSymbol().equals("P"))
		eP += formula.getIsotopeCount(isotope);
	    if (isotope.getSymbol().equals("S"))
		eS += formula.getIsotopeCount(isotope);
	    if (isotope.getSymbol().equals("H"))
		eH += formula.getIsotopeCount(isotope);
	}

	// If there is no carbon, consider the formula OK
	if (eC == 0)
	    return true;

	boolean checkHC = parameters.getParameter(
		ElementalHeuristicParameters.checkHC).getValue();
	boolean checkNOPS = parameters.getParameter(
		ElementalHeuristicParameters.checkNOPS).getValue();
	boolean checkMultiple = parameters.getParameter(
		ElementalHeuristicParameters.checkMultiple).getValue();

	if (checkHC) {
	    double rHC = eH / eC;
	    if ((rHC < 0.1) || (rHC > 6))
		return false;
	}

	if (checkNOPS) {
	    double rPC = eP / eC;
	    double rNC = eN / eC;
	    double rOC = eO / eC;
	    double rSC = eS / eC;
	    if ((rNC > 4) || (rOC > 3) || (rPC > 2) || (rSC > 3))
		return false;
	}

	if (checkMultiple) {

	    // Multiple rule #1
	    if ((eN > 1) && (eO > 1) && (eP > 1) && (eS > 1)) {
		if ((eN >= 10) || (eO >= 20) || (eP >= 4) || (eS >= 3))
		    return false;
	    }

	    // Multiple rule #2
	    if ((eN > 3) && (eO > 3) && (eP > 3)) {
		if ((eN >= 11) || (eO >= 22) || (eP >= 6))
		    return false;
	    }

	    // Multiple rule #3
	    if ((eO > 1) && (eP > 1) && (eS > 1)) {
		if ((eO >= 14) || (eP >= 3) || (eS >= 3))
		    return false;
	    }

	    // Multiple rule #4
	    if ((eN > 1) && (eP > 1) && (eS > 1)) {
		if ((eN >= 4) || (eP >= 3) || (eS >= 3))
		    return false;
	    }

	    // Multiple rule #5
	    if ((eN > 6) && (eO > 6) && (eS > 6)) {
		if ((eN >= 19) || (eO >= 14) || (eS >= 8))
		    return false;
	    }

	}

	return true;

    }

}
