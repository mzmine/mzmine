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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.parameters.parametertypes.tolerances;

import java.util.Collection;

import net.sf.mzmine.parameters.UserParameter;

import org.w3c.dom.Element;

public class RTToleranceParameter implements
	UserParameter<RTTolerance, RTToleranceComponent> {

    private String name, description;
    private RTTolerance value;

    public RTToleranceParameter() {
	this("Retention time tolerance",
		"Maximum allowed difference between two retention time values");
    }

    public RTToleranceParameter(String name, String description) {
	this.name = name;
	this.description = description;
    }

    /**
     * @see net.sf.mzmine.data.Parameter#getName()
     */
    @Override
    public String getName() {
	return name;
    }

    /**
     * @see net.sf.mzmine.data.Parameter#getDescription()
     */
    @Override
    public String getDescription() {
	return description;
    }

    @Override
    public RTToleranceComponent createEditingComponent() {
	return new RTToleranceComponent();
    }

    @Override
    public RTToleranceParameter cloneParameter() {
	RTToleranceParameter copy = new RTToleranceParameter(name, description);
	copy.setValue(this.getValue());
	return copy;
    }

    @Override
    public void setValueFromComponent(RTToleranceComponent component) {
	this.value = component.getValue();
    }

    @Override
    public void setValueToComponent(RTToleranceComponent component,
	    RTTolerance newValue) {
	component.setValue(newValue);
    }

    @Override
    public RTTolerance getValue() {
	return value;
    }

    @Override
    public void setValue(RTTolerance newValue) {
	this.value = newValue;
    }

    @Override
    public void loadValueFromXML(Element xmlElement) {
	String typeAttr = xmlElement.getAttribute("type");
	boolean isAbsolute = !typeAttr.equals("percent");
	String toleranceNum = xmlElement.getTextContent();
	if (toleranceNum.length() == 0)
	    return;
	double tolerance = Double.valueOf(toleranceNum);
	this.value = new RTTolerance(isAbsolute, tolerance);
    }

    @Override
    public void saveValueToXML(Element xmlElement) {
	if (value == null) {
	    return;
	}
	if (value.isAbsolute()) {
	    xmlElement.setAttribute("type", "absolute");
	} else {
	    xmlElement.setAttribute("type", "percent");
	}
	double tolerance = value.getTolerance();
	String toleranceNum = String.valueOf(tolerance);
	xmlElement.setTextContent(toleranceNum);
    }

    @Override
    public boolean checkValue(Collection<String> errorMessages) {
	if (value == null) {
	    errorMessages.add(name + " is not set properly");
	    return false;
	}
	if (value.isAbsolute()) {
	    double absoluteTolerance = value.getTolerance();
	    if (absoluteTolerance < 0) {
		errorMessages.add("Invalid retention time tolerance value.");
		return false;

	    }
	} else {
	    double relativeTolerance = value.getTolerance();
	    if ((relativeTolerance < 0) || (relativeTolerance > 1)) {
		errorMessages.add("Invalid retention time tolerance value.");
		return false;

	    }
	}
	return true;
    }
}
