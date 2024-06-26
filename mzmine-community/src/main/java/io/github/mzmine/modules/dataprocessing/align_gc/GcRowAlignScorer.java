/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.align_gc;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.dataprocessing.align_common.FeatureRowAlignScorer;
import io.github.mzmine.modules.dataprocessing.align_join.RowAlignmentScoreCalculator;
import io.github.mzmine.modules.dataprocessing.align_join.RowVsRowScore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.exceptions.MissingMassListException;
import io.github.mzmine.util.scans.similarity.SpectralSimilarity;
import io.github.mzmine.util.scans.similarity.SpectralSimilarityFunction;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.jetbrains.annotations.Nullable;

/**
 * Align features based on RT and spectral similarity in the {@link GCAlignerTask}
 */
public class GcRowAlignScorer implements FeatureRowAlignScorer {

  private final MZTolerance mzTolerance;
  private final RTTolerance rtTolerance;
  private final MZmineProcessingStep<SpectralSimilarityFunction> similarityFunction;
  private final double rtWeight;

  public GcRowAlignScorer(final ParameterSet parameters) {
    this.mzTolerance = parameters.getValue(GCAlignerParameters.MZ_TOLERANCE);
    this.rtTolerance = parameters.getValue(GCAlignerParameters.RT_TOLERANCE);
    rtWeight = parameters.getValue(GCAlignerParameters.RT_WEIGHT);
    this.similarityFunction = parameters.getValue(GCAlignerParameters.SIMILARITY_FUNCTION);
  }

  @Override
  public void scoreRowAgainstBaseRows(final List<FeatureListRow> baseRowsByRt,
      final FeatureListRow rowToAdd, final ConcurrentLinkedDeque<RowVsRowScore> scoresList) {


    final Range<Float> rtRange = rtTolerance.getToleranceRange(rowToAdd.getAverageRT());
    // find all rows in the aligned rows that might match
    final List<FeatureListRow> candidatesInAligned = FeatureListUtils.getCandidatesWithinRtRange(
        rtRange, baseRowsByRt, true);
    if (candidatesInAligned.isEmpty()) {
      return;
    }

    // calculate score for unaligned row against all candidates
    for (FeatureListRow candidateInAligned : candidatesInAligned) {
      // retention time is already checked for candidates
      SpectralSimilarity similarity = checkSpectralSimilarity(rowToAdd, candidateInAligned);
      if (similarity != null) {
        final RowVsRowScore score = new RowVsRowScore(rowToAdd, candidateInAligned, rtRange,
            rtWeight, similarity.getScore(), 1);
        scoresList.add(score);
      }
    }
  }

  private SpectralSimilarity checkSpectralSimilarity(FeatureListRow row, FeatureListRow candidate) {
    DataPoint[] rowDPs = extractMostIntenseFragmentScan(row);
    DataPoint[] candidateDPs = extractMostIntenseFragmentScan(candidate);
    SpectralSimilarity sim;

    // compare mass list data points of selected scans
    if (rowDPs != null && candidateDPs != null) {
      sim = similarityFunction.getModule()
          .getSimilarity(similarityFunction.getParameterSet(), mzTolerance, 0, rowDPs,
              candidateDPs);
      return sim;
    }
    return null;
  }

  @Nullable
  private DataPoint[] extractMostIntenseFragmentScan(final FeatureListRow row) {
    Scan scan = row.getMostIntenseFragmentScan();
    if (scan == null || scan.getMSLevel() != 1) {
      return null;
    }
    if (scan.getMassList() == null) {
      throw new MissingMassListException(scan);
    }
    return scan.getMassList().getDataPoints();
  }


  /**
   * @param alignedFeatureList   the list to be scored
   * @param originalFeatureLists the scoring is limited to the feature lists (and their samples)
   */
  @Override
  public void calculateAlignmentScores(final ModularFeatureList alignedFeatureList,
      final List<FeatureList> originalFeatureLists) {

    // TODO think about best way to calculate an alignment score that also includes spectral similarity
    RowAlignmentScoreCalculator calculator = new RowAlignmentScoreCalculator(originalFeatureLists,
        mzTolerance, rtTolerance, null, 0, rtWeight, 0);
    FeatureListUtils.addAlignmentScores(alignedFeatureList, calculator, false);
  }
}
