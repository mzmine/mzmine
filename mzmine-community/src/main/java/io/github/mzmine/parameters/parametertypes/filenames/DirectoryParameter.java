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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

/**
 * A parameter that represents a file system directory.
 */
public class DirectoryParameter implements UserParameter<File, DirectoryComponent> {

  private final String name;
  private final String description;
  private File value;

  public DirectoryParameter(final String aName, final String aDescription) {

    name = aName;
    description = aDescription;
  }

  public DirectoryParameter(final String aName, final String aDescription, String defaultPath) {
    name = aName;
    description = aDescription;
    Path path = Paths.get(defaultPath);
    if(Files.exists(path)) {
      value = path.toFile();
    }
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
  public DirectoryComponent createEditingComponent() {

    return new DirectoryComponent();
  }

  @Override
  public File getValue() {

    return value;
  }

  @Override
  public void setValue(final File newValue) {

    value = newValue;
  }

  @Override
  public DirectoryParameter cloneParameter() {

    final DirectoryParameter copy = new DirectoryParameter(name, description);
    copy.setValue(getValue());
    return copy;
  }

  @Override
  public void setValueFromComponent(final DirectoryComponent component) {

    value = component.getValue();
  }

  @Override
  public void setValueToComponent(final DirectoryComponent component, @Nullable final File newValue) {
    component.setValue(newValue);
  }

  @Override
  public void loadValueFromXML(final Element xmlElement) {

    final String fileString = xmlElement.getTextContent();
    if (fileString.length() != 0) {

      value = new File(fileString);
    }
  }

  @Override
  public void saveValueToXML(final Element xmlElement) {

    if (value != null) {

      xmlElement.setTextContent(value.getPath());
    }
  }

  @Override
  public boolean checkValue(final Collection<String> errorMessages) {

    boolean check = true;
    if (value == null) {

      errorMessages.add(name + " is not set properly");
      check = false;
    }
    return check;
  }
}
