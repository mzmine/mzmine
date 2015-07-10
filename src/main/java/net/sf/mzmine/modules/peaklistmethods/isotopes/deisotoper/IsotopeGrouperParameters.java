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

package net.sf.mzmine.modules.peaklistmethods.isotopes.deisotoper;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;

public class IsotopeGrouperParameters extends SimpleParameterSet {

    public static final String ChooseTopIntensity = "Most intense";
    public static final String ChooseLowestMZ = "Lowest m/z";

    public static final String[] representativeIsotopeValues = {
	    ChooseTopIntensity, ChooseLowestMZ };

    public static final PeakListsParameter peakLists = new PeakListsParameter();

    public static final StringParameter suffix = new StringParameter(
	    "Name suffix", "Suffix to be added to peak list name", "deisotoped");

    public static final MZToleranceParameter mzTolerance = new MZToleranceParameter();

    public static final RTToleranceParameter rtTolerance = new RTToleranceParameter();

    public static final BooleanParameter monotonicShape = new BooleanParameter(
	    "Monotonic shape",
	    "If true, then monotonically decreasing height of isotope pattern is required");

    public static final IntegerParameter maximumCharge = new IntegerParameter(
	    "Maximum charge",
	    "Maximum charge to consider for detecting the isotope patterns");

    public static final ComboParameter<String> representativeIsotope = new ComboParameter<String>(
	    "Representative isotope",
	    "Which peak should represent the whole isotope pattern. For small molecular weight\n"
	    + "compounds with monotonically decreasing isotope pattern, the most intense isotope\n"
	    + "should be representative. For high molecular weight peptides, the lowest m/z\n"
	    + "peptides, the lowest m/z isotope may be the representative.",
	    representativeIsotopeValues);

    public static final BooleanParameter autoRemove = new BooleanParameter(
	    "Remove original peaklist",
	    "If checked, original peaklist will be removed and only deisotoped version remains");

    public IsotopeGrouperParameters() {
	super(new Parameter[] { peakLists, suffix, mzTolerance, rtTolerance,
		monotonicShape, maximumCharge, representativeIsotope,
		autoRemove });
    }

}
