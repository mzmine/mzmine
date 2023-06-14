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

package io.github.mzmine.parameters.parametertypes.massdefect;

import io.github.mzmine.parameters.UserParameter;
import java.text.NumberFormat;
import java.util.Collection;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class MassDefectParameter implements UserParameter<MassDefectFilter, MassDefectComponent> {

  private final String name, description;
  protected final boolean valueRequired;
  private final boolean nonEmptyRequired;

  private NumberFormat format;
  private MassDefectFilter value;

  public MassDefectParameter(String name, String description, NumberFormat format) {
    this(name, description, format, true, false, null);
  }

  public MassDefectParameter(String name, String description, NumberFormat format,
      MassDefectFilter defaultValue) {
    this(name, description, format, true, false, defaultValue);
  }

  public MassDefectParameter(String name, String description, NumberFormat format,
      boolean valueRequired, MassDefectFilter defaultValue) {
    this(name, description, format, valueRequired, false, defaultValue);
  }

  public MassDefectParameter(String name, String description, NumberFormat format,
      boolean valueRequired, boolean nonEmptyRequired, MassDefectFilter defaultValue) {
    this.name = name;
    this.description = description;
    this.format = format;
    this.valueRequired = valueRequired;
    this.nonEmptyRequired = nonEmptyRequired;
    this.value = defaultValue;
  }

  /**
   * @see io.github.mzmine.parameters.Parameter#getName()
   */
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
  public MassDefectComponent createEditingComponent() {
    return new MassDefectComponent(format);
  }

  public MassDefectFilter getValue() {
    return value;
  }

  @Override
  public void setValue(MassDefectFilter value) {
    this.value = value;
  }

  @Override
  public MassDefectParameter cloneParameter() {
    MassDefectParameter copy = new MassDefectParameter(name, description, format);
    copy.setValue(this.getValue());
    return copy;
  }

  @Override
  public void setValueFromComponent(MassDefectComponent component) {
    value = component.getValue();
  }

  @Override
  public void setValueToComponent(MassDefectComponent component, @Nullable MassDefectFilter newValue) {
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
    value = new MassDefectFilter(min, max);
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (value == null)
      return;
    Document parentDocument = xmlElement.getOwnerDocument();
    Element newElement = parentDocument.createElement("min");
    newElement.setTextContent(String.valueOf(value.getLowerEndpoint()));
    xmlElement.appendChild(newElement);
    newElement = parentDocument.createElement("max");
    newElement.setTextContent(String.valueOf(value.getUpperEndpoint()));
    xmlElement.appendChild(newElement);
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    if (valueRequired && (value == null)) {
      errorMessages.add(name + " is not set properly");
      return false;
    }

    return true;
  }
}
