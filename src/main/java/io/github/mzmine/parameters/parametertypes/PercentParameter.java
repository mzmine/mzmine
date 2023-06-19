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
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

/**
 * Simple Parameter implementation
 * 
 * 
 */
public class PercentParameter implements UserParameter<Double, PercentComponent> {

  private final String name, description;
  private final Double defaultValue, minValue, maxValue;
  private Double value;

  public PercentParameter(String name, String description) {
    this(name, description, null, 0.0, 1.0);
  }

  public PercentParameter(final String name, final String description, final Double defaultValue) {
    this(name, description, defaultValue, 0.0, 1.0);
  }

  public PercentParameter(final String name, final String description, final Double defaultValue,
      final Double minValue, Double maxValue) {
    this.name = name;
    this.description = description;
    this.defaultValue = defaultValue;
    this.value = defaultValue;
    this.minValue = minValue;
    this.maxValue = maxValue;
  }

  /**
   * @see io.github.mzmine.data.Parameter#getName()
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * @see io.github.mzmine.data.Parameter#getDescription()
   */
  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public PercentComponent createEditingComponent() {
    return new PercentComponent();
  }

  @Override
  public void setValueFromComponent(PercentComponent component) {
    Double componentValue = component.getValue();
    if (componentValue == null)
      return;
    this.value = componentValue;
  }

  @Override
  public void setValue(Double value) {
    this.value = value;
  }

  @Override
  public PercentParameter cloneParameter() {
    PercentParameter copy =
        new PercentParameter(name, description, defaultValue, minValue, maxValue);
    copy.setValue(this.getValue());
    return copy;
  }

  @Override
  public void setValueToComponent(PercentComponent component, @Nullable Double newValue) {
    component.setValue(newValue);
  }

  @Override
  /**
   * Returns the percentage value in the range 0..1
   */
  public Double getValue() {
    return value;
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    String numString = xmlElement.getTextContent();
    if (numString.length() == 0)
      return;
    this.value = Double.parseDouble(numString);
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (value == null)
      return;
    xmlElement.setTextContent(value.toString());
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    if (value == null) {
      errorMessages.add(name + " is not set");
      return false;
    }
    if ((value < minValue) || (value > maxValue)) {
      errorMessages.add(name + " value must be in the range " + (minValue * 100) + " - "
          + (maxValue * 100) + "%");
      return false;
    }
    return true;
  }
}
