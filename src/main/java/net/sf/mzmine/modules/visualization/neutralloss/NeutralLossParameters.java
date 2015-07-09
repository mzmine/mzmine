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

package net.sf.mzmine.modules.visualization.neutralloss;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.WindowSettingsParameter;
import net.sf.mzmine.parameters.parametertypes.ranges.MZRangeParameter;
import net.sf.mzmine.parameters.parametertypes.ranges.RTRangeParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;

public class NeutralLossParameters extends SimpleParameterSet {

    public static final String xAxisPrecursor = "Precursor mass";
    public static final String xAxisRT = "Retention time";

    public static final String[] xAxisTypes = { xAxisPrecursor, xAxisRT };

    public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();

    public static final ComboParameter<String> xAxisType = new ComboParameter<String>(
	    "X axis", "X axis type", xAxisTypes);

    public static final RTRangeParameter retentionTimeRange = new RTRangeParameter();

    public static final MZRangeParameter mzRange = new MZRangeParameter(
	    "Precursor m/z", "Range of precursor m/z values");

    public static final IntegerParameter numOfFragments = new IntegerParameter(
	    "Fragments", "Number of most intense fragments");

    /**
     * Windows size and position
     */
    public static final WindowSettingsParameter windowSettings = new WindowSettingsParameter();

    public NeutralLossParameters() {
	super(new Parameter[] { dataFiles, xAxisType, retentionTimeRange,
		mzRange, numOfFragments, windowSettings });
    }

}
