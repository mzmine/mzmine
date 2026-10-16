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

package io.github.mzmine.modules.tools.tools_autoparam.estimation.domain;

import io.github.mzmine.modules.tools.tools_autoparam.estimation.ParameterEstimationContext;
import java.util.List;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

/**
 * Values retain their domain type; only the optimizer sees their ordinal positions. It is necessary
 * that parameters eg enum values increase/decrease in some way. eg more/less permissive.
 * </br>
 * For example
 * {@link
 * io.github.mzmine.modules.tools.tools_autoparam.estimation.ParameterEstimators#mzTolerance(ParameterEstimationContext)}
 */
public record ChoiceSearchDomain<T>(@NotNull List<T> choices, int firstIndex,
                                    @Nullable Function<@NotNull T, String> formatter) implements
    SearchDomain<T> {

  public ChoiceSearchDomain(@NotNull List<T> choices, int firstIndex) {
    this(choices, firstIndex, null);
  }

  public ChoiceSearchDomain(@NotNull List<T> choices) {
    this(choices, 0, null);
  }

  public ChoiceSearchDomain {
    choices = List.copyOf(choices);
    if (choices.isEmpty() || choices.stream().distinct().count() != choices.size()) {
      throw new IllegalArgumentException("Choices must be nonempty and distinct");
    }
  }

  @Override
  public double lower() {
    return firstIndex;
  }

  @Override
  public double upper() {
    return firstIndex + choices.size() - 1;
  }

  @Override
  public boolean ordinal() {
    return true;
  }

  @Override
  public double encode(@NotNull T value) {
    final int index = choices.indexOf(value);
    if (index < 0) {
      throw new IllegalArgumentException("Value is outside the available choices: " + value);
    }
    return firstIndex + index;
  }

  @Override
  public @NotNull T decode(double value) {
    return choices.get(Math.clamp(Math.round(value) - firstIndex, 0, choices.size() - 1));
  }

  @Override
  public @NotNull SearchScale searchScale() {
    return SearchScale.LINEAR;
  }

  @Override
  public @NotNull String format(@NonNull T value) {
    if (formatter == null) {
      return SearchDomain.super.format(value);
    }
    return formatter.apply(value);
  }
}
