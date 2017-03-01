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

/* Code created was by or on behalf of Syngenta and is released under the open source license in use for the
 * pre-existing code or project. Syngenta does not assert ownership or copyright any over pre-existing work.
 */

package net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.adapnewpeakpickingGC;

import java.awt.Window;
import java.text.NumberFormat;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.PeakResolverSetupDialog;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;

import net.sf.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;
import net.sf.mzmine.util.ExitCode;

import com.google.common.collect.Range;


/**
 * Parameters used by CentWaveDetector.
 */
public class ADAP3NewGCPeakDetectionParameters extends SimpleParameterSet {

//    /**
//     * Peak integration methods.
//     */
//    public enum PeakIntegrationMethod {
//
//	UseSmoothedData("Use smoothed data", 1), UseRawData("Use raw data", 2);
//
//	private final String name;
//	private final int index;
//
//	/**
//	 * Create the method.
//	 *
//	 * @param aName
//	 *            name
//	 * @param anIndex
//	 *            index (as used by findPeaks.centWave)
//	 */
//	PeakIntegrationMethod(final String aName, final int anIndex) {
//
//	    name = aName;
//	    index = anIndex;
//	}
//
//	@Override
//	public String toString() {
//
//	    return name;
//	}
//
//	public int getIndex() {
//
//	    return index;
//	}
//    }

    public static final DoubleRangeParameter PEAK_DURATION = new DoubleRangeParameter(
	    "Peak duration range", "Range of acceptable peak lengths",
	    MZmineCore.getConfiguration().getRTFormat(),
	    Range.closed(0.0, 10.0));
    
    public static final DoubleRangeParameter RT_FOR_CWT_SCALES_DURATION = new DoubleRangeParameter(
	    "RT wavelet range", "Upper and lower bounds of retention times to be used for setting the wavelet scales.",
	    MZmineCore.getConfiguration().getRTFormat(),
	    Range.closed(0.01, 0.1));

    public static final DoubleParameter EDGE_TO_HEIGHT_RATIO =
            new DoubleParameter("Minimum Edge-to-Height Ratio", 
                    "A peak is considered shared if its edge-to-height ratio is below this parameter",
                    NumberFormat.getInstance(), 0.3, 0.0, 1.0);
    
    public static final DoubleParameter DELTA_TO_HEIGHT_RATIO =
            new DoubleParameter("Minimum Delta-to-Height Ratio",
                    "A peak is considered shared if its delta (difference between the edges) -to-height ratio is below this parameter",
                    NumberFormat.getInstance(), 0.2, 0.0, 1.0);
    
    public static final IntegerParameter MAX_WINDOW_SIZE =
            new IntegerParameter("Maximum Window Size (# of scans)", 
                    "Peaks with the combined length exceeding this parameter, cannot be combined", 
                    350);

//    public static final DoubleRangeParameter PEAK_SCALES = new DoubleRangeParameter(
//	    "Wavelet scales",
//	    "Range wavelet widths (smallest, largest) in minutes", MZmineCore
//		    .getConfiguration().getRTFormat(), Range.closed(0.25, 5.0));
    

    public static final DoubleParameter SN_THRESHOLD = new DoubleParameter(
	    "S/N threshold", "Signal to noise ratio threshold",
	    NumberFormat.getNumberInstance(), 10.0, 0.0, null);
    
//    public static final DoubleParameter SHARP_THRESHOLD = new DoubleParameter(
//	    "Sharpness threshold", "This is the angle between the two estimated slopes on the right and left hand side of the peaks.\n"
//                    + "Peak with angles above the set value will be discarded.",
//	    NumberFormat.getNumberInstance(), 10.0, 0.0, null);
    
    public static final DoubleParameter COEF_AREA_THRESHOLD = new DoubleParameter(
        "coefficient/area threshold", "This is a theshold for the maximum coefficient (inner product) devided by the area "
                + "under the curve of the feautre. Filters out bad peaks.",
        NumberFormat.getNumberInstance(), 10.0, 0.0, null);
    
    public static final DoubleParameter MIN_FEAT_HEIGHT = new DoubleParameter(
        "min feature height", "Minimum height of a feature. Should be the same, or similar to, the value - min start intensity - "
                + "set in the chromatogram building.",
        NumberFormat.getNumberInstance(), 10.0, 0.0, null);

//    public static final ComboParameter<PeakIntegrationMethod> INTEGRATION_METHOD = new ComboParameter<PeakIntegrationMethod>(
//	    "Peak integration method",
//	    "Method used to determine RT extents of detected peaks",
//	    PeakIntegrationMethod.values(),
//	    PeakIntegrationMethod.UseSmoothedData);

    public ADAP3NewGCPeakDetectionParameters() {

	//super(new Parameter[] { SN_THRESHOLD,SHARP_THRESHOLD, MIN_FEAT_HEIGHT, PEAK_DURATION, });
        super(new Parameter[] { EDGE_TO_HEIGHT_RATIO, DELTA_TO_HEIGHT_RATIO, MAX_WINDOW_SIZE ,SN_THRESHOLD, MIN_FEAT_HEIGHT, COEF_AREA_THRESHOLD, PEAK_DURATION,RT_FOR_CWT_SCALES_DURATION });
    }

    @Override
    public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {

	final PeakResolverSetupDialog dialog = new PeakResolverSetupDialog(
		parent, valueCheckRequired, this, ADAP3NewGCPeakDetection.class);
	dialog.setVisible(true);
	return dialog.getExitCode();
    }
}
