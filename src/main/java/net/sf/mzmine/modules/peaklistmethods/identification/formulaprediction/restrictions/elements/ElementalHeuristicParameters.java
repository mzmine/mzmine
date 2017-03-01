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

package net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.restrictions.elements;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;

public class ElementalHeuristicParameters extends SimpleParameterSet {

    public static final BooleanParameter checkHC = new BooleanParameter(
	    "H/C ratio", "0.1 <= H/C ratio <= 6", true);

    public static final BooleanParameter checkNOPS = new BooleanParameter(
	    "NOPS/C ratios",
	    "N/C ratio <= 4, O/C ratio <= 3, P/C ratio <= 2, S/C ratio <= 3",
	    true);

    public static final BooleanParameter checkMultiple = new BooleanParameter(
	    "Multiple element counts",
	    "Check for multiple element count restrictions. See help for detailed description of this rule",
	    true);

    public ElementalHeuristicParameters() {
	super(new Parameter[] { checkHC, checkNOPS, checkMultiple });
    }

}
