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


import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid.scoring.LipidQcScoringUtils.ElutionOrderMetrics;
import io.github.mzmine.modules.dataprocessing.id_lipidid.scoring.LipidQcScoringUtils;
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

  /**
   * Flags low configured overall quality and inconsistent retention trends.
   *
   * @param featureList                  feature list providing scoring settings and retention
   *                                     context
   * @param row                          row whose preferred lipid annotation is evaluated
   * @param includeRetentionTimeAnalysis whether to evaluate retention-time QC
   * @return warning reasons, or {@code null} if no outlier criterion is met
   */
  public static @Nullable String potentialFalsePositiveReason(
      final @NotNull ModularFeatureList featureList, final @NotNull FeatureListRow row,
      final boolean includeRetentionTimeAnalysis) {
    final @Nullable MatchedLipid match = LipidQcAnnotationSelectionUtils.getPreferredLipidMatch(
        row);
    if (match == null) {
      return null;
    }

    final List<String> reasons = new ArrayList<>();
    final double overall = LipidQcScoringUtils.computeConfiguredAnnotationScore(featureList, row,
        match, includeRetentionTimeAnalysis);
    if (includeRetentionTimeAnalysis) {
      final ElutionOrderMetrics elution = LipidQcScoringUtils.computeElutionOrderMetrics(
          featureList, row, match);
      if (elution.carbonsTrend().available()
          && elution.carbonsTrend().score() < OUTLIER_ELUTION_THRESHOLD) {
        reasons.add(
            "carbon trend %.0f%% is below %.0f%%".formatted(elution.carbonsTrend().score() * 100d,
                OUTLIER_ELUTION_THRESHOLD * 100d));
      }
      if (elution.dbeTrend().available()
          && elution.dbeTrend().score() < OUTLIER_ELUTION_THRESHOLD) {
        reasons.add("DBE trend %.0f%% is below %.0f%%".formatted(elution.dbeTrend().score() * 100d,
            OUTLIER_ELUTION_THRESHOLD * 100d));
      }
      if (elution.combinedScore() < OUTLIER_ELUTION_THRESHOLD) {
        reasons.add(
            "elution order score %.0f%% is below %.0f%%".formatted(elution.combinedScore() * 100d,
                OUTLIER_ELUTION_THRESHOLD * 100d));
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

}
