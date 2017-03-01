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

package net.sf.mzmine.modules.rawdatamethods.filtering.scanfilters.savitzkygolay;

import java.awt.Window;

import net.sf.mzmine.modules.rawdatamethods.filtering.scanfilters.ScanFilterSetupDialog;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.util.ExitCode;

public class SGFilterParameters extends SimpleParameterSet {

    private static final Integer options[] = new Integer[] { 5, 7, 9, 11, 13,
	    15 };

    public static final ComboParameter<Integer> datapoints = new ComboParameter<Integer>(
	    "Number of datapoints", "Number of datapoints", options);

    public SGFilterParameters() {
	super(new Parameter[] { datapoints });
    }

    public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {
	ScanFilterSetupDialog dialog = new ScanFilterSetupDialog(parent,
		valueCheckRequired, this, SGFilter.class);
	dialog.setVisible(true);
	return dialog.getExitCode();
    }

}
