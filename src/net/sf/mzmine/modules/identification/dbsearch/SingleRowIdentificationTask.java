/*
 * Copyright 2006-2009 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.identification.dbsearch;

import java.text.NumberFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.IonizationType;
import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.isotopes.isotopepatternscore.IsotopePatternScoreCalculator;
import net.sf.mzmine.modules.isotopes.isotopeprediction.IsotopePatternCalculator;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.ExceptionUtils;
import net.sf.mzmine.util.FormulaUtils;

public class SingleRowIdentificationTask implements Task {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	public static final NumberFormat massFormater = MZmineCore.getMZFormat();

	private TaskStatus status = TaskStatus.WAITING;
	private String errorMessage;
	private int finishedItems = 0, numItems;

	private OnlineDatabase db;
	private double searchedMass, massTolerance;
	private int charge;
	private int numOfResults;
	private PeakList peakList;
	private PeakListRow peakListRow;
	private IonizationType ionType;
	private boolean isotopeFilter = false;
	private double isotopeScoreThreshold;
	private DBGateway gateway;

	/**
	 * 
	 * @param parameters
	 * @param peakList
	 * @param peakListRow
	 * @param peak
	 */
	SingleRowIdentificationTask(OnlineDBSearchParameters parameters,
			PeakList peakList, PeakListRow peakListRow) {

		this.peakList = peakList;
		this.peakListRow = peakListRow;

		db = (OnlineDatabase) parameters
				.getParameterValue(OnlineDBSearchParameters.database);

		try {
			gateway = db.getGatewayClass().newInstance();
		} catch (Exception e) {
			e.printStackTrace();
		}

		searchedMass = (Double) parameters
				.getParameterValue(OnlineDBSearchParameters.neutralMass);
		massTolerance = (Double) parameters
				.getParameterValue(OnlineDBSearchParameters.massTolerance);
		numOfResults = (Integer) parameters
				.getParameterValue(OnlineDBSearchParameters.numOfResults);
		charge = (Integer) parameters
				.getParameterValue(OnlineDBSearchParameters.charge);
		isotopeFilter = (Boolean) parameters
				.getParameterValue(OnlineDBSearchParameters.isotopeFilter);

		// If there is no isotope pattern, we cannot use the isotope filter
		if (peakListRow.getBestIsotopePatternPeak() == null)
			isotopeFilter = false;

		isotopeScoreThreshold = (Double) parameters
				.getParameterValue(OnlineDBSearchParameters.isotopeScoreTolerance);
		ionType = (IonizationType) parameters
				.getParameterValue(OnlineDBSearchParameters.ionizationMethod);

	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#cancel()
	 */
	public void cancel() {
		status = TaskStatus.CANCELED;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getErrorMessage()
	 */
	public String getErrorMessage() {
		return errorMessage;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
	 */
	public double getFinishedPercentage() {
		if (numItems == 0)
			return 0;
		return ((double) finishedItems) / numItems;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getStatus()
	 */
	public TaskStatus getStatus() {
		return status;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
	 */
	public String getTaskDescription() {
		return "Peak identification of " + massFormater.format(searchedMass)
				+ " using " + db;
	}

	/**
	 * @see java.lang.Runnable#run()
	 */
	public void run() {

		status = TaskStatus.PROCESSING;

		try {

			Desktop desktop = MZmineCore.getDesktop();
			NumberFormat massFormater = MZmineCore.getMZFormat();

			OnlineDBSearchWindow window = new OnlineDBSearchWindow(peakList,
					peakListRow, searchedMass, this);
			window.setTitle("Searching for "
					+ massFormater.format(searchedMass) + " amu");
			desktop.addInternalFrame(window);

			String compoundIDs[] = gateway.findCompounds(searchedMass,
					massTolerance, numOfResults);

			// Get the number of results
			numItems = compoundIDs.length;

			if (numItems == 0) {
				window.setTitle("Searching for "
						+ massFormater.format(searchedMass)
						+ " amu: no results found");
			}

			// Process each one of the result ID's.
			for (int i = 0; i < numItems; i++) {

				if (status != TaskStatus.PROCESSING) {
					return;
				}

				DBCompound compound = gateway.getCompound(compoundIDs[i]);

				if (compound.getCompoundFormula() != null) {

					// First modify the formula according to polarity - for
					// negative, remove one hydrogen; for positive, add one
					// hydrogen
					String adjustedFormula = FormulaUtils.ionizeFormula(
							compound.getCompoundFormula(), ionType
									.getPolarity(), charge);

					logger
							.finest("Calculating isotope pattern for compound formula "
									+ compound.getCompoundFormula()
									+ " adjusted to " + adjustedFormula);

					// Generate IsotopePattern for this compound
					IsotopePattern compoundIsotopePattern = IsotopePatternCalculator
							.calculateIsotopePattern(adjustedFormula, charge,
									ionType.getPolarity());
					
					compound.setIsotopePattern(compoundIsotopePattern);

					ChromatographicPeak bestPeakWithIsotopes = peakListRow
							.getBestIsotopePatternPeak();

					if (bestPeakWithIsotopes != null) {

						IsotopePattern rawDataIsotopePattern = bestPeakWithIsotopes
								.getIsotopePattern();

						double score = IsotopePatternScoreCalculator
								.getSimilarityScore(rawDataIsotopePattern,
										compoundIsotopePattern);
						
						compound.setIsotopePatternScore(score);

						// If required, check isotope score
						if (isotopeFilter) {
							if (score >= isotopeScoreThreshold) {
								finishedItems++;
								continue;
							}
						}
					}
				}

				// Add compound to the list of possible candidate and
				// display it in window of results.
				window.addNewListItem(compound);

				// Update window title
				window.setTitle("Searching for "
						+ massFormater.format(searchedMass) + " amu ("
						+ (i + 1) + "/" + numItems + ")");

				finishedItems++;

			}

		} catch (Exception e) {
			logger.log(Level.WARNING, "Could not connect to " + db, e);
			status = TaskStatus.ERROR;
			errorMessage = "Could not connect to " + db + ": "
					+ ExceptionUtils.exceptionToString(e);
			return;
		}

		status = TaskStatus.FINISHED;

	}

	public Object[] getCreatedObjects() {
		return null;
	}

}
