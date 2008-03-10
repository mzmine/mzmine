/*
 * Copyright 2006-2008 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.dataanalysis.projectionplots;

import java.util.Vector;

import jmprojection.PCA;
import jmprojection.Preprocess;
import jmprojection.ProjectionStatus;
import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.userinterface.Desktop;

import org.jfree.data.xy.AbstractXYDataset;

public class PCADataset extends AbstractXYDataset implements
		ProjectionPlotDataset {

	private double[] component1Coords;
	private double[] component2Coords;

	private ProjectionPlotParameters parameters;

	private Parameter selectedParameter;

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

	public PCADataset(ProjectionPlotParameters parameters, int xAxisPC,
			int yAxisPC) {

		this.parameters = parameters;
		this.xAxisPC = xAxisPC;
		this.yAxisPC = yAxisPC;

		selectedParameter = parameters.getSelectedParameter();
		selectedRawDataFiles = parameters.getSelectedDataFiles();
		selectedRows = parameters.getSelectedRows();

		datasetTitle = "Principal component analysis";

		// Determine groups for selected raw data files
		groupsForSelectedRawDataFiles = new int[selectedRawDataFiles.length];

		if (parameters.getColoringMode() == ProjectionPlotParameters.ColoringSingleOption) {
			// All files to a single group
			for (int ind = 0; ind < selectedRawDataFiles.length; ind++)
				groupsForSelectedRawDataFiles[ind] = 0;

			numberOfGroups = 1;
		}

		if (parameters.getColoringMode() == ProjectionPlotParameters.ColoringByFileOption) {
			// Each file to own group
			for (int ind = 0; ind < selectedRawDataFiles.length; ind++)
				groupsForSelectedRawDataFiles[ind] = ind;

			numberOfGroups = selectedRawDataFiles.length;
		}

		if (parameters.getColoringMode() == ProjectionPlotParameters.ColoringByParameterValueOption) {
			// Group files with same parameter value to same group
			MZmineProject project = MZmineCore.getCurrentProject();
			Vector<Object> availableParameterValues = new Vector<Object>();
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
	public Comparable getSeriesKey(int series) {
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

	public RawDataFile getRawDataFile(int item) {
		return selectedRawDataFiles[item];
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

		// Generate matrix of raw data (input to PCA)
		boolean useArea = true;
		if (parameters.getPeakMeasuringMode() == ProjectionPlotParameters.PeakAreaOption)
			useArea = true;
		if (parameters.getPeakMeasuringMode() == ProjectionPlotParameters.PeakHeightOption)
			useArea = false;
		
		double[][] rawData = new double[selectedRawDataFiles.length][selectedRows.length];
		for (int rowIndex = 0; rowIndex < selectedRows.length; rowIndex++) {
			PeakListRow peakListRow = selectedRows[rowIndex];
			for (int fileIndex = 0; fileIndex < selectedRawDataFiles.length; fileIndex++) {
				RawDataFile rawDataFile = selectedRawDataFiles[fileIndex];
				Peak p = peakListRow.getPeak(rawDataFile);
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

		component1Coords = result[xAxisPC - 1];
		component2Coords = result[yAxisPC - 1];

		status = TaskStatus.FINISHED;

		Desktop desktop = MZmineCore.getDesktop();
		ProjectionPlotWindow newFrame = new ProjectionPlotWindow(desktop, this,
				parameters);
		desktop.addInternalFrame(newFrame);

	}

	public void cancel() {
		status = TaskStatus.CANCELED;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public TaskStatus getStatus() {
		return status;
	}

	public String getTaskDescription() {
		if ( (parameters==null) || (parameters.getSourcePeakList()==null) ) 
			return "PCA projection";
		return "PCA projection " + parameters.getSourcePeakList(); 
	}

	public float getFinishedPercentage() {
		if (projectionStatus==null) return 0.0f;
		return projectionStatus.getFinishedPercentage();
	}

}
