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
import org.w3c.dom.Element;

public class ComboFieldParameter<E extends Enum<?>> implements UserParameter<ComboFieldValue<E>, ComboFieldComponent<E>> {
  private final String name, description;
  private final boolean valueRequired;
  private final Class<E> options;
  private ComboFieldValue<E> value;

  public ComboFieldParameter(String name, String description, Class<E> options, boolean valueRequired,
      ComboFieldValue<E> defaultValue) {
    this.name = name;
    this.description = description;
    this.options = options;
    this.valueRequired = valueRequired;
    this.value = defaultValue;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDescription() {
    return description;
  }

  public boolean isValueRequired() {
    return valueRequired;
  }

  @Override
  public ComboFieldComponent<E> createEditingComponent() {
    return new ComboFieldComponent<>(options, value);
  }

  @Override
  public ComboFieldValue<E> getValue() {
    return value;
  }

  @Override
  public void setValue(ComboFieldValue<E> value) {
    this.value = value;
  }

  @Override
  public ComboFieldParameter<E> cloneParameter() {
    return new ComboFieldParameter<>(name, description, options, valueRequired, value);
  }

  @Override
  public void setValueFromComponent(ComboFieldComponent<E> component) {
    value = component.getValue();
  }

  @Override
  public void setValueToComponent(ComboFieldComponent<E> component, ComboFieldValue<E> newValue) {
    component.setValue(newValue);
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    if (value.getFieldText().length() == 0) {
      return;
    }

    String[] splittedString = xmlElement.getTextContent().split("\t");
    value.setFieldText(splittedString[0]);

    for (E option : options.getEnumConstants()) {
      if (option.toString().equals(splittedString[1])) {
        value.setValueType(option);
      }
    }

  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (value == null) {
      return;
    }

    xmlElement.setTextContent(value.getFieldText() + "\t" + value.getValueType().toString());
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    if (valueRequired && value == null) {
      errorMessages.add(name + " is not set properly");
      return false;
    }

    return true;
  }

}
