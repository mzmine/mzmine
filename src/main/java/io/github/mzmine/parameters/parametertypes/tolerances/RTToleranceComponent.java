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
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance.Unit;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;

/**
 *
 */
public class RTToleranceComponent extends BorderPane {

  // the same order that the unit enum in RTTolerance is defined in
  private static final ObservableList<String> toleranceTypes =
          FXCollections.observableArrayList("absolute (min)", "absolute (sec)", "relative (%)");
  private final TextField toleranceField;
  private final ComboBox<String> toleranceType;

  public RTToleranceComponent() {


    // setBorder(BorderFactory.createEmptyBorder(0, 9, 0, 0));

    toleranceField = new TextField();
    toleranceField.setPrefColumnCount(6);

    toleranceType = new ComboBox<String>(toleranceTypes);

    setCenter(toleranceField);
    setRight(toleranceType);

  }

  public RTTolerance getValue() {

    int index = toleranceType.getSelectionModel().getSelectedIndex();
    String valueString = toleranceField.getText();

    double toleranceDouble;
    Unit toleranceUnit = Unit.values()[index];
    try {
      if (toleranceUnit == Unit.SECONDS || toleranceUnit == Unit.MINUTES) {
        toleranceDouble =
                MZmineCore.getConfiguration().getRTFormat().parse(valueString).doubleValue();
      } else {
        Number toleranceValue = Double.parseDouble(valueString);
        toleranceDouble = toleranceValue.doubleValue();
      }
    } catch (Exception e) {
      return null;
    }

    RTTolerance value = new RTTolerance(toleranceDouble, toleranceUnit);

    return value;

  }

  public void setValue(RTTolerance value) {
    double tolerance = value.getTolerance();
    int choiceIndex = value.getUnit().ordinal();

    toleranceType.getSelectionModel().clearAndSelect(choiceIndex);
    String valueString = String.valueOf(tolerance);
    toleranceField.setText(valueString);
  }

  public void setToolTipText(String toolTip) {
    toleranceField.setTooltip(new Tooltip(toolTip));
  }
}
