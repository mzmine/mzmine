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
import javax.annotation.Nonnull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import net.sf.mzmine.parameters.Parameter;

/**
 * Simple Parameter implementation
 * 
 */
public class FileNameListSilentParameter implements Parameter<List<File>> {

  private static final String FILE_ELEMENT = "file";
  private String name;
  private @Nonnull List<File> value;

  private List<FileNameListChangedListener> listener;

  public FileNameListSilentParameter(String name) {
    this.name = name;
    value = new ArrayList<>();
  }

  /**
   * @see net.sf.mzmine.data.Parameter#getName()
   */
  @Override
  public String getName() {
    return name;
  }

  @Override
  @Nonnull
  public List<File> getValue() {
    return value;
  }

  @Override
  public void setValue(List<File> value) {
    if (value == null)
      this.value = new ArrayList<>();
    else
      this.value = value;
    fireChanged();
  }

  public void addFile(File f) {
    if (f == null)
      return;

    // add to last files if not already inserted
    value.remove(f);
    value.add(0, f);
    fireChanged();
  }

  @Override
  public FileNameListSilentParameter cloneParameter() {
    FileNameListSilentParameter copy = new FileNameListSilentParameter(name);
    copy.setValue(new ArrayList<>(this.getValue()));
    return copy;
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    // add all still existing files
    value = new ArrayList<>();
    NodeList nodes = xmlElement.getChildNodes();
    for (int i = 0; i < nodes.getLength(); i++) {
      Node n = nodes.item(i);
      File f = new File(n.getTextContent());
      if (f.exists())
        value.add(f);
    }
    fireChanged();
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
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

  private void fireChanged() {
    if (listener != null)
      listener.stream().forEach(l -> l.fileListChanged(value));
  }

  public void addFileListChangedListener(FileNameListChangedListener list) {
    if (list == null)
      return;
    if (listener == null)
      listener = new ArrayList<>();
    listener.add(list);
  }
}
