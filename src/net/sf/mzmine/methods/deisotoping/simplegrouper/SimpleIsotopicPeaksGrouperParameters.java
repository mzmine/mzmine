/*
 * Copyright 2006 The MZmine Development Team This file is part of MZmine.
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. MZmine is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with MZmine; if not, write to the Free Software Foundation, Inc., 51
 * Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.methods.deisotoping.simplegrouper;

import java.util.HashSet;

import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.Parameter.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.methods.MethodParameters;

/**
 * This class represents parameter settings for the simple isotopic peaks
 * grouper method
 * 
 * @version 31 March 2006
 */
class SimpleIsotopicPeaksGrouperParameters extends MethodParameters {
  
	protected static final Parameter mzTolerance = new SimpleParameter(	
			ParameterType.DOUBLE,
			"M/Z tolerance",
			"Maximum distance in M/Z from the expected location of a peak",
			"Da",
			new Double(0.05));
	
	protected static final Parameter rtTolerance = new SimpleParameter(	
			ParameterType.DOUBLE,
			"RT tolerance",
			"Maximum distance in RT from the expected location of a peak",
			"seconds",
			new Double(5));
	
	protected static final Parameter monotonicShape = new SimpleParameter(	
			ParameterType.BOOLEAN,
			"Monotonic shape",
			"If true, then monotonically decreasing height of isotope pattern is required (monoisotopic peak is strongest).",
			"",
			new Boolean(true));

	protected static final Parameter maximumCharge = new SimpleParameter(	
			ParameterType.INTEGER,
			"Maximum charge",
			"Maximum charge",
			"",
			new Integer(1));	
    
    
	public Parameter[] getParameters() {   
		return new Parameter[] { mzTolerance, rtTolerance, monotonicShape, maximumCharge };
	}

}