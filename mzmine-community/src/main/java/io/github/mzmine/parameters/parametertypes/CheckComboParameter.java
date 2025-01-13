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

import io.github.mzmine.parameters.UserParameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.controlsfx.control.CheckComboBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * CheckComboBox Parameter implementation
 */
public class CheckComboParameter<ValueType> implements
    UserParameter<List<ValueType>, CheckComboBox<ValueType>> {

  public static String XML_ITEM_TAG = "selected";
  protected final String name;
  protected final String description;
  protected final ObservableList<ValueType> choices;
  @NotNull
  protected List<ValueType> value;
  private final boolean requiresSelection;

  public CheckComboParameter(String name, String description, ValueType[] choices) {
    this(name, description, choices, List.of());
  }

  public CheckComboParameter(String name, String description, ValueType[] choices,
      List<ValueType> defaultValue) {
    this(name, description, FXCollections.observableArrayList(choices), defaultValue);
  }

  public CheckComboParameter(String name, String description, List<ValueType> choices) {
    this(name, description, choices, List.of());
  }

  public CheckComboParameter(String name, String description, List<ValueType> choices,
      @NotNull List<ValueType> defaultValue) {
    this(name, description, choices, defaultValue, false);
  }

  public CheckComboParameter(String name, String description, List<ValueType> choices,
      @NotNull List<ValueType> defaultValue, boolean requiresSelection) {
    this.name = name;
    this.description = description;
    // requires defensive copy of choices otherwise may share choices between clones
    this.choices = FXCollections.observableArrayList(choices);
    this.value = defaultValue;
    this.requiresSelection = requiresSelection;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public CheckComboBox<ValueType> createEditingComponent() {
    CheckComboBox<ValueType> combo = new CheckComboBox<>(choices);
    combo.setShowCheckedCount(true);
    return combo;
  }

  @Override
  public @NotNull List<ValueType> getValue() {
    return value;
  }

  @Override
  public void setValue(@NotNull List<ValueType> value) {
    this.value = value;
  }

  public ObservableList<ValueType> getChoices() {
    return choices;
  }

  public void setChoices(ValueType[] newChoices) {
    choices.clear();
    choices.addAll(newChoices);
  }

  @Override
  public CheckComboParameter<ValueType> cloneParameter() {
    return new CheckComboParameter<>(name, description, choices, value);
  }

  @Override
  public void setValueFromComponent(CheckComboBox<ValueType> component) {
    value = List.copyOf(component.getCheckModel().getCheckedItems());
  }

  @Override
  public void setValueToComponent(CheckComboBox<ValueType> component,
      @Nullable List<ValueType> newValue) {
    if (newValue == null || newValue.isEmpty()) {
      component.getCheckModel().clearChecks();
      return;
    }
    component.getCheckModel().clearChecks();
    for (final ValueType value : newValue) {
      component.getCheckModel().check(value);
    }
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    NodeList itemElements = xmlElement.getElementsByTagName(XML_ITEM_TAG);
    Set<ValueType> selectedValues = new HashSet<>();

    for (int i = 0; i < itemElements.getLength(); i++) {
      Node itemElement = itemElements.item(i);

      String selected = itemElement.getTextContent();
      for (ValueType option : choices) {
        if (option.toString().equals(selected)) {
          selectedValues.add(option);
        }
      }
    }
    value = new ArrayList<>(selectedValues);
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (value.isEmpty()) {
      return;
    }

    final Document parent = xmlElement.getOwnerDocument();
    for (final var item : value) {
      final Element element = parent.createElement(XML_ITEM_TAG);
      element.setTextContent(item.toString());
      xmlElement.appendChild(element);
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
    return !requiresSelection || (value != null && !value.isEmpty());
  }

}
