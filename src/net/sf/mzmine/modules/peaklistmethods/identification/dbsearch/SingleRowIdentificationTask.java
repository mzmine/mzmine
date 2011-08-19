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

package net.sf.mzmine.modules.peaklistmethods.identification.dbsearch;

import java.text.NumberFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.mzmine.data.IonizationType;
import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.PeakIdentity;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopepatternscore.IsotopePatternScoreCalculator;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopeprediction.IsotopePatternCalculator;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.MZTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.ExceptionUtils;
import net.sf.mzmine.util.FormulaUtils;

public class SingleRowIdentificationTask extends AbstractTask {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	public static final NumberFormat massFormater = MZmineCore.getMZFormat();

	private int finishedItems = 0, numItems;

	private OnlineDatabase db;
	private double searchedMass;
	private MZTolerance mzTolerance;
	private int charge;
	private int numOfResults;
	private PeakListRow peakListRow;
	private IonizationType ionType;
	private boolean isotopeFilter = false;
	private ParameterSet isotopeFilterParameters;
	private DBGateway gateway;

	/**
	 * 
	 * @param parameters
	 * @param peakList
	 * @param peakListRow
	 * @param peak
	 */
	SingleRowIdentificationTask(ParameterSet parameters, PeakListRow peakListRow) {

		this.peakListRow = peakListRow;

		db = parameters.getParameter(OnlineDBSearchParameters.database)
				.getValue();

		try {
			gateway = db.getGatewayClass().newInstance();
		} catch (Exception e) {
			e.printStackTrace();
		}

		searchedMass = parameters.getParameter(
				OnlineDBSearchParameters.neutralMass).getValue();
		mzTolerance = parameters.getParameter(
				OnlineDBSearchParameters.mzTolerance).getValue();
		numOfResults = parameters.getParameter(
				OnlineDBSearchParameters.numOfResults).getValue();

		ionType = parameters.getParameter(OnlineDBSearchParameters.neutralMass)
				.getIonType();

		isotopeFilter = parameters.getParameter(
				OnlineDBSearchParameters.isotopeFilter).getValue();
		isotopeFilterParameters = parameters.getParameter(
				OnlineDBSearchParameters.isotopeFilter).getEmbeddedParameters();

		// If there is no isotope pattern, we cannot use the isotope filter
		if (peakListRow.getBestIsotopePattern() == null)
			isotopeFilter = false;

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

		setStatus(TaskStatus.PROCESSING);

		try {

			Desktop desktop = MZmineCore.getDesktop();
			NumberFormat massFormater = MZmineCore.getMZFormat();

			ResultWindow window = new ResultWindow(peakListRow, searchedMass,
					this);
			window.setTitle("Searching for "
					+ massFormater.format(searchedMass) + " amu");
			desktop.addInternalFrame(window);

			String compoundIDs[] = gateway.findCompounds(searchedMass,
					mzTolerance, numOfResults);

			// Get the number of results
			numItems = compoundIDs.length;

			if (numItems == 0) {
				window.setTitle("Searching for "
						+ massFormater.format(searchedMass)
						+ " amu: no results found");
			}

			// Process each one of the result ID's.
			for (int i = 0; i < numItems; i++) {

				if (getStatus() != TaskStatus.PROCESSING) {
					return;
				}

				DBCompound compound = gateway.getCompound(compoundIDs[i]);
				String formula = compound
						.getPropertyValue(PeakIdentity.PROPERTY_FORMULA);

				if (formula != null) {

					// First modify the formula according to the ionization
					String adjustedFormula = FormulaUtils.ionizeFormula(
							formula, ionType, charge);

					logger.finest("Calculating isotope pattern for compound formula "
							+ formula + " adjusted to " + adjustedFormula);

					// Generate IsotopePattern for this compound
					IsotopePattern compoundIsotopePattern = IsotopePatternCalculator
							.calculateIsotopePattern(adjustedFormula, 0.001,
									charge, ionType.getPolarity());

					compound.setIsotopePattern(compoundIsotopePattern);

					IsotopePattern rawDataIsotopePattern = peakListRow
							.getBestIsotopePattern();

					// If required, check isotope score
					if (isotopeFilter && (rawDataIsotopePattern != null)
							&& (compoundIsotopePattern != null)) {

						boolean isotopeCheck = IsotopePatternScoreCalculator
								.checkMatch(rawDataIsotopePattern,
										compoundIsotopePattern,
										isotopeFilterParameters);

						if (!isotopeCheck) {
							finishedItems++;
							continue;
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
			setStatus(TaskStatus.ERROR);
			errorMessage = "Could not connect to " + db + ": "
					+ ExceptionUtils.exceptionToString(e);
			return;
		}

		setStatus(TaskStatus.FINISHED);

	}

	public Object[] getCreatedObjects() {
		return null;
	}

}
