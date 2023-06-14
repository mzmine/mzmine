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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

// import org.w3c.dom.Element;

// import io.github.mzmine.parameters.UserParameter;

public class ListDoubleParameter implements UserParameter<List<Double>, ListDoubleComponent>

{
  private final String name, description;
  private final boolean valueRequired;

  private List<Double> value;

  public ListDoubleParameter(String name, String description, boolean valueRequired,
      List<Double> defaultValue) {
    this.name = name;
    this.description = description;
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
  public ListDoubleComponent createEditingComponent() {
    return new ListDoubleComponent();
  }

  @Override
  public List<Double> getValue() {
    return value;
  }

  @Override
  public void setValue(List<Double> value) {
    this.value = value;
  }

  @Override
  public ListDoubleParameter cloneParameter() {
    ListDoubleParameter copy = new ListDoubleParameter(name, description, valueRequired, value);
    copy.setValue(value);
    return copy;
  }

  @Override
  public void setValueFromComponent(ListDoubleComponent component) {
    value = component.getValue();
  }

  @Override
  public void setValueToComponent(ListDoubleComponent component, @Nullable List<Double> newValue) {
    component.setValue(newValue);
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {

    String values = xmlElement.getTextContent().replaceAll("\\s", "");
    String[] strValues = values.split(",");
    double[] doubleValues = new double[strValues.length];
    for (int i = 0; i < strValues.length; i++) {
      try {
        doubleValues[i] = Double.parseDouble(strValues[i]);
      } catch (NumberFormatException nfe) {
        // The string does not contain a parsable integer.
      }
    }
    Double[] doubleArray = ArrayUtils.toObject(doubleValues);
    List<Double> ranges = Arrays.asList(doubleArray);
    value = ranges;
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (value == null)
      return;

    String[] strValues = new String[value.size()];

    for (int i = 0; i < value.size(); i++) {
      strValues[i] = Double.toString(value.get(i));
    }
    String text = String.join(",", strValues);

    xmlElement.setTextContent(text);
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
