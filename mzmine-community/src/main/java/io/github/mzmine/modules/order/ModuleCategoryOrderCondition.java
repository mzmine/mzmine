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

package io.github.mzmine.modules.order;

import io.github.mzmine.modules.MZmineModuleCategory;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/**
 * Positions a module relative to any module in a processing category.
 */
public record ModuleCategoryOrderCondition(@NotNull MZmineModuleCategory category,
                                           @NotNull ModuleOrderPosition position) implements
    ModuleOrderCondition {

  public ModuleCategoryOrderCondition {
    Objects.requireNonNull(category);
    Objects.requireNonNull(position);
  }

  public static @NotNull ModuleCategoryOrderCondition before(
      @NotNull final MZmineModuleCategory category) {
    return new ModuleCategoryOrderCondition(category, ModuleOrderPosition.BEFORE);
  }

  public static @NotNull ModuleCategoryOrderCondition after(
      @NotNull final MZmineModuleCategory category) {
    return new ModuleCategoryOrderCondition(category, ModuleOrderPosition.AFTER);
  }

  @Override
  public @NotNull String description() {
    return "run %s a module in the %s category".formatted(position.name().toLowerCase(), category);
  }

  @Override
  public boolean isSatisfied(@NotNull final ModuleOrderEvaluationContext context) {
    return switch (position) {
      case BEFORE -> context.stepsAfter().stream()
          .anyMatch(step -> step.getModule().getModuleCategory() == category);
      case AFTER -> context.stepsBefore().stream()
          .anyMatch(step -> step.getModule().getModuleCategory() == category);
    };
  }
}
