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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.w3c.dom.Element;

import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.util.maths.CenterFunction;
import io.github.mzmine.util.maths.CenterMeasure;
import io.github.mzmine.util.maths.Weighting;

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

  public CenterMeasureParameter(String name, String description) {
    this(name, description, CenterMeasure.values(), Weighting.values());
  }

  public CenterMeasureParameter(String name, String description, CenterMeasure choices[]) {
    this(name, description, choices, Weighting.values());
  }

  public CenterMeasureParameter(String name, String description, Weighting[] avgTransform) {
    this(name, description, CenterMeasure.values(), avgTransform);
  }

  public CenterMeasureParameter(String name, String description, CenterMeasure choices[],
      Weighting[] avgTransform) {
    this(name, description, choices, avgTransform, CenterMeasure.values()[0],
        Weighting.values()[0]);
  }

  public CenterMeasureParameter(String name, String description, CenterMeasure selectedMeasure) {
    this(name, description, CenterMeasure.values(), Weighting.values(), selectedMeasure,
        Weighting.NONE);
  }

  public CenterMeasureParameter(String name, String description, CenterMeasure selectedMeasure,
      Weighting selectedWeighting) {
    this(name, description, CenterMeasure.values(), Weighting.values(), selectedMeasure,
        selectedWeighting);
  }

  /**
   * 
   */
  public CenterMeasureParameter(String name, String description, CenterMeasure choices[],
      Weighting[] weighting, CenterMeasure selectedMeasure, Weighting selectedWeighting) {
    this.name = name;
    this.description = description;
    this.weighting = weighting;
    this.choices = choices;
    this.selectedMeasure = selectedMeasure;
    this.selectedWeighting = selectedWeighting == null ? Weighting.NONE : selectedWeighting;
    value = new CenterFunction(this.selectedMeasure, this.selectedWeighting);
  }

  /**
   */
  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public CenterMeasureComponent createEditingComponent() {
    return new CenterMeasureComponent(choices, weighting, selectedMeasure, selectedWeighting);
  }

  @Override
  public CenterFunction getValue() {
    return value;
  }

  @Override
  public void setValue(CenterFunction value) {
    this.value = value;
  }

  public void setValue(CenterMeasure value) {
    this.value = new CenterFunction(value);
  }

  @Override
  public CenterMeasureParameter cloneParameter() {
    CenterMeasureParameter copy = new CenterMeasureParameter(name, description, choices, weighting,
        value.getMeasure(), value.getWeightTransform());
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
