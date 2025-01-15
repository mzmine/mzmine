/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance.Unit;
import java.util.Collection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

public class RTToleranceParameter implements UserParameter<RTTolerance, RTToleranceComponent> {

  private static final String DEFAULT_NAME = "Retention time tolerance";
  private static final String DEFAULT_DESC = "Maximum allowed difference between two retention time values";
  private final ObservableList<Unit> toleranceTypes;
  private String name, description;
  private RTTolerance value;

  public RTToleranceParameter() {
    this(DEFAULT_NAME, DEFAULT_DESC);
  }

  public RTToleranceParameter(String name, String description) {
    this(name, description, FXCollections.observableArrayList(RTTolerance.Unit.values()));
  }

  public RTToleranceParameter(String name, String description,
      ObservableList<Unit> toleranceTypes) {
    this.name = name;
    this.description = description;
    this.toleranceTypes = toleranceTypes;
  }

  public RTToleranceParameter(String name, String description, RTTolerance defaultValue) {
    this.name = name;
    this.description = description;
    this.value = defaultValue;
    this.toleranceTypes = FXCollections.observableArrayList(RTTolerance.Unit.values());
  }

  public RTToleranceParameter(RTTolerance defaultValue) {
    this.name = DEFAULT_NAME;
    this.description = DEFAULT_DESC;
    ;
    this.value = defaultValue;
    this.toleranceTypes = FXCollections.observableArrayList(RTTolerance.Unit.values());
  }

  public RTToleranceParameter(String name, String description, RTTolerance defaultValue,
      ObservableList<Unit> toleranceTypes) {
    this.name = name;
    this.description = description;
    this.value = defaultValue;
    this.toleranceTypes = toleranceTypes;
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
  public RTToleranceComponent createEditingComponent() {
    return new RTToleranceComponent(toleranceTypes);
  }

  @Override
  public RTToleranceParameter cloneParameter() {
    RTToleranceParameter copy = new RTToleranceParameter(name, description, toleranceTypes);
    copy.setValue(this.getValue());
    return copy;
  }

  @Override
  public void setValueFromComponent(RTToleranceComponent component) {
    this.value = component.getValue();
  }

  @Override
  public void setValueToComponent(RTToleranceComponent component, @Nullable RTTolerance newValue) {
    component.setValue(newValue);
  }

  @Override
  public RTTolerance getValue() {
    return value;
  }

  @Override
  public void setValue(RTTolerance newValue) {
    this.value = newValue;
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    String unitAttr = xmlElement.getAttribute("unit");
    if (unitAttr == null || unitAttr.isEmpty()) {
      return;
    }
    RTTolerance.Unit toleranceUnit = RTTolerance.Unit.valueOf(unitAttr);
    String toleranceNum = xmlElement.getTextContent();
    if (toleranceNum.length() == 0) {
      return;
    }
    float tolerance = (float) Double.parseDouble(toleranceNum);
    this.value = new RTTolerance(tolerance, toleranceUnit);
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (value == null) {
      return;
    }
    xmlElement.setAttribute("unit", value.getUnit().name());
    float tolerance = (float) value.getTolerance();
    String toleranceNum = String.valueOf(tolerance);
    xmlElement.setTextContent(toleranceNum);
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    if (value == null) {
      errorMessages.add(name + " is not set properly");
      return false;
    }
    if (value.isAbsolute()) {
      double absoluteTolerance = value.getTolerance();
      if (absoluteTolerance < 0) {
        errorMessages.add("Invalid retention time tolerance value.");
        return false;

      }
    } else {
      double relativeTolerance = value.getTolerance();
      if ((relativeTolerance < 0) || (relativeTolerance > 100)) {
        errorMessages.add("Invalid retention time tolerance value.");
        return false;

      }
    }
    return true;
  }
}
