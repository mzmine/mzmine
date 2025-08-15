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

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.parameters.parametertypes.row_type_filter.MatchingMode;
import io.github.mzmine.parameters.parametertypes.row_type_filter.QueryFormatException;
import io.github.mzmine.parameters.parametertypes.row_type_filter.RowTypeFilterOption;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class NumericRowTypeFilter extends AbstractRowTypeFilter {

  private final double queryNumber;
  private final @NotNull Function<FeatureListRow, ? extends @Nullable Number> valueFunction;

  NumericRowTypeFilter(@NotNull RowTypeFilterOption selectedType,
      @NotNull MatchingMode matchingMode, @NotNull String query,
      @NotNull Function<FeatureListRow, ? extends @Nullable Number> valueFunction) {
    super(selectedType, matchingMode, query);
    try {
      this.queryNumber = Double.parseDouble(query);
    } catch (NumberFormatException e) {
      throw new QueryFormatException("Invalid numeric query: " + query + ". ");
    }
    this.valueFunction = valueFunction;
  }


  @Override
  public boolean matches(FeatureListRow row) {
    final Number value = valueFunction.apply(row);
    if (value == null) {
      return false;
    }
    final int compare = Double.compare(queryNumber, value.doubleValue());
    return switch (matchingMode) {
      case EQUAL -> compare == 0;
      case LESSER_EQUAL -> compare >= 0;
      case GREATER_EQUAL -> compare <= 0;
      case NOT_EQUAL -> compare != 0;
      case CONTAINS -> throw new IllegalArgumentException(
          "CONTAINS matching mode is not applicable to numeric filters");
    };
  }

}
