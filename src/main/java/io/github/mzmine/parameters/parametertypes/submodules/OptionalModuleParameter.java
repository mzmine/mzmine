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

package io.github.mzmine.parameters.parametertypes.submodules;

import io.github.mzmine.modules.io.projectsave.RawDataSavingUtils;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterContainer;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.parametertypes.EmbeddedParameterSet;
import java.util.Collection;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;

/**
 * Parameter represented by check box with additional sub-module
 */
public class OptionalModuleParameter<T extends ParameterSet> implements
    UserParameter<Boolean, OptionalModuleComponent>, ParameterContainer, EmbeddedParameterSet {

  private final String name;
  private final String description;
  private T embeddedParameters;
  private Boolean value;

  public OptionalModuleParameter(String name, String description, T embeddedParameters,
      boolean defaultVal) {
    this.name = name;
    this.description = description;
    this.embeddedParameters = embeddedParameters;
    value = defaultVal;
  }

  public OptionalModuleParameter(String name, String description, T embeddedParameters) {
    this(name, description, embeddedParameters, false);
  }

  public T getEmbeddedParameters() {
    return embeddedParameters;
  }

  public void setEmbeddedParameters(T embeddedParameters) {
    this.embeddedParameters = embeddedParameters;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public OptionalModuleComponent createEditingComponent() {
    return new OptionalModuleComponent(embeddedParameters);
  }

  @Override
  public Boolean getValue() {
    // If the option is selected, first check that the module has all
    // parameters set
    if ((value != null) && (value)) {
      for (Parameter<?> p : embeddedParameters.getParameters()) {
        if (p instanceof UserParameter<?, ?> up) {
          Object upValue = up.getValue();
          if (upValue == null) {
            return null;
          }
        }
      }
    }
    return value;
  }

  @Override
  public void setValue(@NotNull Boolean value) {
    this.value = Objects.requireNonNullElse(value, false);
  }

  @Override
  public OptionalModuleParameter<T> cloneParameter() {
    final T embeddedParametersClone = (T) embeddedParameters.cloneParameterSet();
    final OptionalModuleParameter<T> copy = new OptionalModuleParameter<>(name, description,
        embeddedParametersClone);
    copy.setValue(this.getValue());
    return copy;
  }

  @Override
  public void setValueFromComponent(OptionalModuleComponent component) {
    this.value = component.isSelected();
  }

  @Override
  public void setValueToComponent(OptionalModuleComponent component, Boolean newValue) {
    component.setSelected(Objects.requireNonNullElse(newValue, false));
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    embeddedParameters.loadValuesFromXML(xmlElement);
    String selectedAttr = xmlElement.getAttribute("selected");
    this.value = Objects.requireNonNullElse(Boolean.valueOf(selectedAttr), false);
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (value != null) {
      xmlElement.setAttribute("selected", value.toString());
    }
    embeddedParameters.saveValuesToXML(xmlElement);
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    if (value == null) {
      errorMessages.add(name + " is not set properly");
      return false;
    }
    if (value) {
      return embeddedParameters.checkParameterValues(errorMessages);
    }
    return true;
  }

  @Override
  public void setSkipSensitiveParameters(boolean skipSensitiveParameters) {
    // delegate skipSensitiveParameters to embedded ParameterSet
    embeddedParameters.setSkipSensitiveParameters(skipSensitiveParameters);
  }

  @Override
  public boolean valueEquals(Parameter<?> that) {
    if (!(that instanceof OptionalModuleParameter thatOpt)) {
      return false;
    }

    if (value != thatOpt.getValue()) {
      return false;
    }

    return RawDataSavingUtils.parameterSetsEqual(getEmbeddedParameters(),
        thatOpt.getEmbeddedParameters(), false, false);
  }
}
