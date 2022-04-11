/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.parameters.parametertypes.submodules;


import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.parametertypes.EmbeddedParameterSet;
import org.w3c.dom.Element;

import java.util.Collection;

/**
 * Parameter represented by check box with additional sub-parameters
 * 
 */
public class SubModuleParameter<SUB extends ParameterSet>
    implements UserParameter<Boolean, SubModuleComponent>, EmbeddedParameterSet {

  private String name, description;
  private SUB embeddedParameters;

  public SubModuleParameter(String name, String description, SUB embeddedParameters) {
    this.name = name;
    this.description = description;
    this.embeddedParameters = embeddedParameters;
  }

  public SUB getEmbeddedParameters() {
    return embeddedParameters;
  }

  public void setEmbeddedParameters(SUB param) {
    embeddedParameters = param;
  }

  /**
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   */
  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public SubModuleComponent createEditingComponent() {
    return new SubModuleComponent(embeddedParameters);
  }

  @Override
  public Boolean getValue() {
    // If the option is selected, first check that the module has all
    // parameters set
    for (Parameter<?> p : embeddedParameters.getParameters()) {
      if (p instanceof UserParameter) {
        UserParameter<?, ?> up = (UserParameter<?, ?>) p;
        Object upValue = up.getValue();
        if (upValue == null)
          return null;
      }
    }
    return true;
  }

  @Override
  public void setValue(Boolean value) {}

  @Override
  public SubModuleParameter<SUB> cloneParameter() {
    final SUB embeddedParametersClone = (SUB) embeddedParameters.cloneParameterSet();
    final SubModuleParameter<SUB> copy =
        new SubModuleParameter<SUB>(name, description, embeddedParametersClone);
    copy.setValue(this.getValue());
    return copy;
  }

  @Override
  public void setValueFromComponent(SubModuleComponent component) {}

  @Override
  public void setValueToComponent(SubModuleComponent component, Boolean newValue) {}

  @Override
  public void loadValueFromXML(Element xmlElement) {
    embeddedParameters.loadValuesFromXML(xmlElement);
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    embeddedParameters.saveValuesToXML(xmlElement);
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    return embeddedParameters.checkParameterValues(errorMessages);
  }
}
