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

import javax.swing.BorderFactory;

import org.w3c.dom.Element;

import net.sf.mzmine.parameters.UserParameter;

public class StringParameter implements UserParameter<String, StringComponent> {

    private String name, description, value;
    private int inputsize = 20;
    private boolean valueRequired = true;

    public StringParameter(String name, String description) {
        this(name, description, null);
    }

    public StringParameter(String name, String description, int inputsize) {
        this.name = name;
        this.description = description;
        this.inputsize = inputsize;
    }

    public StringParameter(String name, String description,
            String defaultValue) {
        this.name = name;
        this.description = description;
        this.value = defaultValue;
    }

    public StringParameter(String name, String description, String defaultValue,
            boolean valueRequired) {
        this.name = name;
        this.description = description;
        this.value = defaultValue;
        this.valueRequired = valueRequired;
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
    public StringComponent createEditingComponent() {
        StringComponent stringComponent = new StringComponent(inputsize);
        stringComponent.setBorder(
                BorderFactory.createCompoundBorder(stringComponent.getBorder(),
                        BorderFactory.createEmptyBorder(0, 4, 0, 0)));
        return stringComponent;
    }

    public String getValue() {
        return value;
    }

    @Override
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public StringParameter cloneParameter() {
        StringParameter copy = new StringParameter(name, description);
        copy.setValue(this.getValue());
        return copy;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public void setValueFromComponent(StringComponent component) {
        value = component.getText();
    }

    @Override
    public void setValueToComponent(StringComponent component,
            String newValue) {
        component.setText(newValue);
    }

    @Override
    public void loadValueFromXML(Element xmlElement) {
        value = xmlElement.getTextContent();
    }

    @Override
    public void saveValueToXML(Element xmlElement) {
        if (value == null)
            return;
        xmlElement.setTextContent(value);
    }

    @Override
    public boolean checkValue(Collection<String> errorMessages) {
        if (!valueRequired)
            return true;
        if ((value == null) || (value.trim().length() == 0)) {
            errorMessages.add(name + " is not set properly");
            return false;
        }
        return true;
    }

}
