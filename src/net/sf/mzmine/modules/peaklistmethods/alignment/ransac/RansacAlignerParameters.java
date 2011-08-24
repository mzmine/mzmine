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

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.MZToleranceParameter;
import net.sf.mzmine.parameters.parametertypes.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.RTToleranceParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.util.dialogs.ExitCode;

public class RansacAlignerParameters extends SimpleParameterSet {

	public static final PeakListsParameter peakLists = new PeakListsParameter();

	public static final StringParameter peakListName = new StringParameter(
			"Peak list name", "Peak list name", "Aligned peak list");

	public static final MZToleranceParameter MZTolerance = new MZToleranceParameter();

	public static final RTToleranceParameter RTToleranceBefore = new RTToleranceParameter();

	public static final RTToleranceParameter RTToleranceAfter = new RTToleranceParameter(
			"RT tolerance after correction",
			"Maximum allowed absolute RT difference after the algorithm correction for the retention time");

	public static final IntegerParameter Iterations = new IntegerParameter(
			"RANSAC Iterations",
			"Maximum number of iterations allowed in the algorithm");

	public static final DoubleParameter NMinPoints = new DoubleParameter(
			"Minimun Number of Points",
			"Minimum number of aligned peaks required to fit the model");

	public static final DoubleParameter Margin = new DoubleParameter(
			"Threshold value",
			"Threshold value for determining when a data point fits a model");

	public static final BooleanParameter Linear = new BooleanParameter(
			"Linear model", "Switch between polynomial model or lineal model");

	public static final BooleanParameter SameChargeRequired = new BooleanParameter(
			"Require same charge state",
			"If checked, only rows having same charge state can be aligned");

	public ExitCode showSetupDialog() {
		RansacAlignerSetupDialog dialog = new RansacAlignerSetupDialog(this);
		dialog.setVisible(true);
		return dialog.getExitCode();
	}

	public RansacAlignerParameters() {
		super(new Parameter[] { peakLists, peakListName, MZTolerance,
				RTToleranceBefore, RTToleranceAfter, Iterations, NMinPoints,
				Margin, Linear, SameChargeRequired });
	}
}
