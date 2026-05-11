/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.visualization.dash_lipidqc.kendrick;

import static io.github.mzmine.modules.dataprocessing.id_lipidid.scoring.LipidQcScoringUtils.clampToUnit;
import static io.github.mzmine.modules.dataprocessing.id_lipidid.scoring.LipidQcScoringUtils.computeElutionOrderMetrics;
import static io.github.mzmine.modules.dataprocessing.id_lipidid.scoring.LipidQcScoringUtils.computeInterferenceMetrics;
import static io.github.mzmine.modules.dataprocessing.id_lipidid.scoring.LipidQcScoringUtils.computeInterferenceScore;
import static io.github.mzmine.modules.dataprocessing.id_lipidid.scoring.LipidQcScoringUtils.computeMs1Score;
import static io.github.mzmine.modules.dataprocessing.id_lipidid.scoring.LipidQcScoringUtils.detectMs1Tolerance;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid.scoring.LipidQcScoringUtils.ElutionOrderMetrics;
import io.github.mzmine.modules.dataprocessing.id_lipidid.scoring.LipidQcScoringUtils.InterferenceMetrics;
import io.github.mzmine.modules.visualization.dash_lipidqc.LipidQcAnnotationSelectionUtils;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class for detecting potentially false-positive lipid annotations based on overall QC
 * score, elution order consistency, and isobaric interference metrics.
 */
public final class KendrickFalsePositiveUtils {

  private static final double OUTLIER_OVERALL_THRESHOLD = 0.5d;
  private static final double OUTLIER_ELUTION_THRESHOLD = 0.55d;

  private KendrickFalsePositiveUtils() {
  }

  public static @Nullable String potentialFalsePositiveReason(
      final @NotNull ModularFeatureList featureList, final @NotNull FeatureListRow row,
      final boolean includeRetentionTimeAnalysis) {
    final @Nullable MatchedLipid match = LipidQcAnnotationSelectionUtils.getPreferredLipidMatch(row);
    if (match == null) {
      return null;
    }

    final List<String> reasons = new ArrayList<>();
    final double overall;
    if (!includeRetentionTimeAnalysis) {
      overall = computeLegacyOverallQualityScore(featureList, row, match, 0d, false);
    } else {
      final ElutionOrderMetrics elution = computeElutionOrderMetrics(featureList, row, match);
      overall = computeLegacyOverallQualityScore(featureList, row, match, elution.combinedScore(),
          true);
      if (elution.carbonsTrend().available()
          && elution.carbonsTrend().score() < OUTLIER_ELUTION_THRESHOLD) {
        reasons.add("carbon trend %.0f%% is below %.0f%%".formatted(
            elution.carbonsTrend().score() * 100d, OUTLIER_ELUTION_THRESHOLD * 100d));
      }
      if (elution.dbeTrend().available() && elution.dbeTrend().score() < OUTLIER_ELUTION_THRESHOLD) {
        reasons.add("DBE trend %.0f%% is below %.0f%%".formatted(elution.dbeTrend().score() * 100d,
            OUTLIER_ELUTION_THRESHOLD * 100d));
      }
      if (elution.combinedScore() < OUTLIER_ELUTION_THRESHOLD) {
        reasons.add("elution order score %.0f%% is below %.0f%%".formatted(
            elution.combinedScore() * 100d, OUTLIER_ELUTION_THRESHOLD * 100d));
      }
    }
    if (overall < OUTLIER_OVERALL_THRESHOLD) {
      reasons.add("overall quality %.0f%% is below %.0f%%".formatted(overall * 100d,
          OUTLIER_OVERALL_THRESHOLD * 100d));
    }

    if (reasons.isEmpty()) {
      return null;
    }
    return String.join("; ", reasons);
  }

  private static double computeLegacyOverallQualityScore(final @NotNull ModularFeatureList featureList,
      final @NotNull FeatureListRow row, final @NotNull MatchedLipid match,
      final double elutionOrderScore,
      final boolean includeRetentionTimeAnalysis) {
    final double ms1Score = computeMs1Score(row, match, detectMs1Tolerance(featureList));
    final double ms2Score = computeMs2Score(match);
    final double adductScore = computeAdductScore(row, match);
    final double isotopeScore = computeIsotopeScore(row, match);
    final InterferenceMetrics interferenceMetrics = computeInterferenceMetrics(row);
    final double interference = computeInterferenceScore(interferenceMetrics.totalPenaltyCount());
    final double scoreSum = ms1Score + ms2Score + adductScore + isotopeScore + interference
        + (includeRetentionTimeAnalysis ? elutionOrderScore : 0d);
    final int scoreCount = includeRetentionTimeAnalysis ? 6 : 5;
    return scoreSum / scoreCount;
  }

  private static double computeMs2Score(final @NotNull MatchedLipid match) {
    final double explainedIntensity = match.getMsMsScore() == null ? 0d : match.getMsMsScore();
    return clampToUnit(explainedIntensity);
  }

  private static double computeAdductScore(final @NotNull FeatureListRow row,
      final @NotNull MatchedLipid match) {
    if (row.getBestIonIdentity() == null) {
      return 0d;
    }
    final String featureAdduct = normalizeAdduct(row.getBestIonIdentity().toString());
    final String lipidAdduct = normalizeAdduct(match.getIonizationType().getAdductName());
    return featureAdduct.equals(lipidAdduct) ? 1d : 0d;
  }

  private static double computeIsotopeScore(final @NotNull FeatureListRow row,
      final @NotNull MatchedLipid match) {
    if (row.getBestIsotopePattern() == null || match.getIsotopePattern() == null) {
      return 0.35d;
    }
    return io.github.mzmine.modules.tools.isotopepatternscore.IsotopePatternScoreCalculator.getSimilarityScore(
        row.getBestIsotopePattern(), match.getIsotopePattern(),
        new io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance(0.003, 10d), 0d);
  }

  private static @NotNull String normalizeAdduct(final @Nullable String adduct) {
    return adduct == null ? "" : adduct.replaceAll("\\s+", "").toLowerCase();
  }
}
