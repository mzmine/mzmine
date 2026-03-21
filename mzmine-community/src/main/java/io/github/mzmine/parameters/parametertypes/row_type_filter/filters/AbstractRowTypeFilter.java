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
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

abstract class AbstractRowTypeFilter implements RowTypeFilter {

  protected final RowTypeFilterOption selectedType;
  protected final MatchingMode matchingMode;
  protected final String query;


  AbstractRowTypeFilter(@NotNull RowTypeFilterOption selectedType,
      @NotNull MatchingMode matchingMode, @NotNull String query) {
    this.selectedType = selectedType;
    this.matchingMode = matchingMode;
    this.query = query;
  }

  @Override
  public RowTypeFilterOption selectedType() {
    return selectedType;
  }

  @Override
  public MatchingMode matchingMode() {
    return matchingMode;
  }

  @Override
  public String query() {
    return query;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    var that = (AbstractRowTypeFilter) obj;
    return Objects.equals(this.selectedType, that.selectedType) && Objects.equals(this.matchingMode,
        that.matchingMode) && Objects.equals(this.query, that.query);
  }

  @Override
  public int hashCode() {
    return Objects.hash(selectedType, matchingMode, query);
  }

  @Override
  public String toString() {
    return "RowTypeFilter[" + "selectedType=" + selectedType + ", " + "matchingMode=" + matchingMode
        + ", " + "query=" + query + ']';
  }

}
