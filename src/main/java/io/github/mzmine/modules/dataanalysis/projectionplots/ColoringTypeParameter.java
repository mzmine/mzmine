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

package io.github.mzmine.modules.dataanalysis.projectionplots;

import java.util.Collection;
import org.w3c.dom.Element;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.UserParameter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;

/**
 * Simple Parameter implementation
 *
 *
 */
public class ColoringTypeParameter implements UserParameter<ColoringType, ComboBox<ColoringType>> {

  private String name, description;
  private ColoringType value;

  public ColoringTypeParameter() {
    this.name = "Coloring type";
    this.description = "Defines how points will be colored";
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
  public ComboBox<ColoringType> createEditingComponent() {
    ObservableList<ColoringType> choicesList = FXCollections.observableArrayList();
    choicesList.add(ColoringType.NOCOLORING);
    choicesList.add(ColoringType.COLORBYFILE);
    for (UserParameter<?, ?> p : MZmineCore.getProjectManager().getCurrentProject()
        .getParameters()) {
      choicesList.add(new ColoringType(p));
    }
    ComboBox<ColoringType> editor = new ComboBox<ColoringType>(choicesList);
    if (value != null)
      editor.getSelectionModel().select(value);
    return editor;
  }

  @Override
  public ColoringType getValue() {
    return value;
  }

  @Override
  public void setValue(ColoringType value) {
    this.value = value;
  }

  @Override
  public ColoringTypeParameter cloneParameter() {
    ColoringTypeParameter copy = new ColoringTypeParameter();
    copy.setValue(this.getValue());
    return copy;
  }

  @Override
  public void setValueFromComponent(ComboBox<ColoringType> component) {
    value = component.getSelectionModel().getSelectedItem();
  }

  @Override
  public void setValueToComponent(ComboBox<ColoringType> component, ColoringType newValue) {
    component.getSelectionModel().select(newValue);
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    String elementString = xmlElement.getTextContent();
    if (elementString.length() == 0)
      return;
    String attrValue = xmlElement.getAttribute("type");
    if (attrValue.equals("parameter")) {
      for (UserParameter<?, ?> p : MZmineCore.getProjectManager().getCurrentProject()
          .getParameters()) {
        if (p.getName().equals(elementString)) {
          value = new ColoringType(p);
          break;
        }
      }
    } else {
      value = new ColoringType(elementString);
    }
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (value == null)
      return;
    if (value.isByParameter()) {
      xmlElement.setAttribute("type", "parameter");
      xmlElement.setTextContent(value.getParameter().getName());
    } else {
      xmlElement.setTextContent(value.toString());
    }

  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    return true;
  }

}
