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

package net.sf.mzmine.modules.peaklistmethods.filtering.rowsfilter;

import java.text.NumberFormat;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.parameters.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.NumberParameter;
import net.sf.mzmine.parameters.parametertypes.RangeParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;

public class RowsFilterParameters extends SimpleParameterSet {

	public static final StringParameter suffix = new StringParameter(
			"Name suffix", "Suffix to be added to peak list name", "filtered");

	public static final NumberParameter minPeaks = new NumberParameter(
			"Minimum peaks in a row",
			"Minimum number of peak detections required to select a row",
			NumberFormat.getIntegerInstance());

	public static final NumberParameter minIsotopePatternSize = new NumberParameter(
			"Minimum peaks in an isotope pattern",
			"Minimum number of peaks required in an isotope pattern",
			NumberFormat.getIntegerInstance());

	public static final RangeParameter mzRange = new RangeParameter(
			"m/z range", "Range of allowed m/z values",
			MZmineCore.getMZFormat());

	public static final RangeParameter rtRange = new RangeParameter(
			"Retention time range", "Maximum average retention time of a row",
			MZmineCore.getRTFormat());

	public static final BooleanParameter identified = new BooleanParameter(
			"Only identified?", "Select to filter only identified compounds");

	public static final BooleanParameter autoRemove = new BooleanParameter(
			"Remove source peak list after filtering",
			"If checked, original peak list will be removed and only filtered version remains");

	public RowsFilterParameters() {
		super(new UserParameter[] { suffix, minPeaks, minIsotopePatternSize,
				mzRange, rtRange, identified, autoRemove });
	}

}
