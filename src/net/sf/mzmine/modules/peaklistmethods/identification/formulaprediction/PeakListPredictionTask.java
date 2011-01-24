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
import java.util.logging.Logger;

import net.sf.mzmine.data.IonizationType;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.project.ProjectEvent;
import net.sf.mzmine.project.ProjectEvent.ProjectEventType;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.PeakListRowSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

public class PeakListPredictionTask extends AbstractTask {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	public static final NumberFormat massFormater = MZmineCore.getMZFormat();

	private int finishedItems = 0, numItems;

	private double massTolerance;
	private int numOfResults;
	private PeakList peakList;
	private PeakListRow currentRow;
	private IonizationType ionType;
	private boolean isotopeFilter = false;
	private double isotopeScoreThreshold;

	/**
	 * 
	 * @param parameters
	 * @param peakList
	 * @param peakListRow
	 * @param peak
	 */
	PeakListPredictionTask(FormulaPredictionParameters parameters,
			PeakList peakList) {

		this.peakList = peakList;

		massTolerance = (Double) parameters
				.getParameterValue(FormulaPredictionParameters.massTolerance);
		numOfResults = (Integer) parameters
				.getParameterValue(FormulaPredictionParameters.numOfResults);
		isotopeFilter = (Boolean) parameters
				.getParameterValue(FormulaPredictionParameters.isotopeFilter);
		isotopeScoreThreshold = (Double) parameters
				.getParameterValue(FormulaPredictionParameters.isotopeScoreTolerance);
		ionType = (IonizationType) parameters
				.getParameterValue(FormulaPredictionParameters.ionizationMethod);

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
			return "Prediction of formulas in " + peakList;
		} else {
			NumberFormat mzFormat = MZmineCore.getMZFormat();
			return "Prediction of formulas in " + peakList + " ("
					+ mzFormat.format(currentRow.getAverageMZ())
					+ " m/z)";

		}
	}

	/**
	 * @see java.lang.Runnable#run()
	 */
	public void run() {

		setStatus( TaskStatus.PROCESSING );

		PeakListRow rows[] = peakList.getRows();

		// Identify the peak list rows starting from the biggest peaks
		Arrays.sort(rows, new PeakListRowSorter(SortingProperty.Area,
				SortingDirection.Descending));

		numItems = rows.length;

			for (PeakListRow row : rows) {

				if (getStatus( ) != TaskStatus.PROCESSING)
					return;

				// Retrieve results for each row
				retrieveIdentification(row);

				// Notify the tree that peak list has changed
				ProjectEvent newEvent = new ProjectEvent(
						ProjectEventType.PEAKLIST_CONTENTS_CHANGED, peakList);
				MZmineCore.getProjectManager().fireProjectListeners(newEvent);

				finishedItems++;

			}

		setStatus( TaskStatus.FINISHED );

	}

	private void retrieveIdentification(PeakListRow row) {

		currentRow = row;
		
		// TODO

	}

	public Object[] getCreatedObjects() {
		return null;
	}

}
