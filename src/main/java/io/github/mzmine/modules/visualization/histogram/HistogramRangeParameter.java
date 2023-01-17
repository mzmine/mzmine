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

package io.github.mzmine.modules.visualization.histogram;

import java.util.Collection;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.google.common.collect.Range;

import io.github.mzmine.parameters.UserParameter;

public class HistogramRangeParameter implements UserParameter<Range<Double>, HistogramRangeEditor> {

  private String name, description;
  private HistogramDataType selectedType = HistogramDataType.MASS;
  private Range<Double> value;

  public HistogramRangeParameter() {
    this.name = "Plotted data";
    this.description = "Plotted data type and range";
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
  public HistogramRangeEditor createEditingComponent() {
    return new HistogramRangeEditor();
  }

  @Override
  public HistogramRangeParameter cloneParameter() {
    HistogramRangeParameter copy = new HistogramRangeParameter();
    copy.selectedType = this.selectedType;
    copy.value = this.value;
    return copy;
  }

  @Override
  public void setValueFromComponent(HistogramRangeEditor component) {
    this.selectedType = component.getSelectedType();
    this.value = component.getValue();
  }

  @Override
  public void setValueToComponent(HistogramRangeEditor component, Range<Double> newValue) {
    component.setValue(newValue);
  }

  @Override
  public Range<Double> getValue() {
    return value;
  }

  public HistogramDataType getType() {
    return selectedType;
  }

  @Override
  public void setValue(Range<Double> newValue) {
    value = newValue;
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {

    String typeAttr = xmlElement.getAttribute("selected");
    if (typeAttr.length() == 0)
      return;

    this.selectedType = HistogramDataType.valueOf(typeAttr);

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

    xmlElement.setAttribute("selected", selectedType.name());
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    if (value == null) {
      errorMessages.add(name + " is not set");
      return false;
    }
    return true;
  }
}
