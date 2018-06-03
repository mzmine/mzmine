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

package net.sf.mzmine.modules.peaklistmethods.mymodule.isotopestuff;

import net.sf.mzmine.datamodel.PolarityType;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.FormulaParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.PercentParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

public class IsotopePatternCalculatorParameters2 extends SimpleParameterSet {

    public static final FormulaParameter formula = new FormulaParameter();

    public static final MZToleranceParameter mzTolerance = new MZToleranceParameter();

    public static final IntegerParameter charge = new IntegerParameter(
	    "Charge", "Charge of the molecule (z for calculating m/z values)",
	    1);

    public static final ComboParameter<PolarityType> polarity = new ComboParameter<PolarityType>(
	    "Polarity",
	    "Set positive or negative charge of the molecule. Depending on polarity, electron mass is added or removed.",
	    PolarityType.values());

    public static final PercentParameter minAbundance = new PercentParameter(
	    "Minimum abundance", "Minimum abundance of the predicted isotopes",
	    0.001);

    public IsotopePatternCalculatorParameters2() {
	super(new UserParameter[] { formula, charge, polarity, minAbundance });
    }

}
