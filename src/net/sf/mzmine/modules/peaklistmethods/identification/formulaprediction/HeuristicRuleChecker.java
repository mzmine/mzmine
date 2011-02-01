/*
 * Copyright 2006-2011 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction;

import java.util.HashMap;
import java.util.Map;

import org.openscience.cdk.interfaces.IIsotope;
import org.openscience.cdk.interfaces.IMolecularFormula;

public class HeuristicRuleChecker {

	/**
	 * This table defines the typical valence states, in fact e.g. sulphur may
	 * have valence 2, 4 or 6
	 */
	private static final Map<String, Integer> groundValences = new HashMap<String, Integer>();
	static {
		groundValences.put("H", 1);
		groundValences.put("C", 4);
		groundValences.put("N", 3);
		groundValences.put("O", 2);
		groundValences.put("F", 1);
		groundValences.put("Si", 4);
		groundValences.put("P", 3);
		groundValences.put("S", 2);
		groundValences.put("Cl", 1);
		groundValences.put("Br", 1);
	}

	/**
	 * This table defines the maximum valence states
	 */
	private static final Map<String, Integer> maxValences = new HashMap<String, Integer>();
	static {
		maxValences.putAll(groundValences);
		maxValences.put("N", 5);
		maxValences.put("F", 7);
		maxValences.put("P", 5);
		maxValences.put("S", 6);
		maxValences.put("Cl", 7);
		maxValences.put("Br", 7);
	}

	public static Boolean checkRule(IMolecularFormula formula, HeuristicRule rule) {
		switch (rule) {
		case LEWIS:
			return checkLewisOctetRule(formula);
		case SENIOR:
			return checkSeniorRule(formula);
		case HC:
			return checkHC(formula);
		case NOPS:
			return checkNOPS(formula);
		case HNOPS:
			return checkHNOPS(formula);
		default:
			throw new IllegalArgumentException("Unknown rule: " + rule);
		}

	}

	private static Integer calculateE(IMolecularFormula formula) {

		int sum = 0;

		for (IIsotope isotope : formula.isotopes()) {

			Integer maxValence = maxValences.get(isotope.getSymbol());
			if (maxValence == null)
				return null;
			sum += maxValence * formula.getIsotopeCount(isotope);
		}
		return sum;
	}

	private static Integer calculateLewisSum(IMolecularFormula formula) {

		int sum = 0;

		for (IIsotope isotope : formula.isotopes()) {
			Integer valence = groundValences.get(isotope.getSymbol());
			if (valence == null)
				return null;
			sum += valence * formula.getIsotopeCount(isotope);
		}
		return sum;
	}

	private static Boolean checkLewisOctetRule(IMolecularFormula formula) {
		Integer sume = calculateE(formula);
		Integer ls = calculateLewisSum(formula);
		if ((sume == null) || (ls == null)) return null;
		return (sume > 7) && (ls % 2 == 0);
	}

	private static int getAtoms(IMolecularFormula formula) {

		int sum = 0;

		for (IIsotope isotope : formula.isotopes()) {
			sum += formula.getIsotopeCount(isotope);
		}
		return sum;
	}

	private static Boolean checkSeniorRule(IMolecularFormula formula) {

		Integer sume = calculateE(formula);
		int atoms = getAtoms(formula);

		if (sume == null) return null;
		return sume >= (2 * (atoms - 1));
	}

	private static Boolean checkHC(IMolecularFormula formula) {

		double eC = 0, eH = 0;
		for (IIsotope isotope : formula.isotopes()) {
			if (isotope.getSymbol().equals("C"))
				eC += formula.getIsotopeCount(isotope);
			if (isotope.getSymbol().equals("H"))
				eH += formula.getIsotopeCount(isotope);
		}

		if (eC == 0)
			return false;

		double rHC = eH / eC;
		return (rHC > 0) && (rHC < 6);
	}

	private static Boolean checkNOPS(IMolecularFormula formula) {

		double eC = 0, eN = 0, eO = 0, eP = 0, eS = 0;
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
		}

		if (eC == 0)
			return false;

		double rPC = eP / eC;
		double rNC = eN / eC;
		double rOC = eO / eC;
		double rSC = eS / eC;

		return (rNC <= 4) && (rOC <= 3) && (rPC <= 2) && (rSC <= 3);
	}

	private static Boolean checkHNOPS(IMolecularFormula formula) {

		double eC = 0, eH = 0, eN = 0, eO = 0, eP = 0, eS = 0;
		for (IIsotope isotope : formula.isotopes()) {
			if (isotope.getSymbol().equals("C"))
				eC += formula.getIsotopeCount(isotope);
			if (isotope.getSymbol().equals("H"))
				eH += formula.getIsotopeCount(isotope);
			if (isotope.getSymbol().equals("N"))
				eN += formula.getIsotopeCount(isotope);
			if (isotope.getSymbol().equals("O"))
				eO += formula.getIsotopeCount(isotope);
			if (isotope.getSymbol().equals("P"))
				eP += formula.getIsotopeCount(isotope);
			if (isotope.getSymbol().equals("S"))
				eS += formula.getIsotopeCount(isotope);
		}

		if (eC == 0)
			return false;

		double rHC = eH / eC;
		double rPC = eP / eC;
		double rNC = eN / eC;
		double rOC = eO / eC;
		double rSC = eS / eC;

		return (rHC >= 0.2) && (rHC <= 3.1) && (rNC <= 1.3) && (rOC <= 1.2)
				&& (rPC <= 0.3) && (rSC <= 0.8);
	}

}
