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
package net.sf.mzmine.modules.peaklistmethods.alignment.ransac;

import java.text.NumberFormat;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.parameters.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.NumberParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.util.dialogs.ExitCode;

public class RansacAlignerParameters extends SimpleParameterSet {

	public static final StringParameter peakListName = new StringParameter(
			"Peak list name", "Peak list name", "Aligned peak list");

	public static final NumberParameter MZTolerance = new NumberParameter(
			"m/z tolerance", "Maximum allowed M/Z difference",
			MZmineCore.getMZFormat());

	public static final NumberParameter RTToleranceValueAbs = new NumberParameter(
			"RT tolerance after correction",
			"Maximum allowed absolute RT difference after the algorithm correction for the retention time",
			MZmineCore.getRTFormat());

	public static final NumberParameter RTTolerance = new NumberParameter(
			"RT tolerance", "Maximum allowed absolute RT difference",
			MZmineCore.getRTFormat());

	public static final NumberParameter Iterations = new NumberParameter(
			"RANSAC Iterations",
			"Maximum number of iterations allowed in the algorithm",
			NumberFormat.getPercentInstance());

	public static final NumberParameter NMinPoints = new NumberParameter(
			"Minimun Number of Points",
			"Minimum number of aligned peaks required to fit the model",
			NumberFormat.getPercentInstance());

	public static final NumberParameter Margin = new NumberParameter(
			"Threshold value",
			"Threshold value for determining when a data point fits a model",
			MZmineCore.getRTFormat());

	public static final BooleanParameter Linear = new BooleanParameter(
			"Linear model", "Switch between polynomial model or lineal model");

	public static final BooleanParameter SameChargeRequired = new BooleanParameter(
			"Require same charge state",
			"If checked, only rows having same charge state can be aligned");

	public ExitCode showSetupDialog() {
		RansacAlignerSetupDialog dialog = new RansacAlignerSetupDialog(this, null);
		dialog.setVisible(true);
		return dialog.getExitCode();
	}
	
	public RansacAlignerParameters() {
		super(new UserParameter[] { peakListName, MZTolerance, RTToleranceValueAbs,
				RTTolerance, Iterations, NMinPoints, Margin, Linear,
				SameChargeRequired });
	}
}
