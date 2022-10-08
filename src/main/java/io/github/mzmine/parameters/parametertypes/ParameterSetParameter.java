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
