/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.parameters.parametertypes.filenames;

import io.github.mzmine.parameters.Parameter;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Simple Parameter implementation
 */
public class FileNameListSilentParameter implements Parameter<List<File>> {

  private static final String FILE_ELEMENT = "file";
  private String name;
  private @NotNull List<File> value;

  private List<FileNameListChangedListener> listener;

  public FileNameListSilentParameter(String name) {
    this.name = name;
    value = new ArrayList<>();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  @NotNull
  public List<File> getValue() {
    return value;
  }

  @Override
  public void setValue(List<File> value) {
    this.value = Objects.requireNonNullElse(value, new ArrayList<>());
    fireChanged();
  }

  public void addFile(File f) {
    if (f == null) {
      return;
    }

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
      if (f.exists()) {
        value.add(f);
      }
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
    // is no parameter to select values - is to create a list of last used
    // files
    return true;
  }

  private void fireChanged() {
    if (listener != null) {
      listener.stream().forEach(l -> l.fileListChanged(value));
    }
  }

  public void addFileListChangedListener(FileNameListChangedListener list) {
    if (list == null) {
      return;
    }
    if (listener == null) {
      listener = new ArrayList<>();
    }
    listener.add(list);
  }
}
