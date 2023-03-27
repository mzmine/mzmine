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

import io.github.mzmine.parameters.UserParameter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
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
  public void setValueToComponent(OptOutComponent optOutComponent, @Nullable Map<String, Boolean> newValue) {
    // no component (yet)
  }

  @Override
  public UserParameter<Map<String, Boolean>, OptOutComponent> cloneParameter() {
    OptOutParameter clone = new OptOutParameter(name, description);
    value.forEach((k, v) -> clone.getValue().put(k, v.booleanValue())); // we want a duplicate here
    return clone;
  }
}
