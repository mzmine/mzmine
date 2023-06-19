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

import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScale;
import io.github.mzmine.parameters.UserParameter;
import java.util.Collection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

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
  public void setValueToComponent(PaintScaleComponent component, @Nullable PaintScale newValue) {
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
