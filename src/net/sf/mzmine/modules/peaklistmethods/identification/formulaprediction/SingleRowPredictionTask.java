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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.IonizationType;
import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.elements.ElementRule;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.restictions.ElementalHeuristicChecker;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.restictions.RDBERestrictionChecker;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopepatternscore.IsotopePatternScoreCalculator;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopepatternscore.IsotopePatternScoreParameters;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopeprediction.IsotopePatternCalculator;
import net.sf.mzmine.modules.peaklistmethods.msms.msmsscore.MSMSScore;
import net.sf.mzmine.modules.peaklistmethods.msms.msmsscore.MSMSScoreCalculator;
import net.sf.mzmine.modules.peaklistmethods.msms.msmsscore.MSMSScoreParameters;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.MZTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.FormulaUtils;
import net.sf.mzmine.util.Range;

import org.openscience.cdk.formula.MolecularFormula;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class SingleRowPredictionTask extends AbstractTask {

	private ResultWindow resultWindow;

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private Range massRange;
	private ElementRule elementRules[];
	private FormulaGenerator generator;

	private int maxFormulas, foundFormulas = 0;
	private IonizationType ionType;
	private double searchedMass;
	private int charge;
	private PeakList peakList;
	private PeakListRow peakListRow;
	private boolean checkIsotopes, checkMSMS, checkRatios, checkRDBE;
	private ParameterSet isotopeParameters, msmsParameters, ratiosParameters,
			rdbeParameters;

	/**
	 * 
	 * @param parameters
	 * @param peakList
	 * @param peakListRow
	 * @param peak
	 */
	SingleRowPredictionTask(ParameterSet parameters, PeakList peakList,
			PeakListRow peakListRow) {

		this.peakList = peakList;

		searchedMass = parameters.getParameter(
				FormulaPredictionParameters.neutralMass).getValue();
		charge = parameters.getParameter(
				FormulaPredictionParameters.neutralMass).getCharge();
		ionType = parameters.getParameter(
				FormulaPredictionParameters.neutralMass).getIonType();
		MZTolerance mzTolerance = parameters.getParameter(
				FormulaPredictionParameters.mzTolerance).getValue();
		maxFormulas = parameters.getParameter(
				FormulaPredictionParameters.numOfResults).getInt();

		checkIsotopes = parameters.getParameter(
				FormulaPredictionParameters.isotopeFilter).getValue();
		isotopeParameters = parameters.getParameter(
				FormulaPredictionParameters.isotopeFilter)
				.getEmbeddedParameters();

		checkMSMS = parameters.getParameter(
				FormulaPredictionParameters.msmsFilter).getValue();
		msmsParameters = parameters.getParameter(
				FormulaPredictionParameters.msmsFilter).getEmbeddedParameters();

		checkRDBE = parameters.getParameter(
				FormulaPredictionParameters.rdbeRestrictions).getValue();
		rdbeParameters = parameters.getParameter(
				FormulaPredictionParameters.rdbeRestrictions)
				.getEmbeddedParameters();

		checkRatios = parameters.getParameter(
				FormulaPredictionParameters.elementalRatios).getValue();
		ratiosParameters = parameters.getParameter(
				FormulaPredictionParameters.elementalRatios)
				.getEmbeddedParameters();

		Set<ElementRule> rulesSet = new HashSet<ElementRule>();

		massRange = mzTolerance.getToleranceRange(searchedMass);

		String elements = parameters.getParameter(
				FormulaPredictionParameters.elements).getValue();

		String elementsArray[] = elements.split(",");
		for (String elementEntry : elementsArray) {

			try {
				ElementRule rule = new ElementRule(elementEntry);

				// We can ignore elements with max 0 atoms
				if (rule.getMaxCount() == 0)
					continue;

				// Adjust the maximum numbers according to the mass we are
				// searching
				int maxCountAccordingToMass = (int) (massRange.getMax() / rule
						.getMass());
				if (rule.getMaxCount() > maxCountAccordingToMass) {
					rule.setMaxCount(maxCountAccordingToMass);
				}

				rulesSet.add(rule);

			} catch (IllegalArgumentException e) {
				logger.log(Level.WARNING, "Invald element rule format", e);
				continue;
			}

		}

		elementRules = rulesSet.toArray(new ElementRule[0]);
		this.peakListRow = peakListRow;

	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
	 */
	public double getFinishedPercentage() {
		if (foundFormulas >= maxFormulas)
			return 1;
		if (generator == null)
			return 0;
		return generator.getFinishedPercentage();
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
	 */
	public String getTaskDescription() {
		return "Formula prediction for "
				+ MZmineCore.getMZFormat().format(searchedMass);
	}

	/**
	 * @see java.lang.Runnable#run()
	 */
	public void run() {

		setStatus(TaskStatus.PROCESSING);

		Desktop desktop = MZmineCore.getDesktop();
		NumberFormat massFormater = MZmineCore.getMZFormat();

		resultWindow = new ResultWindow("Searching for "
				+ massFormater.format(searchedMass), peakList, peakListRow,
				searchedMass, charge, this);
		desktop.addInternalFrame(resultWindow);

		logger.finest("Starting search for formulas for " + massRange
				+ " Da, elements " + Arrays.toString(elementRules));

		generator = new FormulaGenerator(massRange, elementRules);

		while (foundFormulas < maxFormulas) {

			if (isCanceled())
				return;

			MolecularFormula cdkFormula = generator.getNextFormula();

			if (cdkFormula == null)
				break;

			// Mass is ok, so test other constraints
			checkConstraints(cdkFormula);

		}

		logger.finest("Finished formula search for " + massRange
				+ " m/z, found " + foundFormulas + " formulas");

		resultWindow.setTitle("Finished searching for "
				+ massFormater.format(searchedMass) + " amu, " + foundFormulas
				+ " formulas found");

		setStatus(TaskStatus.FINISHED);

	}

	private void checkConstraints(MolecularFormula cdkFormula) {

		// Check elemental ratios
		if (checkRatios) {
			boolean check = ElementalHeuristicChecker.checkFormula(cdkFormula,
					ratiosParameters);
			if (!check)
				return;
		}

		double rdbeValue = RDBERestrictionChecker.calculateRDBE(cdkFormula);

		// Check RDBE condition
		if (checkRDBE) {
			boolean check = RDBERestrictionChecker.checkRDBE(rdbeValue,
					rdbeParameters);
			if (!check)
				return;
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

			// TODO: make 0.001 user parameter
			predictedIsotopePattern = IsotopePatternCalculator
					.calculateIsotopePattern(adjustedFormula, 0.001, charge,
							ionType.getPolarity());

			isotopeScore = IsotopePatternScoreCalculator
					.getSimilarityScore(detectedPattern,
							predictedIsotopePattern, isotopeParameters);

			double minScore = isotopeParameters.getParameter(
					IsotopePatternScoreParameters.isotopePatternScoreThreshold)
					.getValue();

			if (isotopeScore < minScore)
				return;

		}

		// MS/MS evaluation is slowest, so let's do it last
		Double msmsScore = null;
		ChromatographicPeak bestPeak = peakListRow.getBestPeak();
		RawDataFile dataFile = bestPeak.getDataFile();
		Map<DataPoint, String> msmsAnnotations = null;
		int msmsScanNumber = bestPeak.getMostIntenseFragmentScanNumber();
		if ((checkMSMS) && (msmsScanNumber > 0)) {

			Scan msmsScan = dataFile.getScan(msmsScanNumber);

			MSMSScore score = MSMSScoreCalculator.evaluateMSMS(cdkFormula,
					msmsScan, msmsParameters);

			double minMSMSScore = msmsParameters.getParameter(
					MSMSScoreParameters.msmsMinScore).getValue();

			if (score != null) {
				msmsScore = score.getScore();
				msmsAnnotations = score.getAnnotation();

				// Check the MS/MS condition
				if (msmsScore < minMSMSScore)
					return;
			}

		}

		// Create a new formula entry
		final ResultFormula resultEntry = new ResultFormula(cdkFormula,
				predictedIsotopePattern, rdbeValue, isotopeScore, msmsScore,
				msmsAnnotations);

		// Add the new formula entry
		resultWindow.addNewListItem(resultEntry);

		foundFormulas++;

	}

}
