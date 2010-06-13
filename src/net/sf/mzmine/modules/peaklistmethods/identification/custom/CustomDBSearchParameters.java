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

package net.sf.mzmine.modules.peaklistmethods.identification.custom;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.main.MZmineCore;

/**
 * 
 */
public class CustomDBSearchParameters extends SimpleParameterSet {

	public static final Parameter dataBaseFile = new SimpleParameter(
			ParameterType.FILE_NAME, "Database file",
			"Name of file that contains information for peak identification");

	public static final Parameter fieldSeparator = new SimpleParameter(
			ParameterType.STRING, "Field separator",
			"Character(s) used to separate fields in the database file",
			(Object) ",");

	public static final Parameter fieldOrder = new SimpleParameter(
			ParameterType.ORDERED_LIST, "Field order",
			"Order of items in which they are read from database file", null,
			FieldItem.values());

	public static final Parameter ignoreFirstLine = new SimpleParameter(
			ParameterType.BOOLEAN, "Ignore first line",
			"Ignore the first line of database file", null, true, null, null,
			null);

	public static final Parameter mzTolerance = new SimpleParameter(
			ParameterType.DOUBLE, "m/z tolerance",
			"Maximum allowed m/z difference to set an identification to a peak",
			"m/z", new Double(1.0), new Double(0.0), null, MZmineCore
					.getMZFormat());

	public static final Parameter rtTolerance = new SimpleParameter(
			ParameterType.DOUBLE,
			"RT tolerance",
			"Maximum allowed retention time difference to set an identification to a peak",
			null, new Double(60.0), new Double(0.0), null, MZmineCore
					.getRTFormat());

	public CustomDBSearchParameters() {
		super(new Parameter[] { dataBaseFile, fieldSeparator, fieldOrder,
				ignoreFirstLine, mzTolerance, rtTolerance });
	}

}
