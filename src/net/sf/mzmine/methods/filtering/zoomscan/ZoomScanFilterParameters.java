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

package net.sf.mzmine.methods.filtering.zoomscan;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.Parameter.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterValue;
import net.sf.mzmine.methods.MethodParameters;

/**
 * This class represents parameter for the zoom scan filter method
 */
public class ZoomScanFilterParameters extends MethodParameters {

	protected static final Parameter minMZRange = new SimpleParameter(	
			ParameterType.DOUBLE,
			"Minimum M/Z range",
			"Required minimum M/Z range for a full scan",
			"Da",
			new SimpleParameterValue(100.0),
			new SimpleParameterValue(0.0),
			null,
			null);
    
	public Parameter[] getParameters() {
		return new Parameter[] { minMZRange };
	}

}