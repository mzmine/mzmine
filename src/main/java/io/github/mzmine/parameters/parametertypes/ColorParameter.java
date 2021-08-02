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
import javafx.scene.control.ColorPicker;
import javafx.scene.paint.Color;
import org.w3c.dom.Element;

public class ColorParameter implements UserParameter<Color, ColorPicker> {

  private String name, description;
  private Color value = Color.BLACK;

  public ColorParameter(String name, String description) {
    this(name, description, Color.BLACK);
  }

  public ColorParameter(String name, String description, Color defaultValue) {
    this.name = name;
    this.description = description;
    this.value = defaultValue;
  }

  /**
   * @see io.github.mzmine.data.Parameter#getName()
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * @see io.github.mzmine.data.Parameter#getDescription()
   */
  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public ColorPicker createEditingComponent() {
    ColorPicker colorComponent = new ColorPicker(value);
    // colorComponent.setBorder(BorderFactory.createCompoundBorder(colorComponent.getBorder(),
    // BorderFactory.createEmptyBorder(0, 4, 0, 0)));
    return colorComponent;
  }

  public Color getValue() {
    return value;
  }

  @Override
  public void setValue(Color value) {
    this.value = value;
  }

  @Override
  public ColorParameter cloneParameter() {
    ColorParameter copy = new ColorParameter(name, description);
    copy.setValue(this.getValue());
    return copy;
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public void setValueFromComponent(ColorPicker component) {
    value = component.getValue();
  }

  @Override
  public void setValueToComponent(ColorPicker component, Color newValue) {
    component.setValue(newValue);
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    String t = xmlElement.getTextContent();
    if (t != null) {
      try {
        String[] s = t.split(",");
        double r = Double.valueOf(s[0]);
        double g = Double.valueOf(s[1]);
        double b = Double.valueOf(s[2]);
        double a = Double.valueOf(s[3]);
        value = new Color(r, g, b, a);
      } catch (Exception e) {
      }
    }
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (value == null)
      return;
    StringBuilder s = new StringBuilder();
    s.append(value.getRed());
    s.append(",");
    s.append(value.getGreen());
    s.append(",");
    s.append(value.getBlue());
    s.append(",");
    s.append(value.getOpacity());
    xmlElement.setTextContent(s.toString());
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    return true;
  }

}
