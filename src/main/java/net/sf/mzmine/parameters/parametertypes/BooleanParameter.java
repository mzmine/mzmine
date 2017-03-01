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

package net.sf.mzmine.parameters.parametertypes;

import java.awt.Insets;
import java.util.Collection;

import javax.swing.JCheckBox;

import org.w3c.dom.Element;

import net.sf.mzmine.parameters.UserParameter;

/**
 * Simple Parameter implementation
 * 
 * 
 */
public class BooleanParameter implements UserParameter<Boolean, JCheckBox> {

    private String name, description;
    private Boolean value;

    public BooleanParameter(String name, String description) {
	this(name, description, null);
    }

    public BooleanParameter(String name, String description,
	    Boolean defaultValue) {
	this.name = name;
	this.description = description;
	this.value = defaultValue;
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
    public JCheckBox createEditingComponent() {
    JCheckBox checkBox = new JCheckBox();
        checkBox.setMargin(new Insets(0, 7, 0, 0));
	return checkBox;
    }

    @Override
    public Boolean getValue() {
	return value;
    }

    @Override
    public void setValue(Boolean value) {
	this.value = value;
    }

    @Override
    public BooleanParameter cloneParameter() {
	BooleanParameter copy = new BooleanParameter(name, description);
	copy.setValue(this.getValue());
	return copy;
    }

    @Override
    public void setValueFromComponent(JCheckBox component) {
	value = component.isSelected();
    }

    @Override
    public void setValueToComponent(JCheckBox component, Boolean newValue) {
	component.setSelected(newValue);
    }

    @Override
    public void loadValueFromXML(Element xmlElement) {
	String rangeString = xmlElement.getTextContent();
	if (rangeString.length() == 0)
	    return;
	this.value = Boolean.valueOf(rangeString);
    }

    @Override
    public void saveValueToXML(Element xmlElement) {
	if (value == null)
	    return;
	xmlElement.setTextContent(value.toString());
    }

    @Override
    public boolean checkValue(Collection<String> errorMessages) {
	if (value == null) {
	    errorMessages.add(name + " is not set properly");
	    return false;
	}
	return true;
    }

}
