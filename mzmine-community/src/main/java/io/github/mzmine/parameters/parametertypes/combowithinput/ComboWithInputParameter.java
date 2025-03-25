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

package io.github.mzmine.parameters.parametertypes.combowithinput;

import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.parametertypes.EmbeddedParameter;
import java.util.Collection;
import java.util.Objects;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

/**
 * Parameter represented by combobox and an embedded parameter that only activates when a trigger
 * value is selected in the combo box
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class ComboWithInputParameter<EnumType, ValueType extends ComboWithInputValue<EnumType, ?>, EmbeddedParameterType extends UserParameter<?, ?>> extends
    EmbeddedParameter<ValueType, EmbeddedParameterType, ComboWithInputComponent<EnumType>> {

  protected final ObservableList<EnumType> choices;
  protected final EnumType inputTrigger;
  protected ValueType value;

  public ComboWithInputParameter(EmbeddedParameterType embeddedParameter, final EnumType[] values,
      final EnumType inputTrigger, ValueType defaultValue) {
    this(embeddedParameter, FXCollections.observableArrayList(values), inputTrigger, defaultValue);
  }

  public ComboWithInputParameter(EmbeddedParameterType embeddedParameter,
      final ObservableList<EnumType> values, final EnumType inputTrigger, ValueType defaultValue) {
    super(defaultValue, embeddedParameter);
    choices = FXCollections.observableArrayList(values);
    this.inputTrigger = inputTrigger;
    setValue(defaultValue);
  }

  /**
   * Create a new value
   */
  public abstract ValueType createValue(final EnumType option,
      final EmbeddedParameterType embeddedParameter);


  @Override
  public ComboWithInputComponent createEditingComponent() {
    return new ComboWithInputComponent(embeddedParameter, choices, inputTrigger, value);
  }

  @Override
  public ValueType getValue() {
    return value;
  }

  @Override
  public void setValue(final ValueType newValue) {
    value = newValue;
    ((Parameter) embeddedParameter).setValue(value == null ? null : value.getEmbeddedValue());
  }


  @Override
  public void setValueFromComponent(ComboWithInputComponent<EnumType> component) {
    var option = component.getSelectedOption();
    if (useEmbeddedParameter()) {
      Node embeddedComponent = component.getEmbeddedComponent();
      ((UserParameter) this.embeddedParameter).setValueFromComponent(embeddedComponent);
    }
    setValue(createValue(option, embeddedParameter));
  }

  /**
   * Use embedded parameter when value triggerInput is selected
   *
   * @return true if trigger is selected
   */
  public boolean useEmbeddedParameter() {
    return value != null && Objects.equals(value.getSelectedOption(), inputTrigger);
  }

  @Override
  public void setValueToComponent(ComboWithInputComponent<EnumType> component, @Nullable ValueType newValue) {
    component.setValue(newValue);
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    embeddedParameter.loadValueFromXML(xmlElement);
    String selectedAttr = xmlElement.getAttribute("selected");

    if (selectedAttr.isEmpty()) {
      return;
    }
    for (EnumType option : choices) {
      if (option instanceof UniqueIdSupplier uid) {
        if (uid.getUniqueID().equals(selectedAttr)) {
          setValue(createValue(option, embeddedParameter));
        }
      } else {
        if (option.toString().equals(selectedAttr)) {
          setValue(createValue(option, embeddedParameter));
          break;
        }
      }
    }
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (value == null) {
      return;
    }
    if (value instanceof UniqueIdSupplier uniqueId) {
      xmlElement.setAttribute("selected", uniqueId.getUniqueID());
    } else {
      xmlElement.setAttribute("selected", value.getSelectedOption().toString());
    }
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
