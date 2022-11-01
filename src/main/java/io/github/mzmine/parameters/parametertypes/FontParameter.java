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

import java.util.Collection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import io.github.mzmine.parameters.UserParameter;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class FontParameter implements UserParameter<FontSpecs, FontSpecsComponent> {

  private String name, description;
  private FontSpecs value = new FontSpecs(Color.BLACK, Font.font("Arial", FontWeight.NORMAL, 11.0));

  public FontParameter(String name, String description) {
    this(name, description, null);
  }

  public FontParameter(String name, String description, FontSpecs defaultValue) {
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
  public FontSpecsComponent createEditingComponent() {
    FontSpecsComponent f = new FontSpecsComponent();
    // f.setBorder(BorderFactory.createCompoundBorder(f.getBorder(),BorderFactory.createEmptyBorder(0,
    // 4, 0, 0)));
    return f;
  }

  @Override
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
  public void setValueFromComponent(FontSpecsComponent component) {
    Font font = component.getFont();
    Color color = component.getColor();
    value = new FontSpecs(color, font);
  }

  @Override
  public void setValueToComponent(FontSpecsComponent component, FontSpecs newValue) {
    component.setFont(newValue.getFont());
    component.setColor(newValue.getColor());
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
      FontWeight style = FontWeight.findByName(s[1]);
      double size = Double.valueOf(s[2]);
      Font f = Font.font(name, style, size);

      // color
      s = scolor.split(",");
      double r = Double.valueOf(s[0]);
      double g = Double.valueOf(s[1]);
      double b = Double.valueOf(s[2]);
      double a = Double.valueOf(s[3]);
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
    s.append(f.getName());
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
    s.append(c.getOpacity());

    newElement = parentDocument.createElement("color");
    newElement.setTextContent(s.toString());
    xmlElement.appendChild(newElement);
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    return true;
  }

}
