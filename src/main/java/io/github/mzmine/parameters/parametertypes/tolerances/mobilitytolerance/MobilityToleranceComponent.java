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
package io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance;

import io.github.mzmine.datamodel.MobilityType;
import java.util.Arrays;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;

/**
 *
 */
public class MobilityToleranceComponent extends BorderPane {

  // the same order that the unit enum in RTTolerance is defined in
  private static final ObservableList<MobilityType> toleranceTypes =
      FXCollections.observableArrayList(Arrays.asList(MobilityType.values()));
  private final TextField toleranceField;
  private final ComboBox<MobilityType> toleranceType;

  public MobilityToleranceComponent() {

    toleranceField = new TextField();
    toleranceField.setPrefColumnCount(6);

    toleranceType = new ComboBox<>(toleranceTypes);
    toleranceType.getSelectionModel().select(MobilityType.TIMS);

    setCenter(toleranceField);
    setRight(toleranceType);
  }

  public MobilityTolerance getValue() {

    final MobilityType toleranceUnit = toleranceType.getValue();

    final String valueString = toleranceField.getText();
    final Number toleranceValue = Double.parseDouble(valueString);
    final double toleranceFloat = toleranceValue.doubleValue();

    return new MobilityTolerance(toleranceFloat, toleranceUnit);
  }

  public void setValue(MobilityTolerance value) {
    double tolerance = value.getTolerance();
    toleranceType.getSelectionModel().select(value.getMobilityType());
    String valueString = String.valueOf(tolerance);
    toleranceField.setText(valueString);
  }

  public void setToolTipText(String toolTip) {
    toleranceField.setTooltip(new Tooltip(toolTip));
  }
}
