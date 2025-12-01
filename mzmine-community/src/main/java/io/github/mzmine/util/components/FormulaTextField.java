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

package io.github.mzmine.util.components;

import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.javafx.validation.FxValidation;
import io.github.mzmine.util.FormulaStringFlavor;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.StringUtils;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Tooltip;
import org.controlsfx.control.textfield.CustomTextField;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IMolecularFormula;

/**
 * Text is bound to the formula which is read only.
 */
public class FormulaTextField extends CustomTextField {

  private final ObjectProperty<FormulaStringFlavor> stringFlavor = new SimpleObjectProperty<>(
      FormulaStringFlavor.DEFAULT_CHARGED);
  private final ReadOnlyObjectWrapper<IMolecularFormula> formula = new ReadOnlyObjectWrapper<>();
  private final ReadOnlyStringWrapper parsedFormula = new ReadOnlyStringWrapper();
  private final StringProperty parsingError = new SimpleStringProperty();
  private final boolean requireValue;

  /**
   * Shows charge and allows empty value
   */
  public FormulaTextField() {
    this(FormulaStringFlavor.DEFAULT_CHARGED, false);
  }

  public FormulaTextField(boolean requireValue) {
    this(FormulaStringFlavor.DEFAULT_CHARGED, requireValue);
  }

  public FormulaTextField(FormulaStringFlavor flavor, final boolean requireValue) {
    this.requireValue = requireValue;
    stringFlavor.set(flavor);
    setTooltip(new Tooltip(
        "Enter a formula formatted like: %s (%s)".formatted(flavor.getInputExample(),
            requireValue ? "value required" : "empty allowed")));

    textProperty().subscribe(this::parseFormula);
    parsedFormula.bind(formula.map(f -> {
      if (f == null) {
        return null;
      }
      try {
        return FormulaUtils.getFormulaString(f, stringFlavor.get());
      } catch (Exception e) {
        return null;
      }
    }));

    final ObservableValue<String> harmonizeTooltip = parsedFormula.map(
        parsed -> "Harmonize formula format: %s => %s".formatted(getText(), parsed));

    final ButtonBase harmonizeButton = FxIconUtil.newIconButton(FxIcons.RELOAD, harmonizeTooltip,
        this::harmonizeFormula);

    final BooleanBinding mayHarmonize = Bindings.createBooleanBinding(
        () -> getText() == null || getText().trim().equals(getParsedFormula()));
    harmonizeButton.disableProperty().bind(mayHarmonize);
    setRight(harmonizeButton);

    FxValidation.registerErrorValidator(this, parsingError);
  }

  private void harmonizeFormula() {
    if (formula.get() == null) {
      return;
    }
    // this will set the formula to the text property by formatting the formula
    setText(parsedFormula.get());
  }

  private void parseFormula(@Nullable String text) {
    final FormulaStringFlavor flavor = stringFlavor.get();
    String format = flavor.getInputExample();

    if (StringUtils.isBlank(text)) {
      formula.set(null);
      parsingError.set(
          requireValue ? "Empty value not allowed. Requires formula format: " + format : null);
      return;
    }

    try {
      final IMolecularFormula parsed = FormulaUtils.createMajorIsotopeMolFormulaWithCharge(text);
      formula.set(parsed);
    } catch (Exception e) {
      formula.set(null);
    }
    if (formula.get() == null) {
      String error = "Cannot parse formula, required format: " + format;
      parsingError.set(error);
    }
  }

  /**
   * Create formula field and bind bidirectionally
   *
   * @return formula field
   */
  public static FormulaTextField newFormulaTextField(final boolean requireValue) {
    return newFormulaTextField(FormulaStringFlavor.DEFAULT_CHARGED, requireValue);
  }

  public static FormulaTextField newFormulaTextField(final FormulaStringFlavor flavor,
      final boolean requireValue) {
    return newFormulaTextField(flavor, requireValue, null);
  }

  public static FormulaTextField newFormulaTextField(final FormulaStringFlavor flavor,
      final boolean requireValue, @Nullable final StringProperty formulaString) {
    var field = new FormulaTextField(flavor, requireValue);
    if (formulaString != null) {
      field.textProperty().bindBidirectional(formulaString);
    }
    return field;
  }

  /**
   * Create formula field and bind bidirectionally
   *
   * @return formula field
   */
  public static FormulaTextField newFormulaTextField(@Nullable final StringProperty formulaString) {
    return newFormulaTextField(FormulaStringFlavor.DEFAULT_CHARGED, false, formulaString);
  }

  public @Nullable IMolecularFormula getFormula() {
    return formulaProperty().get();
  }

  public ReadOnlyObjectProperty<IMolecularFormula> formulaProperty() {
    return formula.getReadOnlyProperty();
  }

  public void setFormula(@Nullable IMolecularFormula formula) {
    if (formula == null) {
      setText(null);
      return;
    }
    setText(FormulaUtils.getFormulaString(formula, stringFlavor.get()));
  }

  public ObjectProperty<FormulaStringFlavor> stringFlavorProperty() {
    return stringFlavor;
  }

  public ReadOnlyStringProperty parsedFormulaProperty() {
    return parsedFormula.getReadOnlyProperty();
  }

  public String getParsedFormula() {
    return parsedFormula.get();
  }
}
