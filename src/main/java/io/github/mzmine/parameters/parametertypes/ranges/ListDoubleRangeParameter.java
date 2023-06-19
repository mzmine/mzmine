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
package io.github.mzmine.parameters.parametertypes.ranges;

import com.google.common.collect.Range;

import io.github.mzmine.parameters.UserParameter;

import java.util.Collection;
import java.util.List;

import org.w3c.dom.Element;

/**
 *
 * @author Du-Lab Team <dulab.binf@gmail.com>
 */

public class ListDoubleRangeParameter
    implements UserParameter<List<Range<Double>>, ListDoubleRangeComponent> {
  private final String name, description;
  private final boolean valueRequired;

  private List<Range<Double>> value;

  public ListDoubleRangeParameter(String name, String description, boolean valueRequired,
      List<Range<Double>> defaultValue) {
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
  public ListDoubleRangeComponent createEditingComponent() {
    return new ListDoubleRangeComponent();
  }

  @Override
  public List<Range<Double>> getValue() {
    return value;
  }

  @Override
  public void setValue(List<Range<Double>> value) {
    this.value = value;
  }

  @Override
  public ListDoubleRangeParameter cloneParameter() {
    ListDoubleRangeParameter copy =
        new ListDoubleRangeParameter(name, description, valueRequired, value);
    copy.setValue(value);
    return copy;
  }

  @Override
  public void setValueFromComponent(ListDoubleRangeComponent component) {
    value = component.getValue();
  }

  @Override
  public void setValueToComponent(ListDoubleRangeComponent component,
      List<Range<Double>> newValue) {
    component.setValue(newValue);
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    value = dulab.adap.common.algorithms.String.toRanges(xmlElement.getTextContent());
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (value == null)
      return;

    xmlElement.setTextContent(dulab.adap.common.algorithms.String.fromRanges(value));
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
