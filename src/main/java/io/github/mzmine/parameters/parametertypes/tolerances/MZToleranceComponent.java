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

package io.github.mzmine.parameters.parametertypes.tolerances;

import io.github.mzmine.main.MZmineCore;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.util.converter.NumberStringConverter;

public class MZToleranceComponent extends FlowPane {

  private final TextField mzToleranceField, ppmToleranceField;

  public MZToleranceComponent() {

    // setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));

    mzToleranceField = new TextField();
    mzToleranceField.setTextFormatter(new TextFormatter<>(
        new NumberStringConverter(MZmineCore.getConfiguration().getMZFormat())));

    ppmToleranceField = new TextField();
    ppmToleranceField.setPrefColumnCount(6);
    ppmToleranceField.setTextFormatter(new TextFormatter<>(
        new NumberStringConverter(MZmineCore.getConfiguration().getPPMFormat())));

    getChildren().addAll(mzToleranceField, new Label("m/z  or"), ppmToleranceField,
        new Label("ppm"));
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

  public void setValue(MZTolerance value) {
    mzToleranceField.setText(String.valueOf(value.getMzTolerance()));
    ppmToleranceField.setText(String.valueOf(value.getPpmTolerance()));
  }

  public void setToolTipText(String toolTip) {
    mzToleranceField.setTooltip(new Tooltip(toolTip));
    ppmToleranceField.setTooltip(new Tooltip(toolTip));
  }

  public void setListener(Runnable listener) {
    mzToleranceField.textProperty().addListener((o, oldValue, newValue) -> listener.run());
    ppmToleranceField.textProperty().addListener((o, oldValue, newValue) -> listener.run());
  }
}
