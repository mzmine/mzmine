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

package io.github.mzmine.parameters.parametertypes;

import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.parameters.UserParameter;
import java.util.Collection;
import javafx.scene.control.TextField;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

public class StringParameter implements UserParameter<String, TextField> {

  protected String name, description, value;
  protected int inputsize = 20;
  protected boolean valueRequired = true;
  protected final boolean sensitive;

  public StringParameter(String name, String description) {
    this(name, description, null);
  }

  public StringParameter(String name, String description, boolean isSensitive) {
    this(name, description, null, true, isSensitive);
  }

  public StringParameter(String name, String description, int inputsize) {
    this.name = name;
    this.description = description;
    this.inputsize = inputsize;
    this.sensitive = false;
  }

  public StringParameter(String name, String description, String defaultValue) {
    this(name, description, defaultValue, true, false);
  }

  public StringParameter(String name, String description, String defaultValue,
      boolean valueRequired) {
    this(name, description, defaultValue, valueRequired, false);
  }

  public StringParameter(String name, String description, String defaultValue,
      boolean valueRequired, boolean isSensitive) {
    this.name = name;
    this.description = description;
    this.value = defaultValue;
    this.valueRequired = valueRequired;
    this.sensitive = isSensitive;
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
  public TextField createEditingComponent() {
    TextField stringComponent = new TextField();
    stringComponent.setPrefColumnCount(inputsize);
    return stringComponent;
  }

  @Override
  public String getValue() {
    return value;
  }

  @Override
  public void setValue(String value) {
    this.value = value;
  }

  @Override
  public StringParameter cloneParameter() {
    StringParameter copy = new StringParameter(name, description, getValue(), valueRequired,
        isSensitive());
    copy.setValue(this.getValue());

    return copy;
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public void setValueFromComponent(TextField component) {
    value = component.getText();
  }

  @Override
  public void setValueToComponent(TextField component, @Nullable String newValue) {
    component.setText(requireNonNullElse(newValue, ""));
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    value = xmlElement.getTextContent();
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (value == null) {
      return;
    }
    xmlElement.setTextContent(value);
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    if (!valueRequired) {
      return true;
    }
    if ((value == null) || (value.trim().length() == 0)) {
      errorMessages.add(name + " is not set properly");
      return false;
    }
    return true;
  }

  @Override
  public boolean isSensitive() {
    return sensitive;
  }
}
