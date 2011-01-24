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

import net.sf.mzmine.data.IonizationType;
import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopepatternscore.IsotopePatternScoreCalculator;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopeprediction.IsotopePatternCalculator;
import net.sf.mzmine.util.FormulaUtils;

import org.openscience.cdk.formula.MolecularFormula;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class CandidateFormula {

	private final IMolecularFormula cdkFormula;
	private String stringFormula, htmlFormula;
	private Boolean lewisRule, seniorRule, hcRule, nopsRule, hnopsRule;
	private IsotopePattern predictedIsotopePattern, detectedPattern;
	private IonizationType ionType;
	private int charge;
	private Double isotopeScore;

	CandidateFormula(ElementRule elements[], int elementCounts[],
			IsotopePattern detectedPattern, IonizationType ionType, int charge) {

		cdkFormula = new MolecularFormula();

		for (int i = 0; i < elements.length; i++) {
			if (elementCounts[i] == 0)
				continue;
			cdkFormula.addIsotope(elements[i].getElementObject(),
					elementCounts[i]);
		}

		this.detectedPattern = detectedPattern;
		this.ionType = ionType;
		this.charge = charge;
	}

	public boolean conformsLEWIS() {
		if (lewisRule == null)
			lewisRule = HeuristicRuleChecker.checkLewisOctetRule(cdkFormula);
		return lewisRule;
	}

	public boolean conformsSENIOR() {
		if (seniorRule == null)
			seniorRule = HeuristicRuleChecker.checkSeniorRule(cdkFormula);
		return seniorRule;
	}

	public boolean conformsHC() {
		if (hcRule == null)
			hcRule = HeuristicRuleChecker.checkHC(cdkFormula);
		return hcRule;
	}

	public boolean conformsNOPS() {
		if (nopsRule == null)
			nopsRule = HeuristicRuleChecker.checkNOPS(cdkFormula);
		return nopsRule;
	}

	public boolean conformsHNOPS() {
		if (hnopsRule == null)
			hnopsRule = HeuristicRuleChecker.checkHNOPS(cdkFormula);
		return hnopsRule;
	}

	public String getFormulaAsString() {
		if (stringFormula == null)
			stringFormula = MolecularFormulaManipulator.getString(cdkFormula);
		return stringFormula;
	}

	public String getFormulaAsHTML() {
		if (htmlFormula == null)
			htmlFormula = MolecularFormulaManipulator.getHTML(cdkFormula);
		return htmlFormula;
	}

	public IMolecularFormula getFormulaAsObject() {
		return cdkFormula;
	}

	public IsotopePattern getPredictedIsotopes() {
		if (predictedIsotopePattern == null) {
			String originalFormula = getFormulaAsString();
			String adjustedFormula = FormulaUtils.ionizeFormula(
					originalFormula, ionType.getPolarity(), charge);
			predictedIsotopePattern = IsotopePatternCalculator
					.calculateIsotopePattern(adjustedFormula, charge,
							ionType.getPolarity());
		}
		return predictedIsotopePattern;
	}

	public double getIsotopeScore() {
		if (detectedPattern == null)
			return 0;
		if (isotopeScore == null) {
			isotopeScore = IsotopePatternScoreCalculator.getSimilarityScore(
					detectedPattern, getPredictedIsotopes());
		}
		return isotopeScore;
	}

	public double getExactMass() {
		return MolecularFormulaManipulator.getTotalExactMass(cdkFormula);
	}

	public String toString() {
		return getFormulaAsString();
	}

}
