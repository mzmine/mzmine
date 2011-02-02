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
import java.util.Map;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.IonizationType;
import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopepatternscore.IsotopePatternScoreCalculator;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopeprediction.IsotopePatternCalculator;
import net.sf.mzmine.modules.peaklistmethods.msms.msmsscore.MSMSScore;
import net.sf.mzmine.modules.peaklistmethods.msms.msmsscore.MSMSScoreCalculator;
import net.sf.mzmine.util.FormulaUtils;
import net.sf.mzmine.util.Range;

import org.openscience.cdk.formula.MolecularFormula;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class FormulaPredictionEngine {

	private long testedCombinations, totalNumberOfCombinations = 1;

	private Range massRange;
	private FormulaAcceptor acceptor;
	private ElementRule elementRules[];

	private int foundFormulas = 0;
	private IonizationType ionType;
	private int charge;
	private int maxFormulas;
	private PeakListRow peakListRow;
	private boolean checkIsotopes, checkMSMS;
	private HeuristicRule[] heuristicRules;
	private double minIsotopeScore, minMSMSScore, msmsTolerance,
			msmsNoiseLevel, isotopeMassTolerance;
	private boolean canceled = false;

	public FormulaPredictionEngine(Range massRange, ElementRule elementRules[],
			FormulaAcceptor acceptor) {
		this(massRange, elementRules, new HeuristicRule[] {
				HeuristicRule.LEWIS, HeuristicRule.SENIOR }, false, 0, 0, 0,
				null, false, 0d, 0, 0, 1, null, acceptor);
	}

	FormulaPredictionEngine(Range massRange, ElementRule elementRules[],
			HeuristicRule heuristicRules[], boolean checkIsotopes,
			double isotopeMassTolerance, double minIsotopeScore, int charge,
			IonizationType ionType, boolean checkMSMS, double minMSMSScore,
			double msmsTolerance, double msmsNoiseLevel, int maxFormulas,
			PeakListRow peakListRow, FormulaAcceptor acceptor) {

		this.massRange = massRange;
		this.elementRules = elementRules;
		this.heuristicRules = heuristicRules;
		this.checkIsotopes = checkIsotopes;
		this.isotopeMassTolerance = isotopeMassTolerance;
		this.minIsotopeScore = minIsotopeScore;
		this.charge = charge;
		this.ionType = ionType;
		this.checkMSMS = checkMSMS;
		this.msmsTolerance = msmsTolerance;
		this.msmsNoiseLevel = msmsNoiseLevel;
		this.minMSMSScore = minMSMSScore;
		this.maxFormulas = maxFormulas;
		this.acceptor = acceptor;
		this.peakListRow = peakListRow;
	}

	/**
	 * 
	 * @return Number of formulas found
	 */
	public int run() {

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

				MolecularFormula cdkFormula = new MolecularFormula();

				for (int i = 0; i < elementRules.length; i++) {
					if (currentCounts[i] == 0)
						continue;
					cdkFormula.addIsotope(elementRules[i].getElementObject(),
							currentCounts[i]);
				}

				// Mass is ok, so test other constraints, if required
				ResultFormula resultEntry = checkConstraints(cdkFormula);

				if (resultEntry != null) {
					// Add the new formula entry
					if (acceptor != null)
						acceptor.addFormula(resultEntry);

					foundFormulas++;
				}

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

	private ResultFormula checkConstraints(MolecularFormula cdkFormula) {

		// Check if the required rules are fulfilled
		for (HeuristicRule rule : heuristicRules) {
			Boolean conformity = HeuristicRuleChecker.checkRule(cdkFormula,
					rule);
			if ((conformity != null) && (conformity == false))
				return null;
		}

		if (peakListRow == null) {
			final ResultFormula resultEntry = new ResultFormula(cdkFormula,
					null, null, null, null);
			return resultEntry;
		}

		// Calculate isotope similarity score
		IsotopePattern detectedPattern = peakListRow.getBestIsotopePattern();
		IsotopePattern predictedIsotopePattern = null;
		Double isotopeScore = null;
		if ((checkIsotopes) && (detectedPattern != null)) {

			String originalFormula = MolecularFormulaManipulator
					.getString(cdkFormula);
			String adjustedFormula = FormulaUtils.ionizeFormula(
					originalFormula, ionType.getPolarity(), charge);

			predictedIsotopePattern = IsotopePatternCalculator
					.calculateIsotopePattern(adjustedFormula, charge,
							ionType.getPolarity());

			isotopeScore = IsotopePatternScoreCalculator.getSimilarityScore(
					detectedPattern, predictedIsotopePattern,
					isotopeMassTolerance);

			// Check the isotope condition
			if (isotopeScore < minIsotopeScore)
				return null;
		}

		// MS/MS evaluation is slowest, so let's do it last
		Double msmsScore = null;
		ChromatographicPeak bestPeak = peakListRow.getBestPeak();
		RawDataFile dataFile = bestPeak.getDataFile();
		Map<DataPoint, String> msmsAnnotations = null;
		int msmsScanNumber = bestPeak.getMostIntenseFragmentScanNumber();
		if ((checkMSMS) && (msmsScanNumber > 0)) {

			Scan msmsScan = dataFile.getScan(msmsScanNumber);

			MSMSScore score = MSMSScoreCalculator.evaluateMSMS(msmsScan,
					msmsTolerance, msmsNoiseLevel, cdkFormula);

			if (score != null) {
				msmsScore = score.getScore();
				msmsAnnotations = score.getAnnotation();

				// Check the MS/MS condition
				if (msmsScore < minMSMSScore)
					return null;
			}

		}

		// Create a new formula entry
		final ResultFormula resultEntry = new ResultFormula(cdkFormula,
				predictedIsotopePattern, isotopeScore, msmsScore,
				msmsAnnotations);

		return resultEntry;

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
