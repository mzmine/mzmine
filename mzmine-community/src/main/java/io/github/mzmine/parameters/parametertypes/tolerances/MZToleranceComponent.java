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

import io.github.mzmine.javafx.components.factories.FxTextFields;
import io.github.mzmine.javafx.components.formatters.FormatDoubleStringConverter;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ValuePropertyComponent;
import java.util.Objects;
import java.util.logging.Logger;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import org.jetbrains.annotations.Nullable;

public class MZToleranceComponent extends FlowPane implements ValuePropertyComponent<MZTolerance> {

  private static final Logger logger = Logger.getLogger(MZToleranceComponent.class.getName());

  private final TextField mzToleranceField, ppmToleranceField;
  private final ObjectProperty<MZTolerance> valueProperty = new SimpleObjectProperty<>();
  private final TextFormatter<Double> absoluteFormatter;
  private final TextFormatter<Double> relativeFormatter;

  public MZToleranceComponent() {
    setAlignment(Pos.CENTER_LEFT);
    setHgap(5);
    // setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));

    mzToleranceField = new TextField();
    mzToleranceField.setPrefColumnCount(6);
    absoluteFormatter = new TextFormatter<>(
        new FormatDoubleStringConverter(MZmineCore.getConfiguration().getMZFormat()));
    FxTextFields.attachDelayedTextFormatter(mzToleranceField, absoluteFormatter);

    ppmToleranceField = new TextField();
    ppmToleranceField.setPrefColumnCount(6);
    relativeFormatter = new TextFormatter<>(
        new FormatDoubleStringConverter(MZmineCore.getConfiguration().getPPMFormat()));
    FxTextFields.attachDelayedTextFormatter(ppmToleranceField, relativeFormatter);

    getChildren().addAll(mzToleranceField, new Label("m/z  or"), ppmToleranceField,
        new Label("ppm"));

    absoluteFormatter.valueProperty().addListener((__, _, newValue) -> {
      final MZTolerance value = getValue();
      if (value != null) {
        // only update if valid
        valueProperty.set(value);
      }
    });
    relativeFormatter.valueProperty().addListener((__, _, newValue) -> {
      final MZTolerance value = getValue();
      if (value != null) {
        // only update if valid
        valueProperty.set(getValue());
      }
    });

    valueProperty.addListener((__, oldValue, newValue) -> {
      if (Objects.equals(oldValue, newValue)) {
        return;
      }

      if (newValue == null) {
        ppmToleranceField.setText("");
        mzToleranceField.setText("");
        return;
      }

      // only update the formatted text instead of the value of the formatter, this way we circumvent precision errors
      ppmToleranceField.setText(
          relativeFormatter.getValueConverter().toString(newValue.getPpmTolerance()));
      mzToleranceField.setText(
          absoluteFormatter.getValueConverter().toString(newValue.getMzTolerance()));
      logger.finest("Value property changed to " + newValue);
    });
  }

  public MZTolerance getValue() {
    try {
      double mzTolerance = Double.parseDouble(mzToleranceField.getText().trim());
      double ppmTolerance = Double.parseDouble(ppmToleranceField.getText().trim());
      MZTolerance value = new MZTolerance(mzTolerance, ppmTolerance);
      return value;
    } catch (NumberFormatException | NullPointerException e) {
      return null;
    }
  }

  public void setValue(@Nullable MZTolerance value) {
    if (value == null) {
      mzToleranceField.setText("");
      ppmToleranceField.setText("");
    } else {
      mzToleranceField.setText(String.valueOf(value.getMzTolerance()));
      ppmToleranceField.setText(String.valueOf(value.getPpmTolerance()));
    }
  }

  public void setToolTipText(String toolTip) {
    mzToleranceField.setTooltip(new Tooltip(toolTip));
    ppmToleranceField.setTooltip(new Tooltip(toolTip));
  }

  public void setListener(Runnable listener) {
    mzToleranceField.textProperty().addListener((_, _, _) -> listener.run());
    ppmToleranceField.textProperty().addListener((_, _, _) -> listener.run());
  }

  @Override
  public Property<MZTolerance> valueProperty() {
    return valueProperty;
  }
}
