/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
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
