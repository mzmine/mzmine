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

import java.util.Collection;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import io.github.mzmine.parameters.UserParameter;

/**
 * Simple Parameter implementation
 * 
 * 
 */
public class OrderParameter<ValueType>
    implements UserParameter<ValueType[], OrderComponent<ValueType>> {

  private String name, description;
  private ValueType value[];

  public OrderParameter(String name, String description, ValueType value[]) {

    assert value != null;

    this.name = name;
    this.description = description;
    this.value = value;
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
  public ValueType[] getValue() {
    return value;
  }

  @Override
  public OrderComponent<ValueType> createEditingComponent() {
    return new OrderComponent<ValueType>();
  }

  @Override
  public OrderParameter<ValueType> cloneParameter() {
    OrderParameter<ValueType> copy = new OrderParameter<ValueType>(name, description, value);
    copy.setValue(this.getValue());
    return copy;
  }

  @Override
  public void setValueFromComponent(OrderComponent<ValueType> component) {
    Object newOrder[] = component.getValues();
    System.arraycopy(newOrder, 0, this.value, 0, newOrder.length);
  }

  @Override
  public void setValueToComponent(OrderComponent<ValueType> component, ValueType[] newValue) {
    component.setValues(newValue);
  }

  @Override
  public void setValue(ValueType[] newValue) {
    assert newValue != null;
    this.value = newValue;
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    NodeList items = xmlElement.getElementsByTagName("item");
    ValueType newValues[] = value.clone();
    for (int i = 0; i < items.getLength(); i++) {
      String itemString = items.item(i).getTextContent();
      for (int j = i + 1; j < newValues.length; j++) {
        if (newValues[j].toString().equals(itemString)) {
          ValueType swap = newValues[i];
          newValues[i] = newValues[j];
          newValues[j] = swap;
        }
      }
    }
    value = newValues;
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (value == null)
      return;
    Document parentDocument = xmlElement.getOwnerDocument();
    for (ValueType item : value) {
      Element newElement = parentDocument.createElement("item");
      newElement.setTextContent(item.toString());
      xmlElement.appendChild(newElement);
    }
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    if (value == null) {
      errorMessages.add(name + " is not set properly");
      return false;
    }
    return true;
  }

}
