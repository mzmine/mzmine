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

package net.sf.mzmine.modules.visualization.threed;

import java.text.NumberFormat;
import java.util.Hashtable;

import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.SimpleParameterSet;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.parameters.parametertypes.MSLevelParameter;
import net.sf.mzmine.parameters.parametertypes.NumberParameter;
import net.sf.mzmine.parameters.parametertypes.RangeParameter;
import net.sf.mzmine.parameters.parametertypes.RawDataFilesParameter;
import net.sf.mzmine.util.Range;
import net.sf.mzmine.util.RawDataFileUtils;
import net.sf.mzmine.util.dialogs.ExitCode;

/**
 * 3D visualizer parameter set
 */
public class ThreeDVisualizerParameters extends SimpleParameterSet {

	public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();

	public static final MSLevelParameter msLevel = new MSLevelParameter();

	public static final RangeParameter retentionTimeRange = new RangeParameter(
			"Retention time", "Retention time (X axis) range",
			MZmineCore.getRTFormat());

	public static final NumberParameter rtResolution = new NumberParameter(
			"Retention time resolution",
			"Number of data points on retention time axis",
			NumberFormat.getIntegerInstance());

	public static final RangeParameter mzRange = new RangeParameter(
			"m/z range", "m/z (Y axis) range", MZmineCore.getMZFormat());

	public static final NumberParameter mzResolution = new NumberParameter(
			"m/z resolution", "Number of data points on m/z axis",
			NumberFormat.getIntegerInstance());

	public ThreeDVisualizerParameters() {
		super(new Parameter[] { dataFiles, msLevel, retentionTimeRange,
				rtResolution, mzRange, mzResolution });
	}

	public ExitCode showSetupDialog() {
		Hashtable<UserParameter, Object> autoValues = null;
		RawDataFile selectedFiles[] = getParameter(
				ThreeDVisualizerParameters.dataFiles).getValue();
		if ((selectedFiles != null) && (selectedFiles.length > 0)) {
			autoValues = new Hashtable<UserParameter, Object>();
			autoValues.put(ThreeDVisualizerParameters.msLevel, 1);
			Range rtRange = RawDataFileUtils.findTotalRTRange(selectedFiles, 1);
			Range mzRange = RawDataFileUtils.findTotalMZRange(selectedFiles, 1);
			autoValues.put(ThreeDVisualizerParameters.retentionTimeRange,
					rtRange);
			autoValues.put(ThreeDVisualizerParameters.mzRange, mzRange);
		}
		return super.showSetupDialog(autoValues);
	}
}
