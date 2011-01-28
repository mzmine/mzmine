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

import net.sf.mzmine.data.IsotopePattern;

import org.openscience.cdk.formula.MolecularFormula;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class ResultFormula {

	private final IMolecularFormula cdkFormula;
	private HeuristicRule conformingRules[];
	private Double isotopeScore, msmsScore;
	private IsotopePattern predictedIsotopePattern;

	ResultFormula(MolecularFormula cdkFormula,
			HeuristicRule conformingRules[],
			IsotopePattern predictedIsotopePattern, Double isotopeScore,
			Double msmsScore) {

		this.cdkFormula = cdkFormula;
		this.conformingRules = conformingRules;
		this.predictedIsotopePattern = predictedIsotopePattern;
		this.isotopeScore = isotopeScore;
		this.msmsScore = msmsScore;

	}

	public String getFormulaAsString() {
		return MolecularFormulaManipulator.getString(cdkFormula);
	}

	public String getFormulaAsHTML() {
		return MolecularFormulaManipulator.getHTML(cdkFormula);
	}

	public IMolecularFormula getFormulaAsObject() {
		return cdkFormula;
	}

	public IsotopePattern getPredictedIsotopes() {
		return predictedIsotopePattern;
	}

	public Double getIsotopeScore() {
		return isotopeScore;
	}
	
	public Double getMSMSScore() {
		return msmsScore;
	}

	public double getExactMass() {
		return MolecularFormulaManipulator.getTotalExactMass(cdkFormula);
	}

	public boolean conformsRule(HeuristicRule rule) {
		for (HeuristicRule cr : conformingRules) {
			if (cr.equals(rule))
				return true;
		}
		return false;
	}

}
