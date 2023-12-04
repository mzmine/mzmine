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

import io.github.mzmine.parameters.UserParameter;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javafx.stage.FileChooser.ExtensionFilter;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Simple Parameter implementation
 */
public class FileNameParameter implements UserParameter<File, FileNameComponent> {

  private static final String CURRENT_FILE_ELEMENT = "current_file";
  private static final String LAST_FILE_ELEMENT = "last_file";
  private final String name;
  private final String description;
  private final FileSelectionType type;
  private final List<ExtensionFilter> filters;
  private File value;
  private List<File> lastFiles;
  private final boolean allowEmptyString;

  /**
   * @param name
   * @param description
   * @param type        FileSelectionType.OPEN to open a file, FileSelectionType.SAVE to save to a
   *                    file.
   */
  public FileNameParameter(String name, String description, FileSelectionType type) {
    this(name, description, List.of(), type, true);
  }

  /**
   * @param name
   * @param description
   * @param type             FileSelectionType.OPEN to open a file, FileSelectionType.SAVE to save
   *                         to a file.
   * @param allowEmptyString
   */
  public FileNameParameter(String name, String description, FileSelectionType type,
      boolean allowEmptyString) {
    this(name, description, List.of(), type, allowEmptyString);
  }

  /**
   * @param name
   * @param description
   * @param filters
   * @param type        FileSelectionType.OPEN to open a file, FileSelectionType.SAVE to save to a
   *                    file.
   */
  public FileNameParameter(String name, String description, List<ExtensionFilter> filters,
      FileSelectionType type) {
    this(name, description, filters, type, true);
  }

  /**
   * @param name
   * @param description
   * @param filters
   * @param type             FileSelectionType.OPEN to open a file, FileSelectionType.SAVE to save
   *                         to a file.
   * @param allowEmptyString
   */
  public FileNameParameter(String name, String description, List<ExtensionFilter> filters,
      FileSelectionType type, boolean allowEmptyString) {
    this.name = name;
    this.description = description;
    this.filters = filters;
    lastFiles = new ArrayList<>();
    this.type = type;
    this.allowEmptyString = allowEmptyString;
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
  public FileNameComponent createEditingComponent() {
    return new FileNameComponent(lastFiles, type, filters);
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
    FileNameParameter copy = new FileNameParameter(name, description, filters, type, allowEmptyString);
    copy.setValue(this.getValue());
    copy.setLastFiles(new ArrayList<>(lastFiles));
    return copy;
  }

  @Override
  public void setValueFromComponent(FileNameComponent component) {
    File compValue = component.getValue(allowEmptyString);
    if (compValue == null) {
      this.value = compValue;
      return;
    }

    // add to last files if not already inserted
    lastFiles.remove(compValue);
    lastFiles.add(0, compValue);
    setLastFiles(lastFiles);

    this.value = compValue;
  }

  @Override
  public void setValueToComponent(FileNameComponent component, @Nullable File newValue) {
    component.setValue(newValue);
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    NodeList current = xmlElement.getElementsByTagName(CURRENT_FILE_ELEMENT);
    if (current.getLength() == 1) {
      setValue(new File(current.item(0).getTextContent()));
    }
    // add all still existing files
    lastFiles = new ArrayList<>();

    NodeList last = xmlElement.getElementsByTagName(LAST_FILE_ELEMENT);
    for (int i = 0; i < last.getLength(); i++) {
      Node n = last.item(i);
      if (n.getTextContent() != null) {
        File f = new File(n.getTextContent());
        if (f.exists()) {
          lastFiles.add(f);
        }
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
    if (value == null && !allowEmptyString) {
      errorMessages.add(name + " is not set properly");
      return false;
    }
    return true;
  }

}
