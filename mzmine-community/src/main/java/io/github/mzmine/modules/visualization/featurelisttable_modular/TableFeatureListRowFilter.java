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

package io.github.mzmine.modules.visualization.featurelisttable_modular;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRow;
import io.github.mzmine.parameters.parametertypes.row_type_filter.filters.RowTypeFilter;
import io.github.mzmine.util.collections.IndexRange;
import java.util.List;
import java.util.function.Predicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The filter used in the feature table.
 * <p>
 * When the table shows a compound tree ({@link CompoundRow} parents with member children), the
 * non-RT filters ({@link #matchesAllExceptRT(FeatureListRow)}) are applied to both the top-level rows and
 * their children so a compound is kept (and expanded) when any member matches. The RT filter is
 * special: it is only ever evaluated against the top-level rows (see
 * {@link FeatureTableFX#applyTreeRowFilter}).
 */
public record TableFeatureListRowFilter(@Nullable List<IndexRange> idRanges,
                                        @Nullable List<IndexRange> compoundIdRanges,
                                        @Nullable Range<Double> mzRange,
                                        @Nullable Range<Double> rtRange,
                                        @Nullable RowTypeFilter rowTypeFilter) implements
    Predicate<FeatureListRow> {

  @Override
  public boolean test(FeatureListRow row) {
    // flat (non-tree) evaluation: every filter, including RT, on the single row
    return matchesRT(row) && matchesAllExceptRT(row);
  }

  /**
   * All filters except RT. Applied to top-level rows and recursively to child rows of a compound so
   * the compound is retained when any member matches.
   */
  boolean matchesAllExceptRT(@NotNull final FeatureListRow row) {
    return matchesId(row) && matchesCompoundId(row) && matchesMZ(row) && matchAnyRowTypeFilter(row);
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

  /**
   * RT is special: only ever evaluated against the top-level tree row and never propagated to
   * children.
   */
  boolean matchesRT(@NotNull final FeatureListRow row) {
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

  /**
   * Matches the compound id assigned by the compound grouping step. Only {@link CompoundRow}s carry
   * a compound id, so plain feature rows never match an active compound-id filter.
   */
  private boolean matchesCompoundId(FeatureListRow row) {
    if (compoundIdRanges == null || compoundIdRanges.isEmpty()) {
      return true;
    }
    if (!(row instanceof CompoundRow cr)) {
      return false;
    }
    final int compoundId = cr.getCompoundId();
    for (IndexRange range : compoundIdRanges) {
      if (range.contains(compoundId)) {
        return true;
      }
    }
    return false;
  }

}
