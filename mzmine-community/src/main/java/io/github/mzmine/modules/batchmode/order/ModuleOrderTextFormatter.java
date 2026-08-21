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

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineProcessingModule;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class ModuleOrderTextFormatter {

  private ModuleOrderTextFormatter() {
  }

  static @NotNull String describeRule(@NotNull final ModuleOrderRule rule) {
    final String anchorName = switch (rule) {
      case RelativeModuleOrderRule relativeRule -> {
        final MZmineProcessingModule anchor = MZmineCore.getModuleInstance(
            relativeRule.anchorModule());
        yield anchor == null ? relativeRule.anchorModule().getSimpleName() : anchor.getName();
      }
      case CustomModuleOrderRule ignored -> null;
    };
    return describeRule(rule, anchorName);
  }

  static @NotNull String describeRule(@NotNull final ModuleOrderRule rule,
      @Nullable final String anchorName) {
    final String level = ModuleOrderRules.level(rule) == ModuleOrderLevel.MUST ? "MUST" : "SHOULD";
    return switch (rule) {
      case RelativeModuleOrderRule relativeRule ->
          describeRelativeRule(relativeRule, Objects.requireNonNull(anchorName), level);
      case CustomModuleOrderRule customRule ->
          "%s %s".formatted(level, customRule.condition().description());
    };
  }

  private static @NotNull String describeRelativeRule(@NotNull final RelativeModuleOrderRule rule,
      @NotNull final String anchorName, @NotNull final String level) {
    return switch (rule.anchorRequirement()) {
      case REQUIRED -> "%s run %s %s".formatted(level, positionText(rule.position()), anchorName);
      case IF_PRESENT -> "If %s is present, %s run %s it".formatted(anchorName, level,
          positionText(rule.position()));
    };
  }

  private static @NotNull String positionText(@NotNull final ModuleOrderPosition position) {
    return switch (position) {
      case BEFORE -> "before";
      case AFTER -> "after";
    };
  }
}
