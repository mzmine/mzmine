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

import javax.swing.JComponent;

import net.sf.mzmine.parameters.UserParameter;

import org.w3c.dom.Element;

/**
 * Parameter represented by check box with an additional sub-parameter
 * 
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class OptionalParameter<EmbeddedParameterType extends UserParameter<?, ?>>
        implements UserParameter<Boolean, OptionalParameterComponent<?>> {

    private EmbeddedParameterType embeddedParameter;

    // It is important to set default value here, otherwise the embedded value
    // is not shown in the parameter setup dialog
    private Boolean value = false;

    public OptionalParameter(EmbeddedParameterType embeddedParameter) {
        this.embeddedParameter = embeddedParameter;
    }

    public EmbeddedParameterType getEmbeddedParameter() {
        return embeddedParameter;
    }

    /**
     * @see net.sf.mzmine.data.Parameter#getName()
     */
    @Override
    public String getName() {
        return embeddedParameter.getName();
    }

    /**
     * @see net.sf.mzmine.data.Parameter#getDescription()
     */
    @Override
    public String getDescription() {
        return embeddedParameter.getDescription();
    }

    @Override
    public OptionalParameterComponent<?> createEditingComponent() {
        return new OptionalParameterComponent(embeddedParameter);
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
    public OptionalParameter cloneParameter() {
        final UserParameter<?, ?> embeddedParameterClone = embeddedParameter
                .cloneParameter();
        final OptionalParameter copy = new OptionalParameter(
                embeddedParameterClone);
        copy.setValue(this.getValue());
        return copy;
    }

    public void setValueFromComponent(OptionalParameterComponent component) {
        this.value = component.isSelected();
        if (value) {
            JComponent embeddedComponent = component.getEmbeddedComponent();
            ((UserParameter) this.embeddedParameter)
                    .setValueFromComponent(embeddedComponent);
        }
    }

    @Override
    public void setValueToComponent(OptionalParameterComponent<?> component,
            Boolean newValue) {
        component.setSelected(newValue);
        if (embeddedParameter.getValue() != null) {
            ((UserParameter) this.embeddedParameter).setValueToComponent(
                    component.getEmbeddedComponent(),
                    embeddedParameter.getValue());
        }
    }

    @Override
    public void loadValueFromXML(Element xmlElement) {
        embeddedParameter.loadValueFromXML(xmlElement);
        String selectedAttr = xmlElement.getAttribute("selected");
        this.value = Boolean.valueOf(selectedAttr);
    }

    @Override
    public void saveValueToXML(Element xmlElement) {
        if (value != null)
            xmlElement.setAttribute("selected", value.toString());
        embeddedParameter.saveValueToXML(xmlElement);
    }

    @Override
    public boolean checkValue(Collection<String> errorMessages) {
        if (value == null) {
            errorMessages.add(getName() + " is not set properly");
            return false;
        }
        if (value == true) {
            return embeddedParameter.checkValue(errorMessages);
        }
        return true;
    }

}
