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

package net.sf.mzmine.modules.masslistmethods.shoulderpeaksfilter;

import java.awt.Window;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.MassListParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import net.sf.mzmine.util.ExitCode;

public class ShoulderPeaksFilterParameters extends SimpleParameterSet {

    public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();

    public static final MassListParameter massList = new MassListParameter();

    public static final DoubleParameter resolution = new DoubleParameter(
	    "Mass resolution",
	    "Mass resolution is the dimensionless ratio of the mass of the peak divided by its width."
		    + "\nPeak width is taken as the full width at half maximum intensity (FWHM).");

    public static final ComboParameter<PeakModelType> peakModel = new ComboParameter<PeakModelType>(
	    "Peak model function",
	    "Peaks under the curve of this peak model will be removed",
	    PeakModelType.values());

    public static final StringParameter suffix = new StringParameter("Suffix",
	    "This string is added to mass list name as a suffix", "filtered");

    public static final BooleanParameter autoRemove = new BooleanParameter(
	    "Remove original mass list",
	    "If checked, original mass list will be removed and only filtered version remains");

    public ShoulderPeaksFilterParameters() {
	super(new Parameter[] { dataFiles, massList, resolution, peakModel,
		suffix, autoRemove });

    }

    public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {
	ShoulderPeaksFilterSetupDialog dialog = new ShoulderPeaksFilterSetupDialog(
		parent, valueCheckRequired, this);
	dialog.setVisible(true);
	return dialog.getExitCode();
    }

}
