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

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;

/**
 */
public class PercentComponent extends FlowPane {

  private TextField percentField;

  public PercentComponent() {

    // setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 0));
    percentField = new TextField();
    percentField.setPrefColumnCount(4);

    getChildren().addAll(percentField, new Label("%"));

  }

  public void setValue(double value) {
    String stringValue = String.valueOf(value * 100);
    percentField.setText(stringValue);
  }

  public Double getValue() {
    String stringValue = percentField.getText();
    try {
      double doubleValue = Double.parseDouble(stringValue) / 100;
      return doubleValue;
    } catch (NumberFormatException e) {
      return null;
    }
  }

  public void setToolTipText(String toolTip) {
    percentField.setTooltip(new Tooltip(toolTip));
  }

}
