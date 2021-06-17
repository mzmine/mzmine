/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.parameters.parametertypes.absoluterelative;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;

public class AbsoluteNRelativeIntComponent extends FlowPane {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private final TextField absField, relField;

  public AbsoluteNRelativeIntComponent() {
    absField = new TextField();
    absField.setPrefColumnCount(8);

    relField = new TextField();
    relField.setPrefColumnCount(8);

    getChildren().addAll(new Label("abs="),absField, new Label("rel="), relField,
            new Label("%"));
  }

  public void setValue(AbsoluteNRelativeInt value) {
    absField.setText(String.valueOf(value.getAbsolute()));
    relField.setText(String.valueOf(value.getRelative() * 100.f));
  }

  public AbsoluteNRelativeInt getValue() {
    try {
      int abs = Integer.parseInt(absField.getText().trim());
      float rel = Float.parseFloat(relField.getText().trim()) / 100.f;
      AbsoluteNRelativeInt value = new AbsoluteNRelativeInt(abs, rel);
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
