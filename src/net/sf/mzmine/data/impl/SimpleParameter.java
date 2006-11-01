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
import net.sf.mzmine.data.ParameterValue;

/**
 * Simple Parameter implementation
 */
public class SimpleParameter extends AbstractDataUnit implements Parameter {

    private ParameterType type;
    private String name, description, units;
    private ParameterValue value, defaultValue, minValue, maxValue, possibleValues[];
    private Parameter numberFormatParameter;

    public SimpleParameter(ParameterType type, String name, String description) {
        this(type, name, description, null, null, null, null, null, null);
    }

    public SimpleParameter(ParameterType type, String name, String description,
            String units) {
        this(type, name, description, units, null, null, null, null, null);
    }

    public SimpleParameter(ParameterType type, String name, String description,
            String units, Parameter numberFormatParameter) {
        this(type, name, description, units, null, null, null, null, numberFormatParameter);
    }

    public SimpleParameter(ParameterType type, String name, String description,
            String units, ParameterValue defaultValue) {
        this(type, name, description, units, defaultValue, null, null, null,
                null);
    }

    public SimpleParameter(ParameterType type, String name, String description,
            String units, ParameterValue defaultValue, Parameter numberFormatParameter) {
        this(type, name, description, units, defaultValue, null, null, null, numberFormatParameter);
    }

    public SimpleParameter(ParameterType type, String name, String description,
            String units, ParameterValue defaultValue, ParameterValue possibleValues[],
            Parameter numberFormatParameter) {
        this(type, name, description, units, defaultValue, null, null,
                possibleValues, numberFormatParameter);
    }

    public SimpleParameter(ParameterType type, String name, String description,
    		ParameterValue defaultValue, ParameterValue possibleValues[]) {
        this(type, name, description, null, defaultValue, null, null,
                possibleValues, null);
    }    
    
    public SimpleParameter(ParameterType type, String name, String description,
            String units, ParameterValue defaultValue, ParameterValue minValue,
            ParameterValue maxValue, Parameter numberFormatParameter) {
        this(type, name, description, units, defaultValue, minValue, maxValue,
                null, numberFormatParameter);
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
   
    private SimpleParameter(ParameterType type, String name,
            String description, String units, ParameterValue defaultValue,
            ParameterValue minValue, ParameterValue maxValue, ParameterValue[] possibleValues,
            Parameter numberFormatParameter) {
        this.type = type;
        this.name = name;
        this.description = description;
        this.units = units;
        this.defaultValue = defaultValue;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.possibleValues = possibleValues;
        this.numberFormatParameter = numberFormatParameter;
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
    public ParameterValue[] getPossibleValues() {
        return possibleValues;
    }
   
    /**
     * @see net.sf.mzmine.data.Parameter#getDefaultValue()
     */
    public ParameterValue getDefaultValue() {
        return defaultValue;
    }

    /**
     * @see net.sf.mzmine.data.Parameter#getMinimumValue()
     */
    public ParameterValue getMinimumValue() {
        return minValue;
    }

    /**
     * @see net.sf.mzmine.data.Parameter#getMaximumValue()
     */
    public ParameterValue getMaximumValue() {
        return maxValue;
    }

    /**
     * @see net.sf.mzmine.data.Parameter#getFormat()
     */
    public Parameter getNumberFormatParameter() {
        return numberFormatParameter;
    }
    
    public String toString() {
    	return getName();
    }

}
