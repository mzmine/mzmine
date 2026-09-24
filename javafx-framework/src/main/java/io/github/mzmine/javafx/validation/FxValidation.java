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

package io.github.mzmine.javafx.validation;

import static java.util.Objects.requireNonNullElse;

import java.util.function.Function;
import java.util.function.Predicate;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Control;
import javafx.scene.control.TextInputControl;
import javafx.scene.paint.Color;
import javafx.util.Subscription;
import org.controlsfx.control.decoration.Decoration;
import org.controlsfx.validation.Severity;
import org.controlsfx.validation.ValidationResult;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.decoration.CompoundValidationDecoration;
import org.controlsfx.validation.decoration.StyleClassValidationDecoration;
import org.controlsfx.validation.decoration.ValidationDecoration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FxValidation {

  private static final IconValidationDecoration ICON_DECORATOR = new IconValidationDecoration();
  private static final ValidationDecoration DEFAULT_DECORATOR = new CompoundValidationDecoration(
      new StyleClassValidationDecoration(), ICON_DECORATOR);

  public static ValidationSupport newValidationSupport() {
    final ValidationSupport support = new ValidationSupport();
    support.setValidationDecorator(DEFAULT_DECORATOR);
    return support;
  }

  /**
   * Adds a message icon with tooltip to any node. {@link ValidationSupport} only works on
   * {@link Control}s with a value extractor. Use
   * {@link DecorationTargetProvider#findDecorationTarget(Node)} to find a suitable target within a
   * composite component.
   * <p>
   * The decoration is intentionally not flagged as a validation decoration so that
   * {@link ValidationSupport#redecorate()} on the same node does not remove it.
   * <p>
   * May be called before the target is shown, the decoration is then added once the target is part
   * of a scene.
   *
   * @param target a {@link Parent}, usually a {@link Control}
   * @return subscription to remove the decoration
   */
  public static @NotNull Subscription addMessageDecoration(@NotNull Node target,
      @NotNull Severity severity, @NotNull String message, @NotNull Pos pos) {
    return addMessageDecoration(target, severity, message, pos, null);
  }

  /**
   * Same as {@link #addMessageDecoration(Node, Severity, String, Pos)} with a custom icon color.
   *
   * @param target a {@link Parent}, usually a {@link Control}
   * @param color  icon color, e.g., the positive or negative color of a color palette. null for the
   *               default color of the severity
   * @return subscription to remove the decoration
   */
  public static @NotNull Subscription addMessageDecoration(@NotNull Node target,
      @NotNull Severity severity, @NotNull String message, @NotNull Pos pos,
      @Nullable Color color) {
    final Decoration decoration = new TooltipFixGraphicDecoration(
        ICON_DECORATOR.createDecorationNode(severity, message, color), pos);
    return SceneAwareDecoration.add(target, decoration);
  }

  /**
   * Marks a node with a checkmark, e.g., to show that its value was changed automatically.
   *
   * @param target  a {@link Parent}, usually a {@link Control}
   * @param message tooltip message
   * @return subscription to remove the decoration
   */
  public static @NotNull Subscription markChanged(@NotNull Node target, @NotNull String message) {
    return markChanged(target, message, null);
  }

  /**
   * Marks a node with a checkmark, e.g., to show that its value was changed automatically.
   *
   * @param target  a {@link Parent}, usually a {@link Control}
   * @param message tooltip message
   * @param color   icon color, e.g., the positive color of a color palette. null for the default
   * @return subscription to remove the decoration
   */
  public static @NotNull Subscription markChanged(@NotNull Node target, @NotNull String message,
      @Nullable Color color) {
    return addMessageDecoration(target, Severity.OK, message, Pos.TOP_RIGHT, color);
  }

  /**
   * Marks a node with an error icon, e.g., to show that its value is invalid.
   *
   * @param target  a {@link Parent}, usually a {@link Control}
   * @param message tooltip message
   * @return subscription to remove the decoration
   */
  public static @NotNull Subscription markError(@NotNull Node target, @NotNull String message) {
    return markError(target, message, null);
  }

  /**
   * Marks a node with an error icon, e.g., to show that its value is invalid.
   *
   * @param target  a {@link Parent}, usually a {@link Control}
   * @param message tooltip message
   * @param color   icon color, e.g., the negative color of a color palette. null for the default
   * @return subscription to remove the decoration
   */
  public static @NotNull Subscription markError(@NotNull Node target, @NotNull String message,
      @Nullable Color color) {
    return addMessageDecoration(target, Severity.ERROR, message, Pos.TOP_RIGHT, color);
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
