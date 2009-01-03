/*
 * Copyright 2006-2009 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.peakpicking.chromatogrambuilder.massdetection.recursive;

import java.text.NumberFormat;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.main.MZmineCore;

public class RecursiveMassDetectorParameters extends SimpleParameterSet {

	public static final NumberFormat percentFormat = NumberFormat
			.getPercentInstance();

	public static final Parameter noiseLevel = new SimpleParameter(
			ParameterType.DOUBLE, "Noise level",
			"Intensities less than this value are interpreted as noise",
			"absolute", new Double(10.0), new Double(0.0), null, MZmineCore
					.getIntensityFormat());

	public static final Parameter minimumMZPeakWidth = new SimpleParameter(
			ParameterType.DOUBLE, "Min m/z peak width",
			"Minimum acceptable peak width in m/z", "m/z", new Double(0.2),
			new Double(0.0), null, MZmineCore.getMZFormat());

	public static final Parameter maximumMZPeakWidth = new SimpleParameter(
			ParameterType.DOUBLE, "Max m/z peak width",
			"Maximum acceptable peak width in m/z", "m/z", new Double(1.00),
			new Double(0.0010), null, MZmineCore.getMZFormat());

	public RecursiveMassDetectorParameters() {
		super(new Parameter[] { noiseLevel, minimumMZPeakWidth,
				maximumMZPeakWidth });

	}

}
