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

package net.sf.mzmine.modules.rawdatamethods.peakpicking.gridmass;

import com.google.common.collect.Range;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelection;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;

public class GridMassParameters extends SimpleParameterSet {

    public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();

    public static final ScanSelectionParameter scanSelection = new ScanSelectionParameter(
            new ScanSelection(1));

    public static final DoubleRangeParameter timeSpan = new DoubleRangeParameter(
            "Min-max width time (min)",
            "Time range for a peak to be recognized as a 'mass'.\n"
                    + "The optimal value depends on the chromatography system setup.\nSee 2D raw data to determine typical time spans.",
            MZmineCore.getConfiguration().getRTFormat(),
            Range.closed(0.1, 3.0));

    public static final DoubleParameter minimumHeight = new DoubleParameter(
            "Minimum height",
            "Minimum GLOBAL intensity of the highest data point in the mass. A value closer to 95% of the baseline-corrected distribution is recommended.",
            MZmineCore.getConfiguration().getMZFormat(), 20.0);

    public static final DoubleParameter mzTolerance = new DoubleParameter(
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

    public static final String[] debugLevels = new String[] { "No debug",
            "Basic information", "Final peak information", "All information" };

    public static final ComboParameter<String> showDebug = new ComboParameter<String>(
            "Debugging level",
            "Shows details of the process. Useful to optimize parameters.",
            debugLevels, debugLevels[0]);

    public static final StringParameter ignoreTimes = new StringParameter(
            "False+: Ignore times",
            "To avoid estimation of features at specific times in minutes. Use 0-0 to ignore. Format: time1-time2, time3-time4, ... ",
            "0-0");

    public GridMassParameters() {
        super(new Parameter[] { dataFiles, scanSelection, suffix, minimumHeight,
                mzTolerance, timeSpan, smoothingTimeSpan, smoothingTimeMZ,
                intensitySimilarity, ignoreTimes, showDebug });
    }

}
