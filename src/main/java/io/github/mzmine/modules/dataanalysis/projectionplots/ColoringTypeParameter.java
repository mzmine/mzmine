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
