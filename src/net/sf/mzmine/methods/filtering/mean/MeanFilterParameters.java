/*
 * Copyright 2006 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.methods.filtering.mean;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.Parameter.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterValue;
import net.sf.mzmine.methods.MethodParameters;
import net.sf.mzmine.userinterface.DesktopParameters;

/**
 * This class represents parameter for the mean filter method
 */
public class MeanFilterParameters extends MethodParameters {


	protected static final Parameter oneSidedWindowLength = new SimpleParameter(		
			ParameterType.DOUBLE,
			"Window length",
			"One-sided length of the smoothing window",
			"Da",
			new SimpleParameterValue(0.1),
			new SimpleParameterValue(0.0),
			null,
			DesktopParameters.mzNumberFormatParameter);	
    


	public Parameter[] getParameters() {
		return new Parameter[] {oneSidedWindowLength};
	}

}