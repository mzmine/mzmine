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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.dataanalysis.projectionplots;

import net.sf.mzmine.parameters.ParameterSet;

import org.jfree.chart.labels.StandardXYItemLabelGenerator;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;

public class ProjectionPlotItemLabelGenerator extends
	StandardXYItemLabelGenerator {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private enum LabelMode {
	None, FileName, ParameterValue
    }

    private LabelMode[] labelModes;
    private int labelModeIndex = 0;

    ProjectionPlotItemLabelGenerator(ParameterSet parameters) {

	labelModes = new LabelMode[] { LabelMode.None };
	ColoringType coloringType = ColoringType.NOCOLORING;
	try {
	    coloringType = parameters.getParameter(
		    ProjectionPlotParameters.coloringType).getValue();
	} catch (IllegalArgumentException exeption) {
	}
	if (coloringType.equals(ColoringType.NOCOLORING))
	    labelModes = new LabelMode[] { LabelMode.None, LabelMode.FileName };

	if (coloringType.equals(ColoringType.COLORBYFILE))
	    labelModes = new LabelMode[] { LabelMode.None, LabelMode.FileName };

	if (coloringType.isByParameter())
	    labelModes = new LabelMode[] { LabelMode.None, LabelMode.FileName,
		    LabelMode.ParameterValue };

    }

    protected void cycleLabelMode() {
	labelModeIndex++;

	if (labelModeIndex >= labelModes.length)
	    labelModeIndex = 0;

    }

    public String generateLabel(ProjectionPlotDataset dataset, int series,
	    int item) {

	switch (labelModes[labelModeIndex]) {
	case None:
	default:
	    return "";

	case FileName:
	    return dataset.getRawDataFile(item);

	case ParameterValue:
	    int groupNumber = dataset.getGroupNumber(item);
	    Object paramValue = dataset.getGroupParameterValue(groupNumber);
	    if (paramValue != null)
		return paramValue.toString();
	    else
		return "";

	}

    }

    public String generateLabel(XYDataset dataset, int series, int item) {
	if (dataset instanceof ProjectionPlotDataset)
	    return generateLabel((ProjectionPlotDataset) dataset, series, item);
	else
	    return null;
    }

    public String generateLabel(XYZDataset dataset, int series, int item) {
	if (dataset instanceof ProjectionPlotDataset)
	    return generateLabel((ProjectionPlotDataset) dataset, series, item);
	else
	    return null;
    }

}
