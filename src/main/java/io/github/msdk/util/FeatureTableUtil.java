/*
 * (C) Copyright 2015-2017 by MSDK Development Team
 *
 * This software is dual-licensed under either
 *
 * (a) the terms of the GNU Lesser General Public License version 2.1 as published by the Free
 * Software Foundation
 *
 * or (per the licensee's choosing)
 *
 * (b) the terms of the Eclipse Public License v1.0 as published by the Eclipse Foundation.
 */
package io.github.msdk.util;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import com.google.common.collect.Range;

import io.github.msdk.datamodel.FeatureTable;
import io.github.msdk.datamodel.FeatureTableRow;

/**
 * <p>
 * FeatureTableUtil class.
 * </p>
 */
public class FeatureTableUtil {

  /**
   * <p>getRowsInsideRange.</p>
   *
   * @param table a {@link FeatureTable} object.
   * @param rtRange a {@link Range} object.
   * @param mzRange a {@link Range} object.
   * @return a {@link List} object.
   */
  public static @Nonnull List<FeatureTableRow> getRowsInsideRange(@Nonnull FeatureTable table,
      @Nonnull Range<Float> rtRange, @Nonnull Range<Double> mzRange) {
    List<FeatureTableRow> featureTableRows = table.getRows();
    List<FeatureTableRow> result = new ArrayList<>();
    for (FeatureTableRow row : featureTableRows) {
      Float rowRT = row.getRT();
      if ((rowRT != null) && rtRange.contains(rowRT) && mzRange.contains(row.getMz()))
        result.add(row);
    }
    return result;
  }

}
