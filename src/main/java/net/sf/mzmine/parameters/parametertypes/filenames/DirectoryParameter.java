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

/* Code created was by or on behalf of Syngenta and is released under the open source license in use for the
 * pre-existing code or project. Syngenta does not assert ownership or copyright any over pre-existing work.
 */
package net.sf.mzmine.parameters.parametertypes.filenames;

import java.io.File;
import java.util.Collection;

import net.sf.mzmine.parameters.UserParameter;

import org.w3c.dom.Element;

/**
 * A parameter that represents a file system directory.
 */
public class DirectoryParameter implements
	UserParameter<File, DirectoryComponent> {

    private final String name;
    private final String description;
    private File value;

    public DirectoryParameter(final String aName, final String aDescription) {

	name = aName;
	description = aDescription;
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
    public DirectoryComponent createEditingComponent() {

	return new DirectoryComponent();
    }

    @Override
    public File getValue() {

	return value;
    }

    @Override
    public void setValue(final File newValue) {

	value = newValue;
    }

    @Override
    public DirectoryParameter cloneParameter() {

	final DirectoryParameter copy = new DirectoryParameter(name,
		description);
	copy.setValue(getValue());
	return copy;
    }

    @Override
    public void setValueFromComponent(final DirectoryComponent component) {

	value = component.getValue();
    }

    @Override
    public void setValueToComponent(final DirectoryComponent component,
	    final File newValue) {

	component.setValue(newValue);
    }

    @Override
    public void loadValueFromXML(final Element xmlElement) {

	final String fileString = xmlElement.getTextContent();
	if (fileString.length() != 0) {

	    value = new File(fileString);
	}
    }

    @Override
    public void saveValueToXML(final Element xmlElement) {

	if (value != null) {

	    xmlElement.setTextContent(value.getPath());
	}
    }

    @Override
    public boolean checkValue(final Collection<String> errorMessages) {

	boolean check = true;
	if (value == null) {

	    errorMessages.add(name + " is not set properly");
	    check = false;
	}
	return check;
    }
}
