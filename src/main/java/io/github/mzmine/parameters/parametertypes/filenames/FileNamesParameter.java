/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.parameters.parametertypes.filenames;

import com.google.common.collect.ImmutableList;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.UserParameter;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javafx.stage.FileChooser.ExtensionFilter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 *
 */
public class FileNamesParameter implements UserParameter<File[], FileNamesComponent> {

  private String name, description;
  private File value[];
  private List<ExtensionFilter> filters;

  public FileNamesParameter(String name) {
    this(name, "", List.of());
  }

  public FileNamesParameter(String name, String description, List<ExtensionFilter> filters) {
    this.name = name;
    this.description = description;
    this.filters = ImmutableList.copyOf(filters);
  }

  /**
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   */
  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public FileNamesComponent createEditingComponent() {
    return new FileNamesComponent(filters);
  }

  @Override
  public File[] getValue() {
    return value;
  }

  @Override
  public void setValue(File[] value) {
    this.value = value;
  }

  @Override
  public FileNamesParameter cloneParameter() {
    FileNamesParameter copy = new FileNamesParameter(name, description, filters);
    copy.setValue(this.getValue());
    return copy;
  }

  @Override
  public void setValueFromComponent(FileNamesComponent component) {
    this.value = component.getValue();
  }

  @Override
  public void setValueToComponent(FileNamesComponent component, File[] newValue) {
    component.setValue(newValue);
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    NodeList list = xmlElement.getElementsByTagName("file");
    File newFiles[] = new File[list.getLength()];
    for (int i = 0; i < list.getLength(); i++) {
      Element nextElement = (Element) list.item(i);
      newFiles[i] = new File(nextElement.getTextContent());
    }
    this.value = newFiles;
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (value == null)
      return;
    Document parentDocument = xmlElement.getOwnerDocument();
    for (File f : value) {
      Element newElement = parentDocument.createElement("file");
      newElement.setTextContent(f.getPath());
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

  @Override
  public boolean valueEquals(Parameter<?> that) {
    if(that == null) {
      return false;
    }
    if(!(that instanceof FileNamesParameter thatParam)) {
      return false;
    }

    File[] thatValue = thatParam.getValue();
    return Arrays.equals(value, thatValue);
  }
}
