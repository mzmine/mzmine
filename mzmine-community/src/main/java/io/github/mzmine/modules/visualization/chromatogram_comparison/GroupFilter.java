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

package io.github.mzmine.modules.visualization.chromatogram_comparison;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import org.jetbrains.annotations.NotNull;

/**
 * Filters the groups of the table by their issues.
 */
record GroupFilter(@NotNull String label, @NotNull Predicate<ChromatogramGroup> predicate) {

  static final GroupFilter ALL = new GroupFilter("All groups", _ -> true);
  static final GroupFilter ANY_ISSUE = new GroupFilter("Any issue", ChromatogramGroup::hasIssues);
  static final GroupFilter UNEXPECTED_ISSUE = new GroupFilter("Unexpected issues",
      group -> group.issues().stream().anyMatch(issue -> !issue.isExpected()));

  /**
   * @return all groups, groups with any issue, with unexpected issues, and one filter per issue
   */
  static @NotNull List<GroupFilter> values() {
    final List<GroupFilter> filters = new ArrayList<>(List.of(ALL, ANY_ISSUE, UNEXPECTED_ISSUE));
    for (final ChromatogramIssue issue : ChromatogramIssue.values()) {
      filters.add(new GroupFilter(issue.getLabel(), group -> group.issues().contains(issue)));
    }
    return filters;
  }

  @Override
  public String toString() {
    return label;
  }
}
