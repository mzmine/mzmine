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

import io.github.mzmine.datamodel.features.FeatureListRow;

/**
 * This class represents a score between feature list row and aligned feature list row
 */
public class RowVsRowScore implements Comparable<RowVsRowScore> {

  double score;
  private final FeatureListRow peakListRow;
  private final FeatureListRow alignedRow;

  public RowVsRowScore(FeatureListRow peakListRow, FeatureListRow alignedRow, double mzMaxDiff,
      double mzWeight, double rtMaxDiff, double rtWeight) {

    this.peakListRow = peakListRow;
    this.alignedRow = alignedRow;

    // Calculate differences between m/z and RT values
    double mzDiff = Math.abs(peakListRow.getAverageMZ() - alignedRow.getAverageMZ());

    double rtDiff = Math.abs(peakListRow.getAverageRT() - alignedRow.getAverageRT());

    score = ((1 - mzDiff / mzMaxDiff) * mzWeight) + ((1 - rtDiff / rtMaxDiff) * rtWeight);

  }

  public RowVsRowScore(FeatureListRow peakListRow, FeatureListRow alignedRow, double mzMaxDiff,
      double mzWeight, double rtMaxDiff, double rtWeight, double mobilityMaxDiff,
      double mobilityWeight) {

    this.peakListRow = peakListRow;
    this.alignedRow = alignedRow;

    // Calculate differences between m/z and RT values
    double mzDiff = Math.abs(peakListRow.getAverageMZ() - alignedRow.getAverageMZ());

    double rtDiff = Math.abs(peakListRow.getAverageRT() - alignedRow.getAverageRT());

    Float row1Mobility = peakListRow.getAverageMobility();
    Float row2Mobility = alignedRow.getAverageMobility();
    if (row1Mobility != null && row2Mobility != null) {
      float mobilityDiff = Math.abs(row1Mobility - row2Mobility);
      score = ((1 - mzDiff / mzMaxDiff) * mzWeight) + ((1 - rtDiff / rtMaxDiff) * rtWeight) + (
          (1 - mobilityDiff / mobilityMaxDiff) * mobilityWeight);
    } else {
      score = ((1 - mzDiff / mzMaxDiff) * mzWeight) + ((1 - rtDiff / rtMaxDiff) * rtWeight);
    }
  }

  /**
   * This method returns the feature list row which is being aligned
   */
  public FeatureListRow getRowToAdd() {
    return peakListRow;
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
    return Double.compare(object.getScore(), score);
  }

}
