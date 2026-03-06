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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.id_isotopepeakscanner;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityTolerance;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MajorIsotopeIdentifier {

  private final Map<Integer, Double> resultingScores = new HashMap<>();
  private final List<FeatureListRow> rowsWithBestIPs = new ArrayList<>();
  private final Map<Integer, IsotopePattern> resultingIsotopePattern = new HashMap<>();
  private final PeakListHandler resultMapOfIsotopes = new PeakListHandler();
  private final PeakListHandler resultMapOfMajorIsotopesWithBestScores = new PeakListHandler();

  public void findMajorIsotopes(List<FeatureListRow> rowsWithIPs, Map<Integer, Double> scores,
      Map<Integer, IsotopePattern> detectedIsotopePattern, RTTolerance rtTolerance,
      MobilityTolerance mobTolerance, Boolean resolvedMobility) {
    for (FeatureListRow candidate : rowsWithIPs) {

      boolean bestScore = checkIfRowHasTheHighestIPSimilarityScore(rowsWithIPs, candidate, scores,
          detectedIsotopePattern.get(candidate.getID()).getDataPointMZRange(), rtTolerance,
          mobTolerance, detectedIsotopePattern, resolvedMobility);
      double candidateScore = scores.get(candidate.getID());
      if (bestScore) {
        if (resultMapOfIsotopes.containsID(candidate.getID())
            && resultingScores.get(candidate.getID()) > candidateScore) {
          continue;
        }
        resultMapOfIsotopes.addRow(candidate);
        rowsWithBestIPs.add(candidate);
        resultingIsotopePattern.put(candidate.getID(),
            detectedIsotopePattern.get(candidate.getID()));
        resultingScores.put(candidate.getID(), scores.get(candidate.getID()));
      }
    }
  }

  public void findAllIsotopes(List<FeatureListRow> rowsWithIPs, Map<Integer, Double> scores,
      Map<Integer, IsotopePattern> detectedIsotopePattern) {
    for (FeatureListRow candidate : rowsWithIPs) {

      if (resultMapOfIsotopes.containsID(candidate.getID())
          && resultingScores.get(candidate.getID()) > scores.get(candidate.getID())) {
        continue;
      }
      resultMapOfIsotopes.addRow(candidate);
      rowsWithBestIPs.add(candidate);
      resultingIsotopePattern.put(candidate.getID(), detectedIsotopePattern.get(candidate.getID()));
      resultingScores.put(candidate.getID(), scores.get(candidate.getID()));
    }
  }


  public void findMajorIsotopesWithBestScores(RTTolerance rtTolerance,
      MobilityTolerance mobTolerance, Boolean resolvedMobility) {
    for (FeatureListRow candidate : rowsWithBestIPs) {

      boolean bestScoreOfAllIPs = checkIfRowHasTheHighestIPSimilarityScore(rowsWithBestIPs,
          candidate, resultingScores,
          resultingIsotopePattern.get(candidate.getID()).getDataPointMZRange(), rtTolerance,
          mobTolerance, resultingIsotopePattern, resolvedMobility);
      if (bestScoreOfAllIPs) {
        candidate.getBestFeature()
            .setIsotopePattern(resultingIsotopePattern.get(candidate.getID()));
        resultMapOfMajorIsotopesWithBestScores.addRow(candidate);
      }
    }
  }

  // Comparing the scores of all features within the RT range and MZ range of the candidate feature's IsotopePattern
// with the candidate's isotope pattern score, if the candidate has the highest score the value becomes true
  public Boolean checkIfRowHasTheHighestIPSimilarityScore(List<FeatureListRow> rows,
      FeatureListRow candidate, Map<Integer, Double> scores, Range<Double> DataPointMZRange,
      RTTolerance rtTolerance, MobilityTolerance mobTolerance,
      Map<Integer, IsotopePattern> resultingIsotopePattern, Boolean resolvedMobility) {
    for (FeatureListRow row : rows) {

      Range<Double> MZRangeOfRow = resultingIsotopePattern.get(row.getID()).getDataPointMZRange();

      //A distinction is made between mobility-resolved data, where only one feature within
      //the same mobility range is selected, and MS data without mobility data, where only scores within
      //the same mass ranges of the detected  isotope patterns are compared and the features with the
      // highest scores within these mass ranges is selected to consider possible co-elutions

      if (resolvedMobility && rtTolerance.checkWithinTolerance(row.getAverageRT(),
          candidate.getAverageRT()) && checkMobility(mobTolerance, candidate, row)) {
        if (scores.get(row.getID()) > (scores.get(candidate.getID()) + 0.01)) {
          return false;
        }
      } else if (!resolvedMobility
          || candidate.getAverageMobility() == null && rtTolerance.checkWithinTolerance(
          row.getAverageRT(), candidate.getAverageRT()) && (
          DataPointMZRange.contains(row.getAverageMZ()) || MZRangeOfRow.contains(
              candidate.getAverageMZ()))) {

        if (scores.get(row.getID()) > (scores.get(candidate.getID()) + 0.01)) {
          return false;
        }
      }

    }
    return true;
  }


  public static Boolean checkMobility(MobilityTolerance mobTolerance, FeatureListRow candidate,
      FeatureListRow row) {
    if (candidate.getAverageMobility() != null && row.getAverageMobility() != null) {
      return mobTolerance.checkWithinTolerance(candidate.getAverageMobility(),
          row.getAverageMobility());
    } else {
      return false;
    }
  }
  public Map<Integer, IsotopePattern> getResultingIsotopePattern() {
    return resultingIsotopePattern;
  }

  public Map<Integer, Double> getResultingScores() {
    return resultingScores;
  }

  public List<FeatureListRow> getRowsWithBestIPs() {
    return rowsWithBestIPs;
  }

  public PeakListHandler getResultMapOfIsotopes() {
    return resultMapOfIsotopes;
  }

  public PeakListHandler getResultMapOfMajorIsotopesWithBestScores() {
    return resultMapOfMajorIsotopesWithBestScores;
  }
}
