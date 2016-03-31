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

package net.sf.mzmine.parameters.parametertypes.filenames;

import java.io.File;
import java.util.Collection;

import net.sf.mzmine.parameters.UserParameter;

import org.w3c.dom.Element;

/**
 * Simple Parameter implementation
 * 
 */
public class FileNameParameter
        implements UserParameter<File, FileNameComponent> {

    private String name, description;
    private File value;
    private String extension;
    private int textfield_columns = 15;

    public FileNameParameter(String name, String description) {
        this(name, description, null);
    }

    public FileNameParameter(String name, String description,
            String extension) {
        this.name = name;
        this.description = description;
        this.extension = extension;
    }

    public FileNameParameter(String name, String description, String extension,
            int textfield_columns) {
        this.name = name;
        this.description = description;
        this.extension = extension;
        this.textfield_columns = textfield_columns;
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
    public FileNameComponent createEditingComponent() {
        return new FileNameComponent(textfield_columns);
    }

    @Override
    public File getValue() {
        return value;
    }

    @Override
    public void setValue(File value) {
        this.value = value;
    }

    @Override
    public FileNameParameter cloneParameter() {
        FileNameParameter copy = new FileNameParameter(name, description);
        copy.setValue(this.getValue());
        return copy;
    }

    @Override
    public void setValueFromComponent(FileNameComponent component) {
        File compValue = component.getValue();
        if (extension != null) {
            if (!compValue.getName()
                    .toLowerCase().endsWith(extension.toLowerCase()))
                compValue = new File(compValue.getPath() + "." + extension);
        }
        this.value = compValue;
    }

    @Override
    public void setValueToComponent(FileNameComponent component,
            File newValue) {
        component.setValue(newValue);
    }

    @Override
    public void loadValueFromXML(Element xmlElement) {
        String fileString = xmlElement.getTextContent();
        if (fileString.length() == 0)
            return;
        this.value = new File(fileString);
    }

    @Override
    public void saveValueToXML(Element xmlElement) {
        if (value == null)
            return;
        xmlElement.setTextContent(value.getPath());
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
