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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class MZToleranceParameter implements
	UserParameter<MZTolerance, MZToleranceComponent> {

    private String name, description;
    private MZTolerance value;

    public MZToleranceParameter() {
	this(
		"m/z tolerance",
		"Maximum allowed difference between two m/z values to be considered same.\n"
			+ "The value is specified both as absolute tolerance (in m/z) and relative tolerance (in ppm).\n"
			+ "The tolerance range is calculated using maximum of the absolute and relative tolerances.");
    }

    public MZToleranceParameter(String name, String description) {
	this.name = name;
	this.description = description;
    }

    @Override
    public String getName() {
	return name;
    }

    @Override
    public String getDescription() {
	return description;
    }

    @Override
    public MZToleranceComponent createEditingComponent() {
	return new MZToleranceComponent();
    }

    @Override
    public MZToleranceParameter cloneParameter() {
	MZToleranceParameter copy = new MZToleranceParameter(name, description);
	copy.setValue(this.getValue());
	return copy;
    }

    @Override
    public void setValueFromComponent(MZToleranceComponent component) {
	value = component.getValue();
    }

    @Override
    public void setValueToComponent(MZToleranceComponent component,
	    MZTolerance newValue) {
	component.setValue(newValue);
    }

    @Override
    public MZTolerance getValue() {
	return value;
    }

    @Override
    public void setValue(MZTolerance newValue) {
	this.value = newValue;
    }

    @Override
    public void loadValueFromXML(Element xmlElement) {
	// Set some default values
	double mzTolerance = 0.001;
	double ppmTolerance = 5;
	NodeList items = xmlElement.getElementsByTagName("absolutetolerance");
	for (int i = 0; i < items.getLength(); i++) {
	    String itemString = items.item(i).getTextContent();
	    mzTolerance = Double.parseDouble(itemString);
	}
	items = xmlElement.getElementsByTagName("ppmtolerance");
	for (int i = 0; i < items.getLength(); i++) {
	    String itemString = items.item(i).getTextContent();
	    ppmTolerance = Double.parseDouble(itemString);
	}

	this.value = new MZTolerance(mzTolerance, ppmTolerance);
    }

    @Override
    public void saveValueToXML(Element xmlElement) {
	if (value == null)
	    return;
	Document parentDocument = xmlElement.getOwnerDocument();
	Element newElement = parentDocument.createElement("absolutetolerance");
	newElement.setTextContent(String.valueOf(value.getMzTolerance()));
	xmlElement.appendChild(newElement);
	newElement = parentDocument.createElement("ppmtolerance");
	newElement.setTextContent(String.valueOf(value.getPpmTolerance()));
	xmlElement.appendChild(newElement);
    }

    @Override
    public boolean checkValue(Collection<String> errorMessages) {
	if (value == null) {
	    errorMessages.add(name + " is not set properly");
	    return false;
	}
	if ((value.getMzTolerance() <= 0.0) && (value.getPpmTolerance() <= 0.0)) {
	    errorMessages.add(name + " must be greater than zero");
	    return false;
	}
	return true;
    }

}
