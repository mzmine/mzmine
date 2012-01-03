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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.identification.dbsearch;

import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopepatternscore.IsotopePatternScoreParameters;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.MZToleranceParameter;
import net.sf.mzmine.parameters.parametertypes.NeutralMassParameter;
import net.sf.mzmine.parameters.parametertypes.OptionalModuleParameter;

public class SingleRowIdentificationParameters extends SimpleParameterSet {

	public static final ComboParameter<OnlineDatabase> database = new ComboParameter<OnlineDatabase>(
			"Database", "Database to search", OnlineDatabase.values());

	public static final NeutralMassParameter neutralMass = new NeutralMassParameter(
			"Neutral mass", "Value to use in the search query");

	public static final IntegerParameter numOfResults = new IntegerParameter(
			"Number of results", "Maximum number of results to display", 100);

	public static final MZToleranceParameter mzTolerance = new MZToleranceParameter();

	public static final OptionalModuleParameter isotopeFilter = new OptionalModuleParameter(
			"Isotope pattern filter",
			"Search only for compounds with a isotope pattern similar",
			new IsotopePatternScoreParameters());

	public SingleRowIdentificationParameters() {
		super(new Parameter[] { database, neutralMass, numOfResults,
				mzTolerance, isotopeFilter });
	}

}
