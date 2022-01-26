/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.util;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.FeatureListRow;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class FeatureListUtils {

  /**
   * Copies the FeatureListAppliedMethods from <b>source</b> to <b>target</b>
   *
   * @param source The source feature list.
   * @param target the target feature list.
   */
  public static void copyPeakListAppliedMethods(FeatureList source, FeatureList target) {
    for (FeatureListAppliedMethod proc : source.getAppliedMethods()) {
      target.addDescriptionOfAppliedTask(proc);
    }
  }

  /**
   * @param rows       The rows to search.
   * @param rtRange    The rt range. if row's retention time is null or -1 this range will not
   *                   filter (usually only for imaging datasets)
   * @param mzRange    The m/z range.
   * @param sortedByMz If the list is sorted by m/z (ascending)
   * @param <T>
   * @return List of matching rows.
   */
  @NotNull
  public static <T extends FeatureListRow> List<T> getRows(@NotNull final List<T> rows,
      @NotNull final Range<Float> rtRange, @NotNull final Range<Double> mzRange,
      boolean sortedByMz) {
    final List<T> validRows = new ArrayList<>();

    if (sortedByMz) {
      final double lower = mzRange.lowerEndpoint();
      final double upper = mzRange.upperEndpoint();

      for (T row : rows) {
        final double mz = row.getAverageMZ();
        if (mz < lower) {
          continue;
        } else if (mz > upper) {
          break;
        }
        // if retention time is not set or -1, then apply no retention time filter
        final Float averageRT = row.getAverageRT();
        if (averageRT == null || averageRT < 0 || rtRange.contains(averageRT)) {
          validRows.add(row);
        }
      }
    } else {
      for (T row : rows) {
        if (mzRange.contains(row.getAverageMZ()) && rtRange.contains(row.getAverageRT())) {
          validRows.add(row);
        }
      }
    }

    return validRows;
  }

}
