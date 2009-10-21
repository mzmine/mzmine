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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */
package net.sf.mzmine.modules.rawdata.datasetfilters;

import java.lang.reflect.Constructor;
import java.util.logging.Logger;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.rawdata.datasetfilters.preview.RawDataFilter;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskStatus;

/**
 * @see
 */
class RawDataFilteringTask implements Task {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	private RawDataFile[] dataFiles,  filteredRawDataFile;
	private TaskStatus status = TaskStatus.WAITING;
	private String errorMessage;

	// scan counter
	private int processedScans = 0,  totalScans;
	private int[] scanNumbers;

	// User parameters
	private String suffix;
	private boolean removeOriginal;
	private int rawDataFilterTypeNumber;

	// Raw Data Filter
	private RawDataFilter rawDataFilter;
	private ParameterSet parameters;
	private SimplePeakList newPeakList;

	/**
	 * @param dataFiles
	 * @param parameters
	 */
	RawDataFilteringTask(RawDataFile[] dataFiles,
			RawDataFilteringParameters parameters) {

		this.dataFiles = dataFiles;

		rawDataFilterTypeNumber = parameters.getRawDataFilterTypeNumber();
		this.parameters = parameters.getRawDataFilteringParameters(rawDataFilterTypeNumber);

		suffix = parameters.getSuffix();

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
			return rawDataFilter.getProgres();
		} else {
			return 0;
		}
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getStatus()
	 */
	public TaskStatus getStatus() {
		return status;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getErrorMessage()
	 */
	public String getErrorMessage() {
		return errorMessage;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#cancel()
	 */
	public void cancel() {
		status = TaskStatus.CANCELED;
	}

	/**
	 * @see Runnable#run()
	 */
	public void run() {

		status = TaskStatus.PROCESSING;

		//logger.info("Started filtering scans on " + dataFile);


		// Create raw data filter according with the user's selection
		String rawDataFilterClassName = RawDataFilteringParameters.rawDataFilterClasses[rawDataFilterTypeNumber];
		try {
			Class rawDataFilterClass = Class.forName(rawDataFilterClassName);
			Constructor rawDataFilterConstruct = rawDataFilterClass.getConstructors()[0];
			rawDataFilter = (RawDataFilter) rawDataFilterConstruct.newInstance(parameters);
		} catch (Exception e) {
			errorMessage = "Error trying to make an instance of raw data filter " + rawDataFilterClassName;
			status = TaskStatus.ERROR;
			return;
		}
		try {
			for (RawDataFile dataFile : dataFiles) {
				RawDataFile newDataFiles = rawDataFilter.getNewDataFiles(dataFile);
				if (newDataFiles != null) {
					newDataFiles.setName(newDataFiles.getName() + suffix);
					MZmineCore.getCurrentProject().addFile(newDataFiles);

					// Remove the original file if requested
					if (removeOriginal) {
						MZmineCore.getCurrentProject().removeFile(dataFile);

					}
				}
			}

			status = TaskStatus.FINISHED;
		//logger.info("Finished scan filter on " + dataFile);

		} catch (Exception e) {
			status = TaskStatus.ERROR;
			errorMessage = e.toString();
			return;
		}

	}

	public Object[] getCreatedObjects() {
		return new Object[]{newPeakList};
	}
}
