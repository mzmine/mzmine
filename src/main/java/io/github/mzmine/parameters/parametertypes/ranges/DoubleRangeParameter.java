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
import java.text.NumberFormat;
import java.util.Collection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class DoubleRangeParameter implements UserParameter<Range<Double>, DoubleRangeComponent> {

  private final String name, description;
  protected final boolean valueRequired;
  private final boolean nonEmptyRequired;
  private NumberFormat format;
  private Range<Double> value;
  private Range<Double> maxAllowedRange;

  public DoubleRangeParameter(String name, String description, NumberFormat format) {
    this(name, description, format, true, false, null);
  }

  public DoubleRangeParameter(String name, String description, NumberFormat format,
      Range<Double> defaultValue) {
    this(name, description, format, true, false, defaultValue);
  }

  public DoubleRangeParameter(String name, String description, NumberFormat format,
      boolean valueRequired, Range<Double> defaultValue) {
    this(name, description, format, valueRequired, false, defaultValue);
  }

  public DoubleRangeParameter(String name, String description, NumberFormat format,
      boolean valueRequired, boolean nonEmptyRequired, Range<Double> defaultValue) {
    this(name, description, format, valueRequired, nonEmptyRequired, defaultValue, null);
  }

  public DoubleRangeParameter(String name, String description, NumberFormat format,
      boolean valueRequired, boolean nonEmptyRequired, Range<Double> defaultValue, Range<Double> maxAllowedRange) {
    this.name = name;
    this.description = description;
    this.format = format;
    this.valueRequired = valueRequired;
    this.nonEmptyRequired = nonEmptyRequired;
    this.value = defaultValue;
    this.maxAllowedRange = maxAllowedRange;
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
  public DoubleRangeComponent createEditingComponent() {
    return new DoubleRangeComponent(format);
  }

  public Range<Double> getValue() {
    return value;
  }

  @Override
  public void setValue(Range<Double> value) {
    this.value = value;
  }

  @Override
  public DoubleRangeParameter cloneParameter() {
    DoubleRangeParameter copy = new DoubleRangeParameter(name, description, format);
    copy.setValue(this.getValue());
    return copy;
  }

  @Override
  public void setValueFromComponent(DoubleRangeComponent component) {
    value = component.getValue();
  }

  @Override
  public void setValueToComponent(DoubleRangeComponent component, Range<Double> newValue) {
    component.setValue(newValue);
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    NodeList minNodes = xmlElement.getElementsByTagName("min");
    if (minNodes.getLength() != 1)
      return;
    NodeList maxNodes = xmlElement.getElementsByTagName("max");
    if (maxNodes.getLength() != 1)
      return;
    String minText = minNodes.item(0).getTextContent();
    String maxText = maxNodes.item(0).getTextContent();
    double min = Double.valueOf(minText);
    double max = Double.valueOf(maxText);
    value = Range.closed(min, max);
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (value == null)
      return;
    Document parentDocument = xmlElement.getOwnerDocument();
    Element newElement = parentDocument.createElement("min");
    newElement.setTextContent(String.valueOf(value.lowerEndpoint()));
    xmlElement.appendChild(newElement);
    newElement = parentDocument.createElement("max");
    newElement.setTextContent(String.valueOf(value.upperEndpoint()));
    xmlElement.appendChild(newElement);
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    if (valueRequired && (value == null)) {
      errorMessages.add(name + " is not set properly");
      return false;
    }
    if (value != null) {
      if (!nonEmptyRequired && value.lowerEndpoint() > value.upperEndpoint()) {
        errorMessages.add(name + " range maximum must be higher than minimum, or equal");
        return false;
      }
      if (nonEmptyRequired && value.lowerEndpoint() >= value.upperEndpoint()) {
        errorMessages.add(name + " range maximum must be higher than minimum");
        return false;
      }
    }

    if (value != null && maxAllowedRange != null) {
      if (maxAllowedRange.intersection(value) != value) {
        errorMessages.add(name + " must be within " + maxAllowedRange.toString());
        return false;
      }
    }

    return true;
  }

}
