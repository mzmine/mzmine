/*
 * Copyright 2006-2018 The MZmine 2 Development Team
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

package net.sf.mzmine.parameters.parametertypes.filenames;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import net.sf.mzmine.parameters.UserParameter;

/**
 * Simple Parameter implementation
 * 
 */
public class FileNameParameter implements UserParameter<File, FileNameComponent> {

  private static final String CURRENT_FILE_ELEMENT = "current_file";
  private static final String LAST_FILE_ELEMENT = "last_file";
  private String name, description;
  private File value;
  private List<File> lastFiles;
  private String extension;
  private int textfield_columns = 15;

  public FileNameParameter(String name, String description) {
    this(name, description, null);
  }

  public FileNameParameter(String name, String description, String extension) {
    this.name = name;
    this.description = description;
    this.extension = extension;
    lastFiles = new ArrayList<>();
  }

  public FileNameParameter(String name, String description, String extension,
      int textfield_columns) {
    this.name = name;
    this.description = description;
    this.extension = extension;
    this.textfield_columns = textfield_columns;
    lastFiles = new ArrayList<>();
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
    return new FileNameComponent(textfield_columns, lastFiles);
  }

  @Override
  public File getValue() {
    return value;
  }

  @Override
  public void setValue(File value) {
    this.value = value;
  }

  public List<File> getLastFiles() {
    return lastFiles;
  }

  public void setLastFiles(List<File> lastFiles) {
    this.lastFiles = lastFiles;
  }

  @Override
  public FileNameParameter cloneParameter() {
    FileNameParameter copy = new FileNameParameter(name, description);
    copy.setValue(this.getValue());
    copy.setLastFiles(new ArrayList<>(lastFiles));
    return copy;
  }

  @Override
  public void setValueFromComponent(FileNameComponent component) {
    File compValue = component.getValue();
    if (extension != null) {
      if (!compValue.getName().toLowerCase().endsWith(extension.toLowerCase()))
        compValue = new File(compValue.getPath() + "." + extension);
    }
    if (compValue != null) {
      // add to last files if not already inserted
      lastFiles.remove(compValue);
      lastFiles.add(0, compValue);
      setLastFiles(lastFiles);
    }

    this.value = compValue;
  }

  @Override
  public void setValueToComponent(FileNameComponent component, File newValue) {
    component.setValue(newValue);
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    NodeList current = xmlElement.getElementsByTagName(CURRENT_FILE_ELEMENT);
    if (current.getLength() == 1)
      setValue(new File(current.item(0).getTextContent()));
    // add all still existing files
    lastFiles = new ArrayList<>();

    NodeList last = xmlElement.getElementsByTagName(LAST_FILE_ELEMENT);
    for (int i = 0; i < last.getLength(); i++) {
      Node n = last.item(i);
      if (n.getTextContent() != null) {
        File f = new File(n.getTextContent());
        if (f.exists())
          lastFiles.add(f);
      }
    }
    setLastFiles(lastFiles);
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    // add new element for each file
    Document parentDocument = xmlElement.getOwnerDocument();
    if (value != null) {
      Element paramElement = parentDocument.createElement(CURRENT_FILE_ELEMENT);
      paramElement.setTextContent(value.getAbsolutePath());
      xmlElement.appendChild(paramElement);
    }

    if (lastFiles != null) {
      for (File f : lastFiles) {
        Element paramElement = parentDocument.createElement(LAST_FILE_ELEMENT);
        paramElement.setTextContent(f.getAbsolutePath());
        xmlElement.appendChild(paramElement);
      }
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
