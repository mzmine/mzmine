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
package net.sf.mzmine.modules.rawdatamethods.peakpicking.targetedpeakdetection;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.PercentParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;

public class TargetedPeakDetectionParameters extends SimpleParameterSet {

    public static final RawDataFilesParameter rawDataFile = new RawDataFilesParameter();
    public static final StringParameter suffix = new StringParameter(
	    "Name suffix", "Suffix to be added to peak list name",
	    "detectedPeak");
    public static final FileNameParameter peakListFile = new FileNameParameter(
	    "Peak list file",
	    "Name of the file that contains a list of peaks for targeted peak detection.");
    public static final StringParameter fieldSeparator = new StringParameter(
	    "Field separator",
	    "Character(s) used to separate fields in the database file", ",");
    public static final BooleanParameter ignoreFirstLine = new BooleanParameter(
	    "Ignore first line", "Ignore the first line of database file");
    public static final PercentParameter intTolerance = new PercentParameter(
	    "Intensity tolerance",
	    "Maximum allowed deviation from expected /\\ shape of a peak in chromatographic direction");
    public static final DoubleParameter noiseLevel = new DoubleParameter(
	    "Noise level",
	    "Intensities less than this value are interpreted as noise",
	    MZmineCore.getConfiguration().getIntensityFormat());
    public static final MZToleranceParameter MZTolerance = new MZToleranceParameter();
    public static final RTToleranceParameter RTTolerance = new RTToleranceParameter();

    public TargetedPeakDetectionParameters() {
	super(new Parameter[] { rawDataFile, suffix, peakListFile,
		fieldSeparator, ignoreFirstLine, intTolerance, noiseLevel, 
		MZTolerance, RTTolerance });
    }
}
