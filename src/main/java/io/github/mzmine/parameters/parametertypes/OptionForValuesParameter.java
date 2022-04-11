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
import io.github.mzmine.util.maths.MathOperator;
import java.text.NumberFormat;
import java.util.Collection;
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
  public void setValueToComponent(OptionForValuesComponent component, OptionForValues newValue) {
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
