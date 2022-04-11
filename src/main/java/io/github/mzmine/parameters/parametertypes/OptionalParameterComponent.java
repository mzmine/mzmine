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
