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
import io.github.mzmine.util.CollectionUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.controlsfx.control.CheckListView;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Simple Parameter implementation
 */
public class MultiChoiceParameter<ValueType> implements
    UserParameter<ValueType[], CheckListView<ValueType>> {

  private final String name, description;
  private final int minNumber;
  private ValueType[] choices, values;

  /**
   * We need the choices parameter non-null even when the length may be 0. We need it to determine
   * the class of the ValueType.
   */
  public MultiChoiceParameter(String name, String description, ValueType[] choices) {
    this(name, description, choices, null, 1);
  }

  public MultiChoiceParameter(String name, String description, ValueType[] choices,
      ValueType[] values) {
    this(name, description, choices, values, 1);
  }

  public MultiChoiceParameter(String name, String description, ValueType[] choices,
      ValueType[] values, int minNumber) {

    assert choices != null;

    this.name = name;
    this.description = description;
    this.choices = choices;
    this.values = values;
    this.minNumber = minNumber;
  }

  /**
   *
   */
  @Override
  public String getName() {
    return name;
  }

  public void setChoices(ValueType[] choices) {
    this.choices = choices;
  }

  public ValueType[] getChoices() {
    return choices;
  }

  /**
   *
   */
  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public CheckListView<ValueType> createEditingComponent() {
    final ObservableList<ValueType> choicesList = FXCollections.observableArrayList(
        Arrays.asList(choices));
    final CheckListView<ValueType> comp = new CheckListView<>(choicesList);
    comp.setPrefHeight(150);
    return comp;
  }

  @Override
  public ValueType[] getValue() {
    return values;
  }

  @Override
  public void setValue(ValueType[] values) {
    this.values = values;
  }

  @Override
  public MultiChoiceParameter<ValueType> cloneParameter() {
    MultiChoiceParameter<ValueType> copy = new MultiChoiceParameter<ValueType>(name, description,
        choices, values);
    copy.setValue(this.getValue());
    return copy;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void setValueFromComponent(CheckListView<ValueType> component) {
    ObservableList<ValueType> selectedList = component.getCheckModel().getCheckedItems();
    Class<ValueType> arrayType = (Class<ValueType>) this.choices.getClass().getComponentType();
    this.values = CollectionUtils.changeArrayType(selectedList.toArray(), arrayType);
  }

  @Override
  public void setValueToComponent(CheckListView<ValueType> component, ValueType[] newValue) {
    component.getSelectionModel().clearSelection();
    component.getCheckModel().clearChecks();
    for (ValueType v : newValue) {
      component.getSelectionModel().select(v);
      component.getCheckModel().check(v);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void loadValueFromXML(Element xmlElement) {
    NodeList items = xmlElement.getElementsByTagName("item");
    ArrayList<ValueType> newValues = new ArrayList<ValueType>();
    for (int i = 0; i < items.getLength(); i++) {
      String itemString = items.item(i).getTextContent();
      for (int j = 0; j < choices.length; j++) {
        if (choices[j].toString().equals(itemString)) {
          newValues.add(choices[j]);
        }
      }
    }
    Class<ValueType> arrayType = (Class<ValueType>) this.choices.getClass().getComponentType();
    Object[] newArray = newValues.toArray();
    this.values = CollectionUtils.changeArrayType(newArray, arrayType);
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (values == null) {
      return;
    }
    Document parentDocument = xmlElement.getOwnerDocument();
    for (ValueType item : values) {
      Element newElement = parentDocument.createElement("item");
      newElement.setTextContent(item.toString());
      xmlElement.appendChild(newElement);
    }
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    if (values == null) {
      errorMessages.add(name + " is not set properly");
      return false;
    }
    if (values.length < minNumber) {
      errorMessages.add("At least " + minNumber + " option(s) must be selected for " + name);
      return false;
    }
    return true;
  }
}
