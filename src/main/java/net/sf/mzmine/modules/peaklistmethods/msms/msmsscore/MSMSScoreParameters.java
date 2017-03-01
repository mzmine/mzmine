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

package net.sf.mzmine.modules.peaklistmethods.msms.msmsscore;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.MassListParameter;
import net.sf.mzmine.parameters.parametertypes.PercentParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

public class MSMSScoreParameters extends SimpleParameterSet {

    public static final MassListParameter massList = new MassListParameter();

    public static final MZToleranceParameter msmsTolerance = new MZToleranceParameter(
	    "MS/MS m/z tolerance",
	    "Tolerance of the mass value to search (+/- range)");

    public static final PercentParameter msmsMinScore = new PercentParameter(
	    "MS/MS score threshold",
	    "If the score for MS/MS is lower, discard this match");

    public MSMSScoreParameters() {
	super(new Parameter[] { massList, msmsTolerance, msmsMinScore });
    }

}
