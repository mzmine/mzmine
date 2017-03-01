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

package net.sf.mzmine.modules.rawdatamethods.filtering.alignscans;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;

public class AlignScansParameters extends SimpleParameterSet {

    public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();

    public static final IntegerParameter scanSpan = new IntegerParameter(
	    "Horizontal Scans",
	    "Number of scans to be considered in the correlation (to the left and to the right of the scan being aligned).",
	    5);

    public static final IntegerParameter mzSpan = new IntegerParameter(
	    "Max Vertical Alignment",
	    "Maximum number of shifts to be compared. This depends on equipment, normally this should be 1.",
	    1);

    public static final DoubleParameter minimumHeight = new DoubleParameter(
	    "Minimum height",
	    "Minimum intensity to be considered for the align correlation.\nIf chromatogram height is below this level, it is not used in the correlation calculation.",
	    MZmineCore.getConfiguration().getIntensityFormat(), 1000.0);

    public static final BooleanParameter logTransform = new BooleanParameter(
	    "Correlation in Log",
	    "Transform intensities to Log scale before comparing correlation.",
	    false);

    public static final BooleanParameter removeOld = new BooleanParameter(
	    "Remove prev files", "Remove processed files to save memory.",
	    false);

    public static final StringParameter suffix = new StringParameter("Suffix",
	    "This string is added to filename as suffix", "align");

    public AlignScansParameters() {
	super(new Parameter[] { dataFiles, suffix, scanSpan, mzSpan,
		minimumHeight, logTransform, removeOld });
    }

}
