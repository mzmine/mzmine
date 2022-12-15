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

package io.github.mzmine.parameters.parametertypes.tolerances;

import java.util.Collection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import io.github.mzmine.parameters.UserParameter;

public class MZToleranceParameter implements UserParameter<MZTolerance, MZToleranceComponent> {

  private String name;
  private String description;
  private MZTolerance value;
  private boolean zeroAllowed;

  public MZToleranceParameter() {
    this("m/z tolerance",
        "Maximum allowed difference between two m/z values to be considered same.\n"
            + "The value is specified both as absolute tolerance (in m/z) and relative tolerance (in ppm).\n"
            + "The tolerance range is calculated using maximum of the absolute and relative tolerances.");
  }

  public MZToleranceParameter(String name, String description) {
    this.name = name;
    this.description = description;
    this.zeroAllowed = false;
  }

  public MZToleranceParameter(String name, String description, double deltaMZ, double ppm) {
    this.name = name;
    this.description = description;
    this.zeroAllowed = false;
    value = new MZTolerance(deltaMZ, ppm);
  }

  public MZToleranceParameter(String name, String description, double deltaMZ, double ppm,
      boolean zeroAllowed) {
    this.name = name;
    this.description = description;
    value = new MZTolerance(deltaMZ, ppm);
    this.zeroAllowed = zeroAllowed;
  }

  public MZToleranceParameter(double deltaMZ, double ppm) {
    this();
    value = new MZTolerance(deltaMZ, ppm);
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
  public MZToleranceComponent createEditingComponent() {
    return new MZToleranceComponent();
  }

  @Override
  public MZToleranceParameter cloneParameter() {
    MZToleranceParameter copy = new MZToleranceParameter(name, description);
    copy.setValue(this.getValue());
    return copy;
  }

  @Override
  public void setValueFromComponent(MZToleranceComponent component) {
    value = component.getValue();
  }

  @Override
  public void setValueToComponent(MZToleranceComponent component, MZTolerance newValue) {
    component.setValue(newValue);
  }

  @Override
  public MZTolerance getValue() {
    return value;
  }

  @Override
  public void setValue(MZTolerance newValue) {
    this.value = newValue;
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    // Set some default values
    double mzTolerance = 0.001;
    double ppmTolerance = 5;
    NodeList items = xmlElement.getElementsByTagName("absolutetolerance");
    for (int i = 0; i < items.getLength(); i++) {
      String itemString = items.item(i).getTextContent();
      mzTolerance = Double.parseDouble(itemString);
    }
    items = xmlElement.getElementsByTagName("ppmtolerance");
    for (int i = 0; i < items.getLength(); i++) {
      String itemString = items.item(i).getTextContent();
      ppmTolerance = Double.parseDouble(itemString);
    }
    this.value = new MZTolerance(mzTolerance, ppmTolerance);
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (value == null) {
      return;
    }
    Document parentDocument = xmlElement.getOwnerDocument();
    Element newElement = parentDocument.createElement("absolutetolerance");
    newElement.setTextContent(String.valueOf(value.getMzTolerance()));
    xmlElement.appendChild(newElement);
    newElement = parentDocument.createElement("ppmtolerance");
    newElement.setTextContent(String.valueOf(value.getPpmTolerance()));
    xmlElement.appendChild(newElement);
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    if (value == null) {
      errorMessages.add(name + " is not set properly");
      return false;
    }

    if (((value.getMzTolerance() <= 0.0) && (value.getPpmTolerance() <= 0.0) && !zeroAllowed) ||
        (value.getMzTolerance() < 0.0 && value.getPpmTolerance() < 0.0 && zeroAllowed)) {
      errorMessages.add(name + " must be greater than zero");
      return false;
    }
    return true;
  }

}
