/*
 * Copyright 2006-2015 The du-lab Development Team
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
 /*
 * author Owen Myers (Oweenm@gmail.com)
 */

package net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.ADAPpeakpicking;

import java.awt.Window;
import java.text.NumberFormat;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.PeakResolverSetupDialog;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;
import net.sf.mzmine.util.ExitCode;

import com.google.common.collect.Range;
import net.sf.mzmine.parameters.parametertypes.ModuleComboParameter;
import net.sf.mzmine.parameters.parametertypes.ranges.RTRangeParameter;

/**
 * Parameters used by CentWaveDetector.
 */
public class ADAPDetectorParameters extends SimpleParameterSet {

//    private static final NumberFormat numberFormat = NumberFormat.getInstance();

    private static final SNEstimatorChoice[] SNESTIMATORS ={ new IntensityWindowsSNEstimator(),
                                                            new WaveletCoefficientsSNEstimator()};

    public static final DoubleRangeParameter PEAK_DURATION = new DoubleRangeParameter(
	    "Peak duration range", "Range of acceptable peak lengths",
	    MZmineCore.getConfiguration().getRTFormat(),
	    Range.closed(0.0, 10.0));
    
    public static final RTRangeParameter RT_FOR_CWT_SCALES_DURATION = new RTRangeParameter(
	        "RT wavelet range",
            "Upper and lower bounds of retention times to be used for setting the wavelet scales.",
            MZmineCore.getConfiguration().getRTFormat(),
            true,
            Range.closed(0.001, 0.1));

//    public static final DoubleRangeParameter PEAK_SCALES = new DoubleRangeParameter(
//	    "Wavelet scales",
//	    "Range wavelet widths (smallest, largest) in minutes", MZmineCore
//		    .getConfiguration().getRTFormat(), Range.closed(0.25, 5.0));
    public static final ModuleComboParameter<SNEstimatorChoice> SN_ESTIMATORS = new ModuleComboParameter<SNEstimatorChoice>(
	    "S/N estimator", "SN description", SNESTIMATORS);

    public static final DoubleParameter SN_THRESHOLD = new DoubleParameter(
	    "S/N threshold", "Signal to noise ratio threshold",
	    NumberFormat.getNumberInstance(), 10.0, 0.0, null);
    
    public static final DoubleParameter COEF_AREA_THRESHOLD = new DoubleParameter(
        "coefficient/area threshold", "This is a theshold for the maximum coefficient (inner product) devided by the area "
                + "under the curve of the feautre. Filters out bad peaks.",
        NumberFormat.getNumberInstance(), 110.0, 0.0, null);
    
    public static final DoubleParameter MIN_FEAT_HEIGHT = new DoubleParameter(
        "min feature height", "Minimum height of a feature. Should be the same, or similar to, the value - min start intensity - "
                + "set in the chromatogram building.",
        NumberFormat.getNumberInstance(), 10.0, 0.0, null);

    public ADAPDetectorParameters() {

	//super(new Parameter[] { SN_THRESHOLD,SHARP_THRESHOLD, MIN_FEAT_HEIGHT, PEAK_DURATION, });
        super(new Parameter[] { SN_THRESHOLD,SN_ESTIMATORS, MIN_FEAT_HEIGHT, COEF_AREA_THRESHOLD, PEAK_DURATION,RT_FOR_CWT_SCALES_DURATION });

//        numberFormat.setMaximumFractionDigits(6);
    }

    @Override
    public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {

	final PeakResolverSetupDialog dialog = new PeakResolverSetupDialog(
		parent, valueCheckRequired, this, ADAPDetector.class);
	dialog.setVisible(true);
	return dialog.getExitCode();
    }
}
