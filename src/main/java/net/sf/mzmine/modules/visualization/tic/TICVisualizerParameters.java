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

package net.sf.mzmine.modules.visualization.tic;

import java.awt.Window;
import java.util.HashMap;
import java.util.Map;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.MultiChoiceParameter;
import net.sf.mzmine.parameters.parametertypes.WindowSettingsParameter;
import net.sf.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;
import net.sf.mzmine.parameters.parametertypes.ranges.MZRangeParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelection;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;
import net.sf.mzmine.util.ExitCode;

public class TICVisualizerParameters extends SimpleParameterSet {

    /**
     * The data file.
     */
    public static final RawDataFilesParameter DATA_FILES = new RawDataFilesParameter();

    /**
     * Scans (used to be RT range).
     */
    public static final ScanSelectionParameter scanSelection = new ScanSelectionParameter(
            new ScanSelection(1));

    /**
     * Type of plot.
     */
    public static final ComboParameter<TICPlotType> PLOT_TYPE = new ComboParameter<TICPlotType>(
            "Plot type",
            "Type of Y value calculation (TIC = sum, base peak = max)",
            TICPlotType.values());

    /**
     * m/z range.
     */
    public static final DoubleRangeParameter MZ_RANGE = new MZRangeParameter();

    /**
     * Peaks to display.
     */
    public static final MultiChoiceParameter<Feature> PEAKS = new MultiChoiceParameter<Feature>(
            "Peaks", "Please choose peaks to visualize", new Feature[0], null,
            0);

    // Maps peaks to their labels - not a user configurable parameter.
    private Map<Feature, String> peakLabelMap;

    /**
     * Windows size and position
     */
    public static final WindowSettingsParameter WINDOWSETTINGSPARAMETER = new WindowSettingsParameter();

    /**
     * Create the parameter set.
     */
    public TICVisualizerParameters() {
        super(new Parameter[] { DATA_FILES, scanSelection, PLOT_TYPE, MZ_RANGE,
                PEAKS, WINDOWSETTINGSPARAMETER });
        peakLabelMap = null;
    }

    /**
     * Gets the peak labels map.
     * 
     * @return the map.
     */
    public Map<Feature, String> getPeakLabelMap() {

        return peakLabelMap == null ? null : new HashMap<Feature, String>(
                peakLabelMap);
    }

    /**
     * Sets the peak labels map.
     * 
     * @param map
     *            the new map.
     */
    public void setPeakLabelMap(final Map<Feature, String> map) {

        peakLabelMap = map == null ? null : new HashMap<Feature, String>(map);
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
    public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired,
            final RawDataFile[] allFiles, final RawDataFile[] selectedFiles,
            final Feature[] allPeaks, final Feature[] selectedPeaks) {

        getParameter(DATA_FILES).setValue(
                RawDataFilesSelectionType.SPECIFIC_FILES, selectedFiles);
        getParameter(PEAKS).setChoices(allPeaks);
        getParameter(PEAKS).setValue(selectedPeaks);
        return super.showSetupDialog(parent, valueCheckRequired);
    }
}
