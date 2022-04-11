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
package io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance;

import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;

/**
 *
 */
public class MobilityToleranceComponent extends BorderPane {

  private final TextField toleranceField;

  public MobilityToleranceComponent() {
    toleranceField = new TextField();
    toleranceField.setPrefColumnCount(6);
    setCenter(toleranceField);
  }

  public MobilityTolerance getValue() {
    final String valueString = toleranceField.getText();
    final Number toleranceValue = Float.parseFloat(valueString);
    final float tolerance = toleranceValue.floatValue();
    return new MobilityTolerance(tolerance);
  }

  public void setValue(MobilityTolerance value) {
    float tolerance = value.getTolerance();
    String valueString = String.valueOf(tolerance);
    toleranceField.setText(valueString);
  }

  public void setToolTipText(String toolTip) {
    toleranceField.setTooltip(new Tooltip(toolTip));
  }
}
