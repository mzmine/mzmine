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

import io.github.mzmine.javafx.components.factories.FxComboBox;
import io.github.mzmine.javafx.components.factories.FxTextFields;
import io.github.mzmine.javafx.components.formatters.FormatDoubleStringConverter;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.parameters.ValuePropertyComponent;
import java.text.ParseException;
import java.util.EnumMap;
import java.util.Map;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A single m/z tolerance: one text field and the unit it is given in.
 * <p>
 * The value of the other unit is remembered while it is not shown, so that switching the unit back
 * and forth does not lose what was typed. Switching also swaps the text formatter, because an
 * absolute tolerance and a ppm value need a very different number of decimals.
 *
 * @see SingleMzToleranceParameter
 */
public class SingleMzToleranceComponent extends HBox implements
    ValuePropertyComponent<MZTolerance> {

  private final TextField toleranceField;
  private final ComboBox<MzToleranceUnit> unitCombo;
  private final ObjectProperty<MZTolerance> valueProperty = new SimpleObjectProperty<>();

  /**
   * The last value seen per unit, so that the unit the user switches away from is restored with
   * what it had rather than with a value converted from the other unit.
   */
  private final Map<MzToleranceUnit, Double> lastValues = new EnumMap<>(MzToleranceUnit.class);

  /**
   * Set while the component writes into its own controls, so that the intermediate states of
   * {@link #setValue(MZTolerance)} are not published as values.
   */
  private boolean updating = false;

  /**
   * @param unitDefaults the value each unit starts out with, read as one term per unit. Used until
   *                     a value in that unit has been set or typed, so that switching the unit
   *                     before entering anything still shows something sensible.
   */
  public SingleMzToleranceComponent(@NotNull final MZTolerance unitDefaults) {

    setSpacing(FxLayout.DEFAULT_SPACE);
    setAlignment(Pos.CENTER_LEFT);

    for (final MzToleranceUnit unit : MzToleranceUnit.values()) {
      lastValues.put(unit, unit.valueOf(unitDefaults));
    }

    toleranceField = new TextField();
    toleranceField.setPrefColumnCount(6);
    // attaches the delayed commit to the text property once; the formatter itself is replaced
    // whenever the unit changes, see applyUnit
    FxTextFields.attachDelayedTextFormatter(toleranceField, newFormatter(MzToleranceUnit.DA));

    unitCombo = FxComboBox.createComboBox("The unit of the tolerance", MzToleranceUnit.values(),
        null);
    unitCombo.setValue(MzToleranceUnit.DA);

    getChildren().addAll(toleranceField, unitCombo);

    unitCombo.valueProperty().addListener((_, previous, unit) -> {
      if (updating || unit == null) {
        return;
      }
      // remember what the unit being left was showing, then show what the new one had
      remember(previous);
      applyUnit(unit, lastValues.get(unit));
      publish();
    });

    toleranceField.textProperty().addListener((_, _, _) -> {
      if (!updating) {
        publish();
      }
    });
  }

  /**
   * @return the tolerance the controls describe, or null while the text is not a number.
   */
  public @Nullable MZTolerance getValue() {

    final MzToleranceUnit unit = unitCombo.getValue();
    if (unit == null) {
      return null;
    }

    final Double tolerance = parse(unit);
    return tolerance == null ? null : unit.toTolerance(tolerance);
  }

  /**
   * Shows the given tolerance, in the unit it is expressed in, see
   * {@link MzToleranceUnit#of(MZTolerance)}.
   */
  public void setValue(@Nullable final MZTolerance value) {

    if (value == null) {
      updating = true;
      try {
        toleranceField.setText("");
      } finally {
        updating = false;
      }
      valueProperty.set(null);
      return;
    }

    final MzToleranceUnit unit = MzToleranceUnit.of(value);
    lastValues.put(unit, unit.valueOf(value));

    updating = true;
    try {
      unitCombo.setValue(unit);
      applyUnit(unit, unit.valueOf(value));
    } finally {
      updating = false;
    }

    publish();
  }

  public void setToolTipText(final String toolTip) {
    toleranceField.setTooltip(new Tooltip(toolTip));
    unitCombo.setTooltip(new Tooltip(toolTip));
  }

  @Override
  public Property<MZTolerance> valueProperty() {
    return valueProperty;
  }

  /**
   * Installs the formatter of the given unit and shows the given value in it.
   * <p>
   * The formatter is replaced rather than reused, because setting a formatter resets the text of
   * the field from the formatter's own value - so the text has to be written afterwards.
   */
  private void applyUnit(@NotNull final MzToleranceUnit unit, @Nullable final Double value) {

    final boolean wasUpdating = updating;
    updating = true;
    try {
      toleranceField.setTextFormatter(newFormatter(unit));
      toleranceField.setText(value == null ? "" : unit.format().format(value));
    } finally {
      updating = wasUpdating;
    }
  }

  /**
   * Keeps the currently shown number as the value of the given unit. Called with the unit that is
   * being left, which is no longer the one the combo box shows.
   */
  private void remember(@Nullable final MzToleranceUnit unit) {

    if (unit == null) {
      return;
    }

    final Double shown = parse(unit);
    if (shown != null) {
      lastValues.put(unit, shown);
    }
  }

  /**
   * Publishes the current controls as the component value, and keeps it as the last value of its
   * unit.
   */
  private void publish() {

    final MZTolerance value = getValue();
    if (value == null) {
      // half typed input, keep the last valid value rather than publishing null
      return;
    }

    remember(unitCombo.getValue());
    valueProperty.set(value);
  }

  /**
   * @return the number in the text field, or null if it is not one. Parsed with the format the
   * field is written with, so that a locale using a decimal comma reads back what it displays.
   */
  private @Nullable Double parse(@NotNull final MzToleranceUnit unit) {

    final String text = toleranceField.getText();
    if (text == null || text.isBlank()) {
      return null;
    }

    try {
      return unit.format().parse(text.trim()).doubleValue();
    } catch (ParseException e) {
      return null;
    }
  }

  private static @NotNull TextFormatter<Double> newFormatter(@NotNull final MzToleranceUnit unit) {
    return new TextFormatter<>(new FormatDoubleStringConverter(unit.format()));
  }
}
