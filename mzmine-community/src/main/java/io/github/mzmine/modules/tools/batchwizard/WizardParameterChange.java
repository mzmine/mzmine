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

package io.github.mzmine.modules.tools.batchwizard;

import io.github.mzmine.parameters.UserParameter;
import java.util.Arrays;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A parameter value in a {@link WizardPart} that was changed automatically, e.g., by parameter
 * estimation or optimization. Only used to highlight the change to the user.
 *
 * @param part      the wizard part of the changed step
 * @param parameter only used as a key to find the parameter and its component by name. Its value is
 *                  not used, use {@link #oldValue()} and {@link #newValue()} instead.
 * @param oldValue  value before the change or null if unset or the parameter did not exist before
 * @param newValue  value after the change
 */
public record WizardParameterChange(@NotNull WizardPart part,
                                    @NotNull UserParameter<?, ?> parameter,
                                    @Nullable Object oldValue, @Nullable Object newValue) {

  private static final int MAX_VALUE_LENGTH = 80;

  private static @NotNull String formatValue(@Nullable Object value) {
    final String text = switch (value) {
      case null -> "none";
      case Object[] array -> Arrays.deepToString(array);
      default -> Objects.toString(value);
    };
    if (text.length() <= MAX_VALUE_LENGTH) {
      return text;
    }
    return text.substring(0, MAX_VALUE_LENGTH - 1) + "…";
  }

  /**
   * @param source what changed the value, e.g., "parameter estimation"
   * @return a tooltip text like "Changed by source: old → new"
   */
  public @NotNull String formatTooltip(@NotNull String source) {
    return "Changed by %s:\n%s → %s".formatted(source, formatValue(oldValue),
        formatValue(newValue));
  }
}
