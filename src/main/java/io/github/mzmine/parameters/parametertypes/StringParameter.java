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

package io.github.mzmine.parameters.parametertypes;

import io.github.mzmine.parameters.UserParameter;
import java.util.Collection;
import java.util.Objects;
import javafx.scene.control.TextField;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;

public class StringParameter implements UserParameter<String, TextField> {

  protected final boolean sensitive;
  protected String name, description;
  protected @NotNull String value;
  protected int inputsize = 20;
  protected boolean valueRequired = true;

  public StringParameter(String name, String description) {
    this(name, description, "");
  }

  public StringParameter(String name, String description, boolean isSensitive) {
    this(name, description, "", true, isSensitive);
  }

  public StringParameter(String name, String description, int inputsize) {
    this.name = name;
    this.description = description;
    this.inputsize = inputsize;
    this.sensitive = false;
  }

  public StringParameter(String name, String description, @NotNull String defaultValue) {
    this(name, description, defaultValue, true, false);
  }

  public StringParameter(String name, String description, @NotNull String defaultValue,
      boolean valueRequired) {
    this(name, description, defaultValue, valueRequired, false);
  }

  public StringParameter(String name, String description, @NotNull String defaultValue,
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
    // stringComponent.setBorder(BorderFactory.createCompoundBorder(stringComponent.getBorder(),
    // BorderFactory.createEmptyBorder(0, 4, 0, 0)));
    return stringComponent;
  }

  @Override
  public String getValue() {
    return value;
  }

  @Override
  public void setValue(String value) {
    assert value != null;
    this.value = value;
  }

  @Override
  public StringParameter cloneParameter() {
    StringParameter copy = new StringParameter(name, description);
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
  public void setValueToComponent(TextField component, String newValue) {
    component.setText(newValue);
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    value = Objects.requireNonNullElse(xmlElement.getTextContent(), "");
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    xmlElement.setTextContent(value);
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    if (!valueRequired) {
      return true;
    }
    if (value.trim().length() == 0) {
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
