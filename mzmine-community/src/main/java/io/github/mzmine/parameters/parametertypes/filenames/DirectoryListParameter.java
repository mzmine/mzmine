/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * An ordered directory list with a compact preview and a popup editor. Can directly replace a
 * {@link DirectoryParameter} of the same name.
 */
public class DirectoryListParameter implements UserParameter<File[], DirectoryListComponent> {

  private final String name;
  private final String description;
  private File[] value;

  public DirectoryListParameter(String name, String description) {
    this(name, description, new File[0]);
  }

  public DirectoryListParameter(String name, String description,
      @Nullable File[] defaultDirectories) {
    this.name = name;
    this.description = description;
    setValue(defaultDirectories);
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
  public @NotNull File[] getValue() {
    return value.clone();
  }

  @Override
  public void setValue(@Nullable File[] value) {
    this.value = value == null ? new File[0] : value.clone();
  }

  @Override
  public DirectoryListComponent createEditingComponent() {
    return new DirectoryListComponent();
  }

  @Override
  public void setValueFromComponent(DirectoryListComponent component) {
    setValue(component.getValue());
  }

  @Override
  public void setValueToComponent(DirectoryListComponent component, @Nullable File[] newValue) {
    component.setValue(newValue);
  }

  @Override
  public DirectoryListParameter cloneParameter() {
    return new DirectoryListParameter(name, description, value);
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    NodeList files = xmlElement.getElementsByTagName("file");
    if (files.getLength() == 0) {
      // DirectoryParameter stored a single path directly in the parameter element.
      String path = xmlElement.getTextContent().trim();
      setValue(path.isEmpty() ? new File[0] : new File[]{new File(path)});
      return;
    }
    File[] directories = new File[files.getLength()];
    for (int i = 0; i < files.getLength(); i++) {
      directories[i] = new File(files.item(i).getTextContent());
    }
    setValue(directories);
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (value.length == 1) {
      // Keep single-directory preferences readable by older mzmine versions.
      xmlElement.setTextContent(value[0].getPath());
      return;
    }
    for (File directory : value) {
      Element file = xmlElement.getOwnerDocument().createElement("file");
      file.setTextContent(directory.getPath());
      xmlElement.appendChild(file);
    }
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    if (value.length == 0) {
      errorMessages.add(name + " requires at least one directory");
      return false;
    }
    boolean valid = true;
    for (File directory : value) {
      // Missing directories are created when the configuration is applied.
      if (directory == null || (directory.exists() && !directory.isDirectory())) {
        errorMessages.add(name + " contains an invalid directory: " + directory);
        valid = false;
      }
    }
    return valid;
  }
}
