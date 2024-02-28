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

package io.github.mzmine.modules.dataprocessing.align_ransac;

import io.github.mzmine.datamodel.features.FeatureListRow;

/**
 * This class represents a score between feature list row and aligned feature list row
 */
public class RowVsRowScore implements Comparable<RowVsRowScore> {

  private FeatureListRow peakListRow, alignedRow;
  double score;
  private String errorMessage;

  public RowVsRowScore(FeatureListRow peakListRow, FeatureListRow alignedRow, double mzMaxDiff,
      double rtMaxDiff, double correctedRT) throws Exception {

    this.alignedRow = alignedRow;
    this.peakListRow = peakListRow;

    // Calculate differences between m/z and RT values
    double mzDiff = Math.abs(peakListRow.getAverageMZ() - alignedRow.getAverageMZ());
    double rtDiff = Math.abs(correctedRT - alignedRow.getAverageRT());

    score = ((1 - mzDiff / mzMaxDiff) + (1 - rtDiff / rtMaxDiff));
  }

  /**
   * This method returns the feature list row which is being aligned
   */
  public FeatureListRow getPeakListRow() {
    return peakListRow;
  }

  /**
   * This method returns the row of aligned feature list
   */
  public FeatureListRow getAlignedRow() {
    return alignedRow;
  }

  /**
   * This method returns score between the these two peaks (the lower score, the better match)
   */
  public double getScore() {
    return score;
  }

  String getErrorMessage() {
    return errorMessage;
  }

  /**
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  public int compareTo(RowVsRowScore object) {

    // We must never return 0, because the TreeSet in JoinAlignerTask would
    // treat such elements as equal
    if (score < object.getScore()) {
      return 1;
    } else {
      return -1;
    }

  }

}
