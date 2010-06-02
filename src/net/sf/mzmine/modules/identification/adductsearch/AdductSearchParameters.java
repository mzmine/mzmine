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
package net.sf.mzmine.modules.identification.adductsearch;

import java.text.NumberFormat;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.main.MZmineCore;

public class AdductSearchParameters extends SimpleParameterSet {

	public static final NumberFormat percentFormat = NumberFormat
			.getPercentInstance();

	public static final Parameter rtTolerance = new SimpleParameter(
			ParameterType.DOUBLE,
			"Time tolerance",
			"Maximum allowed difference of time to set a relationship between peaks",
			null, new Double(10.0), new Double(0.0), null, MZmineCore
					.getRTFormat());

	public static final Parameter adducts = new SimpleParameter(
			ParameterType.MULTIPLE_SELECTION,
			"Adducts",
			"List of adducts, each one refers a specific distance in m/z axis between related peaks",
			null, AdductType.values());

	public static final Parameter customAdductValue = new SimpleParameter(
			ParameterType.DOUBLE, "Custom adduct value",
			"Mass value (m/z difference) for custom adduct", "m/z", new Double(
					0.0), new Double(0.0), null, MZmineCore.getMZFormat());

	public static final Parameter mzTolerance = new SimpleParameter(
			ParameterType.DOUBLE, "m/z tolerance",
			"Tolerance value of the m/z difference between peaks", "m/z",
			new Double(0.1), new Double(0.0), null, MZmineCore.getMZFormat());

	public static final Parameter maxAdductHeight = new SimpleParameter(
			ParameterType.DOUBLE,
			"Max adduct peak height",
			"Maximum height of the recognized adduct peak, relative to the main peak",
			"%", new Double(0.20), new Double(0.0), null, percentFormat);

	public AdductSearchParameters() {
		super(new Parameter[] { rtTolerance, adducts, customAdductValue,
				mzTolerance, maxAdductHeight });
	}

}
