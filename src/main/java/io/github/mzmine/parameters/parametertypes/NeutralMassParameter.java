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

import java.util.Collection;

import org.w3c.dom.Element;

import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.parameters.UserParameter;

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
    if (ionMass != null)
      component.setIonMass(ionMass);
    if (charge != null)
      component.setCharge(charge);
    if (ionType != null)
      component.setIonType(ionType);
  }

  @Override
  public Double getValue() {
    // This is important for the dialog to realize that something is set
    if (value == null)
      return ionMass;
    return value;
  }

  public Double getIonMass() {
    return ionMass;
  }

  public IonizationType getIonType() {
    return ionType;
  }

  public Integer getCharge() {
    return charge;
  }

  public void setCharge(Integer charge) {
    this.charge = charge;
  }

  @Override
  public void setValue(Double newValue) {
    this.value = newValue;
  }

  public void setIonMass(Double newValue) {
    this.ionMass = newValue;
  }

  public void setIonType(IonizationType newType) {
    this.ionType = newType;
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    String typeAttr = xmlElement.getAttribute("type");
    if (typeAttr.length() == 0)
      return;
    this.ionType = IonizationType.valueOf(typeAttr);
    String chargeAttr = xmlElement.getAttribute("charge");
    this.charge = Integer.valueOf(chargeAttr);
    String elementText = xmlElement.getTextContent();
    this.ionMass = Double.valueOf(elementText);
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (value == null)
      return;
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
