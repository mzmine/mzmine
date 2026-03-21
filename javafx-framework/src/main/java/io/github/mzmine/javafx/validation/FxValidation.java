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

package io.github.mzmine.javafx.validation;

import static java.util.Objects.requireNonNullElse;

import java.util.function.Function;
import java.util.function.Predicate;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Control;
import javafx.scene.control.TextInputControl;
import org.controlsfx.validation.ValidationResult;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.decoration.CompoundValidationDecoration;
import org.controlsfx.validation.decoration.StyleClassValidationDecoration;
import org.controlsfx.validation.decoration.ValidationDecoration;
import org.jetbrains.annotations.NotNull;

public class FxValidation {

  private static final ValidationDecoration DEFAULT_DECORATOR = new CompoundValidationDecoration(
      new StyleClassValidationDecoration(), new IconValidationDecoration());

  public static ValidationSupport newValidationSupport() {
    final ValidationSupport support = new ValidationSupport();
    support.setValidationDecorator(DEFAULT_DECORATOR);
    return support;
  }

  public static void registerErrorValidator(@NotNull Control field,
      @NotNull ObservableValue<String> errorMessage) {
    registerErrorValidator(newValidationSupport(), field, errorMessage);
  }

  public static void registerErrorValidator(@NotNull ValidationSupport support,
      @NotNull Control control, @NotNull ObservableValue<String> errorMessage) {
    support.registerValidator(control, false, (_, _) -> {
      final String error = requireNonNullElse(errorMessage.getValue(), "");
      return ValidationResult.fromErrorIf(control, error, !error.isBlank());
    });
    errorMessage.subscribe((_, _) -> {
      support.revalidate(control);
    });
  }

  /**
   * Factory method to create a validator, which evaluates the value validity with a given
   * predicate. Error is created if the evaluation is <code>false</code>.
   *
   * @param message    text of a message to be created if value is invalid
   * @param validCheck the check to be used for the value validity evaluation.
   * @return new validator
   */
  public static void registerOnException(ValidationSupport support, TextInputControl control,
      Predicate<String> validCheck, Function<String, String> message) {
    registerOnException(support, (Control) control, validCheck, message);
  }

  /**
   * Factory method to create a validator, which evaluates the value validity with a given
   * predicate. Error is created if the evaluation is <code>false</code>.
   *
   * @param message    text of a message to be created if value is invalid
   * @param validCheck the check to be used for the value validity evaluation.
   * @return new validator
   */
  public static <T> void registerOnException(ValidationSupport support, Control control,
      Predicate<T> validCheck, Function<T, String> message) {
    support.registerValidator(control, false, (_, s) -> {
      boolean valid;
      try {
        valid = validCheck.test((T) s);
      } catch (Exception e) {
        valid = false;
      }
      final String error = message.apply((T) s);
      return ValidationResult.fromErrorIf(control, error, !valid);
    });
  }
}
