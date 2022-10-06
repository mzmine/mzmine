/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.parameters.parametertypes;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;

/**
 */
public class PercentComponent extends FlowPane {

  private TextField percentField;

  public PercentComponent() {

    // setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 0));
    percentField = new TextField();
    percentField.setPrefColumnCount(4);

    getChildren().addAll(percentField, new Label("%"));

  }

  public void setValue(double value) {
    String stringValue = String.valueOf(value * 100);
    percentField.setText(stringValue);
  }

  public Double getValue() {
    String stringValue = percentField.getText();
    try {
      double doubleValue = Double.parseDouble(stringValue) / 100;
      return doubleValue;
    } catch (NumberFormatException e) {
      return null;
    }
  }

  public void setToolTipText(String toolTip) {
    percentField.setTooltip(new Tooltip(toolTip));
  }

}
