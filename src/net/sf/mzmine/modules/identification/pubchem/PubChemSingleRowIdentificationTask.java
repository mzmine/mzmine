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

package net.sf.mzmine.modules.identification.pubchem;

import java.text.NumberFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.mzmine.data.IonizationType;
import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.isotopes.isotopepatternscore.IsotopePatternScoreCalculator;
import net.sf.mzmine.modules.isotopes.isotopeprediction.FormulaAnalyzer;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.ExceptionUtils;
import net.sf.mzmine.util.Range;

public class PubChemSingleRowIdentificationTask implements Task {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	public static final NumberFormat massFormater = MZmineCore.getMZFormat();

	private TaskStatus status = TaskStatus.WAITING;
	private String errorMessage;
	private int finishedItems = 0, numItems;
	private double valueOfQuery, massTolerance;
	private int charge;
	private int maxNumOfResults;
	private PeakList peakList;
	private PeakListRow peakListRow;
	private IonizationType ionType;
	private boolean chargedMol = false, isotopeFilter = false;
	private double isotopeScoreThreshold;
	private FormulaAnalyzer analyzer = new FormulaAnalyzer();
	private boolean isProxy = false;
	private String proxyAddress;
	private String proxyPort;
	
	/**
	 * 
	 * @param parameters
	 * @param peakList
	 * @param peakListRow
	 * @param peak
	 */
	PubChemSingleRowIdentificationTask(PubChemSearchParameters parameters,
			PeakList peakList, PeakListRow peakListRow) {

		this.peakList = peakList;
		this.peakListRow = peakListRow;

		valueOfQuery = (Double) parameters
				.getParameterValue(PubChemSearchParameters.neutralMass);
		massTolerance = (Double) parameters
				.getParameterValue(PubChemSearchParameters.mzToleranceField);
		maxNumOfResults = (Integer) parameters
				.getParameterValue(PubChemSearchParameters.numOfResults);
		charge = (Integer) parameters
				.getParameterValue(PubChemSearchParameters.charge);
		chargedMol = (Boolean) parameters
				.getParameterValue(PubChemSearchParameters.chargedMol);
		isotopeFilter = (Boolean) parameters
				.getParameterValue(PubChemSearchParameters.isotopeFilter);

		isProxy = (Boolean) parameters
				.getParameterValue(PubChemSearchParameters.proxy);
		proxyAddress = (String) parameters
				.getParameterValue(PubChemSearchParameters.proxyAddress);
		proxyPort = (String) parameters
				.getParameterValue(PubChemSearchParameters.proxyPort);

		// If there is no isotope pattern, we cannot use the isotope filter
		if (peakListRow.getBestIsotopePattern() == null)
			isotopeFilter = false;

		isotopeScoreThreshold = (Double) parameters
				.getParameterValue(PubChemSearchParameters.isotopeScoreTolerance);
		ionType = (IonizationType) parameters
				.getParameterValue(PubChemSearchParameters.ionizationMethod);

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
		return "Peak identification of " + massFormater.format(valueOfQuery)
				+ " using PubChem Compound database";
	}

	/**
	 * @see java.lang.Runnable#run()
	 */
	public void run() {

		status = TaskStatus.PROCESSING;

		try {

			Desktop desktop = MZmineCore.getDesktop();
			PubChemSearchWindow window = new PubChemSearchWindow(peakList, peakListRow, valueOfQuery);
			desktop.addInternalFrame(window);

			Range massRange = new Range(valueOfQuery - massTolerance,
					valueOfQuery + massTolerance);

			boolean chargedOnly = false;
			if ((chargedMol) && (ionType.equals(IonizationType.NO_IONIZATION)))
				chargedOnly = true;

			int resultCIDs[] = PubChemGateway.findPubchemCID(massRange,
					maxNumOfResults, chargedOnly, isProxy, proxyAddress, proxyPort);

			// Get the number of results
			numItems = resultCIDs.length;

			// Process each one of the result ID's.
			for (int i = 0; i < numItems; i++) {

				if (status != TaskStatus.PROCESSING) {
					return;
				}

				String compoundName = PubChemGateway.getName(resultCIDs[i]);

				PubChemCompound compound = new PubChemCompound(resultCIDs[i],
						compoundName, null, null);

				PubChemGateway.getSummary(compound);

				// Generate IsotopePattern for this compound
				IsotopePattern compoundIsotopePattern = analyzer
						.getIsotopePattern(compound.getCompoundFormula(), 0.01,
								charge, ionType.isPositiveCharge(), 0, true,
								true, ionType);
				compound.setIsotopePattern(compoundIsotopePattern);

				// If required, check isotope score
				if (isotopeFilter) {

					IsotopePattern rawDataIsotopePattern = peakListRow
							.getBestIsotopePattern();

					double score = IsotopePatternScoreCalculator.getScore(
							rawDataIsotopePattern, compoundIsotopePattern);

					compound.setIsotopePatternScore(String.valueOf(score));

					if (score >= isotopeScoreThreshold) {
						finishedItems++;
						continue;
					}

				}

				// Add compound to the list of possible candidate and
				// display it in window of results.
				window.addNewListItem(compound);

				finishedItems++;

			}

		} catch (Exception e) {
			logger.log(Level.WARNING, "Could not connect to PubChem ", e);
			status = TaskStatus.ERROR;
			errorMessage = "Could not connect to PubChem: "
					+ ExceptionUtils.exceptionToString(e);
			return;
		}

		status = TaskStatus.FINISHED;

	}

	public Object[] getCreatedObjects() {
		return null;
	}

}
