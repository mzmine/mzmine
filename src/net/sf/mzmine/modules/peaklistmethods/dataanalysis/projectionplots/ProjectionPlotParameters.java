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

package net.sf.mzmine.modules.peaklistmethods.dataanalysis.projectionplots;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;

public class ProjectionPlotParameters extends SimpleParameterSet {

	// Normal (stored) parameters
	public static final String ColoringTypeSingleColor = "No coloring";
	public static final String ColoringTypeByParameterValue = "Color by parameter value";
	public static final String ColoringTypeByFile = "Color by file";
	private static final String[] ColoringTypePossibleValues = {
			ColoringTypeSingleColor, ColoringTypeByParameterValue,
			ColoringTypeByFile };

	public static final Parameter coloringType = new SimpleParameter(
			ParameterType.STRING, "Coloring type", "Measure peaks using",
			ColoringTypeSingleColor, ColoringTypePossibleValues);

	public static final String PeakMeasurementTypeHeight = "Peak height";
	public static final String PeakMeasurementTypeArea = "Peak area";
	public static final String[] PeakMeasurementTypePossibleValues = {
			PeakMeasurementTypeHeight, PeakMeasurementTypeArea };

	public static final Parameter peakMeasurementType = new SimpleParameter(
			ParameterType.STRING, "Peak measurement type",
			"Measure peaks using", PeakMeasurementTypeHeight,
			PeakMeasurementTypePossibleValues);

	public static final Integer[] componentPossibleValues = { 1, 2, 3, 4, 5 };

	public static final Parameter xAxisComponent = new SimpleParameter(
			ParameterType.INTEGER, "X-axis component",
			"Component on the X-axis", componentPossibleValues[0],
			componentPossibleValues);

	public static final Parameter yAxisComponent = new SimpleParameter(
			ParameterType.INTEGER, "Y-axis component",
			"Component on the Y-axis", componentPossibleValues[1],
			componentPossibleValues);

	// Non-stored parameter values

	private PeakList sourcePeakList;

	private Parameter selectedParameter; // Parameter used when coloring by
	// parameter value
	private RawDataFile[] selectedDataFiles;
	private PeakListRow[] selectedRows;

	public ProjectionPlotParameters(PeakList sourcePeakList) {
		this();
		
		this.sourcePeakList = sourcePeakList;
		this.selectedDataFiles = sourcePeakList.getRawDataFiles();
		this.selectedRows = sourcePeakList.getRows();

		selectedParameter = null;
	}

	private ProjectionPlotParameters(PeakList sourcePeakList,
			Object coloringTypeValue, Object peakMeasuringValue,
			Object xAxisComponentNumber, Object yAxisComponentNumber,
			Parameter selectedParameter, RawDataFile[] selectedDataFiles,
			PeakListRow[] selectedRows) {

		this();
		
		setParameterValue(coloringType, coloringTypeValue);
		setParameterValue(peakMeasurementType, peakMeasuringValue);
		setParameterValue(xAxisComponent, xAxisComponentNumber);
		setParameterValue(yAxisComponent, yAxisComponentNumber);

		this.sourcePeakList = sourcePeakList;
		this.selectedParameter = selectedParameter;
		this.selectedDataFiles = selectedDataFiles;
		this.selectedRows = selectedRows;
	}

	/**
	 * Represent method's parameters and their values in human-readable format
	 */
	public String toString() {
		return "Coloring mode: " + getParameterValue(coloringType)
				+ ", peak measurement type: "
				+ getParameterValue(peakMeasurementType)
				+ ", selected parameter: " + selectedParameter;
	}

	public ProjectionPlotParameters clone() {
		return new ProjectionPlotParameters(sourcePeakList, coloringType,
				peakMeasurementType, xAxisComponent, yAxisComponent,
				selectedParameter, selectedDataFiles, selectedRows);
	}

	public RawDataFile[] getSelectedDataFiles() {
		return selectedDataFiles;
	}

	public void setSelectedDataFiles(RawDataFile[] selectedDataFiles) {
		this.selectedDataFiles = selectedDataFiles;
	}

	public Parameter getSelectedParameter() {
		return selectedParameter;
	}

	public void setSelectedParameter(Parameter selectedParameter) {
		this.selectedParameter = selectedParameter;
	}

	public PeakListRow[] getSelectedRows() {
		return selectedRows;
	}

	public void setSelectedRows(PeakListRow[] selectedRows) {
		this.selectedRows = selectedRows;
	}

	public PeakList getSourcePeakList() {
		return sourcePeakList;
	}

	public void setSourcePeakList(PeakList sourcePeakList) {
		this.sourcePeakList = sourcePeakList;
	}

	
	public ProjectionPlotParameters() {
	    super(new Parameter[] { coloringType, peakMeasurementType, xAxisComponent,
	    		yAxisComponent });
	}
}
