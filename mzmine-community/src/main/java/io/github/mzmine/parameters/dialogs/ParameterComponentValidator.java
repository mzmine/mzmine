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

package io.github.mzmine.parameters.dialogs;

import io.github.mzmine.javafx.validation.FxValidation;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.UserParameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.Node;
import javafx.scene.Parent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Checks the current values of parameter components with {@link Parameter#checkValue} and marks
 * invalid ones with an error decoration on {@link UserParameter#getDecorationTarget(Node)}.
 * <p>
 * Values are read into a clone of each parameter so that the actual parameters are not changed.
 */
class ParameterComponentValidator {

  private static final Logger logger = Logger.getLogger(
      ParameterComponentValidator.class.getName());

  /**
   * parameter name to parameter and component
   */
  private final Map<String, ParameterAndComponent<?>> components = new LinkedHashMap<>();
  /**
   * parameter name to current error decoration
   */
  private final Map<String, ComponentDecoration> errorDecorations = new HashMap<>();

  private static <T extends Node> @NotNull Node getDecorationTarget(
      @NotNull ParameterAndComponent<T> comp) {
    return comp.parameter().getDecorationTarget(comp.component());
  }

  /**
   * @return the joined error messages or null if the value is valid
   */
  private static <T extends Node> @Nullable String checkComponentValue(
      @NotNull ParameterAndComponent<T> comp) {
    final UserParameter<?, T> parameter = comp.parameter();
    final List<String> errors = new ArrayList<>();
    try {
      // clone to not change the actual parameter value
      final UserParameter<?, T> clone = parameter.cloneParameter();
      clone.setValueFromComponent(comp.component());
      if (clone.checkValue(errors)) {
        return null;
      }
    } catch (Exception e) {
      // assumption: components may throw on invalid user input, e.g., unparsable numbers
      logger.log(Level.FINEST, "Cannot read value of parameter " + parameter.getName(), e);
      errors.add(Objects.requireNonNullElse(e.getMessage(), "Invalid value"));
    }
    if (errors.isEmpty()) {
      return "Invalid value for " + parameter.getName();
    }
    return String.join("\n", errors);
  }

  /**
   * Checks the current structure as embedded panes may be created later.
   */
  private static boolean containsValidatingPane(@NotNull Node node) {
    if (node instanceof ParameterSetupPane pane) {
      return pane.isValueCheckRequired();
    }
    if (!(node instanceof Parent parent)) {
      return false;
    }
    for (final Node child : parent.getChildrenUnmodifiable()) {
      if (containsValidatingPane(child)) {
        return true;
      }
    }
    return false;
  }

  void register(@NotNull ParameterAndComponent<?> component) {
    components.put(component.parameter().getName(), component);
  }

  /**
   * Checks all registered components and updates their error decorations.
   */
  void validate() {
    for (final Map.Entry<String, ParameterAndComponent<?>> entry : components.entrySet()) {
      final ParameterAndComponent<?> comp = entry.getValue();
      // decision: embedded parameter panes validate their own parameters, so errors are shown
      // once on the actual field
      final String error =
          containsValidatingPane(comp.component()) ? null : checkComponentValue(comp);
      updateDecoration(entry.getKey(), comp, error);
    }
  }

  private void updateDecoration(@NotNull String name, @NotNull ParameterAndComponent<?> comp,
      @Nullable String error) {
    final ComponentDecoration current = errorDecorations.get(name);
    if (current != null && Objects.equals(current.message(), error)) {
      return;
    }
    if (current != null) {
      current.remove();
      errorDecorations.remove(name);
    }
    if (error == null) {
      return;
    }
    final Node target = getDecorationTarget(comp);
    errorDecorations.put(name,
        new ComponentDecoration(FxValidation.markError(target, error), error));
  }
}
