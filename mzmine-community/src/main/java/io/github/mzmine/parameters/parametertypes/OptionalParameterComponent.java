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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import io.github.mzmine.parameters.UserParameter;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;

public class OptionalParameterComponent<EmbeddedComponent extends Node> extends FlowPane
    implements ActionListener {

  private CheckBox checkBox;
  private EmbeddedComponent embeddedComponent;

  public OptionalParameterComponent(UserParameter<?, EmbeddedComponent> embeddedParameter) {

    checkBox = new CheckBox();
    checkBox.setOnAction(e -> {
      boolean checkBoxSelected = checkBox.isSelected();
      embeddedComponent.setDisable(!checkBoxSelected);
    });

    embeddedComponent = embeddedParameter.createEditingComponent();
    embeddedComponent.setDisable(true);

    HBox hBox = new HBox(checkBox, embeddedComponent);
    hBox.setSpacing(7d);
    hBox.setAlignment(Pos.CENTER_LEFT);
    super.getChildren().add(hBox);
  }

  public EmbeddedComponent getEmbeddedComponent() {
    return embeddedComponent;
  }

  public boolean isSelected() {
    return checkBox.isSelected();
  }

  public void setSelected(boolean selected) {
    checkBox.setSelected(selected);
    embeddedComponent.setDisable(!selected);
  }

  @Override
  public void actionPerformed(ActionEvent event) {
    Object src = event.getSource();

    if (src == checkBox) {

    }
  }

  public CheckBox getCheckBox() {
    return checkBox;
  }

  public void setToolTipText(String toolTip) {
    checkBox.setTooltip(new Tooltip(toolTip));
  }

  /*
   * public void addItemListener(ItemListener il) { checkBox.addItemListener(il); }
   */
}
