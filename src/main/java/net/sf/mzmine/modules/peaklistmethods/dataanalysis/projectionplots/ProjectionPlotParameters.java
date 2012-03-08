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

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.MultiChoiceParameter;
import net.sf.mzmine.parameters.parametertypes.PeakListsParameter;
import net.sf.mzmine.util.ExitCode;
import net.sf.mzmine.util.PeakMeasurementType;

public class ProjectionPlotParameters extends SimpleParameterSet {

	public static final PeakListsParameter peakLists = new PeakListsParameter();

	public static final MultiChoiceParameter<RawDataFile> dataFiles = new MultiChoiceParameter<RawDataFile>(
			"Data files", "Samples", new RawDataFile[0]);

	public static final ColoringTypeParameter coloringType = new ColoringTypeParameter();

	public static final ComboParameter<PeakMeasurementType> peakMeasurementType = new ComboParameter<PeakMeasurementType>(
			"Peak measurement type", "Measure peaks using",
			PeakMeasurementType.values());

	public static final Integer[] componentPossibleValues = { 1, 2, 3, 4, 5 };

	public static final ComboParameter<Integer> xAxisComponent = new ComboParameter<Integer>(
			"X-axis component", "Component on the X-axis",
			componentPossibleValues);

	public static final ComboParameter<Integer> yAxisComponent = new ComboParameter<Integer>(
			"Y-axis component", "Component on the Y-axis",
			componentPossibleValues, componentPossibleValues[1]);

	public static final MultiChoiceParameter<PeakListRow> rows = new MultiChoiceParameter<PeakListRow>(
			"Peak list rows", "Peak list rows to include in calculation",
			new PeakListRow[0]);

	public ProjectionPlotParameters() {
		super(new Parameter[] { peakLists, dataFiles, rows, coloringType,
				peakMeasurementType, xAxisComponent, yAxisComponent });
	}

	@Override
	public ExitCode showSetupDialog() {

		PeakList selectedPeakList[] = getParameter(peakLists).getValue();

		RawDataFile dataFileChoices[];

		if ((selectedPeakList != null) && (selectedPeakList.length == 1)) {
			dataFileChoices = selectedPeakList[0].getRawDataFiles();
		} else {
			dataFileChoices = new RawDataFile[0];
		}

		PeakListRow rowChoices[];
		if ((selectedPeakList != null) && (selectedPeakList.length == 1)) {
			rowChoices = selectedPeakList[0].getRows();
		} else {
			rowChoices = new PeakListRow[0];
		}

		getParameter(ProjectionPlotParameters.dataFiles).setChoices(
				dataFileChoices);
		getParameter(ProjectionPlotParameters.rows).setChoices(rowChoices);

		return super.showSetupDialog();
	}

}
