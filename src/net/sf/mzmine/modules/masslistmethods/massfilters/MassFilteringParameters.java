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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.masslistmethods.massfilters;

import net.sf.mzmine.modules.masslistmethods.massfilters.shoulderpeaksfilter.ShoulderPeaksFilter;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.MassListParameter;
import net.sf.mzmine.parameters.parametertypes.ModuleComboParameter;
import net.sf.mzmine.parameters.parametertypes.RawDataFilesParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;

public class MassFilteringParameters extends SimpleParameterSet {

	public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();

	// This parameter will be used in the sub-modules. We need this parameter to
	// provide previews.
	public static final MassListParameter massList = new MassListParameter();

	public static final MassFilter massFilters[] = { new ShoulderPeaksFilter() };

	public static final ModuleComboParameter<MassFilter> massFilter = new ModuleComboParameter<MassFilter>(
			"Mass filter", "Mass filter", massFilters);

	public static final StringParameter suffix = new StringParameter("Suffix",
			"This string is added to name as suffix", "filtered");

	public static final BooleanParameter autoRemove = new BooleanParameter(
			"Remove original mass list",
			"If checked, original mass list will be removed and only filtered version remains");

	public MassFilteringParameters() {
		super(new Parameter[] { dataFiles, massFilter, suffix, autoRemove });
	}

}
