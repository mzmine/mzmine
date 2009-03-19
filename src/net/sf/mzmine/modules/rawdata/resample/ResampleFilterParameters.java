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

package net.sf.mzmine.modules.rawdata.resample;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.main.mzmineclient.MZmineCore;

public class ResampleFilterParameters extends SimpleParameterSet {

	public static final Parameter suffix = new SimpleParameter(
			ParameterType.STRING, "Filename suffix",
			"Suffix to be added to filename", null, "resampled", null);

	public static final Parameter binSize = new SimpleParameter(
			ParameterType.DOUBLE, "m/z bin length", "The length of on m/z bin",
			"m/z", new Double(1.0), new Double(0.00001), new Double(10.0),
			MZmineCore.getMZFormat());

	public static final Parameter autoRemove = new SimpleParameter(
			ParameterType.BOOLEAN,
			"Remove source file",
			"If checked, original file will be removed and only resampled version remains",
			new Boolean(true));

	public ResampleFilterParameters() {
		super(new Parameter[] { suffix, binSize, autoRemove });
	}

}
