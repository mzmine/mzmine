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
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.util.ExitCode;

/**
 * @description Peak Detection baseline corrector parameters.
 * 
 */
public class PeakDetectionCorrectorParameters extends SimpleParameterSet {

    /**
     * Smallest peak width.
     */
    public static final IntegerParameter LEFT = new IntegerParameter(
            "left (number of scans)",
            "Smallest window size for peak widths (in number of scans).", 1, 0,
            null);
    /**
     * Largest peak width.
     */
    public static final IntegerParameter RIGHT = new IntegerParameter(
            "right (number of scans)",
            "Largest window size for peak widths (in number of scans).", 1, 0,
            null);

    /**
     * Smallest minimums and medians spectra removal.
     */
    public static final IntegerParameter LWIN = new IntegerParameter(
            "lwin (number of scans)",
            "Smallest window size for minimums and medians in peak removed spectra (in number of scans).",
            1, 0, null);
    /**
     * Largest minimums and medians spectra removal.
     */
    public static final IntegerParameter RWIN = new IntegerParameter(
            "rwin (number of scans)",
            "Largest window size for minimums and medians in peak removed spectra (in number of scans).",
            1, 0, null);

    /**
     * Minimum signal to noise ratio.
     */
    public static final DoubleParameter SNMINIMUM = new DoubleParameter(
            "snminimum", "Minimum signal to noise ratio for accepting peaks.",
            DecimalFormat.getNumberInstance(), 0.0, 0.0, 1.0);

    /**
     * Monotonically decreasing baseline.
     */
    public static final DoubleParameter MONO = new DoubleParameter("mono",
            "Monotonically decreasing baseline if ‘mono’>0.",
            DecimalFormat.getNumberInstance(), 0.0, 0.0, null);

    /**
     * Window size multiplier.
     */
    public static final DoubleParameter MULTIPLIER = new DoubleParameter(
            "multiplier", "Internal window size multiplier.",
            DecimalFormat.getNumberInstance(), 1.0, 1.0, null);

    public PeakDetectionCorrectorParameters() {
        super(new UserParameter[] { LEFT, RIGHT, LWIN, RWIN, SNMINIMUM, MONO,
                MULTIPLIER });
    }

    public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {
        BaselineCorrectorSetupDialog dialog = new BaselineCorrectorSetupDialog(
                parent, valueCheckRequired, this, PeakDetectionCorrector.class);
        dialog.setVisible(true);
        return dialog.getExitCode();
    }
}
