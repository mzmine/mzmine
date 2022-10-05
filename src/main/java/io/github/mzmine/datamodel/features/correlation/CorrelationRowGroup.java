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
