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

package net.sf.mzmine.modules.visualization.scatterplot;

import java.util.Vector;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;

/**
 * This class represents axis selected in the scatter plot visualizer. This can
 * be either a RawDataFile, or a project parameter value representing several
 * RawDataFiles. In the second case, the average peak area is calculated.
 * 
 */
public class ScatterPlotAxisSelection {

    private RawDataFile file;
    private UserParameter<?, ?> parameter;
    private Object parameterValue;

    public ScatterPlotAxisSelection(RawDataFile file) {
	this.file = file;
    }

    public ScatterPlotAxisSelection(UserParameter<?, ?> parameter,
	    Object parameterValue) {
	this.parameter = parameter;
	this.parameterValue = parameterValue;
    }

    public String toString() {
	if (file != null)
	    return file.getName();
	return parameter.getName() + ": " + parameterValue;
    }

    public double getValue(PeakListRow row) {
	if (file != null) {
	    Feature peak = row.getPeak(file);
	    if (peak == null)
		return 0;
	    else
		return peak.getArea();
	}

	double totalArea = 0;
	int numOfFiles = 0;
	for (RawDataFile dataFile : row.getRawDataFiles()) {
	    Object fileValue = MZmineCore.getProjectManager()
		    .getCurrentProject().getParameterValue(parameter, dataFile);
	    if (fileValue == null) continue;
	    if (fileValue.toString().equals(parameterValue.toString())) {
		Feature peak = row.getPeak(dataFile);
		if ((peak != null) && (peak.getArea() > 0)) {
		    totalArea += peak.getArea();
		    numOfFiles++;
		}
	    }
	}
	if (numOfFiles == 0)
	    return 0;
	totalArea /= numOfFiles;
	return totalArea;

    }

    static ScatterPlotAxisSelection[] generateOptionsForPeakList(
	    PeakList peakList) {

	Vector<ScatterPlotAxisSelection> options = new Vector<ScatterPlotAxisSelection>();

	for (RawDataFile dataFile : peakList.getRawDataFiles()) {
	    ScatterPlotAxisSelection newOption = new ScatterPlotAxisSelection(
		    dataFile);
	    options.add(newOption);
	}

	for (UserParameter<?, ?> parameter : MZmineCore.getProjectManager()
		.getCurrentProject().getParameters()) {
	    if (!(parameter instanceof ComboParameter))
		continue;

	    Object possibleValues[] = ((ComboParameter<?>) parameter)
		    .getChoices();
	    for (Object value : possibleValues) {
		ScatterPlotAxisSelection newOption = new ScatterPlotAxisSelection(
			parameter, value);
		options.add(newOption);
	    }
	}

	return options.toArray(new ScatterPlotAxisSelection[0]);

    }

}