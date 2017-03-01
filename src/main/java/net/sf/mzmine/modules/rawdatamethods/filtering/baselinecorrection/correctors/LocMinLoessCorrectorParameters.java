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
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.util.ExitCode;

/**
 * @description Local Minima + LOESS baseline corrector parameters.
 * 
 */
public class LocMinLoessCorrectorParameters extends SimpleParameterSet {

    /**
     * Method.
     */
    public static final String[] choices = new String[] { "loess", "approx" };
    public static final ComboParameter<String> METHOD = new ComboParameter<String>(
            "method",
            "\"loess\" (smoothed low-percentile intensity) or \"approx\" (linear interpolation).",
            choices, choices[0]);

    /**
     * Determine noise automatically.
     */
    public static final DoubleParameter BW = new DoubleParameter("bw",
            "The bandwidth to be passed to loess.",
            DecimalFormat.getNumberInstance(), 0.0, 0.0, null);

    /**
     * Number of breaks.
     */
    public static final IntegerParameter BREAKS = new IntegerParameter(
            "breaks",
            "Number of breaks set to M/Z values for finding the local minima or points below a centain quantile of intensities; breaks -1 equally spaced intervals on the log M/Z scale.",
            null, true, 1, null);
    /**
     * Break widthy.
     */
    public static final IntegerParameter BREAK_WIDTH = new IntegerParameter(
            "break width (number of scans)",
            "Overrides \"breaks\" value. Width of a single break. Usually the maximum width (in number of scans) of the largest peak.",
            -1, true, -1, null);
    // TODO: Turn it into Retention Time value rather than number of scans

    /**
     * Quantile feature.
     */
    public static final DoubleParameter QNTL = new DoubleParameter(
            "qntl",
            "If 0, find local minima; if >0 find intensities < qntl*100th quantile locally.",
            DecimalFormat.getNumberInstance(), 0.0, 0.0, null);

    public LocMinLoessCorrectorParameters() {
        super(new UserParameter[] { METHOD, BW, BREAKS, BREAK_WIDTH, QNTL });
    }

    public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {
        BaselineCorrectorSetupDialog dialog = new BaselineCorrectorSetupDialog(
                parent, valueCheckRequired, this, LocMinLoessCorrector.class);
        dialog.setVisible(true);
        return dialog.getExitCode();
    }
}
