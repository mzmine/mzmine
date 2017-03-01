/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.peaklistmethods.identification.customdbsearch;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.OrderParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;

/**
 * 
 */
public class CustomDBSearchParameters extends SimpleParameterSet {

    public static final PeakListsParameter peakLists = new PeakListsParameter();

    public static final FileNameParameter dataBaseFile = new FileNameParameter(
	    "Database file",
	    "Name of file that contains information for peak identification");

    public static final StringParameter fieldSeparator = new StringParameter(
	    "Field separator",
	    "Character(s) used to separate fields in the database file", ",");

    public static final OrderParameter<FieldItem> fieldOrder = new OrderParameter<FieldItem>(
	    "Field order",
	    "Order of items in which they are read from database file",
	    FieldItem.values());

    public static final BooleanParameter ignoreFirstLine = new BooleanParameter(
	    "Ignore first line", "Ignore the first line of database file");

    public static final MZToleranceParameter mzTolerance = new MZToleranceParameter();

    public static final RTToleranceParameter rtTolerance = new RTToleranceParameter();

    public CustomDBSearchParameters() {
	super(new Parameter[] { peakLists, dataBaseFile, fieldSeparator,
		fieldOrder, ignoreFirstLine, mzTolerance, rtTolerance });
    }

}
