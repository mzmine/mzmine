/*
 * Copyright 2006-2012 The MZmine 2 Development Team
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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.rawdatamethods.filtering.datasetfilters;

import net.sf.mzmine.modules.rawdatamethods.filtering.datasetfilters.cropper.CropFilter;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.ModuleComboParameter;
import net.sf.mzmine.parameters.parametertypes.RawDataFilesParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;

public class DataSetFiltersParameters extends SimpleParameterSet {

	public static final RawDataSetFilter rawDataFilters[] = { new CropFilter() };

	public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();

	public static final StringParameter suffix = new StringParameter("Suffix",
			"This string is added to filename as suffix", "filtered");

	public static final ModuleComboParameter<RawDataSetFilter> filter = new ModuleComboParameter<RawDataSetFilter>(
			"Filter", "Raw data filter", rawDataFilters);

	public static final BooleanParameter autoRemove = new BooleanParameter(
			"Remove source file after filtering",
			"If checked, original file will be removed and only filtered version remains");

	public DataSetFiltersParameters() {
		super(new Parameter[] { dataFiles, suffix, filter, autoRemove });
	}

}
