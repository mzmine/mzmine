/*
 * Copyright 2006-2011 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.rawdatamethods.peakpicking.massfilters.shoulderpeaksfilter;

import java.text.NumberFormat;

import net.sf.mzmine.parameters.SimpleParameterSet;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.NumberParameter;
import net.sf.mzmine.util.dialogs.ExitCode;

public class ShoulderPeaksFilterParameters extends SimpleParameterSet {

	public static final NumberParameter resolution = new NumberParameter(
			"Mass resolution",
			"Mass resolution is the dimensionless ratio of the mass of the peak divided by its width."
					+ " Peak width is taken as the full width at half maximum intensity (FWHM).",
			NumberFormat.getIntegerInstance());

	public static final ComboParameter<PeakModelType> peakModel = new ComboParameter<PeakModelType>(
			"Peak model function",
			"Peaks under the curve of this peak model are removed",
			PeakModelType.values());


	public ShoulderPeaksFilterParameters() {
		super(new UserParameter[] { resolution, peakModel });

	}

	public ExitCode showSetupDialog() {
		// TODO:
		return super.showSetupDialog();
		/*
		MassFilter filter = parentParameterSet.getParameter(
				ChromatogramBuilderParameters.massFilter).getValue();
		MassDetector detector = parentParameterSet.getParameter(
				ChromatogramBuilderParameters.massDetector).getValue();
		MassFilterSetupDialog dialog = new MassFilterSetupDialog(detector,
				filter);
		dialog.setVisible(true);
		return dialog.getExitCode();*/
	}

}
