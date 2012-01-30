/*
 * Copyright 2006-2012 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.peaklistmethods.identification.glycerophospholipidsearch;

import net.sf.mzmine.data.impl.SimplePeakIdentity;
import net.sf.mzmine.util.FormulaUtils;

public class GPLipidIdentity extends SimplePeakIdentity {

    private final double mass;

    public GPLipidIdentity(GPLipidType lipidType, int fattyAcid1Length,
	    int fattyAcid1DoubleBonds, int fattyAcid2Length,
	    int fattyAcid2DoubleBonds) {

	super(lipidType.getAbbr() + "(" + fattyAcid1Length + ":"
		+ fattyAcid1DoubleBonds + "/" + fattyAcid2Length + ":"
		+ fattyAcid2DoubleBonds + ")");

	String fattyAcid1Formula = "H", fattyAcid2Formula = "H";

	if (fattyAcid1Length > 0) {
	    int numberOfHydrogens = (fattyAcid1Length * 2)
		    - (fattyAcid1DoubleBonds * 2) - 1;
	    fattyAcid1Formula = "C" + fattyAcid1Length + "H"
		    + numberOfHydrogens + "O";
	}

	if (fattyAcid2Length > 0) {
	    int numberOfHydrogens = (fattyAcid2Length * 2)
		    - (fattyAcid2DoubleBonds * 2) - 1;
	    fattyAcid2Formula = "C" + fattyAcid2Length + "H"
		    + numberOfHydrogens + "O";
	}

	String formula = lipidType.getFormula() + fattyAcid1Formula
		+ fattyAcid2Formula;

	this.mass = FormulaUtils.calculateExactMass(formula);

	setPropertyValue(PROPERTY_FORMULA, formula);
	setPropertyValue(PROPERTY_METHOD, "Glycerophospholipid search");

    }

    double getMass() {
	return mass;
    }

}
