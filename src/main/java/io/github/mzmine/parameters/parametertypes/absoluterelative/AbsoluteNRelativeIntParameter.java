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

public class AbsoluteNRelativeIntParameter
    implements UserParameter<AbsoluteNRelativeInt, AbsoluteNRelativeIntComponent> {

  private String name, description;
  private AbsoluteNRelativeInt value;
  private Integer minAbs, maxAbs;

  public AbsoluteNRelativeIntParameter(String name, String description) {
    this(name, description, 0, 0);
  }

  public AbsoluteNRelativeIntParameter(String name, String description, int abs, float rel) {
    this(name, description, abs, rel, AbsoluteNRelativeInt.Mode.ROUND, null, null);
  }


  public AbsoluteNRelativeIntParameter(String name, String description, int abs, float rel,
      int minAbs) {
    this(name, description, abs, rel, AbsoluteNRelativeInt.Mode.ROUND, minAbs, null);
  }

  public AbsoluteNRelativeIntParameter(String name, String description, int abs, float rel,
      int minAbs, int maxAbs) {
    this(name, description, abs, rel, AbsoluteNRelativeInt.Mode.ROUND, minAbs, maxAbs);
  }

  public AbsoluteNRelativeIntParameter(String name, String description, int abs, float rel,
      AbsoluteNRelativeInt.Mode roundMode) {
    this.name = name;
    this.description = description;
    value = new AbsoluteNRelativeInt(abs, rel, roundMode);
  }

  public AbsoluteNRelativeIntParameter(String name, String description, int abs, float rel,
      AbsoluteNRelativeInt.Mode roundMode, int minAbs) {
    this(name, description, abs, rel, roundMode, minAbs, null);
  }

  public AbsoluteNRelativeIntParameter(String name, String description, int abs, float rel,
      AbsoluteNRelativeInt.Mode roundMode, Integer minAbs, Integer maxAbs) {
    this.name = name;
    this.description = description;
    this.minAbs = minAbs;
    this.maxAbs = maxAbs;
    value = new AbsoluteNRelativeInt(abs, rel, roundMode);
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
  public AbsoluteNRelativeIntComponent createEditingComponent() {
    return new AbsoluteNRelativeIntComponent();
  }

  @Override
  public AbsoluteNRelativeIntParameter cloneParameter() {
    AbsoluteNRelativeIntParameter copy = new AbsoluteNRelativeIntParameter(name, description);
    copy.setValue(this.getValue());
    return copy;
  }

  @Override
  public void setValueFromComponent(AbsoluteNRelativeIntComponent component) {
    value = component.getValue();
  }

  @Override
  public void setValueToComponent(AbsoluteNRelativeIntComponent component,
      AbsoluteNRelativeInt newValue) {
    component.setValue(newValue);
  }

  @Override
  public AbsoluteNRelativeInt getValue() {
    return value;
  }

  @Override
  public void setValue(AbsoluteNRelativeInt newValue) {
    this.value = newValue;
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    // Set some default values
    int abs = 0;
    float rel = 0;
    NodeList items = xmlElement.getElementsByTagName("abs");
    for (int i = 0; i < items.getLength(); i++) {
      String itemString = items.item(i).getTextContent();
      abs = Integer.parseInt(itemString);
    }
    items = xmlElement.getElementsByTagName("rel");
    for (int i = 0; i < items.getLength(); i++) {
      String itemString = items.item(i).getTextContent();
      rel = Float.parseFloat(itemString);
    }

    this.value = new AbsoluteNRelativeInt(abs, rel);
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

  private boolean checkBounds(final int abs) {
    return (minAbs == null || minAbs <= abs) && (maxAbs == null || abs <= maxAbs);
  }
}
