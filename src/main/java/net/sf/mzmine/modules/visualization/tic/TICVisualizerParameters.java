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

package net.sf.mzmine.modules.visualization.tic;

import java.util.HashMap;
import java.util.Map;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.MSLevelParameter;
import net.sf.mzmine.parameters.parametertypes.MZRangeParameter;
import net.sf.mzmine.parameters.parametertypes.MultiChoiceParameter;
import net.sf.mzmine.parameters.parametertypes.RTRangeParameter;
import net.sf.mzmine.parameters.parametertypes.RangeParameter;
import net.sf.mzmine.util.ExitCode;
import net.sf.mzmine.util.RawDataFileUtils;

public class TICVisualizerParameters extends SimpleParameterSet {

    /**
     * The data file.
     */
    public static final MultiChoiceParameter<RawDataFile> DATA_FILES = new MultiChoiceParameter<RawDataFile>(
	    "Data files", "Please choose raw data files to plot",
	    new RawDataFile[0]);

    /**
     * MS level.
     */
    public static final MSLevelParameter MS_LEVEL = new MSLevelParameter();

    /**
     * Type of plot.
     */
    public static final ComboParameter<PlotType> PLOT_TYPE = new ComboParameter<PlotType>(
	    "Plot type",
	    "Type of Y value calculation (TIC = sum, base peak = max)",
	    PlotType.values());

    /**
     * RT range.
     */
    public static final RTRangeParameter RT_RANGE = new RTRangeParameter();

    /**
     * m/z range.
     */
    public static final RangeParameter MZ_RANGE = new MZRangeParameter();

    /**
     * Peaks to display.
     */
    public static final MultiChoiceParameter<ChromatographicPeak> PEAKS = new MultiChoiceParameter<ChromatographicPeak>(
	    "Peaks", "Please choose peaks to visualize",
	    new ChromatographicPeak[0], null, 0);

    // Maps peaks to their labels - not a user configurable parameter.
    private Map<ChromatographicPeak, String> peakLabelMap;

    /**
     * Create the parameter set.
     */
    public TICVisualizerParameters() {
	super(new Parameter[] { DATA_FILES, MS_LEVEL, PLOT_TYPE, RT_RANGE,
		MZ_RANGE, PEAKS });
	peakLabelMap = null;
    }

    /**
     * Gets the peak labels map.
     * 
     * @return the map.
     */
    public Map<ChromatographicPeak, String> getPeakLabelMap() {

	return peakLabelMap == null ? null
		: new HashMap<ChromatographicPeak, String>(peakLabelMap);
    }

    /**
     * Sets the peak labels map.
     * 
     * @param map
     *            the new map.
     */
    public void setPeakLabelMap(final Map<ChromatographicPeak, String> map) {

	peakLabelMap = map == null ? null
		: new HashMap<ChromatographicPeak, String>(map);
    }

    /**
     * Show the setup dialog.
     * 
     * @return an ExitCode indicating the user's action.
     */
    @Override
    public ExitCode showSetupDialog() {

	return showSetupDialog(MZmineCore.getCurrentProject().getDataFiles(),
		MZmineCore.getDesktop().getSelectedDataFiles(),
		new ChromatographicPeak[0], new ChromatographicPeak[0]);
    }

    /**
     * Show the setup dialog.
     * 
     * @param allFiles
     *            files to choose from.
     * @param selectedFiles
     *            default file selections.
     * @param allPeaks
     *            peaks to choose from.
     * @param selectedPeaks
     *            default peak selections.
     * @return an ExitCode indicating the user's action.
     */
    public ExitCode showSetupDialog(final RawDataFile[] allFiles,
	    final RawDataFile[] selectedFiles,
	    final ChromatographicPeak[] allPeaks,
	    final ChromatographicPeak[] selectedPeaks) {

	getParameter(DATA_FILES).setChoices(allFiles);
	getParameter(DATA_FILES).setValue(selectedFiles);
	getParameter(PEAKS).setChoices(allPeaks);
	getParameter(PEAKS).setValue(selectedPeaks);

	Map<UserParameter, Object> autoValues = null;
	if (selectedFiles != null && selectedFiles.length > 0) {

	    autoValues = new HashMap<UserParameter, Object>(3);
	    autoValues.put(MS_LEVEL, 1);
	    autoValues.put(RT_RANGE,
		    RawDataFileUtils.findTotalRTRange(selectedFiles, 1));
	    autoValues.put(MZ_RANGE,
		    RawDataFileUtils.findTotalMZRange(selectedFiles, 1));
	}

	return super.showSetupDialog();
    }
}
