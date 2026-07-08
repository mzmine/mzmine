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

package io.github.mzmine.parameters.parametertypes;

import io.github.mzmine.parameters.ValueChangeDecorator;
import io.github.mzmine.parameters.ValuePropertyComponent;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.util.converter.IntegerStringConverter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Integer input component with a synchronized slider.
 */
public class WeightSliderComponent extends HBox implements ValueChangeDecorator,
    ValuePropertyComponent<Integer> {

  private static final int TEXT_FIELD_WIDTH = 60;

  private final int minimum;
  private final int maximum;
  private final @NotNull TextField textField;
  private final @NotNull Slider slider;
  private final @NotNull ObjectProperty<Integer> valueProperty;
  private boolean internalUpdate;

  public WeightSliderComponent(final int minimum, final int maximum) {
    this.minimum = minimum;
    this.maximum = maximum;
    this.valueProperty = new SimpleObjectProperty<>(minimum);

    setSpacing(8);
    setPadding(Insets.EMPTY);

    textField = new TextField();
    textField.setPrefWidth(TEXT_FIELD_WIDTH);
    textField.setTextFormatter(new TextFormatter<>(new IntegerStringConverter(), minimum,
        this::filterIntegerChange));

    slider = new Slider(minimum, maximum, minimum);
    slider.setBlockIncrement(1d);
    slider.setMajorTickUnit(25d);
    slider.setMinorTickCount(4);
    slider.setShowTickMarks(true);
    slider.setShowTickLabels(true);
    slider.setSnapToTicks(true);
    slider.setPrefWidth(220);

    getChildren().addAll(slider, textField);
    bindSynchronously();
  }

  private void bindSynchronously() {
    textField.textProperty().addListener((_, _, newText) -> {
      if (internalUpdate || newText == null || newText.isEmpty()) {
        return;
      }
      try {
        final int parsedValue = Integer.parseInt(newText);
        updateValue(parsedValue);
      } catch (NumberFormatException ignored) {
      }
    });

    slider.valueProperty().addListener((_, _, newValue) -> {
      if (internalUpdate || newValue == null) {
        return;
      }
      updateValue((int) Math.round(newValue.doubleValue()));
    });
  }

  private void updateValue(final int newValue) {
    final int bounded = Math.max(minimum, Math.min(maximum, newValue));
    internalUpdate = true;
    try {
      if (valueProperty.get() != bounded) {
        valueProperty.set(bounded);
      }
      if (!Integer.toString(bounded).equals(textField.getText())) {
        textField.setText(Integer.toString(bounded));
      }
      if (Math.round(slider.getValue()) != bounded) {
        slider.setValue(bounded);
      }
    } finally {
      internalUpdate = false;
    }
  }

  private @Nullable TextFormatter.Change filterIntegerChange(
      final @Nullable TextFormatter.Change change) {
    if (change == null) {
      return null;
    }
    final @Nullable String newText = change.getControlNewText();
    if (newText == null || newText.isEmpty()) {
      return change;
    }
    if (!newText.matches("\\d+")) {
      return null;
    }
    try {
      final int parsed = Integer.parseInt(newText);
      return parsed >= minimum && parsed <= maximum ? change : null;
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  public @Nullable Integer getValue() {
    final @Nullable String text = textField.getText();
    if (text == null || text.isBlank()) {
      return null;
    }
    try {
      return Integer.parseInt(text.trim());
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  public void setValue(final @Nullable Integer value) {
    updateValue(value == null ? minimum : value);
  }

  public void setToolTipText(final @NotNull String toolTip) {
    final @NotNull Tooltip tooltip = new Tooltip(toolTip);
    textField.setTooltip(tooltip);
    slider.setTooltip(tooltip);
  }

  @Override
  public void addValueChangedListener(final @NotNull Runnable onChange) {
    valueProperty.addListener((_, _, _) -> onChange.run());
  }

  @Override
  public @NotNull Property<Integer> valueProperty() {
    return valueProperty;
  }
}
