/*
 * Copyright 2006-2009 The MZmine 2 Development Team
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

package net.sf.mzmine.util;

import net.sf.mzmine.data.Polarity;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IIsotope;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class FormulaUtils {

	/**
	 * Calculates exact monoisotopic mass of a given formula
	 */
	public static double calculateExactMass(String formula) {

		IChemObjectBuilder builder = DefaultChemObjectBuilder.getInstance();

		IMolecularFormula formulaObject = MolecularFormulaManipulator
				.getMolecularFormula(formula, builder);

		double mass = MolecularFormulaManipulator
				.getMajorIsotopeMass(formulaObject);

		return mass;

	}

	/**
	 * Modifies the formula according to polarity - for negative, remove one
	 * hydrogen; for positive, add one hydrogen
	 */
	public static String ionizeFormula(String formula, Polarity polarity) {

		IChemObjectBuilder builder = DefaultChemObjectBuilder.getInstance();

		IMolecularFormula formulaObject = MolecularFormulaManipulator
				.getMolecularFormula(formula, builder);

		for (IIsotope isotope : formulaObject.isotopes()) {
			if (isotope.getSymbol().equals("H")) {
				int current = formulaObject.getIsotopeCount(isotope);
				formulaObject.removeIsotope(isotope);
				formulaObject.addIsotope(isotope, current + polarity.getSign());
				break;
			}
		}

		String newFormula = MolecularFormulaManipulator
				.getString(formulaObject);

		return newFormula;

	}

}
