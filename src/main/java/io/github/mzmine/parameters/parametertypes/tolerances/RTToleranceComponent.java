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
package io.github.mzmine.parameters.parametertypes.tolerances;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance.Unit;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.util.converter.NumberStringConverter;

/**
 *
 */
public class RTToleranceComponent extends BorderPane {

  // the same order that the unit enum in RTTolerance is defined in
  private static final ObservableList<String> toleranceTypes = FXCollections.observableArrayList(
      "absolute (min)", "absolute (sec)", "relative (%)");
  private final NumberFormat format = new DecimalFormat("0.000");
  private final TextFormatter<Number> textFormatter = new TextFormatter<>(
      new NumberStringConverter(format));
  private final TextField toleranceField;
  private final ComboBox<String> toleranceType;

  public RTToleranceComponent() {

    // setBorder(BorderFactory.createEmptyBorder(0, 9, 0, 0));

    toleranceField = new TextField();
    toleranceField.setPrefColumnCount(6);
    toleranceField.setTextFormatter(textFormatter);

    toleranceType = new ComboBox<String>(toleranceTypes);
    toleranceType.getSelectionModel().select(0);

    setCenter(toleranceField);
    setRight(toleranceType);

  }

  public RTTolerance getValue() {

    int index = toleranceType.getSelectionModel().getSelectedIndex();
    String valueString = toleranceField.getText();

    float toleranceFloat;
    Unit toleranceUnit = Unit.values()[index];
    try {
      if (toleranceUnit == Unit.SECONDS || toleranceUnit == Unit.MINUTES) {
        toleranceFloat = MZmineCore.getConfiguration().getRTFormat().parse(valueString)
            .floatValue();
      } else {
        Number toleranceValue = Double.parseDouble(valueString);
        toleranceFloat = toleranceValue.floatValue();
      }
    } catch (Exception e) {
      return null;
    }

    RTTolerance value = new RTTolerance(toleranceFloat, toleranceUnit);

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
