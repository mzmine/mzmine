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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.masslistmethods.chromatogrambuilder;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.MassListParameter;
import net.sf.mzmine.parameters.parametertypes.OptionalParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.ranges.RTRangeParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelection;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

public class ChromatogramBuilderParameters extends SimpleParameterSet {

    public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();

    public static final ScanSelectionParameter scanSelection = new ScanSelectionParameter(
            new ScanSelection(1));

    public static final MassListParameter massList = new MassListParameter();

    public static final DoubleParameter minimumTimeSpan = new DoubleParameter(
            "Min time span (min)",
            "Minimum time span over which the same ion must be observed in order to be recognized as a chromatogram.\n"
                    + "The optimal value depends on the chromatography system setup. The best way to set this parameter\n"
                    + "is by studying the raw data and determining what is the typical time span of chromatographic peaks.",
            MZmineCore.getConfiguration().getRTFormat());

    public static final DoubleParameter minimumHeight = new DoubleParameter(
            "Min height",
            "Minimum intensity of the highest data point in the chromatogram. If chromatogram height is below this level, it is discarded.",
            MZmineCore.getConfiguration().getIntensityFormat());

    public static final MZToleranceParameter mzTolerance = new MZToleranceParameter();
    public static final OptionalParameter<DoubleParameter> mzRangeMSMS = new OptionalParameter<>(new DoubleParameter(
    		"m/z range for MS2 scan pairing (Da)",
    		"M/z range: Will work only if ticked.\n"
    			+ "Maximum allowed difference between the m/z value of MS1 scan and the m/z value of precursor ion of MS2 scan (in Daltons) to be\n"
    			+ "considered belonging to the same feature. If not activated, the m/z tolerance set above will be used.\n"));
    public static final OptionalParameter<DoubleParameter> RetentionTimeMSMS = new OptionalParameter<>(new DoubleParameter(
    		"RT range for MS2 scan pairing (min)",
    		"RT range: Will work only if ticked.\n"+
    		"Maximum allowed difference between the retention time value of MS1 scan and the retention time value of the MS2 scan (in min) to be\n"
    			+ "considered belonging to the same feature. If not activated, the pairing of MS1 scan with the corresponding MS2 scan\n"
    			+ "will be done on the full retention time range of the chromatogram."));

    public static final StringParameter suffix = new StringParameter("Suffix",
            "This string is added to filename as suffix", "chromatograms");

    public ChromatogramBuilderParameters() {
        super(new Parameter[] { dataFiles, scanSelection, massList,
                minimumTimeSpan, minimumHeight, mzTolerance, mzRangeMSMS,RetentionTimeMSMS,suffix });
    }

}
