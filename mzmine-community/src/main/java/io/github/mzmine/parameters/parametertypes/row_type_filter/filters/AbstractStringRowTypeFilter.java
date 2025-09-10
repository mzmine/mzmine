/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.parameters.parametertypes.row_type_filter.filters;

import io.github.mzmine.parameters.parametertypes.row_type_filter.MatchingMode;
import io.github.mzmine.parameters.parametertypes.row_type_filter.RowTypeFilterOption;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

abstract class AbstractStringRowTypeFilter extends AbstractRowTypeFilter {

  private final boolean caseSensitive;

  public AbstractStringRowTypeFilter(@NotNull RowTypeFilterOption selectedType,
      @NotNull MatchingMode matchingMode, @NotNull String query, boolean caseSensitive) {
    super(selectedType, matchingMode, caseSensitive ? query.trim() : query.toLowerCase().trim());
    this.caseSensitive = caseSensitive;
  }

  public boolean matchesString(@Nullable Object valueObject) {
    if (valueObject == null) {
      return false;
    }

    final String value =
        caseSensitive ? valueObject.toString().trim() : valueObject.toString().toLowerCase().trim();

    return switch (matchingMode) {
      case EQUAL -> query.equals(value);
      case CONTAINS -> value.contains(query);
      case NOT_EQUAL -> !query.equals(value);
      case LESSER_EQUAL -> value.compareTo(query) <= 0;
      case GREATER_EQUAL -> value.compareTo(query) >= 0;
    };
  }

}
