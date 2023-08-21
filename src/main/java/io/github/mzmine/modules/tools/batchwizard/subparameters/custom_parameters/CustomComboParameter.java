/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

package io.github.mzmine.modules.tools.batchwizard.subparameters.custom_parameters;

import io.github.mzmine.parameters.UserParameter;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

public abstract class CustomComboParameter<E extends Enum<?>, VALUETYPE extends CustomComboValue<E>> implements
    UserParameter<VALUETYPE, CustomComboComponent<E, VALUETYPE>> {

  protected static final String CHOICES_ATTRIBUTE = "choices";
  protected static final String VALUE_ATTRIBUTE = "selected_value";

  private final String name, description;
  private final boolean valueRequired;
  protected E[] options;
  protected VALUETYPE value;

  public CustomComboParameter(String name, String description, E[] options, boolean valueRequired,
      VALUETYPE defaultValue) {
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

  @Override
  public void setValueToComponent(final CustomComboComponent<E, VALUETYPE> eCustomComboComponent,
      @Nullable final VALUETYPE newValue) {
    eCustomComboComponent.setValue(newValue);
  }

  @Override
  public void setValueFromComponent(
      final CustomComboComponent<E, VALUETYPE> eCustomComboComponent) {
    try {
      value = eCustomComboComponent.getValue();
    } catch (Exception e) {
      value = null;
    }
  }

  public boolean isValueRequired() {
    return valueRequired;
  }

  @Override
  public VALUETYPE getValue() {
    return value;
  }

  @Override
  public void setValue(VALUETYPE value) {
    this.value = value;
  }


  @Override
  public void saveValueToXML(Element xmlElement) {
    // always save the choices
    xmlElement.setAttribute(CHOICES_ATTRIBUTE,
        Arrays.stream(options).map(Enum::name).collect(Collectors.joining(",")));
    xmlElement.setAttribute(VALUE_ATTRIBUTE, value == null ? "null" : value.getValueType().name());
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
