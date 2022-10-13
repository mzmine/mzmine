/*
 * Copyright 2006-2022 The MZmine Development Team
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

import io.github.mzmine.modules.io.projectsave.RawDataSavingUtils;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterContainer;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.UserParameter;
import java.util.Collection;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;

/**
 * This adds an accordion to the parameter pane with additional parameters. Those parameters should
 * only be used if the value (check box) is selected. One use case is the advanced batch mode.
 *
 * @author Robin Schmid <a href="https://github.com/robinschmid">https://github.com/robinschmid</a>
 */
public class AdvancedParametersParameter<T extends ParameterSet> implements
    UserParameter<Boolean, AdvancedParametersComponent>, ParameterContainer, EmbeddedParameterSet {

  private final String name;
  private final String description;
  private T embeddedParameters;
  private Boolean value;

  public AdvancedParametersParameter(String name, String description, T embeddedParameters,
      boolean defaultVal) {
    this.name = name;
    this.description = description;
    this.embeddedParameters = embeddedParameters;
    value = defaultVal;
  }

  public AdvancedParametersParameter(String name, String description, T embeddedParameters) {
    this(name, description, embeddedParameters, false);
  }

  public AdvancedParametersParameter(T embeddedParameters) {
    this("Advanced", "Advanced parameters", embeddedParameters, false);
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
  public AdvancedParametersComponent createEditingComponent() {
    return new AdvancedParametersComponent(embeddedParameters, name, value);
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
  public AdvancedParametersParameter<T> cloneParameter() {
    final T embeddedParametersClone = (T) embeddedParameters.cloneParameterSet();
    final AdvancedParametersParameter<T> copy = new AdvancedParametersParameter<>(name, description,
        embeddedParametersClone);
    copy.setValue(this.getValue());
    return copy;
  }

  @Override
  public void setValueFromComponent(AdvancedParametersComponent component) {
    this.value = component.isSelected();
    component.getValue();
  }

  @Override
  public void setValueToComponent(AdvancedParametersComponent component, Boolean newValue) {
    component.setSelected(Objects.requireNonNullElse(newValue, false));
    component.setValue(embeddedParameters);
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
    if (!(that instanceof AdvancedParametersParameter thatOpt)) {
      return false;
    }

    if (value != thatOpt.getValue()) {
      return false;
    }

    return RawDataSavingUtils.parameterSetsEqual(getEmbeddedParameters(),
        thatOpt.getEmbeddedParameters(), false, false);
  }
}
