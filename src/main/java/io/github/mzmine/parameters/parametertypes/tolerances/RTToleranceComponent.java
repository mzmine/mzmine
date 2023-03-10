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
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance.Unit;
import java.text.DecimalFormat;
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

  // the same order that the unit enum in RTTolerance is defined in
  private static final ObservableList<String> toleranceTypes = FXCollections.observableArrayList(
      "absolute (min)", "absolute (sec)", "relative (%)");
  private final NumberFormat format = new DecimalFormat("0.000");
  private final TextFormatter<Number> textFormatter = new TextFormatter<>(
      new NumberStringConverter(format));
  private final TextField toleranceField;
  private final ComboBox<String> toleranceType;

  public RTToleranceComponent() {
    setSpacing(5);

    toleranceField = new TextField();
    toleranceField.setPrefColumnCount(6);
    toleranceField.setTextFormatter(textFormatter);

    toleranceType = new ComboBox<>(toleranceTypes);
    toleranceType.getSelectionModel().select(0);

    getChildren().addAll(toleranceField, toleranceType);
  }

  public RTTolerance getValue() {

    int index = toleranceType.getSelectionModel().getSelectedIndex();
    String valueString = toleranceField.getText();

    float toleranceFloat;
    Unit toleranceUnit = Unit.values()[index];
    try {
      if (toleranceUnit == Unit.SECONDS || toleranceUnit == Unit.MINUTES) {
        toleranceFloat = MZmineCore.getConfiguration().getRTFormat().parse(valueString)
            .floatValue();
      } else {
        Number toleranceValue = Double.parseDouble(valueString);
        toleranceFloat = toleranceValue.floatValue();
      }
    } catch (Exception e) {
      return null;
    }

    RTTolerance value = new RTTolerance(toleranceFloat, toleranceUnit);

    return value;

  }

  public void setValue(@Nullable RTTolerance value) {
    if (value == null) {
      toleranceField.setText("");
      // set to default value
      toleranceType.getSelectionModel().select(toleranceTypes.get(0));
      return;
    }

    double tolerance = value.getTolerance();
    int choiceIndex = value.getUnit().ordinal();

    toleranceType.getSelectionModel().clearAndSelect(choiceIndex);
    String valueString = String.valueOf(tolerance);
    toleranceField.setText(valueString);
  }

  public void setToolTipText(String toolTip) {
    toleranceField.setTooltip(new Tooltip(toolTip));
  }
}
