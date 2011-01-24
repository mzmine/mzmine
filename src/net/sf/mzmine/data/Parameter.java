/*
 * Copyright 2006-2011 The MZmine 2 Development Team
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

package net.sf.mzmine.data;

import java.text.NumberFormat;

/**
 * Parameter interface, represents parameters or variables used in the project
 */
public interface Parameter {

    /**
     * Returns the parameter type
     * 
     * @return Parameter type
     */
    public ParameterType getType();
    
    /**
     * Returns this parameter's name. The name must be unique within one ParameterSet.
     * 
     * @return Parameter name
     */
    public String getName();

    /**
     * 
     * @return Detailed description of the parameter
     */
    public String getDescription();
    
    /**
     * 
     * @return Symbol for units of the parameter or null
     */
    public String getUnits();
  
    /**
     * 
     * @return Default value of this parameter or null
     */
    public Object getDefaultValue();
    
    /**
     * 
     * @return Array of possible values of this parameter or null
     */
    public Object[] getPossibleValues();

    /**
     * 
     * @return Minimum possible value of this parameter or null
     */
    public Object getMinimumValue();

    /**
     * 
     * @return Maximum possible value of this parameter or null
     */
    public Object getMaximumValue();
    
    /**
     * 
     * @return Parameter for the NumberFormat suitable for this parameter or null
     */
    public NumberFormat getNumberFormat();
    

}
