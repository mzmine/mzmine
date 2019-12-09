/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.visualization.twod;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.WindowSettingsParameter;
import io.github.mzmine.parameters.parametertypes.ranges.MZRangeParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;

/**
 * 2D visualizer parameter set
 */
public class TwoDVisualizerParameters extends SimpleParameterSet {

    public static final ComboParameter<PlotType> plotType = new ComboParameter<>(
            "Type of plot", "Resampled data -> fast. Raw data -> slow.",
            PlotType.values());

    public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter(
            1, 1);

    public static final ScanSelectionParameter scanSelection = new ScanSelectionParameter(
            new ScanSelection(1));

    public static final MZRangeParameter mzRange = new MZRangeParameter();

    public static final PeakThresholdParameter peakThresholdSettings = new PeakThresholdParameter();

    /**
     * Windows size and position
     */
    public static final WindowSettingsParameter windowSettings = new WindowSettingsParameter();

    public TwoDVisualizerParameters() {
        super(new Parameter[] { plotType, dataFiles, scanSelection, mzRange,
                peakThresholdSettings, windowSettings });
    }

}
