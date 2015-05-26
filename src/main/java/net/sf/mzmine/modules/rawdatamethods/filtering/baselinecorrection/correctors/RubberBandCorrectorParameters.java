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

package net.sf.mzmine.modules.rawdatamethods.filtering.baselinecorrection.correctors;

import java.awt.Window;
import java.text.DecimalFormat;

import net.sf.mzmine.modules.rawdatamethods.filtering.baselinecorrection.BaselineCorrectorSetupDialog;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.util.ExitCode;

/**
 * @description Rubber Band baseline corrector parameters.
 * 
 */
public class RubberBandCorrectorParameters extends SimpleParameterSet {

    /**
     * Noise level.
     */
    public static final DoubleParameter NOISE = new DoubleParameter(
	    "noise",
	    "Ignored if \"auto noise\" is checked. Noise level to be taken into account.",
	    DecimalFormat.getNumberInstance(), 0.0, 0.0, null);

    /**
     * Determine noise automatically.
     */
    public static final BooleanParameter AUTO_NOISE = new BooleanParameter(
	    "auto noise",
	    "Determine noise level automatically (from lower intensity scan).",
	    false);

    /**
     * Degree of Freedom.
     */
    public static final DoubleParameter DF = new DoubleParameter("df",
	    "Degree of freedom.", DecimalFormat.getNumberInstance(), 0.0, 0.0,
	    null);

    /**
     * Interpolating with spline.
     */
    public static final BooleanParameter SPLINE = new BooleanParameter(
	    "spline",
	    "Logical indicating whether the baseline should be an interpolating spline through the support points or piecewise linear.",
	    true);

    /**
     * Bend additional feature.
     */
    public static final DoubleParameter BEND_FACTOR = new DoubleParameter(
	    "bend factor",
	    "Does nothing if equals to zero. Helps fitting better with low \"df\". Try with 5^4, to start palying with...",
	    DecimalFormat.getNumberInstance(), 0.0, 0.0, null);

    public RubberBandCorrectorParameters() {
	super(
		new UserParameter[] { NOISE, AUTO_NOISE, DF, SPLINE,
			BEND_FACTOR });
    }

    public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {
	BaselineCorrectorSetupDialog dialog = new BaselineCorrectorSetupDialog(
		parent, valueCheckRequired, this, RubberBandCorrector.class);
	dialog.setVisible(true);
	return dialog.getExitCode();
    }
}
