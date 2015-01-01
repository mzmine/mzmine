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

import org.jfree.chart.labels.XYZToolTipGenerator;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;

public class ProjectionPlotToolTipGenerator implements XYZToolTipGenerator {

    private ColoringType coloringType;

    private enum LabelMode {
	FileName, FileNameAndParameterValue
    }

    private LabelMode labelMode;

    ProjectionPlotToolTipGenerator(ParameterSet parameters) {
	try {
	    coloringType = parameters.getParameter(
		    ProjectionPlotParameters.coloringType).getValue();
	} catch (IllegalArgumentException exeption) {
	    coloringType = ColoringType.NOCOLORING;
	}
	if (coloringType.equals(ColoringType.NOCOLORING))
	    labelMode = LabelMode.FileName;

	if (coloringType.equals(ColoringType.COLORBYFILE))
	    labelMode = LabelMode.FileName;

	if (coloringType.isByParameter())
	    labelMode = LabelMode.FileNameAndParameterValue;

    }

    private String generateToolTip(ProjectionPlotDataset dataset, int item) {

	switch (labelMode) {

	case FileName:
	default:
	    return dataset.getRawDataFile(item);

	case FileNameAndParameterValue:
	    String ret = dataset.getRawDataFile(item) + "\n";

	    ret += coloringType.getParameter().getName() + ": ";

	    int groupNumber = dataset.getGroupNumber(item);
	    Object paramValue = dataset.getGroupParameterValue(groupNumber);
	    if (paramValue != null)
		ret += paramValue.toString();
	    else
		ret += "N/A";

	    return ret;
	}

    }

    public String generateToolTip(XYDataset dataset, int series, int item) {
	if (dataset instanceof ProjectionPlotDataset)
	    return generateToolTip((ProjectionPlotDataset) dataset, item);
	else
	    return null;
    }

    public String generateToolTip(XYZDataset dataset, int series, int item) {
	if (dataset instanceof ProjectionPlotDataset)
	    return generateToolTip((ProjectionPlotDataset) dataset, item);
	else
	    return null;
    }
}
