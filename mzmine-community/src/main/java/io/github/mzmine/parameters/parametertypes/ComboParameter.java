/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import io.github.mzmine.parameters.PropertyParameter;
import java.util.Collection;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

/**
 * Combo Parameter implementation
 */
public class ComboParameter<ValueType> implements
    PropertyParameter<ValueType, ComboComponent<ValueType>> {

  private final String name;
  private final String description;
  private final ObservableList<ValueType> choices;
  protected ValueType value;

  public ComboParameter(String name, String description, ValueType[] choices) {
    this(name, description, choices, null);
  }

  public ComboParameter(String name, String description, ValueType[] choices,
      ValueType defaultValue) {
    this(name, description, FXCollections.observableArrayList(choices), defaultValue);
  }

  public ComboParameter(String name, String description, List<ValueType> choices) {
    this(name, description, choices, null);
  }

  public ComboParameter(String name, String description, List<ValueType> choices,
      ValueType defaultValue) {
    this.name = name;
    this.description = description;
    // requires defensive copy of choices otherwise may share choices between clones
    this.choices = FXCollections.observableArrayList(choices);
    this.value = defaultValue;
    if (defaultValue == null && !choices.isEmpty()) {
      this.value = choices.get(0);
    }
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public ComboComponent<ValueType> createEditingComponent() {
    ComboComponent<ValueType> comboComponent = new ComboComponent<>(choices);
    comboComponent.getSelectionModel().selectFirst();
    return comboComponent;
  }

  @Override
  public ValueType getValue() {
    return value;
  }

  @Override
  public void setValue(ValueType value) {
    this.value = value;
  }

  public ObservableList<ValueType> getChoices() {
    return choices;
  }

  public void setChoices(ValueType[] newChoices) {
    choices.setAll(newChoices);
  }

  public void setChoices(ValueType[] newChoices, ValueType active) {
    setChoices(newChoices);
    setValue(active);
  }

  @Override
  public ComboParameter<ValueType> cloneParameter() {
    ComboParameter<ValueType> copy = new ComboParameter<ValueType>(name, description, choices,
        value);
    return copy;
  }

  @Override
  public void setValueFromComponent(ComboComponent<ValueType> component) {
    value = component.getSelectionModel().getSelectedItem();
  }

  @Override
  public void setValueToComponent(ComboComponent<ValueType> component,
      @Nullable ValueType newValue) {
    if (newValue == null) {
      component.getSelectionModel().clearSelection();
      return;
    }
    component.getSelectionModel().select(newValue);
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    String elementString = xmlElement.getTextContent();
    if (elementString.isEmpty()) {
      return;
    }
    for (ValueType option : choices) {
      if ((option instanceof UniqueIdSupplier uis && uis.getUniqueID().equals(elementString))
          || option.toString().equals(elementString)) {
        value = option;
        return;
      }
    }
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (value == null) {
      return;
    }
    if (value instanceof UniqueIdSupplier uis) {
      xmlElement.setTextContent(uis.getUniqueID());
    } else {
      xmlElement.setTextContent(value.toString());
    }
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
