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

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.SimpleParameterSet;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.MultiChoiceParameter;
import net.sf.mzmine.parameters.parametertypes.RangeParameter;

public class TICVisualizerParameters extends SimpleParameterSet {

	public static final MultiChoiceParameter<RawDataFile> dataFiles = new MultiChoiceParameter<RawDataFile>(
			"Raw data files",
			"List of raw data files to display in TIC visualizer",
			new RawDataFile[0]);

	public static final ComboParameter<Integer> msLevel = new ComboParameter<Integer>(
			"MS level", "MS level of plotted scans", new Integer[0]);

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

	public static final MultiChoiceParameter<ChromatographicPeak> selectionPeaks = new MultiChoiceParameter<ChromatographicPeak>(
			"Selected peaks", "List of peaks to display in TIC visualizer",
			new ChromatographicPeak[0]);

	public TICVisualizerParameters() {
		super(new UserParameter[] { dataFiles, msLevel, plotType,
				retentionTimeRange, mzRange, selectionPeaks });
	}

}
