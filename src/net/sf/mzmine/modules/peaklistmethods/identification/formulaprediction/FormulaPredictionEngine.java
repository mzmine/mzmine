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

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.SwingUtilities;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.IonizationType;
import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopepatternscore.IsotopePatternScoreCalculator;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopeprediction.IsotopePatternCalculator;
import net.sf.mzmine.util.FormulaUtils;
import net.sf.mzmine.util.Range;

import org.openscience.cdk.formula.MolecularFormula;
import org.openscience.cdk.interfaces.IIsotope;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class FormulaPredictionEngine {

	public static final NumberFormat massFormater = MZmineCore.getMZFormat();

	private long testedCombinations, totalNumberOfCombinations = 1;

	private Range massRange;
	private ResultWindow window;
	private ElementRule elementRules[];

	private int foundFormulas = 0;
	private IonizationType ionType;
	private int charge;
	private int maxFormulas;
	private PeakListRow peakListRow;
	private boolean checkConstraints, checkIsotopes, checkMSMS;
	private HeuristicRule[] heuristicRules;
	private double minIsotopeScore, minMSMSScore, msmsTolerance,
			msmsNoiseLevel;
	private boolean canceled = false;

	FormulaPredictionEngine(Range massRange, ElementRule elementRules[]) {
		this(massRange, elementRules, null, false, 0d, 0, null, false, 0d, 0,
				0, 1, null, null);
		this.checkConstraints = false;
	}

	FormulaPredictionEngine(Range massRange, ElementRule elementRules[],
			HeuristicRule heuristicRules[], boolean checkIsotopes,
			double minIsotopeScore, int charge, IonizationType ionType,
			boolean checkMSMS, double minMSMSScore, double msmsTolerance,
			double msmsNoiseLevel, int maxFormulas, PeakListRow peakListRow,
			ResultWindow window) {

		this.checkConstraints = true;
		this.massRange = massRange;
		this.elementRules = elementRules;
		this.heuristicRules = heuristicRules;
		this.checkIsotopes = checkIsotopes;
		this.minIsotopeScore = minIsotopeScore;
		this.charge = charge;
		this.ionType = ionType;
		this.checkMSMS = checkMSMS;
		this.msmsTolerance = msmsTolerance;
		this.msmsNoiseLevel = msmsNoiseLevel;
		this.minMSMSScore = minMSMSScore;
		this.maxFormulas = maxFormulas;
		this.window = window;
		this.peakListRow = peakListRow;
	}

	/**
	 * 
	 * @return Number of formulas found
	 */
	int run() {

		// Sort the elements by mass in descending order
		Arrays.sort(elementRules, new ElementRuleSorterByMass());

		// Calculate total number of combinations
		totalNumberOfCombinations = 1;
		for (ElementRule rule : elementRules)
			totalNumberOfCombinations *= rule.getNumberOfCombinations();

		// Prepare counters for elements, start at the minimal count
		int currentCounts[] = new int[elementRules.length];
		for (int i = 0; i < elementRules.length; i++) {
			currentCounts[i] = elementRules[i].getMinCount();
		}

		// Main cycle iterating through element counters
		mainCycle: while (testedCombinations < totalNumberOfCombinations) {

			if (canceled)
				break;

			// Calculate the mass of current element counts
			double mass = 0;
			for (int i = 0; i < elementRules.length; i++) {
				mass += elementRules[i].getMass() * currentCounts[i];
			}

			if (massRange.contains(mass)) {

				// Mass is ok, so test other constraints, too
				if (checkConstraints)
					checkConstraints(currentCounts);
				else
					foundFormulas++;
				;

				// Stopping condition
				if (foundFormulas == maxFormulas)
					break mainCycle;

			} else {

				// Heuristics: if we are over the mass, it is meaningless to add
				// more atoms, so let's jump directly to the maximum count
				if (mass > massRange.getMax()) {
					for (int i = 0; i < currentCounts.length; i++) {
						if (currentCounts[i] > elementRules[i].getMinCount()) {

							long skippedCombinations = (elementRules[i]
									.getMaxCount() - currentCounts[i]) + 1;
							for (int j = 0; j < i; j++) {
								skippedCombinations *= elementRules[j]
										.getNumberOfCombinations();
							}
							testedCombinations += skippedCombinations;
							currentCounts[i] = elementRules[i].getMaxCount();
							increaseCounter(currentCounts, i);
							continue mainCycle;
						}
					}

				}

			}

			testedCombinations++;

			// Increase the count of the most heavy element
			increaseCounter(currentCounts, 0);

		}

		return foundFormulas;

	}

	private void checkConstraints(int currentCounts[]) {

		MolecularFormula cdkFormula = new MolecularFormula();

		for (int i = 0; i < elementRules.length; i++) {
			if (currentCounts[i] == 0)
				continue;
			cdkFormula.addIsotope(elementRules[i].getElementObject(),
					currentCounts[i]);
		}

		// Determine the conformity to all heuristic rules
		ArrayList<HeuristicRule> conformingRules = new ArrayList<HeuristicRule>();
		for (HeuristicRule rule : HeuristicRule.values()) {
			if (HeuristicRuleChecker.checkRule(cdkFormula, rule))
				conformingRules.add(rule);
		}
		HeuristicRule conformingRulesArray[] = conformingRules
				.toArray(new HeuristicRule[0]);

		// Check if the required rules are fulfilled
		for (HeuristicRule rule : heuristicRules) {
			if (!conformingRules.contains(rule))
				return;
		}

		// Calculate isotope similarity score
		String originalFormula = MolecularFormulaManipulator
				.getString(cdkFormula);
		String adjustedFormula = FormulaUtils.ionizeFormula(originalFormula,
				ionType.getPolarity(), charge);
		IsotopePattern predictedIsotopePattern = IsotopePatternCalculator
				.calculateIsotopePattern(adjustedFormula, charge,
						ionType.getPolarity());

		Double isotopeScore = null;
		IsotopePattern detectedPattern = peakListRow.getBestIsotopePattern();
		if (detectedPattern != null) {
			isotopeScore = IsotopePatternScoreCalculator.getSimilarityScore(
					detectedPattern, predictedIsotopePattern);
		}

		// Check the isotope condition
		if ((checkIsotopes) && (isotopeScore != null)) {
			if (isotopeScore < minIsotopeScore)
				return;

		}

		// MS/MS evaluation is slowest, so let's do it last
		Double msmsScore = null;
		ChromatographicPeak bestPeak = peakListRow.getBestPeak();
		RawDataFile dataFile = bestPeak.getDataFile();
		int msmsScanNumber = bestPeak.getMostIntenseFragmentScanNumber();
		if (msmsScanNumber > 0) {

			Scan msmsScan = dataFile.getScan(msmsScanNumber);
			DataPoint msmsPeaks[] = msmsScan.getDataPoints();

			int totalMSMSpeaks = 0, interpretedMSMSpeaks = 0;

			for (DataPoint dp : msmsPeaks) {
				if (dp.getIntensity() < msmsNoiseLevel)
					continue;

				// We don't know the charge of the fragment, so we will simply
				// assume 1
				double neutralLoss = msmsScan.getPrecursorMZ()
						* msmsScan.getPrecursorCharge() - dp.getMZ();

				// Ignore negative neutral losses and parent ion, 5 is a good
				// threshold
				if (neutralLoss < 5)
					continue;

				// Sorted by mass in descending order
				ArrayList<ElementRule> rulesSet = new ArrayList<ElementRule>();
				for (IIsotope isotope : cdkFormula.isotopes()) {
					ElementRule rule = new ElementRule(isotope.getSymbol(), 0,
							cdkFormula.getIsotopeCount(isotope));
					rulesSet.add(rule);
				}
				ElementRule msmsElementRules[] = rulesSet
						.toArray(new ElementRule[0]);

				Range msmsTargetRange = new Range(neutralLoss - msmsTolerance,
						neutralLoss + msmsTolerance);

				FormulaPredictionEngine msmsEngine = new FormulaPredictionEngine(
						msmsTargetRange, msmsElementRules);
				int foundMSMSformulas = msmsEngine.run();
				if (foundMSMSformulas > 0)
					interpretedMSMSpeaks++;

				totalMSMSpeaks++;

			}
			if (totalMSMSpeaks > 0)
				msmsScore = (double) interpretedMSMSpeaks / totalMSMSpeaks;
		}

		// Check the MS/MS condition
		if ((checkMSMS) && (msmsScore != null)) {
			if (msmsScore < minMSMSScore)
				return;

		}

		final ResultTableFormula resultEntry = new ResultTableFormula(
				cdkFormula, conformingRulesArray, predictedIsotopePattern,
				isotopeScore, msmsScore);

		// Update the model in swing thread to avoid exceptions
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				window.addNewListItem(resultEntry);
			}
		});

		foundFormulas++;

	}

	private void increaseCounter(int currentCounts[], int position) {

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

	public void cancel() {
		canceled = true;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
	 */
	public double getFinishedPercentage() {
		return ((double) testedCombinations) / totalNumberOfCombinations;
	}

}
