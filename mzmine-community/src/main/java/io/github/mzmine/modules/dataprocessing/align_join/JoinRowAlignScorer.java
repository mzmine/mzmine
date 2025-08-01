/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.dataprocessing.align_common.FeatureRowAlignScorer;
import io.github.mzmine.modules.tools.isotopepatternscore.IsotopePatternScoreCalculator;
import io.github.mzmine.modules.tools.isotopepatternscore.IsotopePatternScoreParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityTolerance;
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.exceptions.MissingMassListException;
import io.github.mzmine.util.scans.similarity.SpectralSimilarity;
import io.github.mzmine.util.scans.similarity.SpectralSimilarityFunction;
import io.github.mzmine.util.scans.similarity.SpectralSimilarityFunctions;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * the row aligner used by {@link JoinAlignerTask}. Based on weighted mz, rt, mobility difference
 * and additional filters
 */
public class JoinRowAlignScorer implements FeatureRowAlignScorer {

  private final MZTolerance mzTolerance;
  private final RTTolerance rtTolerance;
  private final MobilityTolerance mobilityTolerance;
  private final double mzWeight;
  private final double rtWeight;
  private final double mobilityWeight;
  private final boolean sameIDRequired;
  private final boolean sameChargeRequired;
  private final boolean compareIsotopePattern;
  private final boolean compareSpectraSimilarity;
  private final boolean compareMobility;
  private final Double minIsotopeScore;
  private final Double isotopeNoiseLevel;
  private final MZTolerance isotopeMZTolerance;
  // fields for spectra similarity
  private final SpectralSimilarityFunction simFunction;
  private final MZTolerance mzToleranceSpectralSim;
  private final int msLevel;

  public JoinRowAlignScorer(final ParameterSet parameters) {
    mzTolerance = parameters.getValue(JoinAlignerParameters.MZTolerance);
    rtTolerance = parameters.getValue(JoinAlignerParameters.RTTolerance);

    compareMobility = parameters.getValue(JoinAlignerParameters.mobilityTolerance);
    mobilityTolerance = parameters.getEmbeddedParameterValueIfSelectedOrElse(
        JoinAlignerParameters.mobilityTolerance, null);

    mzWeight = parameters.getValue(JoinAlignerParameters.MZWeight);
    rtWeight = parameters.getValue(JoinAlignerParameters.RTWeight);
    mobilityWeight = parameters.getValue(JoinAlignerParameters.mobilityWeight);

    sameChargeRequired = parameters.getValue(JoinAlignerParameters.SameChargeRequired);

    sameIDRequired = parameters.getValue(JoinAlignerParameters.SameIDRequired);
    compareIsotopePattern = parameters.getValue(JoinAlignerParameters.compareIsotopePattern);
    final ParameterSet isoParam = parameters.getParameter(
        JoinAlignerParameters.compareIsotopePattern).getEmbeddedParameters();

    if (compareIsotopePattern) {
      minIsotopeScore = isoParam.getValue(
          IsotopePatternScoreParameters.isotopePatternScoreThreshold);
      isotopeNoiseLevel = isoParam.getValue(IsotopePatternScoreParameters.isotopeNoiseLevel);
      isotopeMZTolerance = isoParam.getValue(IsotopePatternScoreParameters.mzTolerance);
    } else {
      minIsotopeScore = null;
      isotopeNoiseLevel = null;
      isotopeMZTolerance = null;
    }

    compareSpectraSimilarity = parameters.getValue(JoinAlignerParameters.compareSpectraSimilarity);

    if (compareSpectraSimilarity) {
      var simParam = parameters.getEmbeddedParameterValue(
          JoinAlignerParameters.compareSpectraSimilarity);
      mzToleranceSpectralSim = simParam.getValue(
          JoinAlignerSpectraSimilarityScoreParameters.mzTolerance);
      msLevel = simParam.getValue(JoinAlignerSpectraSimilarityScoreParameters.msLevel);
      var simfuncParam = simParam.getParameter(
          JoinAlignerSpectraSimilarityScoreParameters.similarityFunction).getValueWithParameters();
      simFunction = SpectralSimilarityFunctions.createOption(simfuncParam);
    } else {
      msLevel = 2;
      mzToleranceSpectralSim = null;
      simFunction = null;
    }
  }

  @Override
  public void scoreRowAgainstBaseRows(final List<FeatureListRow> baseRowsByMz,
      final FeatureListRow rowToAdd, final ConcurrentLinkedDeque<RowVsRowScore> scoresList) {
    // ranges are build with prechecks - so if there is no mobility use Range.all() to deactivate the filter
    final Range<Double> mzRange =
        mzWeight > 0 ? mzTolerance.getToleranceRange(rowToAdd.getAverageMZ()) : Range.all();
    final Range<Float> rtRange =
        rtWeight > 0 ? rtTolerance.getToleranceRange(rowToAdd.getAverageRT()) : Range.all();
    final Range<Float> mobilityRange =
        compareMobility && mobilityWeight > 0 && rowToAdd.getAverageMobility() != null
            ? mobilityTolerance.getToleranceRange(rowToAdd.getAverageMobility()) : Range.all();

    // find all rows in the aligned rows that might match
    List<FeatureListRow> candidatesInAligned = FeatureListUtils.getCandidatesWithinRanges(mzRange,
        rtRange, mobilityRange, baseRowsByMz, true);

    if (candidatesInAligned.isEmpty()) {
      return;
    }

    // calculate score for unaligned row against all candidates
    for (FeatureListRow candidateInAligned : candidatesInAligned) {
      // retention time and m/z is already checked for candidates
      if (additionalChecks(rowToAdd, candidateInAligned)) {
        final RowVsRowScore score = new RowVsRowScore(rowToAdd, candidateInAligned, mzRange,
            rtRange, mobilityRange, null, mzWeight, rtWeight, mobilityWeight, 0);
        scoresList.add(score);
      }
    }
  }

  private boolean additionalChecks(final FeatureListRow row,
      final FeatureListRow candidateInAligned) {
    return (!sameChargeRequired || FeatureUtils.compareChargeState(row, candidateInAligned)) //
           && (!sameIDRequired || FeatureUtils.compareIdentities(row, candidateInAligned))
           && checkIsotopePattern(row, candidateInAligned) //
           && checkSpectralSimilarity(row, candidateInAligned);
  }

  private boolean checkSpectralSimilarity(FeatureListRow row, FeatureListRow candidate) {
    // compare the similarity of spectra mass lists on MS1 or
    // MS2 level
    if (compareSpectraSimilarity) {
      DataPoint[] rowDPs = null;
      DataPoint[] candidateDPs = null;
      SpectralSimilarity sim;

      // get data points of mass list of the representative
      // scans
      if (msLevel == 1) {
        rowDPs = getScanMassList(row.getBestFeature().getRepresentativeScan());
        candidateDPs = getScanMassList(candidate.getBestFeature().getRepresentativeScan());
      }

      // get data points of mass list of the best
      // fragmentation scans
      if (msLevel == 2) {
        if (row.getMostIntenseFragmentScan() != null
            && candidate.getMostIntenseFragmentScan() != null) {
          rowDPs = getScanMassList(row.getMostIntenseFragmentScan());
          candidateDPs = getScanMassList(candidate.getMostIntenseFragmentScan());
        } else {
          return false;
        }
      }

      // compare mass list data points of selected scans
      if (rowDPs != null && candidateDPs != null) {

        // calculate similarity using SimilarityFunction
        sim = createSimilarity(rowDPs, candidateDPs);

        // check if similarity is null. Similarity is not
        // null if similarity score is >= the
        // user set threshold
        return sim != null;
      }
    }
    return true;
  }

  private static DataPoint[] getScanMassList(final Scan scan) {
    if (scan == null) {
      return null;
    }
    if (scan.getMassList() == null) {
      throw new MissingMassListException(scan);
    }
    return scan.getMassList().getDataPoints();
  }

  private boolean checkIsotopePattern(FeatureListRow row, FeatureListRow candidate) {
    if (compareIsotopePattern) {
      IsotopePattern ip1 = row.getBestIsotopePattern();
      IsotopePattern ip2 = candidate.getBestIsotopePattern();

      return (ip1 == null) || (ip2 == null) || IsotopePatternScoreCalculator.checkMatch(ip1, ip2,
          isotopeMZTolerance, isotopeNoiseLevel, minIsotopeScore);
    }
    return true;
  }

  /**
   * Uses the similarity function and filter to create similarity.
   *
   * @return positive match with similarity or null if criteria was not met
   */
  private SpectralSimilarity createSimilarity(DataPoint[] library, DataPoint[] query) {
    return simFunction.getSimilarity(mzToleranceSpectralSim, 0, library, query);
  }

  /**
   * @param alignedFeatureList   the list to be scored
   * @param originalFeatureLists the scoring is limited to the feature lists (and their samples)
   */
  @Override
  public void calculateAlignmentScores(final ModularFeatureList alignedFeatureList,
      final List<FeatureList> originalFeatureLists) {

    MobilityTolerance mobTol = compareMobility ? mobilityTolerance : null;
    RowAlignmentScoreCalculator calculator = new RowAlignmentScoreCalculator(originalFeatureLists,
        mzTolerance, rtTolerance, mobTol, mzWeight, rtWeight, mobilityWeight);
    FeatureListUtils.addAlignmentScores(alignedFeatureList, calculator, false);
  }
}
