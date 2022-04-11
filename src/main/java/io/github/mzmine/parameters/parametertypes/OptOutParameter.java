/*
 * Copyright 2006-2021 The MZmine Development Team
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

import io.github.mzmine.parameters.UserParameter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Saves a mapping of string -> boolean to store answers to opt out messages.
 */
public class OptOutParameter implements UserParameter<Map<String, Boolean>, OptOutComponent> {

  public static final String MODULE_TAG = "module";
  public static final String MODULE_NAME = "name";
  private final String name;
  private final String description;
  private Map<String, Boolean> value;

  public OptOutParameter(String name, String description) {
    this.name = name;
    this.description = description;
    this.value = new HashMap<>();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Map<String, Boolean> getValue() {
    return value;
  }

  @Override
  public void setValue(Map<String, Boolean> newValue) {
    this.value = newValue;
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    return true;
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    NodeList nodeList = xmlElement.getElementsByTagName(MODULE_TAG);
    for (int i = 0; i < nodeList.getLength(); i++) {
      Element element = (Element) nodeList.item(i);
      String module = element.getAttribute(MODULE_NAME);
      Boolean val = Boolean.valueOf(element.getTextContent());
      value.put(module, val);
    }
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    Document doc = xmlElement.getOwnerDocument();

    value.forEach((key, value) -> {
      Element element = doc.createElement(MODULE_TAG);
      element.setAttribute(MODULE_NAME, key);
      element.setTextContent(value.toString());
      xmlElement.appendChild(element);
    });
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public OptOutComponent createEditingComponent() {
    return new OptOutComponent();
  }

  @Override
  public void setValueFromComponent(OptOutComponent optOutComponent) {
    // no component (yet)
  }

  @Override
  public void setValueToComponent(OptOutComponent optOutComponent, Map<String, Boolean> newValue) {
    // no component (yet)
  }

  @Override
  public UserParameter<Map<String, Boolean>, OptOutComponent> cloneParameter() {
    OptOutParameter clone = new OptOutParameter(name, description);
    value.forEach((k, v) -> clone.getValue().put(k, v.booleanValue())); // we want a duplicate here
    return clone;
  }
}
