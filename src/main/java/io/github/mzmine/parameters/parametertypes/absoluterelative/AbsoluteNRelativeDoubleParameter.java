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

package io.github.mzmine.parameters.parametertypes.absoluterelative;

import java.util.Collection;

import io.github.mzmine.parameters.UserParameter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class AbsoluteNRelativeDoubleParameter
    implements UserParameter<AbsoluteNRelativeDouble, AbsoluteNRelativeDoubleComponent> {

  private String name, description;
  private AbsoluteNRelativeDouble value;

  public AbsoluteNRelativeDoubleParameter(String name, String description) {
    this(name, description, 0, 0);
  }

  public AbsoluteNRelativeDoubleParameter(String name, String description, double abs, double rel) {
    this.name = name;
    this.description = description;
    value = new AbsoluteNRelativeDouble(abs, rel);
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
  public AbsoluteNRelativeDoubleComponent createEditingComponent() {
    return new AbsoluteNRelativeDoubleComponent();
  }

  @Override
  public AbsoluteNRelativeDoubleParameter cloneParameter() {
    AbsoluteNRelativeDoubleParameter copy = new AbsoluteNRelativeDoubleParameter(name, description);
    copy.setValue(this.getValue());
    return copy;
  }

  @Override
  public void setValueFromComponent(AbsoluteNRelativeDoubleComponent component) {
    value = component.getValue();
  }

  @Override
  public void setValueToComponent(AbsoluteNRelativeDoubleComponent component,
      AbsoluteNRelativeDouble newValue) {
    component.setValue(newValue);
  }

  @Override
  public AbsoluteNRelativeDouble getValue() {
    return value;
  }

  @Override
  public void setValue(AbsoluteNRelativeDouble newValue) {
    this.value = newValue;
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    // Set some default values
    double abs = 0;
    double rel = 0;
    NodeList items = xmlElement.getElementsByTagName("abs");
    for (int i = 0; i < items.getLength(); i++) {
      String itemString = items.item(i).getTextContent();
      abs = Double.parseDouble(itemString);
    }
    items = xmlElement.getElementsByTagName("rel");
    for (int i = 0; i < items.getLength(); i++) {
      String itemString = items.item(i).getTextContent();
      rel = Double.parseDouble(itemString);
    }

    this.value = new AbsoluteNRelativeDouble(abs, rel);
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (value == null)
      return;
    Document parentDocument = xmlElement.getOwnerDocument();
    Element newElement = parentDocument.createElement("abs");
    newElement.setTextContent(String.valueOf(value.getAbsolute()));
    xmlElement.appendChild(newElement);
    newElement = parentDocument.createElement("rel");
    newElement.setTextContent(String.valueOf(value.getRelative()));
    xmlElement.appendChild(newElement);
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    if (value == null) {
      errorMessages.add(name + " is not set properly");
      return false;
    }
    return true;
  }

}
