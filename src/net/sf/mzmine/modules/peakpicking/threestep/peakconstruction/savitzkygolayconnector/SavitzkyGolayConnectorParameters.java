/*
 * Copyright 2006-2008 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peakpicking.threestep.peakconstruction.savitzkygolayconnector;

import java.text.NumberFormat;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.main.MZmineCore;

public class SavitzkyGolayConnectorParameters extends SimpleParameterSet {

	public static final NumberFormat percentFormat = NumberFormat
			.getPercentInstance();

	public static final Parameter minimumPeakHeight = new SimpleParameter(
			ParameterType.FLOAT, "Min peak height",
			"Minimum acceptable peak height", "absolute", new Float(100.0),
			new Float(0.0), null, MZmineCore.getIntensityFormat());

	public static final Parameter minimumPeakDuration = new SimpleParameter(
			ParameterType.FLOAT, "Min peak duration",
			"Minimum acceptable peak duration", null, new Float(10.0),
			new Float(0.0), null, MZmineCore.getRTFormat());

	public static final Parameter mzTolerance = new SimpleParameter(
			ParameterType.FLOAT,
			"M/Z tolerance",
			"Maximum allowed distance in M/Z between data points in successive scans",
			"m/z", new Float(0.1), new Float(0.0), new Float(1.0), MZmineCore
					.getMZFormat());

	public static final Parameter intTolerance = new SimpleParameter(
			ParameterType.FLOAT,
			"Intensity tolerance",
			"Maximum allowed deviation from expected /\\ shape of a peak in chromatographic direction",
			"%", new Float(0.15), new Float(0.0), new Float(1.0), percentFormat);

	
	public SavitzkyGolayConnectorParameters() {
		super(new Parameter[] { minimumPeakHeight, minimumPeakDuration,
				mzTolerance, intTolerance });
	}

}
