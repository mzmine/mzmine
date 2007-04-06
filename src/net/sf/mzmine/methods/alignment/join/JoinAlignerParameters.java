/*
    Copyright 2005 VTT Biotechnology

    This file is part of MZmine.

    MZmine is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    MZmine is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with MZmine; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/

package net.sf.mzmine.methods.alignment.join;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.Parameter.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;


public class JoinAlignerParameters extends SimpleParameterSet {

	protected static final String RTToleranceTypeAbsolute = "Absolute";
	protected static final String RTToleranceTypeRelative = "Relative";
	
	protected static final Object[] RTToleranceTypePossibleValues = {RTToleranceTypeAbsolute , RTToleranceTypeRelative}; 
	
	protected static final Parameter MZvsRTBalance = new SimpleParameter(	ParameterType.DOUBLE,
																			"M/Z vs RT balance",
																			"Used in distance measuring as multiplier of M/Z difference",
																			"",
																			new Double(10.0),
																			new Double(0.0),
																			null
																			);
	
	protected static final Parameter MZTolerance = new SimpleParameter(		ParameterType.DOUBLE,
																			"M/Z tolerance",
																			"Maximum allowed M/Z difference",
																			"Da",
																			new Double(0.2),
																			new Double(0.0),
																			null
																			);
	
	protected static final Parameter RTToleranceType = new SimpleParameter(	ParameterType.STRING,
																			"RT tolerance type",
																			"Maximum RT difference can be defined either using absolute or relative value",
																			RTToleranceTypeAbsolute,
																			RTToleranceTypePossibleValues);
	
	protected static final Parameter RTToleranceValueAbs = new SimpleParameter(	
																			ParameterType.DOUBLE,
																			"Absolute RT tolerance",
																			"Maximum allowed absolute RT difference",
																			"seconds",
																			new Double(15.0),
																			new Double(0.0),
																			null
																			);
	
	protected static final Parameter RTToleranceValuePercent = new SimpleParameter(	
																			ParameterType.DOUBLE,
																			"Relative RT tolerance",
																			"Maximum allowed relative RT difference",
																			"%",
																			new Double(0.15),
																			new Double(0.0),
																			null
																			);
	
	
	public Parameter[] getParameters() {
		return new Parameter[] {MZvsRTBalance, MZTolerance, RTToleranceType, RTToleranceValueAbs, RTToleranceValuePercent};
	}
	


}