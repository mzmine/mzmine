/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

import io.github.mzmine.parameters.UserParameter;
import javafx.beans.property.BooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public class OptionalParameterComponent<EmbeddedComponent extends Node> extends HBox {

  private final CheckBox checkBox;
  private EmbeddedComponent embeddedComponent;

  public OptionalParameterComponent(UserParameter<?, EmbeddedComponent> embeddedParameter) {

    checkBox = new CheckBox();
    embeddedComponent = embeddedParameter.createEditingComponent();
    embeddedComponent.disableProperty().bind(checkBox.selectedProperty().not());

    setSpacing(5);
    getChildren().addAll(checkBox, embeddedComponent);
    setAlignment(Pos.CENTER_LEFT);
    HBox.setHgrow(embeddedComponent, Priority.ALWAYS);
  }

  public EmbeddedComponent getEmbeddedComponent() {
    return embeddedComponent;
  }

  public BooleanProperty selectedProperty() {
    return checkBox.selectedProperty();
  }

  public boolean isSelected() {
    return checkBox.isSelected();
  }

  public void setSelected(boolean selected) {
    checkBox.setSelected(selected);
  }

  public CheckBox getCheckBox() {
    return checkBox;
  }

  public void setToolTipText(String toolTip) {
    checkBox.setTooltip(new Tooltip(toolTip));
  }

}
