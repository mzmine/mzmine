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

package net.sf.mzmine.desktop.preferences;

import java.util.Collection;

import net.sf.mzmine.parameters.UserParameter;

import org.w3c.dom.Element;

/**
 * Simple Parameter implementation
 * 
 * 
 */
public class NumOfThreadsParameter implements
	UserParameter<Integer, NumOfThreadsEditor> {

    private String name, description;
    private boolean automatic;
    private Integer value;

    public NumOfThreadsParameter() {
	this.name = "Number of concurrently running tasks";
	this.description = "Maximum number of tasks running simultaneously";
	this.value = Runtime.getRuntime().availableProcessors();
	this.automatic = true;
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
    public NumOfThreadsEditor createEditingComponent() {
	NumOfThreadsEditor editor = new NumOfThreadsEditor();
	editor.setValue(automatic, value);
	return editor;
    }

    @Override
    public Integer getValue() {
	return value;
    }

    public boolean isAutomatic() {
	return automatic;
    }

    @Override
    public void setValue(Integer value) {
	assert value != null;
	this.value = value;
    }

    @Override
    public NumOfThreadsParameter cloneParameter() {
	return this;
    }

    @Override
    public void setValueFromComponent(NumOfThreadsEditor component) {
	automatic = component.isAutomatic();
	if (automatic) {
	    value = Runtime.getRuntime().availableProcessors();
	} else {
	    Number componentValue = component.getNumOfThreads();
	    if (componentValue == null)
		value = null;
	    else
		value = componentValue.intValue();
	}
    }

    @Override
    public void setValueToComponent(NumOfThreadsEditor component,
	    Integer newValue) {
	component.setValue(automatic, newValue);
    }

    @Override
    public void loadValueFromXML(Element xmlElement) {
	String attrValue = xmlElement.getAttribute("isautomatic");
	if (attrValue.length() > 0) {
	    this.automatic = Boolean.valueOf(attrValue);
	}

	String textContent = xmlElement.getTextContent();
	if (textContent.length() > 0) {
	    this.value = Integer.valueOf(textContent);
	}
    }

    @Override
    public void saveValueToXML(Element xmlElement) {
	xmlElement.setAttribute("isautomatic", String.valueOf(automatic));
	xmlElement.setTextContent(value.toString());
    }

    @Override
    public boolean checkValue(Collection<String> errorMessages) {
	if (value == null) {
	    errorMessages.add(name + " is not set");
	    return false;
	}
	return true;
    }

}
