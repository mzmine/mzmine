/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.align_join;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.util.FeatureListUtils;
import org.jetbrains.annotations.Nullable;

/**
 * This class represents a score between feature list row and aligned feature list row. Natural
 * order sorting puts the highest, best score first
 */
public record RowVsRowScore(double score, FeatureListRow rowToAdd,
                            FeatureListRow alignedRow) implements Comparable<RowVsRowScore> {

  /**
   * @param rowToAddProviderOfRanges row to add/compare to aligned row, all ranges were provided
   *                                 from here are
   * @param alignedRow               the alignedRow, where all ranges were extracted from
   * @param mzRange                  range based on alignedRow or null if deactivated for scoring
   * @param rtRange                  range based on alignedRow or null if deactivated for scoring
   * @param mobilityRange            range based on alignedRow or null if deactivated for scoring
   * @param mzWeight                 factors for contribution
   * @param rtWeight                 factors for contribution
   * @param mobilityWeight           factors for contribution
   */
  public RowVsRowScore(final FeatureListRow rowToAddProviderOfRanges,
      final FeatureListRow alignedRow, @Nullable Range<Double> mzRange,
      @Nullable Range<Float> rtRange, @Nullable Range<Float> mobilityRange,
      @Nullable Range<Float> ccsRange, final double mzWeight, final double rtWeight,
      final double mobilityWeight, final double ccsWeight) {
    this(FeatureListUtils.getAlignmentScore(alignedRow, mzRange, rtRange, mobilityRange, ccsRange,
        mzWeight, rtWeight, mobilityWeight, ccsWeight), rowToAddProviderOfRanges, alignedRow);
  }


  /**
   * This method returns the feature list row which is being aligned
   */
  public FeatureListRow getRowToAdd() {
    return rowToAdd;
  }

  /**
   * This method returns the row of aligned feature list
   */
  public FeatureListRow getAlignedBaseRow() {
    return alignedRow;
  }

  /**
   * This method returns score between the these two peaks (the lower score, the better match)
   */
  public double getScore() {
    return score;
  }

  /**
   * Sorts in descending order
   */
  public int compareTo(RowVsRowScore object) {
    return Double.compare(object.getScore(), score); // reversed order
  }

}
