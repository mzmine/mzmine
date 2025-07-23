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

import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.factories.FxTextFields;
import io.github.mzmine.javafx.components.formatters.FormatDoubleStringConverter;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ValuePropertyComponent;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityTolerance;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import org.jetbrains.annotations.Nullable;

/**
 *
 */
public class NanometerToleranceComponent extends BorderPane implements
    ValuePropertyComponent<NanometerTolerance> {

  private final TextField toleranceField;
  private final NumberFormat numberFormat = new DecimalFormat("0.00");
  private final ObjectProperty<NanometerTolerance> tolerance = new SimpleObjectProperty<>();
  private final TextFormatter<Double> formatter = new TextFormatter<>(
      new FormatDoubleStringConverter(numberFormat));

  public NanometerToleranceComponent() {
    toleranceField = new TextField();
    toleranceField.setPrefColumnCount(6);
    setCenter(toleranceField);
    final Label nm = FxLabels.newBoldLabel("nm");
    setRight(nm);
    BorderPane.setAlignment(nm, Pos.CENTER_LEFT);
    BorderPane.setMargin(nm, FxLayout.DEFAULT_PADDING_INSETS);

    FxTextFields.attachDelayedTextFormatter(toleranceField, formatter);
    formatter.valueProperty().subscribe(val -> {
      if (val != null) {
        valueProperty().setValue(new NanometerTolerance(val));
      } else {
        valueProperty().setValue(null);
      }
    });
  }

  public NanometerTolerance getValue() {
    return valueProperty().getValue();
  }

  public void setValue(@Nullable NanometerTolerance value) {
    toleranceField.setText(value == null ? "" : "" + value.getTolerance());
  }

  public void setToolTipText(String toolTip) {
    toleranceField.setTooltip(new Tooltip(toolTip));
  }

  @Override
  public Property<NanometerTolerance> valueProperty() {
    return tolerance;
  }
}
