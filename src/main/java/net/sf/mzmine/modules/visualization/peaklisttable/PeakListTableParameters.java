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

package net.sf.mzmine.modules.visualization.peaklisttable;

import net.sf.mzmine.modules.visualization.peaklisttable.table.CommonColumnType;
import net.sf.mzmine.modules.visualization.peaklisttable.table.DataFileColumnType;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.WindowSettingsParameter;

public class PeakListTableParameters extends SimpleParameterSet {

    public static final ColumnSettingParameter<CommonColumnType> commonColumns = new ColumnSettingParameter<CommonColumnType>(
	    "Common columns",
	    "Visible common columns. Note: The ID, m/z and RT columns will always be visible, no matter what the setting is.",
	    CommonColumnType.values());

    public static final ColumnSettingParameter<DataFileColumnType> dataFileColumns = new ColumnSettingParameter<DataFileColumnType>(
	    "Data file columns", "Visible common columns",
	    DataFileColumnType.values());

    public static final IntegerParameter rowHeight = new IntegerParameter(
	    "Row height", "Row height", 30);

    public static final ComboParameter<PeakShapeNormalization> peakShapeNormalization = new ComboParameter<PeakShapeNormalization>(
	    "Peak shape normalization", "Peak shape normalization",
	    PeakShapeNormalization.values());

    /**
     * Windows size and position
     */
    public static final WindowSettingsParameter windowSettings = new WindowSettingsParameter();

    public PeakListTableParameters() {
	super(new Parameter[] { commonColumns, dataFileColumns, rowHeight,
		peakShapeNormalization, windowSettings });

	// Set the default settings for data file columns
	DataFileColumnType[] defaultColumns = new DataFileColumnType[] {
		DataFileColumnType.STATUS, DataFileColumnType.HEIGHT,
		DataFileColumnType.AREA };
	getParameter(dataFileColumns).setValue(defaultColumns);
    }

}