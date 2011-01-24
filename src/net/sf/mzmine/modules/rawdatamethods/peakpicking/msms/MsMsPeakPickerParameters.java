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

package net.sf.mzmine.modules.rawdatamethods.peakpicking.msms;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.main.MZmineCore;

public class MsMsPeakPickerParameters extends SimpleParameterSet {

	public static final Parameter mzWindow = new SimpleParameter(
			ParameterType.DOUBLE, "m/z window", "m/z window for peak search",
			"m/z", new Double(1.0), null, null, MZmineCore.getMZFormat());

	public static final Parameter rtWindow = new SimpleParameter(
			ParameterType.DOUBLE, "Time window",
			"Time window", null, new Double(1.0),
			new Double(0), null, MZmineCore.getRTFormat());
	
	public static final Parameter msLevel = new SimpleParameter(
			ParameterType.INTEGER, "MS level", "MS level of scans to use for search",
			"", new Integer(2), null, null, null);
	
	public MsMsPeakPickerParameters() {
		super(new Parameter[] { mzWindow, rtWindow, msLevel });
	}

}
