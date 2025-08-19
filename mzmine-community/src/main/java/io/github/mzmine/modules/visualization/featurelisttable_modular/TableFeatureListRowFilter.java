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

package io.github.mzmine.modules.visualization.featurelisttable_modular;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.parameters.parametertypes.row_type_filter.filters.RowTypeFilter;
import io.github.mzmine.util.collections.IndexRange;
import java.util.List;
import java.util.function.Predicate;
import org.jetbrains.annotations.Nullable;

/**
 * The filter used in the feature table
 */
public record TableFeatureListRowFilter(@Nullable List<IndexRange> idRanges,
                                        @Nullable Range<Double> mzRange,
                                        @Nullable Range<Double> rtRange,
                                        @Nullable RowTypeFilter rowTypeFilter) implements
    Predicate<FeatureListRow> {

  @Override
  public boolean test(FeatureListRow row) {
    return matchesId(row) && matchesRT(row) && matchesMZ(row) && matchAnyRowTypeFilter(row);
  }

  private boolean matchAnyRowTypeFilter(FeatureListRow row) {
    if (rowTypeFilter == null) {
      return true;
    }
    return rowTypeFilter.matches(row);
  }

  private boolean matchesMZ(FeatureListRow row) {
    return mzRange == null || mzRange.contains(row.getAverageMZ());
  }

  private boolean matchesRT(FeatureListRow row) {
    final Float rt = row.getAverageRT();
    if (rtRange == null || rt == null) {
      return true;
    }
    return rtRange.contains(rt.doubleValue());
  }

  private boolean matchesId(FeatureListRow row) {
    if (idRanges == null || idRanges.isEmpty()) {
      return true;
    }

    final int id = row.getID();
    for (IndexRange idRange : idRanges) {
      if (idRange.contains(id)) {
        return true;
      }
    }
    return false;
  }

}
