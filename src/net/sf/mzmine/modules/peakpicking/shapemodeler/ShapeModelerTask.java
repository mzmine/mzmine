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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peakpicking.shapemodeler;

import java.lang.reflect.Constructor;
import java.util.logging.Logger;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListAppliedMethod;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.data.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.data.impl.SimplePeakListRow;
import net.sf.mzmine.main.mzmineclient.MZmineCore;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskStatus;

class ShapeModelerTask implements Task {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private PeakList originalPeakList;

	private TaskStatus status = TaskStatus.WAITING;
	private String errorMessage;

	// scan counter
	private int processedRows = 0, totalRows;
	private int newPeakID = 1;

	// User parameters
	private String suffix;
	private boolean removeOriginal;

	private String shapeModelerType;
	private double resolution;

	public ShapeModelerTask(PeakList peakList, ShapeModelerParameters parameters) {
		this.originalPeakList = peakList;

		shapeModelerType = (String) parameters
				.getParameterValue(ShapeModelerParameters.shapeModelerType);
		suffix = (String) parameters
				.getParameterValue(ShapeModelerParameters.suffix);
		removeOriginal = (Boolean) parameters
				.getParameterValue(ShapeModelerParameters.autoRemove);
		int value = (Integer) parameters
				.getParameterValue(ShapeModelerParameters.massResolution);
		resolution = value;

	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
	 */
	public String getTaskDescription() {
		return "Shape modeling peaks from " + originalPeakList;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
	 */
	public double getFinishedPercentage() {
		if (totalRows == 0)
			return 0;
		else
			return (double) processedRows / totalRows;
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

	public void run() {

		status = TaskStatus.PROCESSING;

		// Create shape model
		String[] shapeModelTypes = ShapeModelerParameters.shapeModelerNames;
		int index = -1;
		for (int i = 0; i < shapeModelTypes.length; i++) {
			if (shapeModelerType.equals(shapeModelTypes[i]))
				index = i;
		}

		if (index < 0) {
			errorMessage = "Error trying to get class name of shape model ";
			status = TaskStatus.ERROR;
			return;
		}

		String shapeModelClassName = ShapeModelerParameters.shapeModelerClasses[index];
		Constructor shapeModelConstruct;

		try {
			Class shapeModelClass = Class.forName(shapeModelClassName);
			shapeModelConstruct = shapeModelClass.getConstructors()[0];

		} catch (Exception e) {
			errorMessage = "Error trying to get constructor of shape model "
					+ shapeModelClassName;
			status = TaskStatus.ERROR;
			return;
		}

		// Get data file information
		RawDataFile dataFile = originalPeakList.getRawDataFile(0);

		// Create new peak list
		SimplePeakList newPeakList = new SimplePeakList(originalPeakList + " "
				+ suffix, dataFile);

		totalRows = originalPeakList.getNumberOfRows();
		int[] scanNumbers;
		double[] retentionTimes, intensities;
		SimplePeakListRow newRow;

		for (PeakListRow row : originalPeakList.getRows()) {

			if (status == TaskStatus.CANCELED)
				return;

			newRow = new SimplePeakListRow(newPeakID);

			try {
				for (ChromatographicPeak peak : row.getPeaks()) {

					// Load the intensities into array
					dataFile = peak.getDataFile();
					scanNumbers = peak.getScanNumbers();
					retentionTimes = new double[scanNumbers.length];
					for (int i = 0; i < scanNumbers.length; i++)
						retentionTimes[i] = dataFile.getScan(scanNumbers[i])
								.getRetentionTime();

					intensities = new double[scanNumbers.length];
					for (int i = 0; i < scanNumbers.length; i++) {
						DataPoint dp = peak.getDataPoint(scanNumbers[i]);
						if (dp != null)
							intensities[i] = dp.getIntensity();
						else
							intensities[i] = 0;
					}

					ChromatographicPeak shapePeak = (ChromatographicPeak) shapeModelConstruct
							.newInstance(peak, scanNumbers, intensities,
									retentionTimes, resolution);

					newRow.addPeak(shapePeak.getDataFile(), shapePeak);
				}

			} catch (Exception e) {
				String message = "Error trying to make an instance of Peak Builder "
						+ shapeModelClassName;
				MZmineCore.getDesktop().displayErrorMessage(message);
				logger.severe(message);
				return;
			}

			newPeakList.addRow(newRow);
			newPeakID++;
			processedRows++;
		}

		// Add new peaklist to the project
		MZmineProject currentProject = MZmineCore.getCurrentProject();
		currentProject.addPeakList(newPeakList);

		// Remove the original peaklist if requested
		if (removeOriginal)
			currentProject.removePeakList(originalPeakList);

		// Load previous applied methods
		for (PeakListAppliedMethod proc : originalPeakList.getAppliedMethods()) {
			newPeakList.addDescriptionOfAppliedTask(proc);
		}

		// Add task description to peakList
		newPeakList
				.addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod(
						"Peaks shaped by " + shapeModelerType + " function",
						null));

		logger.finest("Finished peak shape modeler " + processedRows
				+ " rows processed");

		status = TaskStatus.FINISHED;

	}

}
