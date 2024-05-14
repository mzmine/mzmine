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
