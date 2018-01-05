/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

package net.sf.mzmine.parameters.parametertypes;

import java.awt.Color;
import java.util.Collection;
import javax.swing.BorderFactory;
import org.w3c.dom.Element;
import net.sf.mzmine.parameters.UserParameter;

public class ColorParameter implements UserParameter<Color, ColorComponent> {

  private String name, description;
  private Color value = Color.BLACK;

  public ColorParameter(String name, String description) {
    this(name, description, Color.black);
  }

  public ColorParameter(String name, String description, Color defaultValue) {
    this.name = name;
    this.description = description;
    this.value = defaultValue;
  }

  /**
   * @see net.sf.mzmine.data.Parameter#getName()
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * @see net.sf.mzmine.data.Parameter#getDescription()
   */
  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public ColorComponent createEditingComponent() {
    ColorComponent colorComponent = new ColorComponent(value);
    colorComponent.setBorder(BorderFactory.createCompoundBorder(colorComponent.getBorder(),
        BorderFactory.createEmptyBorder(0, 4, 0, 0)));
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
  public void setValueFromComponent(ColorComponent component) {
    value = component.getColor();
  }

  @Override
  public void setValueToComponent(ColorComponent component, Color newValue) {
    component.setColor(newValue);
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    String t = xmlElement.getTextContent();
    if (t != null) {
      try {
        String[] s = t.split(",");
        int r = Integer.valueOf(s[0]);
        int g = Integer.valueOf(s[1]);
        int b = Integer.valueOf(s[2]);
        int a = Integer.valueOf(s[3]);
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
    s.append(value.getAlpha());
    xmlElement.setTextContent(s.toString());
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    return true;
  }

}
