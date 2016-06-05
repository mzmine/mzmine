/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

import net.sf.mzmine.datamodel.IonizationType;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.impl.HeadLessDesktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

import com.google.common.collect.Range;

public class GPLipidSearchTask extends AbstractTask {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private long finishedSteps, totalSteps;
    private PeakList peakList;

    private GPLipidType[] selectedLipids;
    private int minChainLength, maxChainLength, maxDoubleBonds;
    private MZTolerance mzTolerance;
    private IonizationType ionizationType;

    private ParameterSet parameters;

    /**
     * @param parameters
     * @param peakList
     */
    public GPLipidSearchTask(ParameterSet parameters, PeakList peakList) {

	this.peakList = peakList;
	this.parameters = parameters;

	minChainLength = parameters.getParameter(
		GPLipidSearchParameters.minChainLength).getValue();
	maxChainLength = parameters.getParameter(
		GPLipidSearchParameters.maxChainLength).getValue();
	maxDoubleBonds = parameters.getParameter(
		GPLipidSearchParameters.maxDoubleBonds).getValue();
	mzTolerance = parameters.getParameter(
		GPLipidSearchParameters.mzTolerance).getValue();
	selectedLipids = parameters.getParameter(
		GPLipidSearchParameters.lipidTypes).getValue();
	ionizationType = parameters.getParameter(
		GPLipidSearchParameters.ionizationMethod).getValue();

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
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
	return "Identification of glycerophospholipids in " + peakList;
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {

	setStatus(TaskStatus.PROCESSING);

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
			    if (isCanceled())
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

        // Repaint the window to reflect the change in the peak list
        Desktop desktop = MZmineCore.getDesktop();
        if (!(desktop instanceof HeadLessDesktop))
            desktop.getMainWindow().repaint();

	setStatus(TaskStatus.FINISHED);

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

	logger.finest("Searching for lipid " + lipid.getDescription() + ", "
		+ lipidIonMass + " m/z");

	for (int rowIndex = 0; rowIndex < rows.length; rowIndex++) {

	    if (isCanceled())
		return;

	    Range<Double> mzTolRange = mzTolerance
		    .getToleranceRange(rows[rowIndex].getAverageMZ());

	    if (mzTolRange.contains(lipidIonMass)) {
		rows[rowIndex].addPeakIdentity(lipid, false);

		// Notify the GUI about the change in the project
		MZmineCore.getProjectManager().getCurrentProject()
			.notifyObjectChanged(rows[rowIndex], false);
	    }

	}

    }

}
