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
import java.util.Collection;
import org.w3c.dom.Element;
import io.github.mzmine.parameters.UserParameter;
import javafx.scene.Node;

/**
 * Parameter represented by check box with an additional sub-parameter
 *
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class OptionalParameter<EmbeddedParameterType extends UserParameter<?, ?>>
    implements UserParameter<Boolean, OptionalParameterComponent<?>> {

  private EmbeddedParameterType embeddedParameter;

  // It is important to set default value here, otherwise the embedded value
  // is not shown in the parameter setup dialog
  private Boolean value = false;

  public OptionalParameter(EmbeddedParameterType embeddedParameter) {
    this.embeddedParameter = embeddedParameter;
  }

  public OptionalParameter(EmbeddedParameterType embeddedParameter, boolean defaultValue) {
    this.embeddedParameter = embeddedParameter;
    value = defaultValue;
  }

  public EmbeddedParameterType getEmbeddedParameter() {
    return embeddedParameter;
  }

  /**
   * @see io.github.mzmine.data.Parameter#getName()
   */
  @Override
  public String getName() {
    return embeddedParameter.getName();
  }

  /**
   * @see io.github.mzmine.data.Parameter#getDescription()
   */
  @Override
  public String getDescription() {
    return embeddedParameter.getDescription();
  }

  @Override
  public OptionalParameterComponent<?> createEditingComponent() {
    return new OptionalParameterComponent(embeddedParameter);
  }

  @Override
  public Boolean getValue() {
    return value;
  }

  @Override
  public void setValue(Boolean value) {
    this.value = value;
  }

  @Override
  public OptionalParameter cloneParameter() {
    final UserParameter<?, ?> embeddedParameterClone = embeddedParameter.cloneParameter();
    final OptionalParameter copy = new OptionalParameter(embeddedParameterClone);
    copy.setValue(this.getValue());
    return copy;
  }

  @Override
  public void setValueFromComponent(OptionalParameterComponent component) {
    this.value = component.isSelected();
    if (value) {
      Node embeddedComponent = component.getEmbeddedComponent();
      ((UserParameter) this.embeddedParameter).setValueFromComponent(embeddedComponent);
    }
  }

  @Override
  public void setValueToComponent(OptionalParameterComponent<?> component, Boolean newValue) {
    component.setSelected(newValue);
    if (embeddedParameter.getValue() != null) {
      ((UserParameter) this.embeddedParameter).setValueToComponent(component.getEmbeddedComponent(),
          embeddedParameter.getValue());
    }
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    embeddedParameter.loadValueFromXML(xmlElement);
    String selectedAttr = xmlElement.getAttribute("selected");
    this.value = Boolean.valueOf(selectedAttr);
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (value != null)
      xmlElement.setAttribute("selected", value.toString());
    embeddedParameter.saveValueToXML(xmlElement);
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    if (value == null) {
      errorMessages.add(getName() + " is not set properly");
      return false;
    }
    if (value == true) {
      return embeddedParameter.checkValue(errorMessages);
    }
    return true;
  }

  @Override
  public boolean valueEquals(Parameter<?> that) {
    if(!(that instanceof OptionalParameter thatOpt)) {
      return false;
    }

    if(value != thatOpt.getValue()) {
      return false;
    }

    return getEmbeddedParameter().valueEquals(((OptionalParameter<?>) that).embeddedParameter);
  }
}
