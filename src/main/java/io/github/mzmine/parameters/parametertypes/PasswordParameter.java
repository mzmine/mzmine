/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.parameters.parametertypes;

import org.w3c.dom.Element;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.util.exceptions.DecryptionException;
import io.github.mzmine.util.exceptions.EncryptionException;

import javax.swing.*;
import java.util.Collection;

/**
 * Password parameter
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class PasswordParameter
        implements UserParameter<String, PasswordComponent> {

    private String name, description, value;
    private int inputsize = 20;
    private boolean valueRequired = true;

    public PasswordParameter(String name, String description) {
        this(name, description, null);
    }

    public PasswordParameter(String name, String description, int inputsize) {
        this.name = name;
        this.description = description;
        this.inputsize = inputsize;
    }

    public PasswordParameter(String name, String description,
            String defaultValue) {
        this.name = name;
        this.description = description;
        this.value = defaultValue;
    }

    public PasswordParameter(String name, String description,
            boolean valueRequired) {
        this.name = name;
        this.description = description;
        this.valueRequired = valueRequired;
    }

    public PasswordParameter(String name, String description,
            String defaultValue, boolean valueRequired) {
        this.name = name;
        this.description = description;
        this.value = defaultValue;
        this.valueRequired = valueRequired;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public void setValue(String newValue) {
        this.value = newValue;
    }

    @Override
    public boolean checkValue(Collection<String> errorMessages) {
        if (!valueRequired)
            return true;
        if ((value == null) || (value.trim().isEmpty())) {
            errorMessages.add(name + " is not set properly");
            return false;
        }
        return true;
    }

    @Override
    public void loadValueFromXML(Element xmlElement) {
        try {
            final String nuValue = xmlElement.getTextContent();
            if (nuValue == null || nuValue.trim().isEmpty())
                return;

            value = MZmineCore.getConfiguration().getEncrypter()
                    .decrypt(nuValue);
        } catch (DecryptionException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void saveValueToXML(Element xmlElement) {
        if (value == null || value.isEmpty())
            return;
        try {
            xmlElement.setTextContent(MZmineCore.getConfiguration()
                    .getEncrypter().encrypt(value));
        } catch (EncryptionException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isSensitive() {
        return true;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public PasswordComponent createEditingComponent() {
        PasswordComponent passwordComponent = new PasswordComponent(inputsize);
        passwordComponent.setBorder(BorderFactory.createCompoundBorder(
                passwordComponent.getBorder(),
                BorderFactory.createEmptyBorder(0, 4, 0, 0)));
        return passwordComponent;
    }

    @Override
    public void setValueFromComponent(PasswordComponent component) {
        value = component.getText().toString();
    }

    @Override
    public void setValueToComponent(PasswordComponent component,
            String newValue) {
        component.setText(newValue);
    }

    @Override
    public PasswordParameter cloneParameter() {
        PasswordParameter copy = new PasswordParameter(name, description);
        copy.setValue(this.getValue());
        return copy;
    }

}
