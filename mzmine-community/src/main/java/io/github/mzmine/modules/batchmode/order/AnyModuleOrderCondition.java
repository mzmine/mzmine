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

package io.github.mzmine.modules.batchmode.order;

import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.MZmineProcessingStep;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

/**
 * Matches a batch step that satisfies any of the wrapped conditions. Used to anchor a rule against
 * several interchangeable modules, e.g. two modules that both produce the required feature list.
 */
record AnyModuleOrderCondition(@NotNull List<ModuleOrderCondition> conditions)
    implements ModuleOrderCondition {

  AnyModuleOrderCondition {
    Objects.requireNonNull(conditions);
    if (conditions.size() < 2) {
      throw new IllegalArgumentException("anyOf requires at least two conditions");
    }
    conditions = List.copyOf(conditions);
  }

  @Override
  public @NotNull String description() {
    return conditions.stream().map(ModuleOrderCondition::description)
        .collect(Collectors.joining(" or "));
  }

  @Override
  public boolean matches(
      @NotNull final MZmineProcessingStep<? extends MZmineProcessingModule> step) {
    return conditions.stream().anyMatch(condition -> condition.matches(step));
  }
}
