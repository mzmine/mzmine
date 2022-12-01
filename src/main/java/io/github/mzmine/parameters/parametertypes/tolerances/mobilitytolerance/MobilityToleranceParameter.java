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

package io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance;

import io.github.mzmine.parameters.UserParameter;
import java.util.Collection;
import org.w3c.dom.Element;

public class MobilityToleranceParameter implements UserParameter<MobilityTolerance, MobilityToleranceComponent> {

  private final String name;
  private final String description;
  private MobilityTolerance value;

  public MobilityToleranceParameter() {
    this("Mobility time tolerance",
        "Maximum allowed difference between two mobility values");
  }

  public MobilityToleranceParameter(MobilityTolerance defaultValue) {
    this();
    value = defaultValue;
  }

  public MobilityToleranceParameter(String name, String description) {
    this.name = name;
    this.description = description;
  }

  public MobilityToleranceParameter(String name, String description, MobilityTolerance defaultValue) {
    this.name = name;
    this.description = description;
    this.value = defaultValue;
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
  public MobilityToleranceComponent createEditingComponent() {
    return new MobilityToleranceComponent();
  }

  @Override
  public MobilityToleranceParameter cloneParameter() {
    MobilityToleranceParameter copy = new MobilityToleranceParameter(name, description);
    copy.setValue(this.getValue());
    return copy;
  }

  @Override
  public void setValueFromComponent(
      MobilityToleranceComponent component) {
    this.value = component.getValue();
  }

  @Override
  public void setValueToComponent(
      MobilityToleranceComponent component, MobilityTolerance newValue) {
    component.setValue(newValue);
  }

  @Override
  public MobilityTolerance getValue() {
    return value;
  }

  @Override
  public void setValue(MobilityTolerance newValue) {
    this.value = newValue;
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    String toleranceNum = xmlElement.getTextContent();
    if (toleranceNum.length() == 0)
      return;
    float tolerance = Float.parseFloat(toleranceNum);
    this.value = new MobilityTolerance(tolerance);
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (value == null) {
      return;
    }
    float tolerance = value.getTolerance();
    String toleranceNum = String.valueOf(tolerance);
    xmlElement.setTextContent(toleranceNum);
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
