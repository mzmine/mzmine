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

import java.util.Arrays;

import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.elements.ElementRule;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.elements.ElementRuleSorterByMass;
import net.sf.mzmine.util.Range;

import org.openscience.cdk.formula.MolecularFormula;

/**
 * This class generates molecular formulas within given mass range and elemental
 * composition. Its functionality is basically equivalent to CDK class
 * MassToFormulaTool, but we cannot use MassToFormulaTool because it does not
 * provide any way how to track progress - see getFinishedPercentage()
 * 
 */
public class FormulaGenerator {

	private Range massRange;
	private ElementRule elementRules[];
	private long testedCombinations, totalNumberOfCombinations;
	private int currentCounts[];

	public FormulaGenerator(Range massRange, ElementRule elementRules[]) {

		this.massRange = massRange;
		this.elementRules = elementRules;

		// Sort the elements by mass in descending order
		Arrays.sort(elementRules, new ElementRuleSorterByMass());

		// Calculate total number of combinations
		totalNumberOfCombinations = 1;
		for (ElementRule rule : elementRules)
			totalNumberOfCombinations *= rule.getMaxCount()
					- rule.getMinCount() + 1;

		// Prepare counters for elements, start at the minimal count
		currentCounts = new int[elementRules.length];
		for (int i = 0; i < elementRules.length; i++) {
			currentCounts[i] = elementRules[i].getMinCount();
		}

	}

	/**
	 * Returns next generated formula or null in case no other formula was found
	 */
	public MolecularFormula getNextFormula() {

		// Main cycle iterating through element counters
		mainCycle: while (testedCombinations < totalNumberOfCombinations) {

			// Calculate the mass of current element counts
			double mass = 0;
			for (int i = 0; i < elementRules.length; i++) {
				mass += elementRules[i].getMass() * currentCounts[i];
			}

			if (massRange.contains(mass)) {

				MolecularFormula cdkFormula = generateCDKFormula();

				increaseCounter(0);
				testedCombinations++;

				return cdkFormula;

			}

			// Heuristics: if we are over the mass, it is meaningless to add
			// more atoms, so let's jump directly to the maximum count
			if (mass > massRange.getMax()) {
				for (int i = 0; i < currentCounts.length; i++) {
					if (currentCounts[i] > elementRules[i].getMinCount()) {

						long skippedCombinations = (elementRules[i]
								.getMaxCount() - currentCounts[i]) + 1;
						for (int j = 0; j < i; j++) {
							skippedCombinations *= elementRules[j]
									.getMaxCount()
									- elementRules[j].getMinCount() + 1;
						}
						testedCombinations += skippedCombinations;
						currentCounts[i] = elementRules[i].getMaxCount();
						increaseCounter(i);
						continue mainCycle;
					}
				}

			}

			testedCombinations++;

			// Increase the count of the most heavy element
			increaseCounter(0);

		}

		// All combinations tested, return null
		return null;

	}

	private void increaseCounter(int position) {

		currentCounts[position]++;

		// check the validity of the counts
		for (int i = position; i < currentCounts.length - 1; i++) {
			if (currentCounts[i] > elementRules[i].getMaxCount()) {
				currentCounts[i] = elementRules[i].getMinCount();
				currentCounts[i + 1]++;
				break;
			}
		}

	}

	private MolecularFormula generateCDKFormula() {

		MolecularFormula cdkFormula = new MolecularFormula();

		for (int i = 0; i < elementRules.length; i++) {
			if (currentCounts[i] == 0)
				continue;
			cdkFormula.addIsotope(elementRules[i].getElementObject(),
					currentCounts[i]);
		}

		return cdkFormula;

	}

	public double getFinishedPercentage() {
		if (totalNumberOfCombinations == 0)
			return 0;
		return ((double) testedCombinations) / totalNumberOfCombinations;
	}

}
