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

package net.sf.mzmine.methods.filtering.savitzkygolay;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.Parameter.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.methods.MethodParameters;

/**
 * This class represents parameter for the Savizky-Golay filter
 */
public class SavitzkyGolayFilterParameters extends MethodParameters {
    
	protected static final Integer datapoints5 = new Integer(5);
	protected static final Integer datapoints7 = new Integer(7);
	protected static final Integer datapoints9 = new Integer(9);
	protected static final Integer datapoints11 = new Integer(11);
	protected static final Integer datapoints13 = new Integer(13);
	protected static final Integer datapoints15 = new Integer(15);
	
    protected static final Integer[] numberOfDatapointsPossibleValues = { datapoints5, datapoints7, datapoints9, datapoints11, datapoints13, datapoints15};
    
	protected static final Parameter numberOfDatapoints = new SimpleParameter(	
			ParameterType.OBJECT,
			"Number of datapoints",
			"Number of datapoints",
			numberOfDatapointsPossibleValues[0],
			numberOfDatapointsPossibleValues);
    

	public Parameter[] getParameters() {
		return new Parameter[] { numberOfDatapoints };
	}

}