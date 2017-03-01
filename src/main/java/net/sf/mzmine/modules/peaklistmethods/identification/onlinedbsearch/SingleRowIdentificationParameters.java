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

package net.sf.mzmine.modules.peaklistmethods.identification.onlinedbsearch;

import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopepatternscore.IsotopePatternScoreParameters;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.ModuleComboParameter;
import net.sf.mzmine.parameters.parametertypes.NeutralMassParameter;
import net.sf.mzmine.parameters.parametertypes.OptionalModuleParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

public class SingleRowIdentificationParameters extends SimpleParameterSet {

    public static final ModuleComboParameter<OnlineDatabase> DATABASE = new ModuleComboParameter<OnlineDatabase>(
	    "Database", "Database to search", OnlineDatabase.values());

    public static final NeutralMassParameter NEUTRAL_MASS = new NeutralMassParameter(
	    "Neutral mass", "Value to use in the search query");

    public static final IntegerParameter MAX_RESULTS = new IntegerParameter(
	    "Number of results", "Maximum number of results to display", 100);

    public static final MZToleranceParameter MZ_TOLERANCE = new MZToleranceParameter();

    public static final OptionalModuleParameter ISOTOPE_FILTER = new OptionalModuleParameter(
	    "Isotope pattern filter",
	    "Search only for compounds with a isotope pattern similar",
	    new IsotopePatternScoreParameters());

    public SingleRowIdentificationParameters() {
	super(new Parameter[] { DATABASE, NEUTRAL_MASS, MAX_RESULTS,
		MZ_TOLERANCE, ISOTOPE_FILTER });
    }
}
