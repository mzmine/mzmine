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
import javax.swing.JPanel;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import net.sf.mzmine.parameters.UserParameter;

/**
 * Simple Parameter implementation
 * 
 */
public class FileNameList implements UserParameter<List<File>, JPanel> {

  private static final String FILE_ELEMENT = "file";
  private String name, description;
  private List<File> value;
  private String extension;

  public FileNameList(String name, String description) {
    this(name, description, null);
  }

  public FileNameList(String name, String description, String extension) {
    this.name = name;
    this.description = description;
    this.extension = extension;
    value = new ArrayList<>();
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
  public JPanel createEditingComponent() {
    return new JPanel();
  }

  @Override
  public List<File> getValue() {
    return value;
  }

  @Override
  public void setValue(List<File> value) {
    this.value = value;
  }

  @Override
  public FileNameList cloneParameter() {
    FileNameList copy = new FileNameList(name, description);
    copy.setValue(new ArrayList<>(this.getValue()));
    return copy;
  }

  @Override
  public void setValueFromComponent(JPanel component) {}

  @Override
  public void setValueToComponent(JPanel component, List<File> newValue) {}

  @Override
  public void loadValueFromXML(Element xmlElement) {
    String fileString = xmlElement.getTextContent();
    if (fileString.length() == 0)
      return;
    // add all still existing files
    value = new ArrayList<>();
    NodeList nodes = xmlElement.getChildNodes();
    for (int i = 0; i < nodes.getLength(); i++) {
      Node n = nodes.item(i);
      File f = new File(n.getTextContent());
      if (f.exists())
        value.add(f);
    }
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (value == null)
      return;
    // add new element for each file
    Document parentDocument = xmlElement.getOwnerDocument();
    for (File f : value) {
      Element paramElement = parentDocument.createElement(FILE_ELEMENT);
      paramElement.setTextContent(f.getAbsolutePath());
      xmlElement.appendChild(paramElement);
    }
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    // is no parameter to select values - is to create a list of last used files
    return true;
  }

}
