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

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.UserParameter;
import java.util.Collection;
import java.util.Objects;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import org.w3c.dom.Element;

/**
 * Parameter represented by combobox and an embedded parameter that only activates when a trigger
 * value is selected in the combo box
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ComboWithInputParameter<EnumType, EmbeddedParameterType extends UserParameter<?, ?>> implements
    UserParameter<EnumType, ComboWIthInputComponent<EnumType>>,
    EmbeddedParameter<EmbeddedParameterType> {

  private final EmbeddedParameterType embeddedParameter;
  private final ObservableList<EnumType> choices;
  private final EnumType inputTrigger;
  private EnumType value;

  public ComboWithInputParameter(EmbeddedParameterType embeddedParameter, final EnumType[] values,
      final EnumType selected, final EnumType inputTrigger) {
    this(embeddedParameter, FXCollections.observableArrayList(values), selected, inputTrigger);
  }

  public ComboWithInputParameter(EmbeddedParameterType embeddedParameter,
      final ObservableList<EnumType> values, final EnumType selected, final EnumType inputTrigger) {
    this.embeddedParameter = embeddedParameter;
    choices = FXCollections.observableArrayList(values);
    value = selected;
    this.inputTrigger = inputTrigger;
  }

  @Override
  public EmbeddedParameterType getEmbeddedParameter() {
    return embeddedParameter;
  }

  public Object getEmbeddedValue() {
    return embeddedParameter.getValue();
  }

  @Override
  public String getName() {
    return embeddedParameter.getName();
  }

  @Override
  public String getDescription() {
    return embeddedParameter.getDescription();
  }

  @Override
  public ComboWIthInputComponent createEditingComponent() {
    return new ComboWIthInputComponent(embeddedParameter, choices, value, inputTrigger);
  }

  @Override
  public EnumType getValue() {
    return value;
  }

  @Override
  public void setValue(EnumType value) {
    this.value = value;
  }

  @Override
  public ComboWithInputParameter cloneParameter() {
    final UserParameter<?, ?> embeddedParameterClone = embeddedParameter.cloneParameter();
    return new ComboWithInputParameter(embeddedParameterClone, choices, value, inputTrigger);
  }

  @Override
  public void setValueFromComponent(ComboWIthInputComponent<EnumType> component) {
    this.value = component.getValue();
    if (useEmbeddedParameter()) {
      Node embeddedComponent = component.getEmbeddedComponent();
      ((UserParameter) this.embeddedParameter).setValueFromComponent(embeddedComponent);
    }
  }

  /**
   * Use embedded parameter when value triggerInput is selected
   *
   * @return true if trigger is selected
   */
  public boolean useEmbeddedParameter() {
    return Objects.equals(value, inputTrigger);
  }

  @Override
  public void setValueToComponent(ComboWIthInputComponent<EnumType> component, EnumType newValue) {
    component.setValue(newValue);
    if (embeddedParameter.getValue() != null) {
      ((UserParameter) this.embeddedParameter).setValueToComponent(component.getEmbeddedComponent(),
          embeddedParameter.getValue());
    }
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    embeddedParameter.loadValueFromXML(xmlElement);
    String selectedAttr = xmlElement.getAttribute("selected");

    if (selectedAttr.isEmpty()) {
      return;
    }
    for (EnumType option : choices) {
      if (option.toString().equals(selectedAttr)) {
        value = option;
        break;
      }
    }
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (value == null) {
      return;
    }
    xmlElement.setAttribute("selected", value.toString());
    embeddedParameter.saveValueToXML(xmlElement);
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    if (value == null) {
      errorMessages.add(getName() + " is not set properly");
      return false;
    }
    if (useEmbeddedParameter()) {
      return embeddedParameter.checkValue(errorMessages);
    }
    return true;
  }

  @Override
  public boolean valueEquals(Parameter<?> that) {
    if (!(that instanceof ComboWithInputParameter thatOpt)) {
      return false;
    }

    if (!Objects.equals(value, thatOpt.getValue())) {
      return false;
    }

    return !useEmbeddedParameter() || getEmbeddedParameter().valueEquals(thatOpt.embeddedParameter);
  }
}
