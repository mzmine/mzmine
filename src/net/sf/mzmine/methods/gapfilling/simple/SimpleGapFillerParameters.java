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

package net.sf.mzmine.methods.gapfilling.simple;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterValue;
import net.sf.mzmine.data.Parameter.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterValue;
import net.sf.mzmine.methods.MethodParameters;
import net.sf.mzmine.userinterface.DesktopParameters;

public class SimpleGapFillerParameters extends MethodParameters {

	protected static final ParameterValue RTToleranceTypeAbsolute = new SimpleParameterValue("Absolute");
	protected static final ParameterValue RTToleranceTypeRelative = new SimpleParameterValue("Relative");
	protected static final ParameterValue[] RTToleranceTypePossibleValues = {RTToleranceTypeAbsolute , RTToleranceTypeRelative}; 
	
	
    protected static final Parameter IntTolerance = new SimpleParameter(	
			ParameterType.DOUBLE,
			"Intensity tolerance",
			"Maximum allowed deviation from expected /\\ shape of a peak in chromatographic direction",
			"%",
			new SimpleParameterValue(0.20), 
			new SimpleParameterValue(0.0),
			null,
			DesktopParameters.percentFormatParameter); 

    protected static final Parameter MZTolerance = new SimpleParameter(	
			ParameterType.DOUBLE,
			"M/Z tolerance",
			"Search range size in M/Z direction",
			"Da",
			new SimpleParameterValue(0.050),
			new SimpleParameterValue(0.0),
			null,
			DesktopParameters.mzNumberFormatParameter);
    
	protected static final Parameter RTToleranceType = new SimpleParameter(	ParameterType.OBJECT,
			"RT range type",
			"How to determine search range size in RT direction",
			RTToleranceTypeAbsolute,
			RTToleranceTypePossibleValues);

	protected static final Parameter RTToleranceValueAbs = new SimpleParameter(	
			ParameterType.DOUBLE,
			"Absolute RT tolerance",
			"Absolute search range size in RT direction",
			"seconds",
			new SimpleParameterValue(15.0),
			new SimpleParameterValue(0.0),
			null,
			DesktopParameters.decimalFormatParameter);

	protected static final Parameter RTToleranceValuePercent = new SimpleParameter(	
			ParameterType.DOUBLE,
			"Relative RT tolerance",
			"Relative search range size in RT direction",
			"%",
			new SimpleParameterValue(0.15),
			new SimpleParameterValue(0.0),
			null,
			DesktopParameters.percentFormatParameter);

	public Parameter[] getParameters() {
		return new Parameter[] {IntTolerance, MZTolerance, RTToleranceType, RTToleranceValueAbs, RTToleranceValuePercent};
	}
	
}