/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.visualization.ims;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.WindowSettingsParameter;
import io.github.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;
import io.github.mzmine.parameters.parametertypes.ranges.MZRangeParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;

import java.text.DecimalFormat;

public class ImsVisualizerParameters extends SimpleParameterSet {
    public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter(1, 1);
    public static final ScanSelectionParameter scanSelection =
        new ScanSelectionParameter(new ScanSelection(1));
    public static final MZRangeParameter mzRange = new MZRangeParameter();
    public static final DoubleRangeParameter zScaleRange =
        new DoubleRangeParameter("Range for z-Axis scale", "", new DecimalFormat("##0.00"));
    public static final ComboParameter<String> paintScale =
        new ComboParameter<>("Heatmap style", "Select the style for the third dimension",
            new String[] {"Rainbow", "Monochrome red", "Monochrome green", "Monochrome yellow",
                "Monochrome cyan"});
    /**
     * Windows size and position
     */
    public static final WindowSettingsParameter windowSettings = new WindowSettingsParameter();

    public ImsVisualizerParameters() {
        super(new Parameter[] {dataFiles, scanSelection, paintScale, mzRange, windowSettings});
    }
}
