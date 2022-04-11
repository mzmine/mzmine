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

package io.github.mzmine.parameters.parametertypes.ranges;

import com.google.common.collect.Range;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;

public class IntRangeComponent extends GridPane {

  private TextField minTxtField, maxTxtField;

  public IntRangeComponent() {

    // setBorder(BorderFactory.createEmptyBorder(0, 9, 0, 0));

    minTxtField = new TextField();
    minTxtField.setPrefColumnCount(8);

    maxTxtField = new TextField();
    maxTxtField.setPrefColumnCount(8);


    add(minTxtField, 0, 0);
    add(new Label(" - "), 1, 0);
    add(maxTxtField, 2, 0);
  }

  public Range<Integer> getValue() {
    String minString = minTxtField.getText();
    String maxString = maxTxtField.getText();

    try {
      if (!minString.isEmpty() && !maxString.isEmpty()) {
        return Range.closed(Integer.parseInt(minString), Integer.parseInt(maxString));
      } else if (!minString.isEmpty()) {
        return Range.closed(Integer.parseInt(minString), Integer.MAX_VALUE);
      } else if (!maxString.isEmpty()) {
        return Range.closed(0, Integer.parseInt(maxString));
      } else {
        return null;
      }
    } catch (Exception e) {
      return null;
    }
  }

  public void setValue(Range<Integer> value) {
    minTxtField.setText(String.valueOf(value.lowerEndpoint()));
    maxTxtField.setText(String.valueOf(value.upperEndpoint()));
  }

  public void setToolTipText(String toolTip) {
    minTxtField.setTooltip(new Tooltip(toolTip));
    maxTxtField.setTooltip(new Tooltip(toolTip));
  }


}
