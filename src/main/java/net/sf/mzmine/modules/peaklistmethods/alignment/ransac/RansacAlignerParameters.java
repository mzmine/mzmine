/*
 * Copyright 2006-2014 The MZmine 2 Development Team
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
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.MZToleranceParameter;
import net.sf.mzmine.parameters.parametertypes.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.PercentParameter;
import net.sf.mzmine.parameters.parametertypes.RTToleranceParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.util.ExitCode;

public class RansacAlignerParameters extends SimpleParameterSet {

	public static final PeakListsParameter peakLists = new PeakListsParameter();

	public static final StringParameter peakListName = new StringParameter(
			"Peak list name", "Peak list name", "Aligned peak list");

	public static final MZToleranceParameter MZTolerance = new MZToleranceParameter();

	public static final RTToleranceParameter RTToleranceBefore = new RTToleranceParameter(
			"RT tolerance",
			"This value sets the range, in terms of retention time, to create the model using RANSAC and non-linear regression algorithm. Maximum allowed retention time difference.");

	public static final RTToleranceParameter RTToleranceAfter = new RTToleranceParameter(
			"RT tolerance after correction",
			"This value sets the range, in terms of retention time, to verify for possible peak rows to be aligned. Maximum allowed retention time difference.");

	public static final IntegerParameter Iterations = new IntegerParameter(
			"RANSAC iterations",
			"Maximum number of iterations allowed in the algorithm to find the right model consistent in all the pairs of aligned peaks. When its value is 0, the number of iterations (k) will be estimate automatically.");

	public static final PercentParameter NMinPoints = new PercentParameter(
			"Minimum number of points",
			"% of points required to consider the model valid (d).");

	public static final DoubleParameter Margin = new DoubleParameter(
			"Threshold value",
			"Threshold value (minutes) for determining when a data point fits a model (t)");

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
