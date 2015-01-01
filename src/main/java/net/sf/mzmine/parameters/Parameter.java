/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

package net.sf.mzmine.parameters;

import java.util.Collection;

import org.w3c.dom.Element;

/**
 * Parameter interface, represents parameters or variables used in the project
 */
public interface Parameter<ValueType> {

    /**
     * Returns this parameter's name. The name must be unique within one
     * ParameterSet.
     * 
     * @return Parameter name
     */
    public String getName();

    public ValueType getValue();

    public void setValue(ValueType newValue);

    public boolean checkValue(Collection<String> errorMessages);

    public void loadValueFromXML(Element xmlElement);

    public void saveValueToXML(Element xmlElement);

    /**
     * We use cloneParameter() instead of clone() to force the implementing
     * classes to implement this method. Plain clone() is automatically
     * implemented by the Object class.
     */
    public Parameter<ValueType> cloneParameter();

}
