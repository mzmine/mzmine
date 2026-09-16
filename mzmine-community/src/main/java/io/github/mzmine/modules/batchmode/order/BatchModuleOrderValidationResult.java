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

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

final class BatchModuleOrderValidationResult {

  private final List<BatchModuleOrderIssue> issues;

  BatchModuleOrderValidationResult(@NotNull final List<@NotNull BatchModuleOrderIssue> issues) {
    this.issues = List.copyOf(Objects.requireNonNull(issues));
  }

  public boolean hasIssues() {
    return !issues.isEmpty();
  }

  @NotNull List<@NotNull BatchModuleOrderIssue> issues() {
    return issues;
  }

  private @NotNull List<@NotNull BatchModuleOrderIssue> issues(
      @NotNull final ModuleOrderLevel level) {
    return issues.stream().filter(issue -> issue.level() == level).toList();
  }

  public @NotNull String formatMessage() {
    final String must = formatGroup("Required processing order", issues(ModuleOrderLevel.MUST));
    final String should = formatGroup("Recommended processing order",
        issues(ModuleOrderLevel.SHOULD));
    return List.of(must, should).stream().filter(message -> !message.isBlank())
        .collect(Collectors.joining("\n\n"));
  }

  private static @NotNull String formatGroup(@NotNull final String title,
      @NotNull final List<@NotNull BatchModuleOrderIssue> issues) {
    if (issues.isEmpty()) {
      return "";
    }
    return title + ":\n" + issues.stream().map(issue -> "- " + issue.message())
        .collect(Collectors.joining("\n"));
  }
}
