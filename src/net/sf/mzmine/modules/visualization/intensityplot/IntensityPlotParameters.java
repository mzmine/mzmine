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

package net.sf.mzmine.modules.visualization.intensityplot;

import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.parameters.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.MultiChoiceParameter;

/**
 */
public class IntensityPlotParameters extends SimpleParameterSet {

	public static final MultiChoiceParameter<RawDataFile> dataFiles = new MultiChoiceParameter<RawDataFile>(
			"Raw data files", "Raw data files to display", new RawDataFile[0]);

	public static final ComboParameter<Object> xAxisValueSource = new ComboParameter<Object>(
			"X axis value", "X axis value", new Object[0]);

	public static final ComboParameter<YAxisValueSource> yAxisValueSource = new ComboParameter<YAxisValueSource>(
			"Y axis value", "Y axis value", YAxisValueSource.values());

	public static final MultiChoiceParameter<PeakListRow> selectedRows = new MultiChoiceParameter<PeakListRow>(
			"Peak list rows", "Select peaks to display", new PeakListRow[0]);

	public IntensityPlotParameters() {
		super(new UserParameter[] { dataFiles, xAxisValueSource, xAxisValueSource,
				selectedRows });
	}

}
