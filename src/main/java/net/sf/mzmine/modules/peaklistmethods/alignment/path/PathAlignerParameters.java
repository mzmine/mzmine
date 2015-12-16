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

package net.sf.mzmine.modules.peaklistmethods.alignment.path;

import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopepatternscore.IsotopePatternScoreParameters;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.OptionalModuleParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;

public class PathAlignerParameters extends SimpleParameterSet {

    public static final PeakListsParameter peakLists = new PeakListsParameter();

    public static final StringParameter peakListName = new StringParameter(
	    "Peak list name", "Peak list name");

    public static final MZToleranceParameter MZTolerance = new MZToleranceParameter(
	    "m/z tolerance", "Maximum allowed M/Z difference");

    public static final RTToleranceParameter RTTolerance = new RTToleranceParameter();

    public static final BooleanParameter SameChargeRequired = new BooleanParameter(
	    "Require same charge state",
	    "If checked, only rows having same charge state can be aligned");

    public static final BooleanParameter SameIDRequired = new BooleanParameter(
	    "Require same ID",
	    "If checked, only rows having same compound identities (or no identities) can be aligned");

    public static final OptionalModuleParameter compareIsotopePattern = new OptionalModuleParameter(
	    "Compare isotope pattern",
	    "If both peaks represent an isotope pattern, add isotope pattern score to match score",
	    new IsotopePatternScoreParameters());

    public PathAlignerParameters() {
	super(new Parameter[] { peakLists, peakListName, MZTolerance,
		RTTolerance, SameChargeRequired, SameIDRequired,
		compareIsotopePattern });
    }
}
