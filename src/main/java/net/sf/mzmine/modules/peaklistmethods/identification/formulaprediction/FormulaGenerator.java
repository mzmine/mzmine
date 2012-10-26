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

package net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction;

import java.math.BigInteger;
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

    // We will try maximum 10^15 combinations (10^15 still fits into long)
    private static final BigInteger maximumToCalculate = BigInteger.TEN.pow(15);

    // Let's use two doubles instead of Range, to avoid unnecessary method calls
    private double minMass, maxMass;

    private ElementRule elementRules[];
    private double elementMasses[];

    private long testedCombinations = 0, totalNumberOfCombinations = 0;

    private int currentCounts[], minCounts[], maxCounts[];

    private boolean canceled = false;

    public FormulaGenerator(Range massRange, ElementRule selectedElements[]) {

	// Sort the elements by mass in ascending order. That greatly speeds up
	// the search.
	this.elementRules = selectedElements;
	Arrays.sort(this.elementRules, new ElementRuleSorterByMass());

	// Adjust the maximum numbers according to the mass we are
	// searching
	for (ElementRule rule : selectedElements) {
	    int maxCountAccordingToMass = (int) (massRange.getMax() / rule
		    .getMass());
	    if (rule.getMaxCount() > maxCountAccordingToMass) {
		rule.setMaxCount(maxCountAccordingToMass);
	    }
	}

	// Copy the values for efficiency
	elementMasses = new double[elementRules.length];
	currentCounts = new int[elementRules.length];
	minCounts = new int[elementRules.length];
	maxCounts = new int[elementRules.length];
	for (int i = 0; i < elementRules.length; i++) {
	    elementMasses[i] = elementRules[i].getMass();
	    currentCounts[i] = elementRules[i].getMinCount();
	    minCounts[i] = elementRules[i].getMinCount();
	    maxCounts[i] = elementRules[i].getMaxCount();
	}
	this.minMass = massRange.getMin();
	this.maxMass = massRange.getMax();

	testedCombinations = 0;

	// Calculate total number of combinations. It may be very large, even
	// larger then the long type, so we use BigInteger type
	BigInteger totalCount = BigInteger.valueOf(1);
	for (ElementRule rule : elementRules) {
	    BigInteger factor = BigInteger.valueOf(rule.getMaxCount()
		    - rule.getMinCount() + 1);
	    totalCount = totalCount.multiply(factor);
	}

	// Check if the total number of combinations is too big.
	if (totalCount.compareTo(maximumToCalculate) > 0)
	    throw new IllegalArgumentException(
		    "The total number of combinations is too big ("
			    + totalCount
			    + "). Please reduce the element counts.");

	// Convert the total combinations to long
	totalNumberOfCombinations = totalCount.longValue();

    }

    /**
     * Calculates the exact mass of the currently evaluated formula 
     */
    private double getCurrentMass() {
	double mass = 0;
	for (int i = 0; i < currentCounts.length; i++) {
	    mass += currentCounts[i] * elementMasses[i];
	}
	return mass;
    }
    
    /**
     * Returns next generated formula or null in case no other formula was found
     */
    public MolecularFormula getNextFormula() {

	// Main cycle iterating through element counters
	mainCycle: while (!canceled) {

	    // Heuristics: if we are over the mass, it is meaningless to add
	    // more atoms, so let's jump directly to the maximum count
	    double currentMass = getCurrentMass();
	    if (currentMass > maxMass) {
		for (int i = 0; i < currentCounts.length; i++) {
		    if (currentCounts[i] > minCounts[i]) {

			long skippedCombinations = (maxCounts[i] - currentCounts[i]) + 1;
			for (int j = 0; j < i; j++) {
			    skippedCombinations *= maxCounts[j] - minCounts[j]
				    + 1;
			}
			testedCombinations += skippedCombinations;
			currentCounts[i] = maxCounts[i];

			increaseCounter(i);
			continue mainCycle;
		    }
		}

	    }

	    if (currentMass >= minMass) {
		MolecularFormula cdkFormula = generateCDKFormula();
		testedCombinations++;
		increaseCounter(0);
		return cdkFormula;
	    }

	    // Increase the counter
	    testedCombinations++;
	    increaseCounter(0);

	}

	// All combinations tested, return null
	return null;

    }

    private void increaseCounter(int position) {

	if (currentCounts[position] == maxCounts[position]) {

	    currentCounts[position] = minCounts[position];

	    if (position < elementMasses.length - 1) {
		increaseCounter(position + 1);
	    } else {
		canceled = true;
	    }
	} else {
	    currentCounts[position]++;
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
	return (double) testedCombinations / (double) totalNumberOfCombinations;
    }

    public void cancel() {
	this.canceled = true;
    }

}
