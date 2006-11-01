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

package net.sf.mzmine.methods;

import java.util.Hashtable;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterValue;
import net.sf.mzmine.project.MZmineProject;


/**
 * Classes extending MethodParameters represent a set of parameters used by a Method.
 * They also store values of parameters at the moment when Method ran.
 */
public abstract class MethodParameters {
	
	private Hashtable<Parameter, ParameterValue> values;

	
	/**
	 * Returns array of all method's parameters
	 */
	public abstract Parameter[] getParameters();

	
	/**
	 * Checks if project contains current value for some of the parameters, and initializes this object using those values if present. 
	 *
	 */
	public MethodParameters() {
		// Initialize hashtable for storing values for parameters
		values = new Hashtable<Parameter, ParameterValue>();
		
		// Check if the project contains current value for some of the parameters
		
		
		MZmineProject project = MZmineProject.getCurrentProject();
		for (Parameter p : getParameters()) {
			ParameterValue pVal = project.getParameterValue(p);
			if (pVal!=null) { 
				// Yes, then use the current value to initialize value used in this object
				values.put(p, pVal);
			} else {
				// Else use default value
				values.put(p, p.getDefaultValue());
			}
		}
	}
	
	
	/**
	 * Returns value of a parameter.
	 * Note: returned value is not necessarily the same as current value for this parameter (current value is stored at project level)
	 * @return parameter value or null if value has not been set using setParameterValue method
	 */
	public ParameterValue getParameterValue(Parameter parameter) {
		return values.get(parameter);
	}
	
	/**
	 * Sets value of a parameter.
	 * Also the current value of the parameter at project level is set to this value 
	 */
	public void setParameterValue(Parameter parameter, ParameterValue value) {
		values.put(parameter, value);
		MZmineProject.getCurrentProject().setParameterValue(parameter, value);
	}
	
	/**
	 * Represent method's parameters and their values in human-readable format 
	 */
    public String toString() {
    	String s = "";
		for (Parameter p : getParameters()) {
			s = s.concat(p.getName() + values.get(p) + ", ");
		}
		return s;
	}	
    
}
