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

import java.text.NumberFormat;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.Parameter.ParameterType;

/**
 * Simple Parameter implementation
 */
public class SimpleParameter implements Parameter {

    private ParameterType type;
    private String name, description, units;
    private Object defaultValue, minValue, maxValue, possibleValues[];
    private NumberFormat format;


    public SimpleParameter(ParameterType type, String name, String description, String units) {
        this(type, name, description, units, null, null, null, null, null);
    }
    
    public SimpleParameter(ParameterType type, String name, String description, String units, NumberFormat format) {
        this(type, name, description, units, null, null, null, null, format);
    }

    public SimpleParameter(ParameterType type, String name, String description, String units, Object defaultValue) {
        this(type, name, description, units, defaultValue, null, null, null, null);
    }
    
    public SimpleParameter(ParameterType type, String name, String description, String units, Object defaultValue, NumberFormat format) {
        this(type, name, description, units, defaultValue, null, null, null, format);
    }

    public SimpleParameter(ParameterType type, String name, String description, String units, Object defaultValue, Object possibleValues[], NumberFormat format) {
        this(type, name, description, units, defaultValue, null, null, possibleValues, format);
    }
    
    public SimpleParameter(ParameterType type, String name, String description, String units, Object defaultValue, Object minValue, Object maxValue, NumberFormat format) {
        this(type, name, description, units, defaultValue, minValue, maxValue, null, format);
    }

    /**
     * @param type
     * @param name
     * @param description
     * @param units
     * @param defaultValue
     * @param minValue
     * @param maxValue
     * @param possibleValues
     * @param format
     */
    private SimpleParameter(ParameterType type, String name, String description, String units, Object defaultValue, Object minValue, Object maxValue, Object[] possibleValues, NumberFormat format) {
        this.type = type;
        this.name = name;
        this.description = description;
        this.units = units;
        this.defaultValue = defaultValue;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.possibleValues = possibleValues;
        this.format = format;
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
     * @see net.sf.mzmine.data.Parameter#getUnits()
     */
    public String getUnits() {
        return units;
    }

    /**
     * @see net.sf.mzmine.data.Parameter#getPossibleValues()
     */
    public Object[] getPossibleValues() {
        return possibleValues;
    }

    /**
     * @see net.sf.mzmine.data.Parameter#getDefaultValue()
     */
    public Object getDefaultValue() {
        return defaultValue;
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

    /**
     * @see net.sf.mzmine.data.Parameter#getFormat()
     */
    public NumberFormat getFormat() {
        return format;
    }


}
