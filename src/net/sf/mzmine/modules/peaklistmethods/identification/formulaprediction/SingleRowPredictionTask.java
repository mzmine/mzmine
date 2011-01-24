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
import java.util.Arrays;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.mzmine.data.IonizationType;
import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopepatternscore.IsotopePatternScoreCalculator;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopeprediction.IsotopePatternCalculator;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.CollectionUtils;
import net.sf.mzmine.util.FormulaUtils;
import net.sf.mzmine.util.Range;

import org.openscience.cdk.formula.MolecularFormula;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class SingleRowPredictionTask extends AbstractTask {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	public static final NumberFormat massFormater = MZmineCore.getMZFormat();

	private long testedCombinations, totalNumberOfCombinations = 1;

	private int foundFormulas = 0;
	private ResultWindow window;
	private ElementRule elementRules[];

	private double searchedMass, massTolerance;
	private int charge;
	private int numOfResults;
	private String elements;
	private PeakList peakList;
	private PeakListRow peakListRow;
	private IonizationType ionType;
	private IsotopePattern detectedPattern;
	private boolean isotopeFilter;
	private HeuristicRule[] heuristicRules;
	private double isotopeScoreThreshold;

	/**
	 * 
	 * @param parameters
	 * @param peakList
	 * @param peakListRow
	 * @param peak
	 */
	SingleRowPredictionTask(FormulaPredictionParameters parameters,
			PeakList peakList, PeakListRow peakListRow) {

		this.peakList = peakList;
		this.peakListRow = peakListRow;

		searchedMass = (Double) parameters
				.getParameterValue(FormulaPredictionParameters.neutralMass);
		massTolerance = (Double) parameters
				.getParameterValue(FormulaPredictionParameters.massTolerance);
		numOfResults = (Integer) parameters
				.getParameterValue(FormulaPredictionParameters.numOfResults);
		elements = (String) parameters
				.getParameterValue(FormulaPredictionParameters.elements);
		charge = (Integer) parameters
				.getParameterValue(FormulaPredictionParameters.charge);
		isotopeFilter = (Boolean) parameters
				.getParameterValue(FormulaPredictionParameters.isotopeFilter);
		heuristicRules = CollectionUtils.changeArrayType((Object[]) parameters
				.getParameterValue(FormulaPredictionParameters.heuristicRules), 
				HeuristicRule.class);

		detectedPattern = peakListRow.getBestIsotopePattern();

		// If there is no isotope pattern, we cannot use the isotope filter
		if (peakListRow.getBestIsotopePattern() == null)
			isotopeFilter = false;

		isotopeScoreThreshold = (Double) parameters
				.getParameterValue(FormulaPredictionParameters.isotopeScoreTolerance);
		ionType = (IonizationType) parameters
				.getParameterValue(FormulaPredictionParameters.ionizationMethod);

	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
	 */
	public double getFinishedPercentage() {
		return ((double) testedCombinations) / totalNumberOfCombinations;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
	 */
	public String getTaskDescription() {
		return "Formula prediction for " + massFormater.format(searchedMass);
	}

	/**
	 * @see java.lang.Runnable#run()
	 */
	public void run() {

		setStatus(TaskStatus.PROCESSING);

		Desktop desktop = MZmineCore.getDesktop();
		NumberFormat massFormater = MZmineCore.getMZFormat();

		window = new ResultWindow(peakList, peakListRow, searchedMass, charge,
				detectedPattern, this);
		window.setTitle("Searching for " + massFormater.format(searchedMass)
				+ " amu");
		desktop.addInternalFrame(window);

		// setup rules

		Range targetRange = new Range(searchedMass - massTolerance,
				searchedMass + massTolerance);

		// Sorted by mass in descending order
		TreeSet<ElementRule> rulesSet = new TreeSet<ElementRule>();

		totalNumberOfCombinations = 1;

		String elementsArray[] = elements.split(",");
		for (String elementEntry : elementsArray) {

			try {
				ElementRule rule = new ElementRule(elementEntry);

				// We can ignore elements with max 0 atoms
				if (rule.getMaxCount() == 0)
					continue;

				// Adjust the maximum numbers according to the mass we are
				// searching
				int maxCountAccordingToMass = (int) Math
						.floor((searchedMass + massTolerance) / rule.getMass());
				if (rule.getMaxCount() > maxCountAccordingToMass) {
					rule.setMaxCount(maxCountAccordingToMass);
				}

				totalNumberOfCombinations *= rule.getNumberOfCombinations();

				rulesSet.add(rule);

			} catch (IllegalArgumentException e) {
				logger.log(Level.WARNING, "Invald format", e);
				continue;
			}

		}

		elementRules = rulesSet.toArray(new ElementRule[0]);

		int currentCounts[] = new int[elementRules.length];

		// Set minimal counts
		for (int i = 0; i < elementRules.length; i++) {
			currentCounts[i] = elementRules[i].getMinCount();
		}

		logger.info("Starting search for formulas for " + searchedMass
				+ ", elements " + Arrays.toString(elementRules) + ", total "
				+ totalNumberOfCombinations + " combinations");

		mainCycle: while (testedCombinations < totalNumberOfCombinations) {

			if (getStatus() == TaskStatus.CANCELED)
				return;

			double mass = calculateMass(currentCounts);

			if (targetRange.contains(mass)) {

				// Mass is ok, so test other constraints, too
				testFormula(currentCounts);

				// Stopping condition
				if (foundFormulas == numOfResults)
					break mainCycle;

			} else {

				// Heuristics: if we are over the mass, it is meaningless to add
				// more atoms, so let's jump directly to the maximum count
				if (mass > targetRange.getMax()) {
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

		logger.info("Finished formula search for " + searchedMass + ", tested "
				+ testedCombinations + "/" + totalNumberOfCombinations
				+ " combinations");

		setStatus(TaskStatus.FINISHED);

	}

	private void testFormula(int currentCounts[]) {

		
		CandidateFormula candidate = new CandidateFormula(elementRules, currentCounts, detectedPattern, ionType, charge); 
		
		// Heuristic rules check
		for (HeuristicRule rule : heuristicRules) {
			switch (rule) {
			case LEWIS:
				if (! candidate.conformsLEWIS())
					return;
				break;
			case SENIOR:
				if (! candidate.conformsSENIOR())
					return;
				break;
			case HC:
				if (! candidate.conformsHC())
					return;
				break;
			case NOPS:
				if (! candidate.conformsNOPS())
					return;
				break;
			case HNOPS:
				if (! candidate.conformsHNOPS())
					return;
				break;
			}
		}

		// ISOTOPE FILTER CHECK

		if ((isotopeFilter) && (detectedPattern != null)) {
			double score = candidate.getIsotopeScore();
			if (score < isotopeScoreThreshold)
				return;

		}

		window.addNewListItem(candidate);
		foundFormulas++;
		window.setTitle("Searching for " + massFormater.format(searchedMass)
				+ " amu, " + foundFormulas + " formulas found");
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

	private double calculateMass(int currentCounts[]) {

		double resultMass = 0;
		for (int i = 0; i < elementRules.length; i++) {
			resultMass += elementRules[i].getMass() * currentCounts[i];
		}
		return resultMass;

	}

}
