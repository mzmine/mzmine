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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.savitzkygolay;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.PeakResolverSetupDialog;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.PercentParameter;
import net.sf.mzmine.parameters.parametertypes.RangeParameter;
import net.sf.mzmine.util.ExitCode;
import net.sf.mzmine.util.Range;

public class SavitzkyGolayPeakDetectorParameters extends SimpleParameterSet {

    public static final DoubleParameter MIN_PEAK_HEIGHT = new DoubleParameter(
	    "Min peak height",
	    "Minimum acceptable peak height (absolute intensity)", MZmineCore
		    .getConfiguration().getIntensityFormat());

    public static final RangeParameter PEAK_DURATION = new RangeParameter(
	    "Peak duration range (min)", "Range of acceptable peak lengths",
	    MZmineCore.getConfiguration().getRTFormat(), new Range(0.0, 10.0));

    public static final PercentParameter DERIVATIVE_THRESHOLD_LEVEL = new PercentParameter(
	    "Derivative threshold level",
	    "Minimum acceptable intensity in the 2nd derivative for peak recognition");

    public SavitzkyGolayPeakDetectorParameters() {
	super(new Parameter[] { MIN_PEAK_HEIGHT, PEAK_DURATION,
		DERIVATIVE_THRESHOLD_LEVEL });
    }

    @Override
    public ExitCode showSetupDialog() {
	final PeakResolverSetupDialog dialog = new PeakResolverSetupDialog(
		this, SavitzkyGolayPeakDetector.class);
	dialog.setVisible(true);
	return dialog.getExitCode();
    }

}
