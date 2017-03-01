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

package net.sf.mzmine.modules.peaklistmethods.identification.glycerophospholipidsearch;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.impl.SimplePeakIdentity;
import net.sf.mzmine.util.FormulaUtils;

public class GPLipidIdentity extends SimplePeakIdentity {

    private final double mass;

    public GPLipidIdentity(final GPLipidType lipidType,
	    final int fattyAcid1Length, final int fattyAcid1DoubleBonds,
	    final int fattyAcid2Length, final int fattyAcid2DoubleBonds) {

	this(lipidType.getAbbr() + '(' + fattyAcid1Length + ':'
		+ fattyAcid1DoubleBonds + '/' + fattyAcid2Length + ':'
		+ fattyAcid2DoubleBonds + ')', lipidType.getFormula()
		+ calculateFattyAcidFormula(fattyAcid1Length,
			fattyAcid1DoubleBonds)
		+ calculateFattyAcidFormula(fattyAcid2Length,
			fattyAcid2DoubleBonds));
    }

    private GPLipidIdentity(final String name, final String formula) {

	super(name);
	mass = FormulaUtils.calculateExactMass(formula);
	setPropertyValue(PROPERTY_FORMULA, formula);
	setPropertyValue(PROPERTY_METHOD, "Glycerophospholipid search");
    }

    /**
     * Calculate fatty acid formula.
     *
     * @param fattyAcidLength
     *            acid length.
     * @param fattyAcidDoubleBonds
     *            double bond count.
     * @return fatty acid formula.
     */
    private static String calculateFattyAcidFormula(final int fattyAcidLength,
	    final int fattyAcidDoubleBonds) {

	String fattyAcid1Formula = "H";
	if (fattyAcidLength > 0) {

	    final int numberOfHydrogens = fattyAcidLength * 2
		    - fattyAcidDoubleBonds * 2 - 1;
	    fattyAcid1Formula = "C" + fattyAcidLength + 'H' + numberOfHydrogens
		    + 'O';
	}
	return fattyAcid1Formula;
    }

    /**
     * Get the mass.
     *
     * @return the mass.
     */
    public double getMass() {

	return mass;
    }

    @Override
    public @Nonnull Object clone() {

	return new GPLipidIdentity(getName(),
		getPropertyValue(PROPERTY_FORMULA));
    }
}
