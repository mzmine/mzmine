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
package net.sf.mzmine.methods.deisotoping.incompletefilter;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.Parameter.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterValue;
import net.sf.mzmine.methods.MethodParameters;
import net.sf.mzmine.userinterface.DesktopParameters;


public class IncompleteIsotopePatternFilterParameters extends MethodParameters {

	protected static final Parameter minimumNumberOfPeaks = new SimpleParameter(		
			ParameterType.INTEGER,
			"Minimum number of peaks",
			"Minimum acceptable number of peaks per isotope pattern",
			"",
			new SimpleParameterValue(2),
			new SimpleParameterValue(1),
			null,
			DesktopParameters.integerFormatParameter);
	
	

	public Parameter[] getParameters() {
		return new Parameter[] {minimumNumberOfPeaks};
	}



}