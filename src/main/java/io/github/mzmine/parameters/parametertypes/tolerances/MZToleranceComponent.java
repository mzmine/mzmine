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

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;

public class MZToleranceComponent extends FlowPane {

  private final TextField mzToleranceField, ppmToleranceField;

  public MZToleranceComponent() {

    // setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));

    mzToleranceField = new TextField();
    mzToleranceField.setPrefColumnCount(6);


    ppmToleranceField = new TextField();
    ppmToleranceField.setPrefColumnCount(6);

    getChildren().addAll(mzToleranceField, new Label("m/z  or"), ppmToleranceField,
        new Label("ppm"));
  }

  public void setValue(MZTolerance value) {
    mzToleranceField.setText(String.valueOf(value.getMzTolerance()));
    ppmToleranceField.setText(String.valueOf(value.getPpmTolerance()));
  }

  public MZTolerance getValue() {
    try {
      double mzTolerance = Double.parseDouble(mzToleranceField.getText().trim());
      double ppmTolerance = Double.parseDouble(ppmToleranceField.getText().trim());
      MZTolerance value = new MZTolerance(mzTolerance, ppmTolerance);
      return value;
    } catch (NumberFormatException e) {
      return null;
    }

  }

  public void setToolTipText(String toolTip) {
    mzToleranceField.setTooltip(new Tooltip(toolTip));
    ppmToleranceField.setTooltip(new Tooltip(toolTip));
  }

}
