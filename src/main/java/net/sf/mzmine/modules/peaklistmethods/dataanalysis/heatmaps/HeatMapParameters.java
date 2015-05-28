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
package net.sf.mzmine.modules.peaklistmethods.dataanalysis.heatmaps;

import java.awt.Window;
import java.util.ArrayList;

import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.FileNameParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.PeakListsParameter;
import net.sf.mzmine.util.ExitCode;

public class HeatMapParameters extends SimpleParameterSet {

    public static final String[] fileTypes = { "pdf", "svg", "png", "fig" };

    public static final PeakListsParameter peakLists = new PeakListsParameter(
            1, 1);

    public static final FileNameParameter fileName = new FileNameParameter(
	    "Output name", "Select the path and name of the output file.");
    public static final ComboParameter<String> fileTypeSelection = new ComboParameter<String>(
	    "Output file type", "Output file type", fileTypes, fileTypes[0]);
    public static final ComboParameter<UserParameter<?, ?>> selectionData = new ComboParameter<UserParameter<?, ?>>(
	    "Sample parameter",
	    "One sample parameter has to be selected to be used in the heat map. They can be defined in \"Project -> Set sample parameters\"",
	    new UserParameter[0]);
    public static final ComboParameter<Object> referenceGroup = new ComboParameter<Object>(
	    "Group of reference",
	    "Name of the group that will be used as a reference from the sample parameters",
	    new Object[0]);
    public static final BooleanParameter useIdenfiedRows = new BooleanParameter(
	    "Only identified rows", "Plot only identified rows.", false);
    public static final BooleanParameter usePeakArea = new BooleanParameter(
	    "Use peak area",
	    "Peak area will be used if this option is selected. Peak height will be used otherwise",
	    true);
    public static final BooleanParameter scale = new BooleanParameter(
	    "Scaling",
	    "Scaling the data with the standard deviation of each column.",
	    true);
    public static final BooleanParameter log = new BooleanParameter("Log",
	    "Log scaling of the data", true);
    public static final BooleanParameter plegend = new BooleanParameter(
	    "P-value legend",
	    "Adds the p-value legend and groups the data showing only the different groups in the heat map",
	    true);
    public static final IntegerParameter star = new IntegerParameter(
	    "Size p-value legend", "Size of the p-value legend", 5);
    public static final BooleanParameter showControlSamples = new BooleanParameter(
	    "Show control samples",
	    "Shows control samples if this option is selected", true);
    public static final IntegerParameter height = new IntegerParameter(
	    "Height",
	    "Height of the heat map. It has to be more than 500 if \"png\" has been choosen as an output format",
	    10);
    public static final IntegerParameter width = new IntegerParameter(
	    "Width",
	    "Width of the heat map. It has to be more than 500 if \"png\" has been choosen as an output format",
	    10);
    public static final IntegerParameter columnMargin = new IntegerParameter(
	    "Column margin", "Column margin of the heat map", 10);
    public static final IntegerParameter rowMargin = new IntegerParameter(
	    "Row margin", "Row margin of the heat map", 10);

    public HeatMapParameters() {
        super(new Parameter[] { peakLists, fileName, fileTypeSelection,
                selectionData, referenceGroup, useIdenfiedRows, usePeakArea,
                scale, log, showControlSamples, plegend, star, height, width,
                columnMargin, rowMargin });
    }

    @Override
    public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {

	// Update the parameter choices
	MZmineProject project = MZmineCore.getProjectManager()
		.getCurrentProject();
	UserParameter<?, ?> newChoices[] = project.getParameters();
	getParameter(HeatMapParameters.selectionData).setChoices(newChoices);
	if (newChoices.length > 0) {
	    ArrayList<Object> values = new ArrayList<Object>();
	    for (RawDataFile dataFile : project.getDataFiles()) {
		Object paramValue = project.getParameterValue(newChoices[0],
			dataFile);
		if (paramValue == null) {
		    continue;
		}
		if (!values.contains(paramValue)) {
		    values.add(paramValue);
		}
	    }
	    Object newValues[] = values.toArray();
	    getParameter(HeatMapParameters.referenceGroup)
		    .setChoices(newValues);
	}
	HeatmapSetupDialog dialog = new HeatmapSetupDialog(parent,
		valueCheckRequired, this);
	dialog.setVisible(true);
	return dialog.getExitCode();
    }
}
