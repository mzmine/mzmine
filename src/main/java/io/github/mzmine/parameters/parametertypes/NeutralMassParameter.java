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

package io.github.mzmine.parameters.parametertypes;

import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.parameters.UserParameter;
import java.util.Collection;
import org.w3c.dom.Element;

public class NeutralMassParameter implements UserParameter<Double, NeutralMassComponent> {

  private final String name, description;
  private Double ionMass;
  private Integer charge;
  private IonizationType ionType;
  private Double value;

  public NeutralMassParameter(String name, String description) {
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
  public NeutralMassComponent createEditingComponent() {
    return new NeutralMassComponent();
  }

  @Override
  public NeutralMassParameter cloneParameter() {
    NeutralMassParameter copy = new NeutralMassParameter(name, description);
    copy.ionMass = ionMass;
    copy.ionType = ionType;
    copy.value = value;
    copy.charge = charge;
    return copy;
  }

  @Override
  public void setValueFromComponent(NeutralMassComponent component) {
    value = component.getValue();
    ionMass = component.getIonMass();
    charge = component.getCharge();
    ionType = component.getIonType();
  }

  @Override
  public void setValueToComponent(NeutralMassComponent component, Double newValue) {
    if (ionMass != null) {
      component.setIonMass(ionMass);
    }
    if (charge != null) {
      component.setCharge(charge);
    }
    if (ionType != null) {
      component.setIonType(ionType);
    }
  }

  @Override
  public Double getValue() {
    if (value == null) {
      updateNeutralMass();
    }
    // This is important for the dialog to realize that something is set
    return value == null ? ionMass : value;
  }

  @Override
  public void setValue(Double newValue) {
    this.value = newValue;
  }

  private void updateNeutralMass() {

    Integer charge = getCharge();
    if (charge == null) {
      return;
    }

    Double ionMass = getIonMass();
    if (ionMass == null) {
      return;
    }

    IonizationType ionType = getIonType();
    if (ionType == null) {
      return;
    }

    value = ionMass.doubleValue() * charge.intValue() - ionType.getAddedMass();
  }

  public Double getIonMass() {
    return ionMass;
  }

  public void setIonMass(Double newValue) {
    this.ionMass = newValue;
    value = null;
  }

  public IonizationType getIonType() {
    return ionType;
  }

  public void setIonType(IonizationType newType) {
    this.ionType = newType;
    value = null;
  }

  public Integer getCharge() {
    return charge;
  }

  public void setCharge(Integer charge) {
    this.charge = charge;
    value = null;
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    String typeAttr = xmlElement.getAttribute("type");
    if (typeAttr.length() == 0) {
      return;
    }
    this.ionType = IonizationType.valueOf(typeAttr);
    String chargeAttr = xmlElement.getAttribute("charge");
    this.charge = Integer.valueOf(chargeAttr);
    String elementText = xmlElement.getTextContent();
    this.ionMass = Double.valueOf(elementText);
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (value == null) {
      return;
    }
    xmlElement.setAttribute("type", ionType.name());
    xmlElement.setAttribute("charge", charge.toString());
    xmlElement.setTextContent(ionMass.toString());
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    if ((value == null) || (ionMass == null) || (charge == null) || (ionType == null)) {
      errorMessages.add(name + " is not set properly");
      return false;
    }
    return true;
  }
}
