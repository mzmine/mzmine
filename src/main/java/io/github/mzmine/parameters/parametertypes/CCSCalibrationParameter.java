/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
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
