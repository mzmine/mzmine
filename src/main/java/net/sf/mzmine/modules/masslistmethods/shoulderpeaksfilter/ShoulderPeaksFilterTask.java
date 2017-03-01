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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.masslistmethods.shoulderpeaksfilter;

import java.util.logging.Logger;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.MassList;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimpleMassList;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

/**
 *
 */
public class ShoulderPeaksFilterTask extends AbstractTask {

    private Logger logger = Logger.getLogger(this.getClass().getName());
    private RawDataFile dataFile;

    // scan counter
    private int processedScans = 0, totalScans;
    private int[] scanNumbers;

    // User parameters
    private String massListName, suffix;
    private boolean autoRemove;
    private ParameterSet parameters;

    /**
     * @param dataFile
     * @param parameters
     */
    public ShoulderPeaksFilterTask(RawDataFile dataFile, ParameterSet parameters) {

	this.dataFile = dataFile;
	this.parameters = parameters;

	this.massListName = parameters.getParameter(
		ShoulderPeaksFilterParameters.massList).getValue();

	this.suffix = parameters.getParameter(
		ShoulderPeaksFilterParameters.suffix).getValue();
	this.autoRemove = parameters.getParameter(
		ShoulderPeaksFilterParameters.autoRemove).getValue();

    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
	return "Filtering shoulder peaks in " + dataFile;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public double getFinishedPercentage() {
	if (totalScans == 0)
	    return 0;
	else
	    return (double) processedScans / totalScans;
    }

    public RawDataFile getDataFile() {
	return dataFile;
    }

    /**
     * @see Runnable#run()
     */
    public void run() {

	setStatus(TaskStatus.PROCESSING);

	logger.info("Started mass filter on " + dataFile);

	scanNumbers = dataFile.getScanNumbers();
	totalScans = scanNumbers.length;

	// Check if we have at least one scan with a mass list of given name
	boolean haveMassList = false;
	for (int i = 0; i < totalScans; i++) {
	    Scan scan = dataFile.getScan(scanNumbers[i]);
	    MassList massList = scan.getMassList(massListName);
	    if (massList != null) {
		haveMassList = true;
		break;
	    }
	}
	if (!haveMassList) {
	    setStatus(TaskStatus.ERROR);
	    setErrorMessage(dataFile.getName() + " has no mass list called '"
		    + massListName + "'");
	    return;
	}

	// Process all scans
	for (int i = 0; i < totalScans; i++) {

	    if (isCanceled())
		return;

	    Scan scan = dataFile.getScan(scanNumbers[i]);

	    MassList massList = scan.getMassList(massListName);

	    // Skip those scans which do not have a mass list of given name
	    if (massList == null) {
		processedScans++;
		continue;
	    }

	    DataPoint mzPeaks[] = massList.getDataPoints();

	    DataPoint newMzPeaks[] = ShoulderPeaksFilter.filterMassValues(
		    mzPeaks, parameters);

	    SimpleMassList newMassList = new SimpleMassList(massListName + " "
		    + suffix, scan, newMzPeaks);

	    scan.addMassList(newMassList);

	    // Remove old mass list
	    if (autoRemove)
		scan.removeMassList(massList);

	    processedScans++;
	}

	setStatus(TaskStatus.FINISHED);

	logger.info("Finished shoulder peaks filter on " + dataFile);

    }

}
