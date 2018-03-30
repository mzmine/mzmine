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
import java.awt.Font;
import java.util.Collection;
import javax.swing.BorderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import net.sf.mzmine.framework.fontspecs.FontSpecs;
import net.sf.mzmine.framework.fontspecs.JFontSpecs;
import net.sf.mzmine.parameters.UserParameter;

public class FontParameter implements UserParameter<FontSpecs, JFontSpecs> {

  private String name, description;
  private FontSpecs value = new FontSpecs(Color.BLACK, new Font("Arial", Font.PLAIN, 11));

  public FontParameter(String name, String description) {
    this(name, description, null);
  }

  public FontParameter(String name, String description, FontSpecs defaultValue) {
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
  public JFontSpecs createEditingComponent() {
    JFontSpecs f = new JFontSpecs();
    f.setBorder(BorderFactory.createCompoundBorder(f.getBorder(),
        BorderFactory.createEmptyBorder(0, 4, 0, 0)));
    return f;
  }

  public FontSpecs getValue() {
    return value;
  }

  @Override
  public void setValue(FontSpecs value) {
    this.value = value;
  }

  @Override
  public FontParameter cloneParameter() {
    FontParameter copy = new FontParameter(name, description);
    copy.setValue(this.getValue());
    return copy;
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public void setValueFromComponent(JFontSpecs component) {
    value = component.getFontSpecs();
  }

  @Override
  public void setValueToComponent(JFontSpecs component, FontSpecs newValue) {
    component.setFontSpecs(newValue);
  }


  @Override
  public void loadValueFromXML(Element xmlElement) {
    NodeList colorNodes = xmlElement.getElementsByTagName("color");
    if (colorNodes.getLength() != 1)
      return;
    NodeList fontNodes = xmlElement.getElementsByTagName("font");
    if (fontNodes.getLength() != 1)
      return;
    String scolor = colorNodes.item(0).getTextContent();
    String sfont = fontNodes.item(0).getTextContent();

    try {
      // font
      String[] s = sfont.split(",");
      String name = s[0];
      int style = Integer.valueOf(s[1]);
      int size = Integer.valueOf(s[2]);
      Font f = new Font(name, style, size);

      // color
      s = scolor.split(",");
      int r = Integer.valueOf(s[0]);
      int g = Integer.valueOf(s[1]);
      int b = Integer.valueOf(s[2]);
      int a = Integer.valueOf(s[3]);
      Color c = new Color(r, g, b, a);

      value = new FontSpecs(c, f);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (value == null)
      return;
    Font f = value.getFont();
    StringBuilder s = new StringBuilder();
    s.append(f.getFontName());
    s.append(",");
    s.append(f.getStyle());
    s.append(",");
    s.append(f.getSize());

    Document parentDocument = xmlElement.getOwnerDocument();
    Element newElement = parentDocument.createElement("font");
    newElement.setTextContent(s.toString());
    xmlElement.appendChild(newElement);

    Color c = value.getColor();
    s = new StringBuilder();
    s.append(c.getRed());
    s.append(",");
    s.append(c.getGreen());
    s.append(",");
    s.append(c.getBlue());
    s.append(",");
    s.append(c.getAlpha());

    newElement = parentDocument.createElement("color");
    newElement.setTextContent(s.toString());
    xmlElement.appendChild(newElement);
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    return true;
  }

}
