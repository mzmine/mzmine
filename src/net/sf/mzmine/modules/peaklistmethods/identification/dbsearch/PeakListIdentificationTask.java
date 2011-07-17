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
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.IonizationType;
import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.PeakIdentity;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopepatternscore.IsotopePatternScoreCalculator;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopeprediction.IsotopePatternCalculator;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.MZTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.ExceptionUtils;
import net.sf.mzmine.util.FormulaUtils;
import net.sf.mzmine.util.PeakListRowSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

public class PeakListIdentificationTask extends AbstractTask {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	public static final NumberFormat massFormater = MZmineCore.getMZFormat();

	private int finishedItems = 0, numItems;

	private OnlineDatabase db;
	private MZTolerance mzTolerance;
	private int numOfResults;
	private PeakList peakList;
	private PeakListRow currentRow;
	private boolean isotopeFilter = false;
	private ParameterSet isotopeFilterParameters;
	private DBGateway gateway;
	private IonizationType ionType;

	/**
	 * 
	 * @param parameters
	 * @param peakList
	 * @param peakListRow
	 * @param peak
	 */
	PeakListIdentificationTask(ParameterSet parameters, PeakList peakList) {

		this.peakList = peakList;

		db = parameters.getParameter(OnlineDBSearchParameters.database)
				.getValue();

		try {
			gateway = db.getGatewayClass().newInstance();
		} catch (Exception e) {
			e.printStackTrace();
		}

		mzTolerance = parameters.getParameter(
				OnlineDBSearchParameters.mzTolerance).getValue();
		numOfResults = parameters.getParameter(
				OnlineDBSearchParameters.numOfResults).getInt();
		isotopeFilter = parameters.getParameter(
				OnlineDBSearchParameters.isotopeFilter).getValue();
		isotopeFilterParameters = parameters.getParameter(
				OnlineDBSearchParameters.isotopeFilter).getEmbeddedParameters();
		this.ionType = parameters.getParameter(
				OnlineDBSearchParameters.neutralMass).getIonType();

	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
	 */
	public double getFinishedPercentage() {
		return ((double) finishedItems) / numItems;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
	 */
	public String getTaskDescription() {
		if (currentRow == null) {
			return "Identification of peaks in " + peakList + " using " + db;
		} else {
			NumberFormat mzFormat = MZmineCore.getMZFormat();
			return "Identification of peaks in " + peakList + " ("
					+ mzFormat.format(currentRow.getAverageMZ())
					+ " m/z) using " + db;

		}
	}

	/**
	 * @see java.lang.Runnable#run()
	 */
	public void run() {

		setStatus(TaskStatus.PROCESSING);

		PeakListRow rows[] = peakList.getRows();

		// Identify the peak list rows starting from the biggest peaks
		Arrays.sort(rows, new PeakListRowSorter(SortingProperty.Area,
				SortingDirection.Descending));

		numItems = rows.length;

		try {

			for (PeakListRow row : rows) {

				if (getStatus() != TaskStatus.PROCESSING)
					return;

				// Retrieve results for each row
				retrieveIdentification(row);

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

	private void retrieveIdentification(PeakListRow row) throws Exception {

		currentRow = row;

		ChromatographicPeak bestPeak = row.getBestPeak();

		int charge = bestPeak.getCharge();

		if (charge == 0)
			charge = 1;

		IsotopePattern rowIsotopePattern = bestPeak.getIsotopePattern();

		double massValue = (row.getAverageMZ() - ionType.getAddedMass())
				* charge;

		String compoundIDs[] = gateway.findCompounds(massValue, mzTolerance,
				numOfResults);

		// Process each one of the result ID's.
		for (String compoundID : compoundIDs) {

			if (getStatus() != TaskStatus.PROCESSING) {
				return;
			}

			DBCompound compound = gateway.getCompound(compoundID);
			String formula = compound
					.getPropertyValue(PeakIdentity.PROPERTY_FORMULA);

			// If required, check isotope score
			if (isotopeFilter && (rowIsotopePattern != null)
					&& (formula != null)) {

				// First modify the formula according to ionization
				String adjustedFormula = FormulaUtils.ionizeFormula(formula,
						ionType, charge);

				logger.finest("Calculating isotope pattern for compound formula "
						+ formula + " adjusted to " + adjustedFormula);

				// Generate IsotopePattern for this compound
				IsotopePattern compoundIsotopePattern = IsotopePatternCalculator
						.calculateIsotopePattern(adjustedFormula, 0.001,
								charge, ionType.getPolarity());

				boolean check = IsotopePatternScoreCalculator.checkMatch(
						rowIsotopePattern, compoundIsotopePattern,
						isotopeFilterParameters);

				if (!check)
					continue;

			}

			// Add the retrieved identity to the peak list row
			row.addPeakIdentity(compound, false);

			// Notify the GUI about the change in the project
			MZmineCore.getCurrentProject().notifyObjectChanged(row, false);

		}
	}

	public Object[] getCreatedObjects() {
		return null;
	}

}
