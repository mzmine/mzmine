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

import io.github.mzmine.parameters.UserParameter;
import java.text.NumberFormat;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class AbsoluteAndRelativeDoubleParameter implements
    UserParameter<AbsoluteAndRelativeDouble, AbsoluteAndRelativeDoubleComponent> {

  protected final String absUnit;
  protected final String name;
  protected final String description;
  protected final Double minAbs, maxAbs;
  private final NumberFormat absFormat;
  protected AbsoluteAndRelativeDouble value;

  public AbsoluteAndRelativeDoubleParameter(String name, String description, String absUnit,
      AbsoluteAndRelativeDouble value, @NotNull NumberFormat absFormat) {
    this(name, description, absUnit, value, null, absFormat);
  }


  public AbsoluteAndRelativeDoubleParameter(String name, String description, String absUnit,
      AbsoluteAndRelativeDouble value, Double minAbs, @NotNull NumberFormat absFormat) {
    this(name, description, absUnit, value, minAbs, null, absFormat);
  }

  public AbsoluteAndRelativeDoubleParameter(String name, String description, String absUnit,
      AbsoluteAndRelativeDouble value, Double minAbs, Double maxAbs,
      @NotNull NumberFormat absFormat) {
    this.name = name;
    this.description = description;
    this.absUnit = absUnit;
    this.minAbs = minAbs;
    this.maxAbs = maxAbs;
    this.value = value;
    this.absFormat = absFormat;
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
  public AbsoluteAndRelativeDoubleComponent createEditingComponent() {
    return new AbsoluteAndRelativeDoubleComponent(absUnit, absFormat);
  }

  @Override
  public AbsoluteAndRelativeDoubleParameter cloneParameter() {
    return new AbsoluteAndRelativeDoubleParameter(name, description, absUnit, value, minAbs, maxAbs,
        absFormat);
  }

  @Override
  public void setValueFromComponent(AbsoluteAndRelativeDoubleComponent component) {
    value = component.getValue();
  }

  @Override
  public void setValueToComponent(AbsoluteAndRelativeDoubleComponent component,
      AbsoluteAndRelativeDouble newValue) {
    component.setValue(newValue);
  }

  @Override
  public AbsoluteAndRelativeDouble getValue() {
    return value;
  }

  @Override
  public void setValue(AbsoluteAndRelativeDouble newValue) {
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

    this.value = new AbsoluteAndRelativeDouble(abs, rel);
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (value == null) {
      return;
    }
    Document parentDocument = xmlElement.getOwnerDocument();
    Element newElement = parentDocument.createElement("abs");
    newElement.setTextContent(String.valueOf(value.getAbsolute()));
    xmlElement.appendChild(newElement);
    newElement = parentDocument.createElement("rel");
    newElement.setTextContent(String.valueOf(value.getRelative()));
    xmlElement.appendChild(newElement);
  }

  @Override
  public boolean checkValue(final Collection<String> errorMessages) {
    if ((value == null)) {
      errorMessages.add(name + " is not set properly");
      return false;
    }

    if (!checkBounds(value.getAbsolute())) {
      String max = maxAbs != null ? "" + maxAbs : "";
      String min = minAbs != null ? "" + minAbs : "";

      errorMessages.add(name + " lies outside its bounds: (" + min + " ... " + max + ')');
      return false;
    }

    return true;
  }

  private boolean checkBounds(final double abs) {
    return (minAbs == null || minAbs <= abs) && (maxAbs == null || abs <= maxAbs);
  }
}
