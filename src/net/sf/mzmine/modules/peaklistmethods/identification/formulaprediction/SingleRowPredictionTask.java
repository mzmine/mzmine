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
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.CollectionUtils;
import net.sf.mzmine.util.Range;

public class SingleRowPredictionTask extends AbstractTask {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	public static final NumberFormat massFormater = MZmineCore.getMZFormat();

	private ResultWindow window;
	private ElementRule elementRules[];
	private HeuristicRule heuristicRules[];

	private FormulaPredictionEngine predictionEngine;

	private double searchedMass, massTolerance;
	private int charge;
	private IonizationType ionType;
	private int numOfResults;
	private String elements;
	private PeakList peakList;
	private PeakListRow peakListRow;
	private boolean isotopeFilter, msmsFilter;
	private double isotopeScoreThreshold, msmsScoreThreshold, msmsTolerance,
			msmsNoiseLevel;

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
		ionType = (IonizationType) parameters
				.getParameterValue(FormulaPredictionParameters.ionizationMethod);
		isotopeFilter = (Boolean) parameters
				.getParameterValue(FormulaPredictionParameters.isotopeFilter);
		heuristicRules = CollectionUtils.changeArrayType((Object[]) parameters
				.getParameterValue(FormulaPredictionParameters.heuristicRules),
				HeuristicRule.class);

		// If there is no detected isotope pattern, we cannot use the isotope
		// filter
		if (peakListRow.getBestIsotopePattern() == null)
			isotopeFilter = false;

		isotopeScoreThreshold = (Double) parameters
				.getParameterValue(FormulaPredictionParameters.isotopeScoreTolerance);

		// Sorted by mass in descending order
		TreeSet<ElementRule> rulesSet = new TreeSet<ElementRule>(
				new ElementRuleSorterByMass());

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

				rulesSet.add(rule);

			} catch (IllegalArgumentException e) {
				logger.log(Level.WARNING, "Invald format", e);
				continue;
			}

		}

		elementRules = rulesSet.toArray(new ElementRule[0]);

		msmsTolerance = (Double) parameters
				.getParameterValue(FormulaPredictionParameters.msmsTolerance);
		msmsNoiseLevel = (Double) parameters
				.getParameterValue(FormulaPredictionParameters.msmsNoiseLevel);

	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
	 */
	public double getFinishedPercentage() {
		if (predictionEngine == null)
			return 0d;
		return predictionEngine.getFinishedPercentage();
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
	 */
	public String getTaskDescription() {
		return "Formula prediction for " + massFormater.format(searchedMass);
	}

	public void cancel() {
		super.cancel();
		if (predictionEngine != null)
			predictionEngine.cancel();
	}

	/**
	 * @see java.lang.Runnable#run()
	 */
	public void run() {

		setStatus(TaskStatus.PROCESSING);

		Desktop desktop = MZmineCore.getDesktop();
		NumberFormat massFormater = MZmineCore.getMZFormat();

		window = new ResultWindow("Searching for "
				+ massFormater.format(searchedMass) + " amu", peakList,
				peakListRow, searchedMass, charge, this);
		desktop.addInternalFrame(window);

		Range targetRange = new Range(searchedMass - massTolerance,
				searchedMass + massTolerance);

		logger.info("Starting search for formulas for " + searchedMass
				+ ", elements " + Arrays.toString(elementRules));

		predictionEngine = new FormulaPredictionEngine(targetRange,
				elementRules, heuristicRules, isotopeFilter,
				isotopeScoreThreshold, charge, ionType, msmsFilter,
				msmsScoreThreshold, msmsTolerance, msmsNoiseLevel,
				numOfResults, peakListRow, window);

		int foundFormulas = predictionEngine.run();

		logger.info("Finished formula search for " + searchedMass + ", found "
				+ foundFormulas + " formulas");

		setStatus(TaskStatus.FINISHED);

	}
}
