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

package io.github.mzmine.modules.tools.tools_autoparam.preclassification;

import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Decision of a {@link RawDataClassifier} for its parameter.
 *
 * @param options the valid values, the first one is preselected. Empty for a conflict, where no
 *                value matches all files. A single value is set without asking the user, several
 *                values need a user choice.
 * @param message explains a conflict or a choice to the user, empty for a single value
 * @param <T>     value type of the parameter
 */
public record ClassifierDecision<T>(@NotNull List<@NotNull T> options, @NotNull String message) {

  public ClassifierDecision {
    options = List.copyOf(options);
    if (options.size() != 1 && message.isBlank()) {
      throw new IllegalArgumentException("A conflict or choice needs a message");
    }
  }

  /**
   * The value is set without asking the user.
   */
  public static <T> @NotNull ClassifierDecision<T> fixed(@NotNull T value) {
    return new ClassifierDecision<>(List.of(value), "");
  }

  /**
   * The user has to choose one of the options.
   *
   * @param options at least two valid values, the first one is preselected
   * @param message explains why the user has to choose
   */
  public static <T> @NotNull ClassifierDecision<T> choice(@NotNull List<@NotNull T> options,
      @NotNull String message) {
    if (options.size() < 2) {
      throw new IllegalArgumentException("A choice needs at least two options");
    }
    return new ClassifierDecision<>(options, message);
  }

  /**
   * No value matches all files, the estimation or optimization has to be aborted.
   *
   * @param message explains the conflict to the user
   */
  public static <T> @NotNull ClassifierDecision<T> conflict(@NotNull String message) {
    return new ClassifierDecision<>(List.of(), message);
  }

  public boolean isConflict() {
    return options.isEmpty();
  }

  public boolean needsUserChoice() {
    return options.size() > 1;
  }

  /**
   * @return the fixed or preselected value
   * @throws IllegalStateException for a conflict
   */
  public @NotNull T value() {
    if (isConflict()) {
      throw new IllegalStateException("A conflict has no value: " + message);
    }
    return options.getFirst();
  }
}
