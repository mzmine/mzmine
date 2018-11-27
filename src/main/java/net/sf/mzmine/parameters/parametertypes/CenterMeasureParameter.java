/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.parameters.parametertypes;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.w3c.dom.Element;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.util.maths.CenterFunction;
import net.sf.mzmine.util.maths.CenterMeasure;
import net.sf.mzmine.util.maths.Weighting;

/**
 * Parameter for center measure: median, avg, weighted avg
 * 
 */
public class CenterMeasureParameter
    implements UserParameter<CenterFunction, CenterMeasureComponent> {

  private Logger logger = Logger.getLogger(getClass().getName());

  private String name, description;
  private CenterFunction value;
  private CenterMeasure[] choices;
  private Weighting[] weighting;
  private CenterMeasure selectedMeasure;
  private Weighting selectedWeighting;


  public CenterMeasureParameter() {
    this(CenterMeasure.values(), Weighting.values());
  }

  public CenterMeasureParameter(CenterMeasure choices[]) {
    this(choices, Weighting.values());
  }

  public CenterMeasureParameter(Weighting[] avgTransform) {
    this(CenterMeasure.values(), avgTransform);
  }

  public CenterMeasureParameter(CenterMeasure choices[], Weighting[] avgTransform) {
    this(choices, avgTransform, CenterMeasure.values()[0], Weighting.values()[0]);
  }

  public CenterMeasureParameter(CenterMeasure selectedMeasure) {
    this(CenterMeasure.values(), Weighting.values(), selectedMeasure, Weighting.NONE);
  }

  public CenterMeasureParameter(CenterMeasure selectedMeasure, Weighting selectedWeighting) {
    this(CenterMeasure.values(), Weighting.values(), selectedMeasure, selectedWeighting);
  }

  /**
   * 
   * @param choices
   * @param avgTransform
   * @param selected selected center measure
   * @param selWeighting selected weighting
   */
  public CenterMeasureParameter(CenterMeasure choices[], Weighting[] weighting,
      CenterMeasure selectedMeasure, Weighting selectedWeighting) {
    this.name = "Center measure";
    this.description = "Center measure (weighting options for average calculation)";
    this.weighting = weighting;
    this.choices = choices;
    this.selectedMeasure = selectedMeasure;
    this.selectedWeighting = selectedWeighting;
    value = new CenterFunction(selectedMeasure, selectedWeighting);
  }

  /**
   * @see net.sf.mzmine.data.Parameter#getDescription()
   */
  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public CenterMeasureComponent createEditingComponent() {
    CenterMeasureComponent comboComponent =
        new CenterMeasureComponent(choices, weighting, selectedMeasure, selectedWeighting);
    return comboComponent;
  }

  @Override
  public CenterFunction getValue() {
    return value;
  }

  @Override
  public void setValue(CenterFunction value) {
    this.value = value;
  }

  @Override
  public CenterMeasureParameter cloneParameter() {
    CenterMeasureParameter copy = new CenterMeasureParameter(choices, weighting, value.getMeasure(),
        value.getWeightTransform());
    copy.value = this.value;
    return copy;
  }

  @Override
  public void setValueFromComponent(CenterMeasureComponent component) {
    // never null
    value = component.getSelectedFunction();
  }

  @Override
  public void setValueToComponent(CenterMeasureComponent component, CenterFunction newValue) {
    component.setSelectedItem(newValue);
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    try {
      CenterMeasure measure = CenterMeasure.valueOf(xmlElement.getAttribute("measure"));
      Weighting weighting = Weighting.valueOf(xmlElement.getAttribute("weighting"));
      value = new CenterFunction(measure, weighting);
    } catch (Exception e) {
      logger.log(Level.WARNING, "center measure cannot be loaded from xml", e);
    }
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (value == null)
      return;
    xmlElement.setTextContent("CenterFunction");
    xmlElement.setAttribute("measure", value.getMeasure().name());
    xmlElement.setAttribute("weighting", value.getWeightTransform().name());
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return name;
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
