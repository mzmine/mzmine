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

package net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.restrictions.rdbe;

import java.text.NumberFormat;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;

import com.google.common.collect.Range;

public class RDBERestrictionParameters extends SimpleParameterSet {

    public static final DoubleRangeParameter rdbeRange = new DoubleRangeParameter(
	    "RDBE range",
	    "Range of allowed RDBE (Range or Double Bonds Equivalents) value",
	    NumberFormat.getNumberInstance(), Range.closed(-1.0, 40.0));

    public static final BooleanParameter rdbeWholeNum = new BooleanParameter(
	    "RDBE must be an integer",
	    "Only integer values are allowed for RDBE", true);

    public RDBERestrictionParameters() {
	super(new Parameter[] { rdbeRange, rdbeWholeNum });
    }

}
