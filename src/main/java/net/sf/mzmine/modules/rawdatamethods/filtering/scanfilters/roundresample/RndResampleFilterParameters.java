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

package net.sf.mzmine.modules.rawdatamethods.filtering.scanfilters.roundresample;

import java.awt.Window;

import net.sf.mzmine.modules.rawdatamethods.filtering.scanfilters.ScanFilterSetupDialog;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.util.ExitCode;

public class RndResampleFilterParameters extends SimpleParameterSet {

    public static final BooleanParameter SUM_DUPLICATES = new BooleanParameter(
            "Sum duplicate intensities",
            "Concatenates/sums ions count (intensity) of m/z peaks competing for being rounded at same m/z unit. "
                    + "If unchecked, the intensities are averaged rather than summed.",
            false);

    public static final BooleanParameter REMOVE_ZERO_INTENSITY = new BooleanParameter(
            "Remove zero intensity m/z peaks",
            "Clear all scans spectra from m/z peaks with intensity equal to zero.",
            true);

    public RndResampleFilterParameters() {
        super(new Parameter[] { SUM_DUPLICATES, REMOVE_ZERO_INTENSITY });
    }

    public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {
        ScanFilterSetupDialog dialog = new ScanFilterSetupDialog(parent,
                valueCheckRequired, this, RndResampleFilter.class);
        dialog.setVisible(true);
        return dialog.getExitCode();
    }
}
