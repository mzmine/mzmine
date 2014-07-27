/*
 * Copyright 2006-2014 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.rawdatamethods.peakpicking.gridmass;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.RawDataFilesParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;

public class GridMassParameters extends SimpleParameterSet {

    public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();

    public static final DoubleParameter minimumTimeSpan = new DoubleParameter(
	    "Minimum width (min)",
	    "Minimum time to be recognized as a 'mass'. "
		    + "The optimal value depends on the chromatography system setup.\nSee 2D raw data to determine typical time spans.",
	    MZmineCore.getConfiguration().getRTFormat(), 0.1);

    public static final DoubleParameter maximumTimeSpan = new DoubleParameter(
	    "Maximum width (min)",
	    "Maximum time to be recognized as a true positive. "
		    + "The optimal value depends on the chromatography system setup.\nSee 2D raw data to determine typical time spans.",
	    MZmineCore.getConfiguration().getRTFormat(), 3.0);

    public static final DoubleParameter minimumHeight = new DoubleParameter(
	    "Minimum height",
	    "Minimum GLOBAL intensity of the highest data point in the mass. A value closer to 95% of the baseline-corrected distribution is recommended.",
	    MZmineCore.getConfiguration().getMZFormat(), 20.0);

    public static final DoubleParameter mzTolerance = // new
						      // MZToleranceParameter();
    new DoubleParameter(
	    "M/Z Tolerance",
	    "Maximum difference in m/z to recognize features/peaks as the same.",
	    MZmineCore.getConfiguration().getMZFormat(), 0.10);

    public static final StringParameter suffix = new StringParameter("Suffix",
	    "This string is added to filename as suffix", "chromatograms");

    public static final DoubleParameter smoothingTimeSpan = new DoubleParameter(
	    "Smoothing time (min)",
	    "Time to perform intensity smoothing in time space. \nA value close to minimum time span is recomended. \nSee 2D plot to use at least 3 scans.",
	    MZmineCore.getConfiguration().getRTFormat(), 0.05);

    public static final DoubleParameter smoothingTimeMZ = new DoubleParameter(
	    "Smoothing m/z",
	    "MZ tolerance to perform intensity smoothing in time space. \nA value smaller than m/z tolerance is recommended. \nSee 2D plot to observe closer values.",
	    MZmineCore.getConfiguration().getMZFormat(), 0.05);

    public static final DoubleParameter intensitySimilarity = new DoubleParameter(
	    "False+: Intensity similarity ratio",
	    "To reduce false positives removing similarly joint masses crowed along time (solvents or artifacts) > max time span.",
	    MZmineCore.getConfiguration().getMZFormat(), 0.50);

    public static final IntegerParameter showDebug = new IntegerParameter(
	    "Debugging Level",
	    "Shows details of the process. Useful to optimize parameters.\nLevel 0 = No debug.\nLevel 1 = Basic Information,\nLevel 2 = Final Peak Information.\nLevel 3 = All Information.",
	    0, 0, 3);

    public static final StringParameter ignoreTimes = new StringParameter(
	    "False+: Ignore times",
	    "To avoid estimation of features at specific times in minutes. Use 0-0 to ignore. Format: time1-time2, time3-time4, ... ",
	    "0-0");

    public GridMassParameters() {
	super(new Parameter[] { dataFiles, suffix, minimumHeight, mzTolerance,
		minimumTimeSpan, maximumTimeSpan, smoothingTimeSpan,
		smoothingTimeMZ, intensitySimilarity, ignoreTimes, showDebug });
    }

}
