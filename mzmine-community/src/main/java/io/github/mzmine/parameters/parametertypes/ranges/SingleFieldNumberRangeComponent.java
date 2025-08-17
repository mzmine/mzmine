/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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
package io.github.mzmine.parameters.parametertypes.ranges;

import com.google.common.collect.Range;
import io.github.mzmine.javafx.components.factories.FxTextFields;
import io.github.mzmine.javafx.components.formatters.FormatDoubleRangeStringConverter;
import io.github.mzmine.parameters.ValuePropertyComponent;
import java.text.NumberFormat;
import javafx.beans.property.Property;
import javafx.geometry.Pos;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Single field that defines a range either as a single number like 1.1 would be a range from
 * 1.1-1.2 always using the defined decimals as precision and rounding up to the next decimal of
 * this precision. This means that mz 199 would range from mz 199 - 200.
 * <p>
 * Or defines ranges as two values, e.g., 10-20
 * <p>
 * TODO Currently not used but this would be a great addition for the mz filter in table
 */
public class SingleFieldNumberRangeComponent extends HBox implements
    ValuePropertyComponent<Range<Double>> {

  private final TextField textField;
  private final TextFormatter<Range<Double>> textFormatter;

  public SingleFieldNumberRangeComponent(int inputsize, @NotNull NumberFormat format,
      @Nullable Range<Double> defvalue, boolean autoGrow) {

    textField = new TextField();
    textFormatter = new TextFormatter<>(new FormatDoubleRangeStringConverter(format));
    FxTextFields.attachDelayedTextFormatter(textField, textFormatter);

    textField.setPrefColumnCount(inputsize);
    textFormatter.setValue(defvalue);

    if (autoGrow) {
      FxTextFields.autoGrowFitText(textField);
    }

    getChildren().add(textField);
    setAlignment(Pos.CENTER_LEFT);
  }

  public String getText() {
    return textField.getText().trim();
  }

  public void setText(String text) {
    textField.setText(text);
  }

  public void setToolTipText(String toolTip) {
    textField.setTooltip(new Tooltip(toolTip));
  }

  public TextField getTextField() {
    return textField;
  }

  @Override
  public Property<Range<Double>> valueProperty() {
    return textFormatter.valueProperty();
  }

}
