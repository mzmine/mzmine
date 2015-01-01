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

import java.text.DecimalFormat;
import java.util.Collection;

import net.sf.mzmine.parameters.UserParameter;

import org.w3c.dom.Element;

/**
 * Simple Parameter implementation
 * 
 * 
 */
public class NumberFormatParameter implements
	UserParameter<DecimalFormat, NumberFormatEditor> {

    private String name, description;
    private boolean showExponentOption;
    private DecimalFormat value;

    public NumberFormatParameter(String name, String description,
	    boolean showExponentOption, DecimalFormat defaultValue) {

	assert defaultValue != null;

	this.name = name;
	this.description = description;
	this.showExponentOption = showExponentOption;
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
    public NumberFormatEditor createEditingComponent() {
	NumberFormatEditor editor = new NumberFormatEditor(showExponentOption);
	return editor;
    }

    public DecimalFormat getValue() {
	return value;
    }

    @Override
    public void setValue(DecimalFormat value) {
	assert value != null;
	this.value = value;
    }

    @Override
    public NumberFormatParameter cloneParameter() {
	NumberFormatParameter copy = new NumberFormatParameter(name,
		description, showExponentOption, value);
	copy.setValue(this.getValue());
	return copy;
    }

    @Override
    public void setValueFromComponent(NumberFormatEditor component) {
	final int decimals = component.getDecimals();
	final boolean showExponent = component.getShowExponent();
	String pattern = "0";

	if (decimals > 0) {
	    pattern += ".";
	    for (int i = 0; i < decimals; i++)
		pattern += "0";
	}
	if (showExponent) {
	    pattern += "E0";
	}
	value.applyPattern(pattern);
    }

    @Override
    public void setValueToComponent(NumberFormatEditor component,
	    DecimalFormat newValue) {
	final int decimals = newValue.getMinimumFractionDigits();
	boolean showExponent = newValue.toPattern().contains("E");
	component.setValue(decimals, showExponent);
    }

    @Override
    public void loadValueFromXML(Element xmlElement) {
	String newPattern = xmlElement.getTextContent();
	value.applyPattern(newPattern);
    }

    @Override
    public void saveValueToXML(Element xmlElement) {
	xmlElement.setTextContent(value.toPattern());
    }

    @Override
    public boolean checkValue(Collection<String> errorMessages) {
	return true;
    }

}
