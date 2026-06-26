/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.visualization.isotope_labeling;

import io.github.mzmine.datamodel.features.FeatureListRow;
import java.util.List;

/**
 * One row in the isotope cluster TableView. Immutable after construction.
 */
public final class ClusterTableEntry {

  private final int clusterId;
  private final double mz;
  private final double rtMin;
  private final int isotopologueCount;
  private final double fractionalContribution; // 0–1; NaN if not yet computed
  private final List<FeatureListRow> rows;

  public ClusterTableEntry(int clusterId, double mz, double rtMin, int isotopologueCount,
      double fractionalContribution, List<FeatureListRow> rows) {
    this.clusterId = clusterId;
    this.mz = mz;
    this.rtMin = rtMin;
    this.isotopologueCount = isotopologueCount;
    this.fractionalContribution = fractionalContribution;
    this.rows = rows;
  }

  public int getClusterId() {
    return clusterId;
  }

  public double getMz() {
    return mz;
  }

  public double getRtMin() {
    return rtMin;
  }

  public int getIsotopologueCount() {
    return isotopologueCount;
  }

  public double getFractionalContribution() {
    return fractionalContribution;
  }

  public List<FeatureListRow> getRows() {
    return rows;
  }
}
