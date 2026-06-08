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
import java.util.function.Supplier;
import javafx.scene.Node;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

/**
 * Parameter represented by check box with an additional sub-parameter
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ComponentWrapperParameter<ValueType, EmbeddedParameterType extends UserParameter<ValueType, ?>> extends
    EmbeddedParameter<ValueType, EmbeddedParameterType, ComponentWrapperParameterComponent<?>> {

  @NotNull
  private final Supplier<Node> nodeSupplier;

  public ComponentWrapperParameter(EmbeddedParameterType embeddedParameter, @NotNull Supplier<Node> nodeSupplier) {
    super(embeddedParameter.getValue(), embeddedParameter);
    this.nodeSupplier = nodeSupplier;
  }

  @Override
  public ComponentWrapperParameterComponent<?> createEditingComponent() {
    return new ComponentWrapperParameterComponent(embeddedParameter, nodeSupplier);
  }

  @Override
  public ValueType getValue() {
    return embeddedParameter.getValue();
  }

  @Override
  public void setValue(ValueType value) {
    embeddedParameter.setValue(value);
  }

  @Override
  public ComponentWrapperParameter cloneParameter() {
    return new ComponentWrapperParameter(embeddedParameter.cloneParameter(), nodeSupplier);
  }

  @Override
  public void setValueFromComponent(ComponentWrapperParameterComponent component) {
    ((UserParameter)embeddedParameter).setValueFromComponent(component.getEmbeddedComponent());
  }

  @Override
  public void setValueToComponent(ComponentWrapperParameterComponent<?> component, @Nullable ValueType newValue) {
    ((UserParameter)embeddedParameter).setValueToComponent(component.getEmbeddedComponent(), newValue);
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    embeddedParameter.loadValueFromXML(xmlElement);
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    embeddedParameter.saveValueToXML(xmlElement);
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    return embeddedParameter.checkValue(errorMessages);
  }

  @Override
  public boolean valueEquals(Parameter<?> that) {
    if (!(that instanceof ComponentWrapperParameter thatOpt)) {
      return false;
    }

    return getEmbeddedParameter().valueEquals(thatOpt.embeddedParameter);
  }

}
