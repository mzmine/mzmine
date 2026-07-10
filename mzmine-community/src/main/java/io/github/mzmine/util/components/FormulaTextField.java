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

package io.github.mzmine.util.components;

import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.javafx.validation.FxValidation;
import io.github.mzmine.util.FormulaStringFlavor;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.StringUtils;
import java.util.Objects;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
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
 * Bidirectionally links the text field content and a writable {@link #formulaProperty()}. Typing
 * parses the text into a formula without ever reformatting the text, so the user's input (element
 * order, brackets, transiently invalid states) is preserved. Setting the formula externally (e.g.
 * via a binding) updates the text to the formatted formula. Both directions are guarded by a
 * canonical-string equality check so no update loop can occur.
 */
public class FormulaTextField extends CustomTextField {

  private final ObjectProperty<FormulaStringFlavor> stringFlavor = new SimpleObjectProperty<>(
      FormulaStringFlavor.DEFAULT_CHARGED);
  private final ObjectProperty<IMolecularFormula> formula = new SimpleObjectProperty<>();
  private final ReadOnlyStringWrapper parsedFormula = new ReadOnlyStringWrapper();
  private final StringProperty parsingError = new SimpleStringProperty();
  private final boolean requireValue;

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

    setPromptText("Formula like: " + flavor.getInputExample());

    textProperty().subscribe(this::parseFormula);
    // formula -> text: update text when the formula is set externally (e.g. via a binding) and the
    // current text does not already represent it. addListener (not subscribe) so this does not fire
    // during construction while the text listener is still being wired up.
    formula.addListener((_, _, f) -> updateTextFromFormula(f));
    parsedFormula.bind(formula.map(this::formulaToString));

    final ObservableValue<String> harmonizeTooltip = parsedFormula.map(
        parsed -> "Harmonize formula format: %s => %s".formatted(getText(), parsed));

    final ButtonBase harmonizeButton = FxIconUtil.newIconButton(FxIcons.RELOAD, harmonizeTooltip,
        this::harmonizeFormula);

    final BooleanBinding isHarmonized = Bindings.createBooleanBinding(
        () -> getText() == null || getParsedFormula() == null || getText().trim()
            .equals(getParsedFormula()), textProperty(), parsedFormulaProperty());
    harmonizeButton.disableProperty().bind(isHarmonized);
    harmonizeButton.visibleProperty().bind(harmonizeButton.disableProperty().not());
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

  /**
   * text -> formula: parse the current text and update the formula property, but never reformat the
   * text. Only updates the formula when the parsed value actually differs (canonical-string
   * equality) so the formula -> text listener converges instead of looping.
   */
  private void parseFormula(@Nullable String text) {
    final FormulaStringFlavor flavor = stringFlavor.get();
    final String format = flavor.getInputExample();

    if (StringUtils.isBlank(text)) {
      // guard: only clear if not already null so we do not fire a redundant change
      if (formula.get() != null) {
        formula.set(null);
      }
      parsingError.set(
          requireValue ? "Empty value not allowed. Requires formula format: " + format : null);
      return;
    }

    final IMolecularFormula parsed = parseText(text);
    if (parsed == null) {
      // decision: keep the last valid formula and only highlight the error so a transiently invalid
      // input (e.g. half-typed brackets) does not clear an externally bound formula
      parsingError.set("Cannot parse formula, required format: " + format);
      return;
    }

    parsingError.set(null);
    if (!sameFormula(parsed, formula.get())) {
      formula.set(parsed);
    }
  }

  /**
   * formula -> text: update the text only when it does not already represent {@code f}. Unparseable
   * text never equals a real formula, so an externally set formula always wins over invalid input.
   */
  private void updateTextFromFormula(@Nullable IMolecularFormula f) {
    if (sameFormula(f, parseText(getText()))) {
      // text already represents this formula (or both empty) -> preserve the user's exact input
      return;
    }
    setText(formulaToString(f));
    parsingError.set(
        f == null && requireValue ? "Empty value not allowed. Requires formula format: "
                                    + stringFlavor.get().getInputExample() : null);
  }

  /**
   * @return the formatted formula, or {@code null} if the formula is {@code null} or cannot be
   * formatted.
   */
  private @Nullable String formulaToString(@Nullable final IMolecularFormula f) {
    if (f == null) {
      return null;
    }
    try {
      return FormulaUtils.getFormulaString(f, stringFlavor.get());
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * @return the parsed formula, or {@code null} if the text is blank or cannot be parsed.
   */
  private @Nullable IMolecularFormula parseText(@Nullable final String text) {
    if (StringUtils.isBlank(text)) {
      return null;
    }
    try {
      return FormulaUtils.createMajorIsotopeMolFormulaWithCharge(text);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Compares two formulae by their canonical string representation (both {@code null} are equal).
   * This is the guard that keeps the text <-> formula synchronization from looping.
   */
  private boolean sameFormula(@Nullable final IMolecularFormula a,
      @Nullable final IMolecularFormula b) {
    return Objects.equals(formulaToString(a), formulaToString(b));
  }

  public @Nullable IMolecularFormula getFormula() {
    return formulaProperty().get();
  }

  public ObjectProperty<IMolecularFormula> formulaProperty() {
    return formula;
  }

  public void setFormula(@Nullable final IMolecularFormula formula) {
    this.formula.set(formula);
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
