/*
 * Copyright 2006-2010 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.peakpicking.chromatogrambuilder.massconnection.highestdatapoint;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.main.MZmineCore;

public class HighestDataPointConnectorParameters extends SimpleParameterSet {

	public static final Parameter minimumTimeSpan = new SimpleParameter(
			ParameterType.DOUBLE, "Min time span",
			"Minimum acceptable time span of connected string of m/z peaks",
			null, new Double(10.0), new Double(0.0), null, MZmineCore
					.getRTFormat());

	public static final Parameter minimumHeight = new SimpleParameter(
			ParameterType.DOUBLE, "Min height",
			"Minimum top intensity of the chromatogram", null, new Double(100.0),
			new Double(0.0), null, MZmineCore.getIntensityFormat());

	public static final Parameter mzTolerance = new SimpleParameter(
			ParameterType.DOUBLE,
			"m/z tolerance",
			"Maximum allowed distance in M/Z between data points in successive spectrums",
			"m/z", new Double(0.1), new Double(0.0), new Double(1.0),
			MZmineCore.getMZFormat());

	public HighestDataPointConnectorParameters() {
		super(new Parameter[] { minimumTimeSpan, minimumHeight, mzTolerance });
	}

}
