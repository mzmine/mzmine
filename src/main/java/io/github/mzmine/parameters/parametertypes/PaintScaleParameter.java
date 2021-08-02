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

import java.util.Collection;
import org.w3c.dom.Element;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScale;
import io.github.mzmine.parameters.UserParameter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/*
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class PaintScaleParameter implements UserParameter<PaintScale, PaintScaleComponent> {

  private String name;
  private String description;
  private ObservableList<PaintScale> choices;
  private PaintScale value;

  public PaintScaleParameter(String name, String description, PaintScale[] choices) {
    this(name, description, FXCollections.observableArrayList(choices), choices[0]);
  }

  public PaintScaleParameter(String name, String description, PaintScale[] choices,
      PaintScale defaultValue) {
    this(name, description, FXCollections.observableArrayList(choices), defaultValue);
  }

  public PaintScaleParameter(String name, String description, ObservableList<PaintScale> choices) {
    this(name, description, choices, null);
  }

  public PaintScaleParameter(String name, String description, ObservableList<PaintScale> choices,
      PaintScale defaultValue) {
    this.name = name;
    this.description = description;
    this.choices = choices;
    this.value = defaultValue;
  }

  /**
   * @see io.github.mzmine.data.Parameter#getDescription()
   */
  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public PaintScale getValue() {
    return value;
  }

  public ObservableList<PaintScale> getChoices() {
    return choices;
  }

  public void setChoices(PaintScale[] newChoices) {
    choices.clear();
    choices.addAll(newChoices);
  }

  @Override
  public void setValue(PaintScale value) {
    this.value = value;
  }

  @Override
  public PaintScaleParameter cloneParameter() {
    PaintScaleParameter copy = new PaintScaleParameter(name, description, choices);
    copy.value = this.value;
    return copy;
  }

  @Override
  public void setValueFromComponent(PaintScaleComponent component) {
    value = component.getComboBox().getSelectionModel().getSelectedItem();
  }

  @Override
  public void setValueToComponent(PaintScaleComponent component, PaintScale newValue) {
    component.getComboBox().getSelectionModel().select(newValue);
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    String elementString = xmlElement.getTextContent();
    if (elementString.length() == 0)
      return;
    for (PaintScale option : choices) {
      if (option.toString().equals(elementString)) {
        value = option;
        break;
      }
    }
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (value == null)
      return;
    xmlElement.setTextContent(value.toString());
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

  @Override
  public PaintScaleComponent createEditingComponent() {
    return new PaintScaleComponent(choices);
  }

}
