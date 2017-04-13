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
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.util.ExitCode;

public class IntensityWindowsSNParameters extends SimpleParameterSet{

    public IntensityWindowsSNParameters() {
        super(new Parameter[] { });
    }

    @Override
    public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {

	final SNSetUpDialog dialog = new SNSetUpDialog(
		parent, valueCheckRequired, this);
	dialog.setVisible(true);
	return dialog.getExitCode();
    }
    
}
