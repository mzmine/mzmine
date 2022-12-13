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
