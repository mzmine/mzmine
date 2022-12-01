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
