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

package io.github.mzmine.modules.dataanalysis.compounddashboard;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRow;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Limits the member rows that the dashboard plots draw. GC-EI compounds may have hundreds of
 * fragment ion rows, and one dataset per row makes the chromatogram and MS1 plots slow.
 */
final class CompoundDashboardDisplayedRows {

  /// Maximum member rows drawn in the chromatogram, mobilogram and MS1 plots
  static final int MAX_DISPLAYED_ROWS = 50;

  private CompoundDashboardDisplayedRows() {
  }

  /**
   * @param compound the compound
   * @param file     the sample to rank rows by feature height, or null to use the maximum height of
   *                 each row
   * @param selected the selected member row, always drawn
   * @return the representative, the selected row and the most intense member rows, at most
   * {@link #MAX_DISPLAYED_ROWS} unless the representative and selected row exceed it
   */
  static @NotNull Set<FeatureListRow> select(@NotNull final CompoundRow compound,
      @Nullable final RawDataFile file, @Nullable final FeatureListRow selected) {
    final List<FeatureListRow> rows = CompoundDashboardColoring.flattenAllMemberRows(compound);
    final Set<FeatureListRow> displayed = Collections.newSetFromMap(new IdentityHashMap<>());
    if (rows.size() <= MAX_DISPLAYED_ROWS) {
      displayed.addAll(rows);
      return displayed;
    }
    displayed.add(compound.getPreferredRow());
    if (selected != null) {
      displayed.add(selected);
    }
    final List<FeatureListRow> byHeight = rows.stream()
        .sorted(Comparator.comparingDouble((FeatureListRow row) -> height(row, file)).reversed())
        .toList();
    for (final FeatureListRow row : byHeight) {
      if (displayed.size() >= MAX_DISPLAYED_ROWS) {
        break;
      }
      displayed.add(row);
    }
    return displayed;
  }

  /**
   * @return true if the compound has more member rows than are drawn
   */
  static boolean isLimited(@NotNull final CompoundRow compound) {
    return CompoundDashboardColoring.flattenAllMemberRows(compound).size() > MAX_DISPLAYED_ROWS;
  }

  private static double height(@NotNull final FeatureListRow row,
      @Nullable final RawDataFile file) {
    if (file == null) {
      return Objects.requireNonNullElse(row.getMaxHeight(), 0f);
    }
    final Feature feature = row.getFeature(file);
    return feature == null ? 0d : feature.getHeight();
  }
}
