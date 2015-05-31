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
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.util.ExitCode;

/**
 * @description Asymmetric baseline corrector parameters.
 * 
 */
public class AsymmetryCorrectorParameters extends SimpleParameterSet {

    /**
     * Smoothing factor.
     */
    public static final DoubleParameter SMOOTHING = new DoubleParameter(
	    "Smoothing",
	    "The smoothing factor (>= 0), generally 10^5 - 10^8, the larger it is, the smoother the baseline will be.",
	    DecimalFormat.getNumberInstance(), null, 0.0, null);

    /**
     * Asymmetry.
     */
    public static final DoubleParameter ASYMMETRY = new DoubleParameter(
	    "Asymmetry",
	    "The weight (0 <= p <= 1) for points above the trend line, whereas 1-p is the weight for points below it. Naturally, p should be small for estimating baselines.",
	    DecimalFormat.getNumberInstance(), 0.001, 0.0, 1.0);

    public AsymmetryCorrectorParameters() {
	super(new UserParameter[] { SMOOTHING, ASYMMETRY });
    }

    public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {
	BaselineCorrectorSetupDialog dialog = new BaselineCorrectorSetupDialog(
		parent, valueCheckRequired, this, AsymmetryCorrector.class);
	dialog.setVisible(true);
	return dialog.getExitCode();
    }
}
