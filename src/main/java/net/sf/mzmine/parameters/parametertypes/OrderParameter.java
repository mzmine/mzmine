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

import java.util.Collection;

import net.sf.mzmine.parameters.UserParameter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Simple Parameter implementation
 * 
 * 
 */
public class OrderParameter<ValueType> implements
	UserParameter<ValueType[], OrderComponent<ValueType>> {

    private String name, description;
    private ValueType value[];

    public OrderParameter(String name, String description, ValueType value[]) {

	assert value != null;

	this.name = name;
	this.description = description;
	this.value = value;
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
    public ValueType[] getValue() {
	return value;
    }

    @Override
    public OrderComponent<ValueType> createEditingComponent() {
	return new OrderComponent<ValueType>();
    }

    @Override
    public OrderParameter<ValueType> cloneParameter() {
	OrderParameter<ValueType> copy = new OrderParameter<ValueType>(name,
		description, value);
	copy.setValue(this.getValue());
	return copy;
    }

    @Override
    public void setValueFromComponent(OrderComponent<ValueType> component) {
	Object newOrder[] = component.getValues();
	System.arraycopy(newOrder, 0, this.value, 0, newOrder.length);
    }

    @Override
    public void setValueToComponent(OrderComponent<ValueType> component,
	    ValueType[] newValue) {
	component.setValues(newValue);
    }

    @Override
    public void setValue(ValueType[] newValue) {
	assert newValue != null;
	this.value = newValue;
    }

    @Override
    public void loadValueFromXML(Element xmlElement) {
	NodeList items = xmlElement.getElementsByTagName("item");
	ValueType newValues[] = value.clone();
	for (int i = 0; i < items.getLength(); i++) {
	    String itemString = items.item(i).getTextContent();
	    for (int j = i + 1; j < newValues.length; j++) {
		if (newValues[j].toString().equals(itemString)) {
		    ValueType swap = newValues[i];
		    newValues[i] = newValues[j];
		    newValues[j] = swap;
		}
	    }
	}
	value = newValues;
    }

    @Override
    public void saveValueToXML(Element xmlElement) {
	if (value == null)
	    return;
	Document parentDocument = xmlElement.getOwnerDocument();
	for (ValueType item : value) {
	    Element newElement = parentDocument.createElement("item");
	    newElement.setTextContent(item.toString());
	    xmlElement.appendChild(newElement);
	}
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
