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

import static io.github.mzmine.util.FeatureListRowSorter.MZ_ASCENDING;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.types.alignment.AlignmentScores;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityTolerance;
import io.github.mzmine.util.FeatureListUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.collections.transformation.SortedList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class calculates the alignment score for rows
 */
public class RowAlignmentScoreCalculator {


  private final Map<RawDataFile, SortedList<FeatureListRow>> originalRowsMap;
  private final MZTolerance mzTol;
  private final RTTolerance rtTol;
  private final MobilityTolerance mobTol;
  private final double mzWeight;
  private final double rtWeight;
  private final double mobilityWeight;
  private final int totalSamples;

  /**
   * @param originalFeatureLists the scoring is limited to the feature lists (and their samples)
   * @param mzTol                alignment tolerance to weight scores
   * @param rtTol                alignment tolerance to weight scores
   * @param mobTol               alignment tolerance to weight scores
   */
  public RowAlignmentScoreCalculator(final List<FeatureList> originalFeatureLists,
      @NotNull MZTolerance mzTol, @Nullable RTTolerance rtTol, @Nullable MobilityTolerance mobTol,
      double mzWeight, double rtWeight, double mobilityWeight) {

    originalRowsMap = new HashMap<>(originalFeatureLists.size());
    this.mzTol = mzTol;
    this.rtTol = rtTol;
    this.mobTol = mobTol;
    this.mzWeight = mzWeight;
    this.rtWeight = rtWeight;
    this.mobilityWeight = mobilityWeight;
    for (FeatureList flist : originalFeatureLists) {
      originalRowsMap.put(flist.getRawDataFile(0), flist.getRows().sorted(MZ_ASCENDING));
    }
    totalSamples = originalRowsMap.size();
  }

  /**
   * Calculates score of aligned row
   *
   * @param alignedRow check for each sample how many feature in original might have matched
   * @return alignment score
   */
  public @NotNull AlignmentScores calcScore(@NotNull FeatureListRow alignedRow) {
    Float rt = alignedRow.getAverageRT();
    Float mobility = alignedRow.getAverageMobility();
    Double mz = alignedRow.getAverageMZ();
    var mzRange = mzTol.getToleranceRange(mz);
    Range<Float> rtRange = rt != null && rtTol != null ? rtTol.getToleranceRange(rt) : Range.all();
    Range<Float> mobilityRange =
        mobTol != null && mobility != null ? mobTol.getToleranceRange(mobility) : Range.all();

    // calculate difference
    int testedAlignedFeatures = 0;
    double mzDiff = 0;
    float rtDiff = 0;
    float mobilityDiff = 0;
    double alignmentScore = 0;
    // extra features more than the aligned
    int sumExtra = 0;
    for (var entry : originalRowsMap.entrySet()) {
      RawDataFile raw = entry.getKey();
      SortedList<FeatureListRow> originals = entry.getValue();

      // result is the number of possible features for this raw data file
      List<FeatureListRow> matchedRows = FeatureListUtils.getCandidatesWithinRanges(mzRange,
          rtRange, mobilityRange, originals, true);
      var feature = alignedRow.getFeature(raw);
      // if the row has a feature, remove 1 and then add to the total
      sumExtra += Math.max(0, matchedRows.size() - (feature != null ? 1 : 0));

      if (feature != null) {
        testedAlignedFeatures++;
        if (mz != null && feature.getMZ() != null) {
          mzDiff += Math.abs(feature.getMZ() - mz);
        }
        if (rt != null && feature.getRT() != null) {
          rtDiff += Math.abs(feature.getRT() - rt);
        }
        if (mobility != null && feature.getMobility() != null) {
          mobilityDiff += Math.abs(feature.getMobility() - mobility);
        }
        alignmentScore += FeatureListUtils.getAlignmentScore(feature, mzRange, rtRange, null,
            mobilityRange, mzWeight, rtWeight, mobilityWeight, 1);
      }
    }

    // rows
    mobilityDiff = mobilityDiff / testedAlignedFeatures;
    alignmentScore = alignmentScore / testedAlignedFeatures;
    rtDiff = rtDiff / testedAlignedFeatures;
    mzDiff = mzDiff / testedAlignedFeatures;
    float ppm = (float) (mzDiff / mz * 1_000_000f);
    float rate = testedAlignedFeatures / (float) totalSamples;

    return new AlignmentScores(rate, testedAlignedFeatures, sumExtra, (float) alignmentScore, ppm,
        mzDiff, rt != null ? rtDiff : null, mobility != null ? mobilityDiff : null);
  }
}
