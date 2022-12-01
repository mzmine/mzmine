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

package io.github.mzmine.modules.dataprocessing.align_hierarchical;

import io.github.mzmine.datamodel.features.FeatureListRow;
import java.util.List;

import io.github.mzmine.datamodel.MZmineProject;

public class RowVsRowDistanceProvider {

  MZmineProject project;
  // boolean useOldestRDFancestor;
  // Hashtable<RawDataFile, List<double[]>> rtAdjustementMapping;
  List<FeatureListRow> full_rows_list;
  double mzWeight;
  double rtWeight;
  boolean useApex;
  // boolean useKnownCompoundsAsRef;
  // boolean useDetectedMzOnly;
  // RTTolerance rtToleranceAfter;

  double maximumScore;

  public RowVsRowDistanceProvider(MZmineProject project,
      // boolean useOldestRDFancestor,
      // Hashtable<RawDataFile, List<double[]>> rtAdjustementMapping,
      List<FeatureListRow> full_rows_list, double mzWeight, double rtWeight,
      // boolean useApex, boolean useKnownCompoundsAsRef,
      // boolean useDetectedMzOnly, RTTolerance rtToleranceAfter,
      double maximumScore) {

    this.project = project;
    // this.useOldestRDFancestor = useOldestRDFancestor;
    // this.rtAdjustementMapping = rtAdjustementMapping;
    this.full_rows_list = full_rows_list;
    this.mzWeight = mzWeight;
    this.rtWeight = rtWeight;
    // this.useApex = useApex;
    // this.useKnownCompoundsAsRef = useKnownCompoundsAsRef;
    // this.useDetectedMzOnly = useDetectedMzOnly;
    // this.rtToleranceAfter = rtToleranceAfter;

    this.maximumScore = maximumScore;

  }

  public RowVsRowScoreGC getScore(int row_id, int aligned_row_id, double mzMaxDiff,
      double rtMaxDiff) {

    FeatureListRow peakListRow = full_rows_list.get(row_id);
    FeatureListRow alignedRow = full_rows_list.get(aligned_row_id);

    RowVsRowScoreGC score = new RowVsRowScoreGC(project, // useOldestRDFancestor,
        // rtAdjustementMapping,
        peakListRow, alignedRow, mzMaxDiff, mzWeight, rtMaxDiff, rtWeight, 0.0d// ,
    // useApex, useKnownCompoundsAsRef,
    // useDetectedMzOnly,
    // rtToleranceAfter
    );

    return score;
  }

  // public double getMaximumScore() {
  // return this.maximumScore;
  // }

  public double getSimpleDistance(int i, int j, double mzMaxDiff, double rtMaxDiff,
      double minScore) {

    // Itself
    if (i == j) {
      return 0d;
    }

    return this.maximumScore - this.getScore(i, j, mzMaxDiff, rtMaxDiff).getScore();
  }

  public double getRankedDistance(int i, int j, double mzMaxDiff, double rtMaxDiff,
      double minScore) {

    // Itself
    if (i == j)
      return 0d;

    // if (row_id > aligned_row_id) {
    // int tmp = row_id;
    // row_id = aligned_row_id;
    // aligned_row_id = tmp;
    // }
    // if (row_id < aligned_row_id) {
    // int tmp = row_id;
    // row_id = aligned_row_id;
    // aligned_row_id = tmp;
    // }

    FeatureListRow row = full_rows_list.get(i);
    FeatureListRow k_row = full_rows_list.get(j);

    // System.out.println("(2) Rows: (" + i + "," + j + ")" + row + " | " +
    // k_row);

    // // Same list
    // if ((row_id < 45 && aligned_row_id < 45)
    // || (row_id >= 45 && aligned_row_id >= 45 && row_id < 102 &&
    // aligned_row_id < 102)
    // || (row_id >= 102 && aligned_row_id >= 102)) {
    if (row.getRawDataFiles().get(0) == k_row.getRawDataFiles().get(0)) {
      return 1000.0d;
    }
    // Not candidate
    else {
      // System.out.println("(2) CaseRT: " +
      // Math.abs(row.getBestPeak().getRT() - k_row.getBestPeak().getRT())
      // + " >= " + rtMaxDiff/2.0 + "? " +
      // (Math.abs(row.getBestPeak().getRT() -
      // k_row.getBestPeak().getRT()) >= rtMaxDiff/2.0));
      // System.out.println("(2) CaseMZ: " +
      // Math.abs(row.getBestPeak().getMZ() - k_row.getBestPeak().getMZ())
      // + " >= " + mzMaxDiff/2.0 + "? " +
      // (Math.abs(row.getBestPeak().getMZ() -
      // k_row.getBestPeak().getMZ()) >= mzMaxDiff/2.0));
      if ((Math.abs(row.getBestFeature().getRT() - k_row.getBestFeature().getRT()) >= rtMaxDiff / 2.0
          || Math.abs(row.getBestFeature().getMZ() - k_row.getBestFeature().getMZ()) >= mzMaxDiff
              / 2.0)) {
        return 100.0d;
      }
    }

    double score = this.getScore(i, j, mzMaxDiff, rtMaxDiff).getScore();
    // Score too low
    if (score <= Math.max(HierarAlignerGCTask.MIN_SCORE_ABSOLUTE, minScore)) {
      // System.out.println("Found score " + score + " < " +
      // Math.max(JDXCompoundsIdentificationSingleTask.MIN_SCORE_ABSOLUTE,
      // minScore) + "!");
      // System.out.println("(2) Final dist: " + 10.0f);
      return 10.0d;
    }

    // Score OK
    return this.maximumScore - score;
  }

}
