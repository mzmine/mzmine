/*
 * Copyright 2006-2010 The MZmine 2 Development Team
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
package net.sf.mzmine.modules.peaklistmethods.identification.glycerophospholipidsearch;

import java.util.logging.Logger;

import net.sf.mzmine.data.IonizationType;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.data.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.project.ProjectEvent;
import net.sf.mzmine.project.ProjectEvent.ProjectEventType;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.CollectionUtils;

public class GPLipidSearchTask implements Task {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private TaskStatus status = TaskStatus.WAITING;
	private String errorMessage;

	private long finishedSteps, totalSteps;
	private PeakList peakList;

	private GPLipidType[] selectedLipids;
	private int minChainLength, maxChainLength, maxDoubleBonds;
	private double mzTolerance;
	private IonizationType ionizationType;

	private GPLipidSearchParameters parameters;

	/**
	 * @param parameters
	 * @param peakList
	 */
	public GPLipidSearchTask(GPLipidSearchParameters parameters,
			PeakList peakList) {

		this.peakList = peakList;
		this.parameters = parameters;

		minChainLength = (Integer) parameters
				.getParameterValue(GPLipidSearchParameters.minChainLength);
		maxChainLength = (Integer) parameters
				.getParameterValue(GPLipidSearchParameters.maxChainLength);
		maxDoubleBonds = (Integer) parameters
				.getParameterValue(GPLipidSearchParameters.maxDoubleBonds);
		mzTolerance = (Double) parameters
				.getParameterValue(GPLipidSearchParameters.mzTolerance);

		Object selectedLipidObjects[] = (Object[]) parameters
				.getParameterValue(GPLipidSearchParameters.lipidTypes);
		selectedLipids = CollectionUtils.changeArrayType(selectedLipidObjects,
				GPLipidType.class);

		ionizationType = (IonizationType) parameters
				.getParameterValue(GPLipidSearchParameters.ionizationMethod);

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
		if (totalSteps == 0)
			return 0;
		return ((double) finishedSteps) / totalSteps;
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
		return "Identification of glycerophospholipids in " + peakList;
	}

	/**
	 * @see java.lang.Runnable#run()
	 */
	public void run() {

		status = TaskStatus.PROCESSING;

		logger.info("Starting glycerophospholipid search in " + peakList);

		PeakListRow rows[] = peakList.getRows();

		// Calculate how many possible lipids we will try
		totalSteps = selectedLipids.length * (maxChainLength + 1)
				* (maxDoubleBonds + 1) * (maxChainLength + 1)
				* (maxDoubleBonds + 1);

		// Try all combinations of fatty acid lengths and double bonds
		for (GPLipidType lipidType : selectedLipids) {
			for (int fattyAcid1Length = 0; fattyAcid1Length <= maxChainLength; fattyAcid1Length++) {
				for (int fattyAcid1DoubleBonds = 0; fattyAcid1DoubleBonds <= maxDoubleBonds; fattyAcid1DoubleBonds++) {
					for (int fattyAcid2Length = 0; fattyAcid2Length <= maxChainLength; fattyAcid2Length++) {
						for (int fattyAcid2DoubleBonds = 0; fattyAcid2DoubleBonds <= maxDoubleBonds; fattyAcid2DoubleBonds++) {

							// Task canceled?
							if (status == TaskStatus.CANCELED)
								return;

							// If we have non-zero fatty acid, which is shorter
							// than minimal length, skip this lipid
							if (((fattyAcid1Length > 0) && (fattyAcid1Length < minChainLength))
									|| ((fattyAcid2Length > 0) && (fattyAcid2Length < minChainLength))) {
								finishedSteps++;
								continue;
							}

							// If we have more double bonds than carbons, it
							// doesn't make sense, so let's skip such lipids
							if (((fattyAcid1DoubleBonds > 0) && (fattyAcid1DoubleBonds > fattyAcid1Length - 1))
									|| ((fattyAcid2DoubleBonds > 0) && (fattyAcid2DoubleBonds > fattyAcid2Length - 1))) {
								finishedSteps++;
								continue;
							}

							// Prepare a lipid instance
							GPLipidIdentity lipid = new GPLipidIdentity(
									lipidType, fattyAcid1Length,
									fattyAcid1DoubleBonds, fattyAcid2Length,
									fattyAcid2DoubleBonds);

							// Find all rows that match this lipid
							findPossibleGPL(lipid, rows);

							finishedSteps++;

						}
					}
				}
			}
		}

		// Add task description to peakList
		((SimplePeakList) peakList)
				.addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod(
						"Identification of glycerophospholipids", parameters));

		// Notify the project manager that peaklist contents have changed
		ProjectEvent newEvent = new ProjectEvent(
				ProjectEventType.PEAKLIST_CONTENTS_CHANGED, peakList);
		MZmineCore.getProjectManager().fireProjectListeners(newEvent);

		status = TaskStatus.FINISHED;

		logger.info("Finished glycerophospholipid search in " + peakList);

	}

	/**
	 * Check if candidate peak may be a possible adduct of a given main peak
	 * 
	 * @param mainPeak
	 * @param possibleFragment
	 */
	private void findPossibleGPL(GPLipidIdentity lipid, PeakListRow rows[]) {

		final double lipidIonMass = lipid.getMass()
				+ ionizationType.getAddedMass();
		
		logger.finest("Searching for lipid " + lipid.getDescription() + ", " + lipidIonMass + " m/z");


		for (int rowIndex = 0; rowIndex < rows.length; rowIndex++) {

			if (status == TaskStatus.CANCELED)
				return;

			if (Math.abs(lipidIonMass - rows[rowIndex].getAverageMZ()) <= mzTolerance) {
				rows[rowIndex].addPeakIdentity(lipid, false);
			}

		}

	}

	public Object[] getCreatedObjects() {
		return null;
	}

}
