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

package io.github.mzmine.gui.preferences;

import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.FlowPane;

public class NumberFormatEditor extends FlowPane {

  private Spinner<Integer> decimalsSpinner;
  private CheckBox exponentCheckbox;

  public NumberFormatEditor(boolean showExponentOption) {

    decimalsSpinner = new Spinner<>(0, 20, 1, 1);

    getChildren().addAll(new Label("Decimals"), decimalsSpinner);

    if (showExponentOption) {
      exponentCheckbox = new CheckBox("Show exponent");
      getChildren().addAll(exponentCheckbox);
    }

  }

  public int getDecimals() {
    return decimalsSpinner.getValue();
  }

  public boolean getShowExponent() {
    if (exponentCheckbox == null)
      return false;
    else
      return exponentCheckbox.isSelected();
  }

  public void setValue(int decimals, boolean showExponent) {
    decimalsSpinner.getValueFactory().setValue(decimals);
    if (exponentCheckbox != null)
      exponentCheckbox.setSelected(showExponent);
  }

}
