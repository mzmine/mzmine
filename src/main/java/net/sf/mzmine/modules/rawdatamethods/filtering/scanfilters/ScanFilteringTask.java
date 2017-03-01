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

package net.sf.mzmine.modules.rawdatamethods.filtering.scanfilters;

import java.io.IOException;
import java.util.logging.Logger;

import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.RawDataFileWriter;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineProcessingStep;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

class ScanFilteringTask extends AbstractTask {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private final MZmineProject project;
    private RawDataFile dataFile, filteredRawDataFile;

    // scan counter
    private int processedScans = 0, totalScans;
    private int[] scanNumbers;

    // User parameters
    private String suffix;
    private boolean removeOriginal;

    // Raw Data Filter
    private MZmineProcessingStep<ScanFilter> rawDataFilter;

    /**
     * @param dataFile
     * @param parameters
     */
    ScanFilteringTask(MZmineProject project, RawDataFile dataFile, ParameterSet parameters) {

	this.project = project;
	this.dataFile = dataFile;

	rawDataFilter = parameters.getParameter(ScanFiltersParameters.filter)
		.getValue();

	suffix = parameters.getParameter(ScanFiltersParameters.suffix)
		.getValue();

    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
	return "Filtering scans in " + dataFile;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public double getFinishedPercentage() {
	if (totalScans == 0) {
	    return 0;
	} else {
	    return (double) processedScans / totalScans;
	}
    }

    public RawDataFile getDataFile() {
	return dataFile;
    }

    /**
     * @see Runnable#run()
     */
    public void run() {

	setStatus(TaskStatus.PROCESSING);

	logger.info("Started filtering scans on " + dataFile);

	scanNumbers = dataFile.getScanNumbers(1);
	totalScans = scanNumbers.length;

	try {

	    // Create new raw data file

	    String newName = dataFile.getName() + " " + suffix;
	    RawDataFileWriter rawDataFileWriter = MZmineCore
		    .createNewFile(newName);

	    for (int i = 0; i < totalScans; i++) {

		if (isCanceled()) {
		    return;
		}

		Scan scan = dataFile.getScan(scanNumbers[i]);
		Scan newScan = rawDataFilter.getModule().filterScan(scan,
			rawDataFilter.getParameterSet());
		if (newScan != null) {
		    rawDataFileWriter.addScan(newScan);
		}

		processedScans++;
	    }

	    // Finalize writing
	    try {
		filteredRawDataFile = rawDataFileWriter.finishWriting();
		project.addFile(filteredRawDataFile);

		// Remove the original file if requested
		if (removeOriginal) {
		    project.removeFile(dataFile);
		}
	    } catch (Exception exception) {
		exception.printStackTrace();
	    }

	    setStatus(TaskStatus.FINISHED);
	    logger.info("Finished scan filter on " + dataFile);

	} catch (IOException e) {
	    setStatus(TaskStatus.ERROR);
	    setErrorMessage(e.toString());
	    return;
	}

    }

}
