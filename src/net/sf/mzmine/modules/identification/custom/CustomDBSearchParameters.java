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

package net.sf.mzmine.modules.identification.custom;

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
			"Character(s) used to separate fields in the exported file",
			(Object) ",");

	public static final Parameter fieldOrder = new SimpleParameter(
			ParameterType.DRAG_ORDERED_LIST, "Field order",
			"Order of items in which they are readed from database file", null,
			FieldItem.values());

	public static final Parameter ignoreFirstLine = new SimpleParameter(
			ParameterType.BOOLEAN, "Ignore first line",
			"Ignore the first line of database file", null, true, null, null,
			null);

	public static final Parameter mzTolerance = new SimpleParameter(
			ParameterType.DOUBLE, "m/z tolerance",
			"Tolerance mass difference to set an identification to one peak",
			"m/z", new Double(1.0), new Double(0.0), null, MZmineCore
					.getMZFormat());

	public static final Parameter rtTolerance = new SimpleParameter(
			ParameterType.DOUBLE,
			"Time tolerance",
			"Maximum allowed difference of time to set an identification to one peak",
			null, new Double(60.0), new Double(0.0), null, MZmineCore
					.getRTFormat());

	public CustomDBSearchParameters() {
		super(new Parameter[] { dataBaseFile, fieldSeparator, fieldOrder,
				ignoreFirstLine, mzTolerance, rtTolerance });
	}

}
