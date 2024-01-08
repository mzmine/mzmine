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
import io.github.mzmine.util.maths.MathOperator;
import java.text.NumberFormat;
import java.util.Collection;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Defines options to handle values relative to a value
 */
public class OptionForValuesParameter implements
    UserParameter<OptionForValues, OptionForValuesComponent> {

  protected final boolean valueRequired;
  private final String name, description;
  private final NumberFormat format;
  private final ValueOption[] options;
  private final MathOperator[] operator;
  private OptionForValues value;

  public OptionForValuesParameter(String name, String description, NumberFormat format,
      boolean valueRequired, ValueOption[] options, MathOperator[] operator,
      OptionForValues defaultValue) {
    this.name = name;
    this.description = description;
    this.format = format;
    this.valueRequired = valueRequired;
    this.options = options;
    this.operator = operator;
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
  public OptionForValuesComponent createEditingComponent() {
    return new OptionForValuesComponent(options, operator, format);
  }

  public OptionForValues getValue() {
    return value;
  }

  @Override
  public void setValue(OptionForValues value) {
    this.value = value;
  }

  @Override
  public OptionForValuesParameter cloneParameter() {
    OptionForValuesParameter copy = new OptionForValuesParameter(name, description, format,
        valueRequired, options, operator, value);
    copy.setValue(this.getValue());
    return copy;
  }

  @Override
  public void setValueFromComponent(OptionForValuesComponent component) {
    value = component.getValue();
  }

  @Override
  public void setValueToComponent(OptionForValuesComponent component, @Nullable OptionForValues newValue) {
    component.setValue(newValue);
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    NodeList optionNode = xmlElement.getElementsByTagName("option");
    NodeList valueNode = xmlElement.getElementsByTagName("value");
    NodeList operatorNode = xmlElement.getElementsByTagName("operator");
    if (optionNode.getLength() != 1 || valueNode.getLength() != 1
        || operatorNode.getLength() != 1) {
      return;
    }
    ValueOption option = ValueOption.valueOf(optionNode.item(0).getTextContent());
    MathOperator operator = MathOperator.valueOf(operatorNode.item(0).getTextContent());
    double number = Double.parseDouble(valueNode.item(0).getTextContent());
    this.value = new OptionForValues(option, operator, number);
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (value == null) {
      return;
    }
    Document parentDocument = xmlElement.getOwnerDocument();
    Element newElement = parentDocument.createElement("option");
    newElement.setTextContent(value.option().name());
    xmlElement.appendChild(newElement);
    newElement = parentDocument.createElement("value");
    newElement.setTextContent(String.valueOf(value.value()));
    xmlElement.appendChild(newElement);
    newElement = parentDocument.createElement("operator");
    newElement.setTextContent(value.operator().name());
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
