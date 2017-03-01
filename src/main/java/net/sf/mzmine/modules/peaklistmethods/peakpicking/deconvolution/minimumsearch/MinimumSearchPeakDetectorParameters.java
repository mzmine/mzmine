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

package net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.minimumsearch;

import java.awt.Window;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.PeakResolverSetupDialog;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.PercentParameter;
import net.sf.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;
import net.sf.mzmine.util.ExitCode;

import com.google.common.collect.Range;

public class MinimumSearchPeakDetectorParameters extends SimpleParameterSet {

    public static final PercentParameter CHROMATOGRAPHIC_THRESHOLD_LEVEL = new PercentParameter(
	    "Chromatographic threshold",
	    "Threshold for removing noise. The algorithm finds such intensity that given percentage of the"
	    	+"\nchromatogram data points is below that intensity, and removes all data points below that level.");

    public static final DoubleParameter SEARCH_RT_RANGE = new DoubleParameter(
	    "Search minimum in RT range (min)",
	    "If a local minimum is minimal in this range of retention time, it will be considered a border between two peaks",
	    MZmineCore.getConfiguration().getRTFormat());

    public static final PercentParameter MIN_RELATIVE_HEIGHT = new PercentParameter(
	    "Minimum relative height",
	    "Minimum height of a peak relative to the chromatogram top data point");

    public static final DoubleParameter MIN_ABSOLUTE_HEIGHT = new DoubleParameter(
	    "Minimum absolute height",
	    "Minimum absolute height of a peak to be recognized", MZmineCore
		    .getConfiguration().getIntensityFormat());

    public static final DoubleParameter MIN_RATIO = new DoubleParameter(
	    "Min ratio of peak top/edge",
	    "Minimum ratio between peak's top intensity and side (lowest) data points."
	    	+"\nThis parameter helps to reduce detection of false peaks in case the chromatogram is not smooth.");

    public static final DoubleRangeParameter PEAK_DURATION = new DoubleRangeParameter(
	    "Peak duration range (min)", "Range of acceptable peak lengths",
	    MZmineCore.getConfiguration().getRTFormat(),
	    Range.closed(0.0, 10.0));

    public MinimumSearchPeakDetectorParameters() {
	super(new Parameter[] { CHROMATOGRAPHIC_THRESHOLD_LEVEL,
		SEARCH_RT_RANGE, MIN_RELATIVE_HEIGHT, MIN_ABSOLUTE_HEIGHT,
		MIN_RATIO, PEAK_DURATION });
    }

    @Override
    public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {
	final PeakResolverSetupDialog dialog = new PeakResolverSetupDialog(
		parent, valueCheckRequired, this,
		MinimumSearchPeakDetector.class);
	dialog.setVisible(true);
	return dialog.getExitCode();
    }

}
