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

package net.sf.mzmine.modules.peaklistmethods.dataanalysis.projectionplots;

import java.util.ArrayList;
import java.util.Collection;

import javax.swing.JComboBox;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.UserParameter;

import org.w3c.dom.Element;

/**
 * Simple Parameter implementation
 * 
 * 
 */
public class ColoringTypeParameter implements
	UserParameter<ColoringType, JComboBox<ColoringType>> {

    private String name, description;
    private ColoringType value;

    public ColoringTypeParameter() {
	this.name = "Coloring type";
	this.description = "Defines how points will be colored";
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
    public JComboBox<ColoringType> createEditingComponent() {
	ArrayList<ColoringType> choicesList = new ArrayList<ColoringType>();
	choicesList.add(ColoringType.NOCOLORING);
	choicesList.add(ColoringType.COLORBYFILE);
	for (UserParameter<?, ?> p : MZmineCore.getProjectManager()
		.getCurrentProject().getParameters()) {
	    choicesList.add(new ColoringType(p));
	}
	ColoringType choices[] = choicesList.toArray(new ColoringType[0]);
	JComboBox<ColoringType> editor = new JComboBox<ColoringType>(choices);
	if (value != null)
	    editor.setSelectedItem(value);
	return editor;
    }

    @Override
    public ColoringType getValue() {
	return value;
    }

    @Override
    public void setValue(ColoringType value) {
	this.value = value;
    }

    @Override
    public ColoringTypeParameter cloneParameter() {
	ColoringTypeParameter copy = new ColoringTypeParameter();
	copy.setValue(this.getValue());
	return copy;
    }

    @Override
    public void setValueFromComponent(JComboBox<ColoringType> component) {
	value = (ColoringType) component.getSelectedItem();
    }

    @Override
    public void setValueToComponent(JComboBox<ColoringType> component,
	    ColoringType newValue) {
	component.setSelectedItem(newValue);
    }

    @Override
    public void loadValueFromXML(Element xmlElement) {
	String elementString = xmlElement.getTextContent();
	if (elementString.length() == 0)
	    return;
	String attrValue = xmlElement.getAttribute("type");
	if (attrValue.equals("parameter")) {
	    for (UserParameter<?, ?> p : MZmineCore.getProjectManager()
		    .getCurrentProject().getParameters()) {
		if (p.getName().equals(elementString)) {
		    value = new ColoringType(p);
		    break;
		}
	    }
	} else {
	    value = new ColoringType(elementString);
	}
    }

    @Override
    public void saveValueToXML(Element xmlElement) {
	if (value == null)
	    return;
	if (value.isByParameter()) {
	    xmlElement.setAttribute("type", "parameter");
	    xmlElement.setTextContent(value.getParameter().getName());
	} else {
	    xmlElement.setTextContent(value.toString());
	}

    }

    @Override
    public boolean checkValue(Collection<String> errorMessages) {
	return true;
    }

}
