/*
 * Copyright 2006-2016 The MZmine 3 Development Team
 * 
 * This file is part of MZmine 3.
 * 
 * MZmine 3 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 3 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 3; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.parameters.parametertypes;

import javax.annotation.Nonnull;

import org.w3c.dom.Element;

/**
 * Parameter represented by check box with additional sub-parameters
 * 
 */
public class OptionalParameter<EmbeddedParameter extends AbstractParameter<?>>
    extends AbstractParameter<Boolean> {

  private final EmbeddedParameter embeddedParameter;

  public OptionalParameter(@Nonnull EmbeddedParameter embeddedParameters) {
    super(embeddedParameters.getName(), embeddedParameters.getDescription(),
        embeddedParameters.getCategory(), OptionalEditor.class, null);
    this.embeddedParameter = embeddedParameters;
  }

  public EmbeddedParameter getEmbeddedParameter() {
    return embeddedParameter;
  }

  @SuppressWarnings("unchecked")
  @Override
  public @Nonnull OptionalParameter<EmbeddedParameter> clone() {
    final EmbeddedParameter embeddedParameterClone = (EmbeddedParameter) embeddedParameter.clone();
    final OptionalParameter<EmbeddedParameter> copy =
        new OptionalParameter<EmbeddedParameter>(embeddedParameterClone);
    copy.setValue(this.getValue());
    return copy;
  }

  @Override
  public void loadValueFromXML(@Nonnull Element xmlElement) {
    embeddedParameter.loadValueFromXML(xmlElement);
    String selectedAttr = xmlElement.getAttribute("selected");
    setValue(Boolean.valueOf(selectedAttr));
  }

  @Override
  public void saveValueToXML(@Nonnull Element xmlElement) {
    Boolean value = getValue();
    if (value != null)
      xmlElement.setAttribute("selected", value.toString());
    embeddedParameter.saveValueToXML(xmlElement);
  }

}
