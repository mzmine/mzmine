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

import net.sf.mzmine.modules.rawdatamethods.filtering.baselinecorrection.BaselineCorrectorSetupDialog;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.util.ExitCode;

/**
 * @description Rolling Ball baseline corrector parameters.
 * 
 */
public class RollingBallCorrectorParameters extends SimpleParameterSet {

    /**
     * Local minima search window.
     */
    public static final IntegerParameter MIN_MAX_WIDTH = new IntegerParameter(
	    "wm (number of scans)",
	    "Width of local window for minimization/maximization (in number of scans).",
	    null, 0, null);

    /**
     * Smoothing.
     */
    public static final IntegerParameter SMOOTHING = new IntegerParameter(
	    "ws (number of scans)",
	    "Width of local window for smoothing (in number of scans).", null,
	    0, null);

    public RollingBallCorrectorParameters() {
	super(new UserParameter[] { MIN_MAX_WIDTH, SMOOTHING });
    }

    public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {
	BaselineCorrectorSetupDialog dialog = new BaselineCorrectorSetupDialog(
		parent, valueCheckRequired, this, RollingBallCorrector.class);
	dialog.setVisible(true);
	return dialog.getExitCode();
    }
}
