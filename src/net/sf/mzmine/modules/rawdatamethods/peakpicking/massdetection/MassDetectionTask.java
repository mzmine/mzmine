/*
 * Copyright 2006-2012 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection;

import java.util.logging.Logger;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimpleMassList;
import net.sf.mzmine.modules.MZmineProcessingStep;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

public class MassDetectionTask extends AbstractTask {

    private Logger logger = Logger.getLogger(this.getClass().getName());
    private RawDataFile dataFile;

    // scan counter
    private int processedScans = 0, totalScans = 0;
    private int msLevel;

    // User parameters
    private String name;

    // Mass detector
    private MZmineProcessingStep<MassDetector> massDetector;

    /**
     * @param dataFile
     * @param parameters
     */
    @SuppressWarnings("unchecked")
    public MassDetectionTask(RawDataFile dataFile, ParameterSet parameters) {

	this.dataFile = dataFile;

	this.massDetector = parameters.getParameter(
		MassDetectionParameters.massDetector).getValue();

	this.msLevel = parameters.getParameter(MassDetectionParameters.msLevel)
		.getValue();

	this.name = parameters.getParameter(MassDetectionParameters.name)
		.getValue();

    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
	return "Detecting masses in " + dataFile;
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

	logger.info("Started mass detector on " + dataFile);

	int scanNumbers[] = dataFile.getScanNumbers(msLevel);
	totalScans = scanNumbers.length;

	// Process scans one by one
	for (int i = 0; i < totalScans; i++) {

	    if (isCanceled())
		return;

	    Scan scan = dataFile.getScan(scanNumbers[i]);

	    MassDetector detector = massDetector.getModule();
	    DataPoint mzPeaks[] = detector.getMassValues(scan,
		    massDetector.getParameterSet());

	    SimpleMassList newMassList = new SimpleMassList(name, scan, mzPeaks);

	    // Add new mass list to the scan
	    scan.addMassList(newMassList);

	    processedScans++;
	}

	setStatus(TaskStatus.FINISHED);

	logger.info("Finished mass detector on " + dataFile);

    }

    public Object[] getCreatedObjects() {
	return null;
    }

}
