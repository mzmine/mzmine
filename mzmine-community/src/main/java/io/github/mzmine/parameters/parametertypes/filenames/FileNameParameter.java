/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.project.ProjectService;
import java.io.File;
import java.nio.file.Path;
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

  protected static final String CURRENT_FILE_ELEMENT = "current_file";
  protected static final String LAST_FILE_ELEMENT = "last_file";
  protected final String name;
  protected final String description;
  protected final FileSelectionType type;
  protected final List<ExtensionFilter> filters;
  protected File value;
  protected List<File> lastFiles;
  protected final boolean allowEmptyString;

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
    FileNameParameter copy = new FileNameParameter(name, description, filters, type,
        allowEmptyString);
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
    final File projectFile = ProjectService.getProject().getProjectFile();

    if (current.getLength() == 1) {
      final File absFile = new File(current.item(0).getTextContent());

      final String relPathValue = ((Element) current.item(0)).getAttribute(
          FileNamesParameter.XML_RELATIVE_PATH_ATTRIBUTE);
      final File relFile = relPathValue.isBlank() ? null : new File(relPathValue);

      if (!absFile.exists() && relFile != null && projectFile != null && projectFile.toPath()
          .resolve(relFile.toPath()).toFile().exists()) {
        setValue(projectFile.toPath().resolve(relFile.toPath()).toFile());
      } else {
        setValue(absFile);
      }
    }
    // add all still existing files
    lastFiles = new ArrayList<>();

    NodeList last = xmlElement.getElementsByTagName(LAST_FILE_ELEMENT);
    for (int i = 0; i < last.getLength(); i++) {
      Node n = last.item(i);
      if (n.getTextContent() != null) {
        File absFile = new File(n.getTextContent());

        final String relPathAttr = ((Element) n).getAttribute(
            FileNamesParameter.XML_RELATIVE_PATH_ATTRIBUTE);
        final File relFile = !relPathAttr.isBlank() ? new File(relPathAttr) : null;

        if (absFile.exists()) {
          lastFiles.add(absFile);
        } else if (!absFile.exists() && (relFile != null && projectFile != null
            && projectFile.toPath().resolve(relFile.toPath()).toFile().exists())) {
          lastFiles.add(projectFile.toPath().resolve(relFile.toPath()).toFile());
        }
      }
    }
    setLastFiles(lastFiles);
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    // add new element for each file
    Document parentDocument = xmlElement.getOwnerDocument();
    final MZmineProject project = ProjectService.getProject();

    if (value != null) {
      Element paramElement = parentDocument.createElement(CURRENT_FILE_ELEMENT);
      paramElement.setTextContent(value.getAbsolutePath());

      final Path relativePath = project.getRelativePath(value.toPath());
      if (relativePath != null) {
        paramElement.setAttribute(FileNamesParameter.XML_RELATIVE_PATH_ATTRIBUTE,
            relativePath.toString());
      }
      xmlElement.appendChild(paramElement);
    }

    if (lastFiles != null) {
      for (File f : lastFiles) {
        Element paramElement = parentDocument.createElement(LAST_FILE_ELEMENT);
        paramElement.setTextContent(f.getAbsolutePath());

        final Path relativePath = project.getRelativePath(f.toPath());
        if (relativePath != null && relativePath.toFile().exists()) {
          paramElement.setAttribute(FileNamesParameter.XML_RELATIVE_PATH_ATTRIBUTE,
              relativePath.toString());
        }
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

  public FileSelectionType getType() {
    return type;
  }
}
