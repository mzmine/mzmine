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

package net.sf.mzmine.modules.visualization.msms;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.ranges.MZRangeParameter;
import net.sf.mzmine.parameters.parametertypes.ranges.RTRangeParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import net.sf.mzmine.parameters.parametertypes.WindowSettingsParameter;

/**
 * MS/MS visualizer parameter set
 */
public class MsMsParameters extends SimpleParameterSet {

    public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter(
	    1, 1);

    public static final RTRangeParameter retentionTimeRange = new RTRangeParameter();

    public static final MZRangeParameter mzRange = new MZRangeParameter();

    public static final ComboParameter<IntensityType> intensityType = new ComboParameter<IntensityType>(
	    "Intensity",
	    "The intensity of the data points can calculated based on either\n- Total intensity of the MS/MS scan\n- Intensity of the precursor ion in the MS scan",
	    IntensityType.values());

    public static final ComboParameter<NormalizationType> normalizationType = new ComboParameter<NormalizationType>(
	    "Normalize by",
	    "The color of the data points can normalized based on either\n- All data points\n- Data points with a m/z within 10ppm.",
	    NormalizationType.values());

    public static final DoubleParameter minPeakInt = new DoubleParameter(
	    "Min. MS/MS peak intensity",
	    "The minimum intensity of a single MS/MS ion which has to be present in the\nMS/MS spectrum for it to be included in the MS/MS visualizer.\nSet to 0 to show all.");

    public static final PeakThresholdParameter peakThresholdSettings = new PeakThresholdParameter();

    /**
     * Windows size and position
     */
    public static final WindowSettingsParameter windowSettings = new WindowSettingsParameter();

    public MsMsParameters() {
	super(new Parameter[] { dataFiles, retentionTimeRange, mzRange,
		intensityType, normalizationType, minPeakInt,
		peakThresholdSettings, windowSettings });
    }

}
