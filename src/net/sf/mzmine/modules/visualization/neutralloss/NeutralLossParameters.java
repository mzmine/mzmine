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

package net.sf.mzmine.modules.visualization.neutralloss;

import java.text.NumberFormat;
import java.util.Hashtable;

import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.SimpleParameterSet;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.NumberParameter;
import net.sf.mzmine.parameters.parametertypes.RangeParameter;
import net.sf.mzmine.parameters.parametertypes.RawDataFilesParameter;
import net.sf.mzmine.util.dialogs.ExitCode;

public class NeutralLossParameters extends SimpleParameterSet {

	public static final String xAxisPrecursor = "Precursor mass";
	public static final String xAxisRT = "Retention time";

	public static final String[] xAxisTypes = { xAxisPrecursor, xAxisRT };

	public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();

	public static final ComboParameter<String> xAxisType = new ComboParameter<String>(
			"X axis", "X axis type", xAxisTypes);

	public static final RangeParameter retentionTimeRange = new RangeParameter(
			"Retention time", "Retention time (X axis) range",
			MZmineCore.getRTFormat());

	public static final RangeParameter mzRange = new RangeParameter(
			"Precursor m/z", "Range of precursor m/z values",
			MZmineCore.getMZFormat());

	public static final NumberParameter numOfFragments = new NumberParameter(
			"Fragments", "Number of most intense fragments",
			NumberFormat.getIntegerInstance());

	public NeutralLossParameters() {
		super(new Parameter[] { dataFiles, xAxisType, retentionTimeRange,
				mzRange, numOfFragments });
	}

	public ExitCode showSetupDialog() {

		RawDataFile selectedFiles[] = getParameter(
				NeutralLossParameters.dataFiles).getValue();

		if ((selectedFiles == null) || (selectedFiles.length == 0))
			return super.showSetupDialog();

		Hashtable<UserParameter, Object> autoValues = new Hashtable<UserParameter, Object>();

		autoValues.put(NeutralLossParameters.retentionTimeRange,
				selectedFiles[0].getDataRTRange(2));
		autoValues.put(NeutralLossParameters.mzRange,
				selectedFiles[0].getDataMZRange(1));

		return super.showSetupDialog(autoValues);
	}

}
