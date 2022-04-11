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

package io.github.mzmine.datamodel.features.correlation;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.RowGroup;
import java.util.ArrayList;
import java.util.List;

public class CorrelationRowGroup extends RowGroup {

  // correlation data of all rows to this group
  private R2GroupCorrelationData[] corr;

  public CorrelationRowGroup(final List<RawDataFile> raw, int groupID) {
    super(raw, groupID);
  }

  /**
   * Recalculates all stats for this group
   *
   * @param corrMap
   */
  public void recalcGroupCorrelation(R2RMap<R2RCorrelationData> corrMap) {
    // init
    corr = new R2GroupCorrelationData[this.size()];

    // test all rows against all other rows
    for (int i = 0; i < this.size(); i++) {
      List<R2RCorrelationData> rowCorr = new ArrayList<>();
      FeatureListRow testRow = this.get(i);
      for (int k = 0; k < this.size(); k++) {
        if (i != k) {
          R2RCorrelationData r2r = corrMap.get(testRow, this.get(k));
          if (r2r != null) {
            rowCorr.add(r2r);
          }
        }
      }
      // create group corr object
      corr[i] = new R2GroupCorrelationData(testRow, rowCorr, testRow.getBestFeature().getHeight());
    }
  }

  /**
   * correlation of each row to the group
   *
   * @return
   */
  public R2GroupCorrelationData[] getCorrelation() {
    return corr;
  }

  /**
   * correlation of a row to the group
   *
   * @return
   */
  public R2GroupCorrelationData getCorrelation(int row) {
    return corr[row];
  }

  public R2GroupCorrelationData getCorrelation(FeatureListRow row) {
    if (row == null) {
      return null;
    }
    int index = indexOf(row);
    if (index != -1) {
      return getCorrelation(index);
    }
    return null;
  }

  @Override
  public boolean isCorrelated(int i, int k) {
    if (corr == null || i >= corr.length || k >= corr.length) {
      return false;
    }
    // is correlated if corr is available between i and k
    else {
      return corr[i].getCorrelationToRow(get(k)) != null;
    }
  }

}
