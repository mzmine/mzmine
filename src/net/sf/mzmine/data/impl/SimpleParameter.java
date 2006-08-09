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

package net.sf.mzmine.data.impl;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.Parameter.ParameterType;


/**
 * Simple Parameter implementation
 */
public class SimpleParameter implements Parameter {

    private ParameterType type;
    private String name, description;
    private Object minValue, maxValue, possibleValues[];
    
    /**
     * @param type
     * @param name
     * @param description
     */
    public SimpleParameter(ParameterType type, String name, String description) {
        this.type = type;
        this.name = name;
        this.description = description;
    }

    /**
     * @param type
     * @param name
     * @param description
     * @param possibleValues
     */
    public SimpleParameter(ParameterType type, String name, String description, Object[] possibleValues) {
        this.type = type;
        this.name = name;
        this.description = description;
        this.possibleValues = possibleValues;
    }

    /**
     * @param type
     * @param name
     * @param description
     * @param minValue
     * @param maxValue
     */
    public SimpleParameter(ParameterType type, String name, String description, Object minValue, Object maxValue) {
        this.type = type;
        this.name = name;
        this.description = description;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    /**
     * @see net.sf.mzmine.data.Parameter#getType()
     */
    public ParameterType getType() {
        return type;
    }

    /**
     * @see net.sf.mzmine.data.Parameter#getName()
     */
    public String getName() {
        return name;
    }

    /**
     * @see net.sf.mzmine.data.Parameter#getDescription()
     */
    public String getDescription() {
        return description;
    }

    /**
     * @see net.sf.mzmine.data.Parameter#getPossibleValues()
     */
    public Object[] getPossibleValues() {
        return possibleValues;
    }

    /**
     * @see net.sf.mzmine.data.Parameter#getMinimumValue()
     */
    public Object getMinimumValue() {
        return minValue;
    }

    /**
     * @see net.sf.mzmine.data.Parameter#getMaximumValue()
     */
    public Object getMaximumValue() {
        return maxValue;
    }

}
