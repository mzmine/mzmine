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

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterContainer;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.UserParameter;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * @author aleksandrsmirnov
 */
public class ParameterSetParameter implements UserParameter<ParameterSet, ParameterSetComponent>,
    ParameterContainer, EmbeddedParameterSet {

  private static final Logger logger = Logger.getLogger(ParameterSetParameter.class.getName());
  private final String name;
  private final String description;
  private ParameterSet value;

  private static final String parameterElement = "parameter";
  private static final String nameAttribute = "name";

  public ParameterSetParameter() {
    this("", "", null);
  }

  public ParameterSetParameter(String name, String description, ParameterSet parameters) {
    this.name = name;
    this.description = description;
    this.value = parameters;
  }

  public ParameterSet getValue() {
    return value;
  }

  public void setValue(final ParameterSet parameters) {
    this.value = parameters;
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public String getDescription() {
    return this.description;
  }

  @Override
  public ParameterSetParameter cloneParameter() {
    return new ParameterSetParameter(this.name, this.description, value);
  }

  @Override
  public void setValueToComponent(final ParameterSetComponent component,
      final ParameterSet parameters) {
    component.setValue(parameters);
  }

  @Override
  public void setValueFromComponent(final ParameterSetComponent component) {
    value = component.getValue();
  }

  @Override
  public ParameterSetComponent createEditingComponent() {
    return new ParameterSetComponent(this.value);
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (this.value == null) {
      return;
    }

    xmlElement.setAttribute("type", this.name);
    Document parent = xmlElement.getOwnerDocument();

    for (Parameter p : this.value.getParameters()) {
      Element newElement = parent.createElement(parameterElement);
      newElement.setAttribute(nameAttribute, p.getName());
      xmlElement.appendChild(newElement);
      p.saveValueToXML(newElement);
    }
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    NodeList list = xmlElement.getElementsByTagName(parameterElement);
    for (int i = 0; i < list.getLength(); ++i) {
      Element nextElement = (Element) list.item(i);
      String paramName = nextElement.getAttribute(nameAttribute);
      for (Parameter p : this.value.getParameters()) {
        if (p.getName().equals(paramName)) {
          try {
            p.loadValueFromXML(nextElement);
          } catch (Exception e) {
            logger.log(Level.WARNING, "Error while loading parameter values for " + p.getName(), e);
          }
        }
      }
    }
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {

    boolean result = true;
    for (final Parameter p : this.value.getParameters()) {
      result &= p.checkValue(errorMessages);
    }

    return result;
  }

  @Override
  public void setSkipSensitiveParameters(boolean skipSensitiveParameters) {
    // delegate skipSensitiveParameters embedded ParameterContainers
    value.setSkipSensitiveParameters(skipSensitiveParameters);
  }

  @Override
  public ParameterSet getEmbeddedParameters() {
    return getValue();
  }
}
