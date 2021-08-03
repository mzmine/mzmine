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

package io.github.mzmine.parameters.parametertypes.absoluterelative;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;

public class AbsoluteNRelativeDoubleComponent extends FlowPane {

  private static final long serialVersionUID = 1L;
  private final TextField absField, relField;

  public AbsoluteNRelativeDoubleComponent() {
    absField = new TextField();
    absField.setPrefColumnCount(8);


    relField = new TextField();
    relField.setPrefColumnCount(8);

    getChildren().addAll(new Label("abs="),absField, new Label("rel="), relField,
            new Label("%"));
  }

  public void setValue(AbsoluteNRelativeDouble value) {
    absField.setText(String.valueOf(value.getAbsolute()));
    relField.setText(String.valueOf(value.getRelative() * 100.0));
  }

  public AbsoluteNRelativeDouble getValue() {
    try {
      double abs = Double.parseDouble(absField.getText().trim());
      double rel = Double.parseDouble(relField.getText().trim()) / 100.0;
      AbsoluteNRelativeDouble value = new AbsoluteNRelativeDouble(abs, rel);
      return value;
    } catch (NumberFormatException e) {
      return null;
    }

  }

  public void setToolTipText(String toolTip) {
    absField.setTooltip(new Tooltip(toolTip));
    relField.setTooltip(new Tooltip(toolTip));
  }
}
