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

import java.text.NumberFormat;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.parameters.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.NumberParameter;

public class MsMsPeakPickerParameters extends SimpleParameterSet {

	public static final NumberParameter mzWindow = new NumberParameter(
			"m/z window", "m/z window for peak search",
			MZmineCore.getMZFormat());

	public static final NumberParameter rtWindow = new NumberParameter(
			"Time window", "Time window", MZmineCore.getRTFormat());

	public static final NumberParameter msLevel = new NumberParameter(
			"MS level", "MS level of scans to use for search",
			NumberFormat.getIntegerInstance(), 1);

	public MsMsPeakPickerParameters() {
		super(new UserParameter[] { mzWindow, rtWindow, msLevel });
	}

}
