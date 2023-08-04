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

import com.google.common.collect.ImmutableList;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.UserParameter;
import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser.ExtensionFilter;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 *
 */
public class FileNamesParameter implements UserParameter<File[], FileNamesComponent> {

  private final String name;
  private final String description;
  private final Path defaultDir;
  private File[] value;
  private final List<ExtensionFilter> filters;

  public FileNamesParameter(String name) {
    this(name, "", List.of());
  }

  public FileNamesParameter(String name, String description, List<ExtensionFilter> filters) {
    this(name, description, filters, null, null);
  }

  public FileNamesParameter(String name, String description, List<ExtensionFilter> filters,
      Path defaultDir, File[] defaultFiles) {
    this.name = name;
    this.description = description;
    this.filters = ImmutableList.copyOf(filters);
    this.defaultDir = defaultDir;
    value = defaultFiles;
  }

  /**
   *
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   *
   */
  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public FileNamesComponent createEditingComponent() {
    return new FileNamesComponent(filters, defaultDir);
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
  public void setValueToComponent(FileNamesComponent component, @Nullable File[] newValue) {
    component.setValue(newValue);
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    NodeList list = xmlElement.getElementsByTagName("file");
    File[] newFiles = new File[list.getLength()];
    for (int i = 0; i < list.getLength(); i++) {
      Element nextElement = (Element) list.item(i);
      newFiles[i] = new File(nextElement.getTextContent());
    }
    this.value = newFiles;
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (value == null) {
      return;
    }
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
    if (that == null) {
      return false;
    }
    if (!(that instanceof FileNamesParameter thatParam)) {
      return false;
    }

    File[] thatValue = thatParam.getValue();
    return Arrays.equals(value, thatValue);
  }

  @Override
  public Priority getComponentVgrowPriority() {
    return Priority.SOMETIMES;
  }
}
