/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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
import java.text.NumberFormat;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.util.converter.NumberStringConverter;
import org.jetbrains.annotations.Nullable;

/**
 *
 */
public class RTToleranceComponent extends HBox {

  private final ObservableList<RTTolerance.Unit> toleranceTypes;
  private final NumberFormat format = MZmineCore.getConfiguration().getRTFormat();
  private final TextFormatter<Number> textFormatter = new TextFormatter<>(
      new NumberStringConverter(format));
  private final TextField toleranceField;
  private final ComboBox<RTTolerance.Unit> toleranceType;

  public RTToleranceComponent(ObservableList<RTTolerance.Unit> rtToleranceTypes) {
    this.toleranceTypes = FXCollections.observableArrayList(rtToleranceTypes);

    setSpacing(5);
    toleranceField = new TextField();
    toleranceField.setPrefColumnCount(6);
    toleranceField.setTextFormatter(textFormatter);

    toleranceType = new ComboBox<>(toleranceTypes);
    toleranceType.getSelectionModel().select(0);

    getChildren().addAll(toleranceField, toleranceType);
  }

  public RTTolerance getValue() {
    RTTolerance.Unit selectedUnit = toleranceType.getValue();
    String valueString = toleranceField.getText();

    float toleranceFloat;
    try {
      if (selectedUnit == RTTolerance.Unit.SECONDS || selectedUnit == RTTolerance.Unit.MINUTES) {
        toleranceFloat = MZmineCore.getConfiguration().getRTFormat().parse(valueString)
            .floatValue();
      } else {
        Number toleranceValue = Double.parseDouble(valueString);
        toleranceFloat = toleranceValue.floatValue();
      }
    } catch (Exception e) {
      return null;
    }

    return new RTTolerance(toleranceFloat, selectedUnit);
  }

  public void setValue(@Nullable RTTolerance value) {
    if (value == null) {
      toleranceField.setText("");
      toleranceType.getSelectionModel().select(0);
      return;
    }

    double tolerance = value.getTolerance();
    RTTolerance.Unit selectedUnit = value.getUnit();

    toleranceType.setValue(selectedUnit);
    String valueString = String.valueOf(tolerance);
    toleranceField.setText(valueString);
  }

  public void setToolTipText(String toolTip) {
    toleranceField.setTooltip(new Tooltip(toolTip));
  }
}
