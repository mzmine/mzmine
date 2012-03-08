/*
 * Copyright 2006-2012 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.visualization.twod;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.MSLevelParameter;
import net.sf.mzmine.parameters.parametertypes.MZRangeParameter;
import net.sf.mzmine.parameters.parametertypes.RTRangeParameter;
import net.sf.mzmine.parameters.parametertypes.RawDataFilesParameter;

/**
 * 2D visualizer parameter set
 */
public class TwoDParameters extends SimpleParameterSet {

    public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();

    public static final MSLevelParameter msLevel = new MSLevelParameter();

    public static final RTRangeParameter retentionTimeRange = new RTRangeParameter();

    public static final MZRangeParameter mzRange = new MZRangeParameter();

    public static final PeakThresholdParameter peakThresholdSettings = new PeakThresholdParameter();

    public TwoDParameters() {
	super(new Parameter[] { dataFiles, msLevel, retentionTimeRange,
		mzRange, peakThresholdSettings });
    }

}
