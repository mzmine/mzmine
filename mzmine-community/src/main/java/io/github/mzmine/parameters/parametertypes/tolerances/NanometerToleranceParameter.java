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

import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityTolerance;
import java.util.Collection;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

public class NanometerToleranceParameter implements
    UserParameter<NanometerTolerance, NanometerToleranceComponent> {

  private final String name;
  private final String description;
  private @Nullable NanometerTolerance value;

  public NanometerToleranceParameter() {
    this("Nanometer tolerance", "Maximum allowed difference between two mobility values");
  }

  public NanometerToleranceParameter(@Nullable NanometerTolerance defaultValue) {
    this();
    value = defaultValue;
  }

  public NanometerToleranceParameter(String name, String description) {
    this.name = name;
    this.description = description;
  }

  public NanometerToleranceParameter(String name, String description,
      @Nullable NanometerTolerance defaultValue) {
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
  public NanometerToleranceComponent createEditingComponent() {
    return new NanometerToleranceComponent();
  }

  @Override
  public NanometerToleranceParameter cloneParameter() {
    NanometerToleranceParameter copy = new NanometerToleranceParameter(name, description);
    copy.setValue(this.getValue());
    return copy;
  }

  @Override
  public void setValueFromComponent(NanometerToleranceComponent component) {
    this.value = component.getValue();
  }

  @Override
  public void setValueToComponent(NanometerToleranceComponent component,
      @Nullable NanometerTolerance newValue) {
    component.setValue(newValue);
  }

  @Override
  public @Nullable NanometerTolerance getValue() {
    return value;
  }

  @Override
  public void setValue(@Nullable NanometerTolerance newValue) {
    this.value = newValue;
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    String toleranceNum = xmlElement.getTextContent();
    if (toleranceNum.isEmpty()) {
      return;
    }
    double tolerance = Double.parseDouble(toleranceNum);
    this.value = new NanometerTolerance(tolerance);
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (value == null) {
      return;
    }
    double tolerance = value.getTolerance();
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
