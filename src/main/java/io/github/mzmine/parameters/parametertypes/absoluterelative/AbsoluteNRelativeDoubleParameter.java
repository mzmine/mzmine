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
