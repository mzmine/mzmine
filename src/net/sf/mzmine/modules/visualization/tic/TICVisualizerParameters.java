/*
 * Copyright 2006-2011 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.visualization.tic;

import java.util.Hashtable;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.SimpleParameterSet;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.MSLevelParameter;
import net.sf.mzmine.parameters.parametertypes.MultiChoiceParameter;
import net.sf.mzmine.parameters.parametertypes.RangeParameter;
import net.sf.mzmine.util.Range;
import net.sf.mzmine.util.RawDataFileUtils;
import net.sf.mzmine.util.dialogs.ExitCode;

public class TICVisualizerParameters extends SimpleParameterSet {

	public static final MultiChoiceParameter<RawDataFile> dataFiles = new MultiChoiceParameter<RawDataFile>(
			"Data files", "Please choose raw data files to plot",
			new RawDataFile[0]);

	public static final MSLevelParameter msLevel = new MSLevelParameter();

	public static final ComboParameter<PlotType> plotType = new ComboParameter<PlotType>(
			"Plot type",
			"Type of Y value calculation (TIC = sum, base peak = max)",
			PlotType.values());

	public static final RangeParameter retentionTimeRange = new RangeParameter(
			"Retention time", "Retention time (X axis) range",
			MZmineCore.getRTFormat());

	public static final RangeParameter mzRange = new RangeParameter(
			"m/z range",
			"Range of m/z values. If this range does not include the whole scan m/z range, the resulting visualizer is XIC type.",
			MZmineCore.getMZFormat());

	public static final MultiChoiceParameter<ChromatographicPeak> peaks = new MultiChoiceParameter<ChromatographicPeak>(
			"Peaks", "Please choose peaks to visualize",
			new ChromatographicPeak[0], null, 0);

	public TICVisualizerParameters() {
		super(new Parameter[] { dataFiles, msLevel, plotType,
				retentionTimeRange, mzRange, peaks });
	}

	public ExitCode showSetupDialog() {

		return showSetupDialog(MZmineCore.getCurrentProject().getDataFiles(),
				MZmineCore.getDesktop().getSelectedDataFiles(),
				new ChromatographicPeak[0], new ChromatographicPeak[0]);

	}

	public ExitCode showSetupDialog(RawDataFile allFiles[],
			RawDataFile selectedFiles[], ChromatographicPeak allPeaks[],
			ChromatographicPeak selectedPeaks[]) {

		getParameter(TICVisualizerParameters.dataFiles).setChoices(allFiles);
		getParameter(TICVisualizerParameters.dataFiles).setValue(selectedFiles);

		getParameter(TICVisualizerParameters.peaks).setChoices(allPeaks);
		getParameter(TICVisualizerParameters.peaks).setValue(selectedPeaks);

		Hashtable<UserParameter, Object> autoValues = null;

		if ((selectedFiles != null) && (selectedFiles.length > 0)) {
			autoValues = new Hashtable<UserParameter, Object>();
			autoValues.put(TICVisualizerParameters.msLevel, 1);
			Range rtRange = RawDataFileUtils.findTotalRTRange(selectedFiles, 1);
			Range mzRange = RawDataFileUtils.findTotalMZRange(selectedFiles, 1);
			autoValues.put(TICVisualizerParameters.retentionTimeRange, rtRange);
			autoValues.put(TICVisualizerParameters.mzRange, mzRange);
		}

		return super.showSetupDialog(autoValues);
	}

}
