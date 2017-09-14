/*
 * Copyright (C) 2017 Du-Lab Team <dulab.binf@gmail.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.msdial;

import java.awt.Window;
import java.text.NumberFormat;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.PeakResolverSetupDialog;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.savitzkygolay.SavitzkyGolayPeakDetector;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.PercentParameter;
import net.sf.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;
import net.sf.mzmine.util.ExitCode;

import com.google.common.collect.Range;

public class MsDialPeakDetectorParameters extends SimpleParameterSet {

    public static final IntegerParameter NUM_SMOOTHING_POINTS = new IntegerParameter(
            "Number of smoothing points",
            "", 5);

    public static final DoubleParameter MIN_PEAK_HEIGHT = new DoubleParameter(
	    "Min peak height",
	    "Minimum acceptable peak height (absolute intensity)", MZmineCore
		    .getConfiguration().getIntensityFormat(), 100.0);

    public static final IntegerParameter MIN_POINTS = new IntegerParameter(
            "Min number of scans",
            "Minimum number of data points of a peak",
            5);

//    public static final DoubleParameter AMPLITUDE_NOISE_FOLD = new DoubleParameter(
//            "Amplitude noise fold",
//            "", NumberFormat.getNumberInstance(),4.0);
//
//    public static final DoubleParameter SLOPE_NOISE_FOLD = new DoubleParameter(
//            "Slope noise fold",
//            "", NumberFormat.getNumberInstance(), 2.0);
//
//    public static final DoubleParameter PEAKTOP_NOISE_FOLD = new DoubleParameter(
//            "Peak top noise fold",
//            "", NumberFormat.getNumberInstance(), 2.0);

    public static final DoubleRangeParameter PEAK_DURATION = new DoubleRangeParameter(
	    "Peak duration range (min)", "Range of acceptable peak lengths",
	    MZmineCore.getConfiguration().getRTFormat(),
	    Range.closed(0.0, 10.0));

    public MsDialPeakDetectorParameters() {
	    super(new Parameter[] { NUM_SMOOTHING_POINTS, MIN_PEAK_HEIGHT, MIN_POINTS,
                PEAK_DURATION});
    }

    @Override
    public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {
	final PeakResolverSetupDialog dialog = new PeakResolverSetupDialog(
		parent, valueCheckRequired, this,
		MsDialPeakDetector.class);
	dialog.setVisible(true);
	return dialog.getExitCode();
    }

}
