/*
 * Copyright 2006-2022 The MZmine Development Team
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
