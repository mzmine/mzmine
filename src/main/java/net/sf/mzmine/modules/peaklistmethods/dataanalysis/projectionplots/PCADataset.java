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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.dataanalysis.projectionplots;

import java.util.LinkedList;
import java.util.Vector;
import java.util.logging.Logger;

import jmprojection.PCA;
import jmprojection.Preprocess;
import jmprojection.ProjectionStatus;
import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.TaskEvent;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.PeakMeasurementType;

import org.jfree.data.xy.AbstractXYDataset;

public class PCADataset extends AbstractXYDataset implements
		ProjectionPlotDataset {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	private LinkedList<TaskListener> taskListeners = new LinkedList<TaskListener>();

	private double[] component1Coords;
	private double[] component2Coords;

	private ParameterSet parameters;
	private PeakList peakList;
	
	private ColoringType coloringType;

	private RawDataFile[] selectedRawDataFiles;
	private PeakListRow[] selectedRows;

	private int[] groupsForSelectedRawDataFiles;
	private Object[] parameterValuesForGroups;
	int numberOfGroups;

	private String datasetTitle;
	private int xAxisPC;
	private int yAxisPC;

	private TaskStatus status = TaskStatus.WAITING;
	private String errorMessage;

	private ProjectionStatus projectionStatus;

	public PCADataset(ParameterSet parameters) {

		this.peakList = parameters.getParameter(ProjectionPlotParameters.peakLists).getValue()[0];
		this.parameters = parameters;

		this.xAxisPC = parameters.getParameter(
				ProjectionPlotParameters.xAxisComponent).getValue();
		this.yAxisPC = parameters.getParameter(
				ProjectionPlotParameters.yAxisComponent).getValue();
               
                coloringType = parameters.getParameter(
				ProjectionPlotParameters.coloringType).getValue();
              
                
		selectedRawDataFiles = parameters.getParameter(
				ProjectionPlotParameters.dataFiles).getValue();
		selectedRows = parameters.getParameter(ProjectionPlotParameters.rows)
				.getValue();

		datasetTitle = "Principal component analysis";

		// Determine groups for selected raw data files
		groupsForSelectedRawDataFiles = new int[selectedRawDataFiles.length];

		if (coloringType.equals(ColoringType.NOCOLORING)) {
			// All files to a single group
			for (int ind = 0; ind < selectedRawDataFiles.length; ind++)
				groupsForSelectedRawDataFiles[ind] = 0;

			numberOfGroups = 1;
		}

		if (coloringType.equals(ColoringType.COLORBYFILE)) {
			// Each file to own group
			for (int ind = 0; ind < selectedRawDataFiles.length; ind++)
				groupsForSelectedRawDataFiles[ind] = ind;

			numberOfGroups = selectedRawDataFiles.length;
		}

		if (coloringType.isByParameter()) {
			// Group files with same parameter value to same group
			MZmineProject project = MZmineCore.getCurrentProject();
			Vector<Object> availableParameterValues = new Vector<Object>();
			UserParameter selectedParameter = coloringType.getParameter();
			for (RawDataFile rawDataFile : selectedRawDataFiles) {
				Object paramValue = project.getParameterValue(
						selectedParameter, rawDataFile);
				if (!availableParameterValues.contains(paramValue))
					availableParameterValues.add(paramValue);
			}

			for (int ind = 0; ind < selectedRawDataFiles.length; ind++) {
				Object paramValue = project.getParameterValue(
						selectedParameter, selectedRawDataFiles[ind]);
				groupsForSelectedRawDataFiles[ind] = availableParameterValues
						.indexOf(paramValue);
			}
			parameterValuesForGroups = availableParameterValues.toArray();

			numberOfGroups = parameterValuesForGroups.length;
		}

	}

	public String toString() {
		return datasetTitle;
	}

	public String getXLabel() {
		if (xAxisPC == 1)
			return "1st PC";
		if (xAxisPC == 2)
			return "2nd PC";
		if (xAxisPC == 3)
			return "3rd PC";
		return "" + xAxisPC + "th PC";
	}

	public String getYLabel() {
		if (yAxisPC == 1)
			return "1st PC";
		if (yAxisPC == 2)
			return "2nd PC";
		if (yAxisPC == 3)
			return "3rd PC";
		return "" + yAxisPC + "th PC";
	}

	@Override
	public int getSeriesCount() {
		return 1;
	}

	@Override
	public Comparable<Integer> getSeriesKey(int series) {
		return 1;
	}

	public int getItemCount(int series) {
		return component1Coords.length;
	}

	public Number getX(int series, int item) {
		return component1Coords[item];
	}

	public Number getY(int series, int item) {
		return component2Coords[item];
	}

	public String getRawDataFile(int item) {
		return selectedRawDataFiles[item].getName();
	}

	public int getGroupNumber(int item) {
		return groupsForSelectedRawDataFiles[item];
	}

	public Object getGroupParameterValue(int groupNumber) {
		if (parameterValuesForGroups == null)
			return null;
		if ((parameterValuesForGroups.length - 1) < groupNumber)
			return null;
		return parameterValuesForGroups[groupNumber];
	}

	public int getNumberOfGroups() {
		return numberOfGroups;
	}

	public void run() {

		status = TaskStatus.PROCESSING;

		logger.info("Computing projection plot");

		// Generate matrix of raw data (input to PCA)
		boolean useArea = false;
		if (parameters.getParameter(
				ProjectionPlotParameters.peakMeasurementType).getValue() == PeakMeasurementType.AREA)
			useArea = true;

		double[][] rawData = new double[selectedRawDataFiles.length][selectedRows.length];
		for (int rowIndex = 0; rowIndex < selectedRows.length; rowIndex++) {
			PeakListRow peakListRow = selectedRows[rowIndex];
			for (int fileIndex = 0; fileIndex < selectedRawDataFiles.length; fileIndex++) {
				RawDataFile rawDataFile = selectedRawDataFiles[fileIndex];
				ChromatographicPeak p = peakListRow.getPeak(rawDataFile);
				if (p != null) {
					if (useArea)
						rawData[fileIndex][rowIndex] = p.getArea();
					else
						rawData[fileIndex][rowIndex] = p.getHeight();
				}
			}
		}

		int numComponents = xAxisPC;
		if (yAxisPC > numComponents)
			numComponents = yAxisPC;

		// Scale data and do PCA
		Preprocess.scaleToUnityVariance(rawData);
		PCA pcaProj = new PCA(rawData, numComponents);
		projectionStatus = pcaProj.getProjectionStatus();

		double[][] result = pcaProj.getState();

		if (status == TaskStatus.CANCELED)
			return;

		component1Coords = result[xAxisPC - 1];
		component2Coords = result[yAxisPC - 1];

		ProjectionPlotWindow newFrame = new ProjectionPlotWindow(peakList, this,
				parameters);
		MZmineCore.getDesktop().addInternalFrame(newFrame);

		status = TaskStatus.FINISHED;
		logger.info("Finished computing projection plot.");

	}

	public void cancel() {
		if (projectionStatus != null)
			projectionStatus.cancel();
		status = TaskStatus.CANCELED;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public TaskStatus getStatus() {
		return status;
	}

	public String getTaskDescription() {
		return "PCA projection";
	}

	public double getFinishedPercentage() {
		if (projectionStatus == null)
			return 0;
		return projectionStatus.getFinishedPercentage();
	}

	public Object[] getCreatedObjects() {
		return null;
	}

	/**
	 * Adds a TaskListener to this Task
	 * 
	 * @param t
	 *            The TaskListener to add
	 */
	public void addTaskListener(TaskListener t) {
		this.taskListeners.add(t);
	}

	/**
	 * Returns all of the TaskListeners which are listening to this task.
	 * 
	 * @return An array containing the TaskListeners
	 */
	public TaskListener[] getTaskListeners() {
		return this.taskListeners.toArray(new TaskListener[this.taskListeners
				.size()]);
	}

	private void fireTaskEvent() {
		TaskEvent event = new TaskEvent(this);
		for (TaskListener t : this.taskListeners) {
			t.statusChanged(event);
		}
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#setStatus()
	 */
	public void setStatus(TaskStatus newStatus) {
		this.status = newStatus;
		this.fireTaskEvent();
	}

	public boolean isCanceled() {
		return status == TaskStatus.CANCELED;
	}

	public boolean isFinished() {
		return status == TaskStatus.FINISHED;
	}
}
