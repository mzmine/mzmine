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
import java.util.Collection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import org.w3c.dom.Element;

/**
 * Combo Parameter implementation
 *
 */
public class ComboParameter<ValueType> implements UserParameter<ValueType, ComboBox<ValueType>> {

  private String name, description;
  private ObservableList<ValueType> choices;
  protected ValueType value;

  public ComboParameter(String name, String description, ValueType choices[]) {
    this(name, description, choices, null);
  }

  public ComboParameter(String name, String description, ValueType[] choices,
      ValueType defaultValue) {
    this(name, description, FXCollections.observableArrayList(choices), defaultValue);
  }

  public ComboParameter(String name, String description, ObservableList<ValueType> choices) {
    this(name, description, choices, null);
  }

  public ComboParameter(String name, String description, ObservableList<ValueType> choices,
      ValueType defaultValue) {
    this.name = name;
    this.description = description;
    this.choices = choices;
    this.value = defaultValue;
  }

  /**
   * @see io.github.mzmine.data.Parameter#getDescription()
   */
  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public ComboBox<ValueType> createEditingComponent() {
    ComboBox<ValueType> comboComponent = new ComboBox<ValueType>(choices);
    comboComponent.getSelectionModel().selectFirst();
    return comboComponent;
  }

  @Override
  public ValueType getValue() {
    return value;
  }

  public ObservableList<ValueType> getChoices() {
    return choices;
  }

  public void setChoices(ValueType newChoices[]) {
    choices.clear();
    choices.addAll(newChoices);
  }

  @Override
  public void setValue(ValueType value) {
    this.value = value;
  }

  @Override
  public ComboParameter<ValueType> cloneParameter() {
    ComboParameter<ValueType> copy = new ComboParameter<ValueType>(name, description, choices);
    copy.value = this.value;
    return copy;
  }

  @Override
  public void setValueFromComponent(ComboBox<ValueType> component) {
    value = component.getSelectionModel().getSelectedItem();
  }

  @Override
  public void setValueToComponent(ComboBox<ValueType> component, ValueType newValue) {
    component.getSelectionModel().select(newValue);
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    String elementString = xmlElement.getTextContent();
    if (elementString.length() == 0)
      return;
    for (ValueType option : choices) {
      if (option.toString().equals(elementString)) {
        value = option;
        break;
      }
    }
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (value == null)
      return;
    xmlElement.setTextContent(value.toString());
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return name;
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
