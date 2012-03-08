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

package net.sf.mzmine.modules.rawdatamethods.filtering.datasetfilters;

import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.RawDataFileWriter;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineProcessingStep;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

class DataSetFilteringTask extends AbstractTask {

    private RawDataFile[] dataFiles;

    // User parameters
    private String suffix;
    private boolean removeOriginal;

    // Raw Data Filter
    private MZmineProcessingStep<RawDataSetFilter> rawDataFilter;
    private RawDataFile filteredRawDataFile;

    /**
     * @param dataFiles
     * @param parameters
     */
    @SuppressWarnings("unchecked")
    DataSetFilteringTask(ParameterSet parameters) {

	this.dataFiles = parameters.getParameter(
		DataSetFiltersParameters.dataFiles).getValue();

	rawDataFilter = parameters
		.getParameter(DataSetFiltersParameters.filter).getValue();

	this.removeOriginal = parameters.getParameter(
		DataSetFiltersParameters.autoRemove).getValue();

	suffix = parameters.getParameter(DataSetFiltersParameters.suffix)
		.getValue();

    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
	return "Filtering raw data";
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public double getFinishedPercentage() {
	if (rawDataFilter != null) {
	    return rawDataFilter.getModule().getProgress();
	} else {
	    return 0;
	}
    }

    /**
     * @see Runnable#run()
     */
    public void run() {

	setStatus(TaskStatus.PROCESSING);

	try {
	    for (RawDataFile dataFile : dataFiles) {
		RawDataFileWriter rawDataFileWriter = MZmineCore
			.createNewFile(dataFile.getName() + " " + suffix);
		filteredRawDataFile = rawDataFilter.getModule().filterDatafile(
			dataFile, rawDataFileWriter,
			rawDataFilter.getParameterSet());
		if (filteredRawDataFile != null) {
		    MZmineCore.getCurrentProject().addFile(filteredRawDataFile);

		    // Remove the original file if requested
		    if (removeOriginal) {
			MZmineCore.getCurrentProject().removeFile(dataFile);
		    }
		}
	    }

	    setStatus(TaskStatus.FINISHED);

	} catch (Exception e) {
	    setStatus(TaskStatus.ERROR);
	    errorMessage = e.toString();
	    return;
	}

    }

    public Object[] getCreatedObjects() {
	if (filteredRawDataFile == null)
	    return null;
	return new Object[] { filteredRawDataFile };
    }
}
