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

package net.sf.mzmine.modules.rawdatamethods.filtering.scansmoothing;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;

public class ScanSmoothingParameters extends SimpleParameterSet {

    public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();

    public static final DoubleParameter timeSpan = new DoubleParameter(
	    "Time (min)",
	    "Time span over which intensities will be averaged in the same m/z over scans.\nThe max between this and Scan Span will be used.",
	    MZmineCore.getConfiguration().getRTFormat(), 0.05);

    public static final IntegerParameter scanSpan = new IntegerParameter(
	    "Scan span",
	    "Number of scan in which intensities will be averaged in the same m/z.\nThe max between this and Time Span will be used.",
	    5);

    public static final DoubleParameter mzTolerance = new DoubleParameter(
	    "MZ tolerance",
	    "M/Z range in which intensities will be averaged. The max between this\nand m/z points will be used. If both 0 no mz smoothing will be performed.",
	    MZmineCore.getConfiguration().getRTFormat(), 0.05);

    public static final IntegerParameter mzPoints = new IntegerParameter(
	    "MZ min points",
	    "Number of m/z points used to smooth. The max between this and m/z tol will be used.\nIf both 0 no m/z smoothing will be performed.",
	    3);

    public static final DoubleParameter minimumHeight = new DoubleParameter(
	    "Min height",
	    "Minimum intensity of the highest data point in the chromatogram.\nIf chromatogram height is below this level, it is not used in the average calculation.",
	    MZmineCore.getConfiguration().getIntensityFormat(), 0.0);

    public static final BooleanParameter removeOld = new BooleanParameter(
	    "Remove prev files", "Remove processed files to save memory.",
	    false);

    public static final StringParameter suffix = new StringParameter("Suffix",
	    "This string is added to filename as suffix", "smooth");

    public ScanSmoothingParameters() {
	super(new Parameter[] { dataFiles, suffix, timeSpan, scanSpan,
		mzTolerance, mzPoints, minimumHeight, removeOld });
    }

}
