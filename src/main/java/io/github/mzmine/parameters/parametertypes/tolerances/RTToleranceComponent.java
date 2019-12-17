/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */
package io.github.mzmine.parameters.parametertypes.tolerances;

import io.github.mzmine.main.MZmineCore;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;

/**
 */
public class RTToleranceComponent extends BorderPane {


  private static final ObservableList<String> toleranceTypes =
      FXCollections.observableArrayList("absolute (min)", "relative (%)");
  private TextField toleranceField;
  private ComboBox<String> toleranceType;

  public RTToleranceComponent() {



    // setBorder(BorderFactory.createEmptyBorder(0, 9, 0, 0));

    toleranceField = new TextField();
    toleranceField.setPrefColumnCount(6);

    toleranceType = new ComboBox<String>(toleranceTypes);

    setCenter(toleranceField);
    setRight(toleranceType);

  }

  public void setValue(RTTolerance value) {
    double tolerance = value.getTolerance();
    if (value.isAbsolute()) {
      toleranceType.getSelectionModel().clearAndSelect(0);
      String valueString = String.valueOf(tolerance);
      toleranceField.setText(valueString);
    } else {
      toleranceType.getSelectionModel().clearAndSelect(1);
      String valueString = String.valueOf(tolerance * 100);
      toleranceField.setText(valueString);
    }
  }

  public RTTolerance getValue() {

    int index = toleranceType.getSelectionModel().getSelectedIndex();

    String valueString = toleranceField.getText();

    double toleranceDouble;
    try {
      if (index == 0) {
        toleranceDouble =
            MZmineCore.getConfiguration().getRTFormat().parse(valueString).doubleValue();
      } else {
        Number toleranceValue = Double.parseDouble(valueString);
        toleranceDouble = toleranceValue.doubleValue() / 100;
      }
    } catch (Exception e) {
      return null;
    }

    RTTolerance value = new RTTolerance(index <= 0, toleranceDouble);

    return value;

  }

  public void setToolTipText(String toolTip) {
    toleranceField.setTooltip(new Tooltip(toolTip));
  }
}
