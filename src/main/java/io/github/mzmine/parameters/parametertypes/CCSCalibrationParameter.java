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

import io.github.mzmine.modules.dataprocessing.id_ccscalibration.CCSCalibration;
import io.github.mzmine.parameters.UserParameter;
import java.util.Collection;
import javafx.scene.layout.BorderPane;
import org.w3c.dom.Element;

/**
 * Parameter to store a calibration, does not have a visible component.
 */
public class CCSCalibrationParameter implements
    UserParameter<CCSCalibration, BorderPane> {

  private final String name;
  private final String description;
  private CCSCalibration value;

  public CCSCalibrationParameter(String name, String description) {
    this.name = name;
    this.description = description;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public CCSCalibration getValue() {
    return value;
  }

  @Override
  public void setValue(CCSCalibration newValue) {
    value = newValue;
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    return value != null;
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    value = CCSCalibration.loadFromXML(xmlElement);
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (value == null) {
      return;
    }
    value.saveToXML(xmlElement);
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public BorderPane createEditingComponent() {
    return new BorderPane();
  }

  @Override
  public void setValueFromComponent(BorderPane ccsCalibrationComponent) {
    // no component
  }

  @Override
  public void setValueToComponent(BorderPane ccsCalibrationComponent,
      CCSCalibration newValue) {
// no component
  }

  @Override
  public UserParameter<CCSCalibration, BorderPane> cloneParameter() {
    final CCSCalibrationParameter param = new CCSCalibrationParameter(name, description);
    param.setValue(value);
    return param;
  }
}
