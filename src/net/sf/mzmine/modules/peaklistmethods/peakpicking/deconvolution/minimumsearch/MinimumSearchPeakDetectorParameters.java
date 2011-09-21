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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.minimumsearch;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.PeakResolver;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.PeakResolverSetupDialog;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.PercentParameter;
import net.sf.mzmine.util.dialogs.ExitCode;

public class MinimumSearchPeakDetectorParameters extends SimpleParameterSet {

	private PeakResolver peakResolver;

	public static final PercentParameter chromatographicThresholdLevel = new PercentParameter(
			"Chromatographic threshold",
			"Threshold for removing noise. The algorithm finds such intensity that given percentage of the chromatogram data points is below that intensity, and removes all data points below that level.");

	public static final DoubleParameter searchRTRange = new DoubleParameter(
			"Search minimum in RT range",
			"If a local minimum is minimal in this range of retention time, it will be considered a border between two peaks",
			MZmineCore.getRTFormat(), null, 0.001, null);

	public static final PercentParameter minRelativeHeight = new PercentParameter(
			"Minimum relative height",
			"Minimum height of a peak relative to the chromatogram top data point");

	public static final DoubleParameter minAbsoluteHeight = new DoubleParameter(
			"Minimum absolute height",
			"Minimum absolute height of a peak to be recognized",
			MZmineCore.getIntensityFormat());

	public static final DoubleParameter minRatio = new DoubleParameter(
			"Min ratio of peak top/edge",
			"Minimum ratio between peak's top intensity and side (lowest) data points. This parameter helps to reduce detection of false peaks in case the chromatogram is not smooth.");

	public ExitCode showSetupDialog() {
		PeakResolverSetupDialog dialog = new PeakResolverSetupDialog(
				peakResolver);
		dialog.setVisible(true);
		return dialog.getExitCode();
	}

	public MinimumSearchPeakDetectorParameters(PeakResolver peakResolver) {
		super(new Parameter[] { chromatographicThresholdLevel, searchRTRange,
				minRelativeHeight, minAbsoluteHeight, minRatio });
		this.peakResolver = peakResolver;
	}

}
