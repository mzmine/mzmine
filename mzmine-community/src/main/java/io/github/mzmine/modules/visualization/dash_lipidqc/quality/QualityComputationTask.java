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

package io.github.mzmine.modules.visualization.dash_lipidqc.quality;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.javafx.mvci.FxUpdateTask;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid.scoring.LipidQcScoringUtils;
import io.github.mzmine.modules.dataprocessing.id_lipidid.scoring.LipidQcScoringUtils.InterferenceMetrics;
import io.github.mzmine.modules.visualization.dash_lipidqc.LipidQcAnnotationSelectionUtils;
import io.github.mzmine.modules.visualization.dash_lipidqc.kendrick.KendrickFalseNegativeCandidate;
import io.github.mzmine.modules.visualization.dash_lipidqc.kendrick.KendrickFalseNegativeDetector;
import io.github.mzmine.modules.visualization.dash_lipidqc.kendrick.KendrickFalsePositiveUtils;
import io.github.mzmine.modules.visualization.dash_lipidqc.kendrick.KendrickReviewMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Background task that computes all annotation quality metrics (MS1, MS2, adduct, isotope, elution
 * order, interference, false-positive/negative flags) for the selected row and pushes the result to
 * the {@link AnnotationQualityModel}.
 */
final class QualityComputationTask extends FxUpdateTask<AnnotationQualityModel> {

  // Snapshots taken from model in constructor for thread safety
  private final @Nullable FeatureListRow row;
  private final @Nullable ModularFeatureList featureList;
  private final boolean includeRetentionTimeAnalysis;
  private final @NotNull KendrickReviewMode reviewMode;
  private @NotNull QualityComputationResult result;

  QualityComputationTask(final @NotNull AnnotationQualityModel model) {
    super("lipidqc-quality-update", model);
    this.row = model.getRow();
    this.featureList = model.getFeatureList();
    this.includeRetentionTimeAnalysis = model.isRetentionTimeAnalysisEnabled();
    this.reviewMode = model.getKendrickReviewMode();
    result = new QualityComputationResult("Select a row with lipid annotations.", null,
        new InterferenceMetrics(0, 0), List.of(), List.of(), null, null, null);
  }

  @Override
  protected void process() {
    final @Nullable String falsePositiveReason = row == null || featureList == null
        || reviewMode != KendrickReviewMode.POTENTIAL_FALSE_POSITIVE ? null
        : KendrickFalsePositiveUtils.potentialFalsePositiveReason(featureList, row,
            includeRetentionTimeAnalysis);
    final @Nullable KendrickFalseNegativeCandidate falseNegativeCandidate =
        row == null || featureList == null
            || reviewMode != KendrickReviewMode.POTENTIAL_FALSE_NEGATIVE ? null
            : new KendrickFalseNegativeDetector(featureList).detectCandidate(row);
    final @Nullable QualityCardData falseNegativeQualityCard =
        row == null || falseNegativeCandidate == null ? null
            : LipidQualityCardUtils.computeQualityCardData(row, featureList,
                falseNegativeCandidate.match(), LipidQcScoringUtils.computeInterferenceMetrics(row),
                includeRetentionTimeAnalysis);
    if (row == null) {
      result = new QualityComputationResult("Select a row with lipid annotations.", null,
          new InterferenceMetrics(0, 0), List.of(), List.of(), falsePositiveReason,
          falseNegativeCandidate, falseNegativeQualityCard);
      return;
    }
    final List<MatchedLipid> matches = row.getLipidMatches();
    if (matches.isEmpty()) {
      final InterferenceMetrics emptyRowInterference = LipidQcScoringUtils.computeInterferenceMetrics(
          row);
      final @Nullable String placeholder = falseNegativeQualityCard == null
          ? "No lipid annotations available for selected row." : null;
      result = new QualityComputationResult(placeholder, null, emptyRowInterference, List.of(),
          List.of(), falsePositiveReason, falseNegativeCandidate, falseNegativeQualityCard);
      return;
    }
    final List<MatchedLipid> matchSnapshot = List.copyOf(matches);
    final InterferenceMetrics interferenceMetrics = LipidQcScoringUtils.computeInterferenceMetrics(
        row);
    final MatchedLipid selectedAnnotation = Objects.requireNonNullElse(
        LipidQcAnnotationSelectionUtils.getPreferredLipidMatch(row), matchSnapshot.getFirst());
    final List<MatchedLipid> orderedMatches = new ArrayList<>(matchSnapshot.size());
    orderedMatches.add(selectedAnnotation);
    orderedMatches.addAll(
        matchSnapshot.stream().filter(match -> !Objects.equals(match, selectedAnnotation)).toList());
    final String annotationText = Objects.toString(
        selectedAnnotation.getLipidAnnotation().getAnnotation(), "");
    final List<FeatureListRow> duplicateRows = findDuplicateRowsExcludingSelected(featureList,
        row.getID(), annotationText);
    final List<QualityCardData> cards = new ArrayList<>(orderedMatches.size());
    for (final MatchedLipid match : orderedMatches) {
      cards.add(
          LipidQualityCardUtils.computeQualityCardData(row, featureList, match, interferenceMetrics,
              includeRetentionTimeAnalysis));
    }
    result = new QualityComputationResult(null, selectedAnnotation, interferenceMetrics,
        List.copyOf(duplicateRows), List.copyOf(cards), falsePositiveReason, falseNegativeCandidate,
        falseNegativeQualityCard);
  }

  @Override
  protected void updateGuiModel() {
    model.setQualityResult(result);
  }

  @Override
  public @NotNull String getTaskDescription() {
    return "Calculating lipid annotation quality cards";
  }

  @Override
  public double getFinishedPercentage() {
    return 0d;
  }

  /**
   * Finds all rows in the feature list that share the given annotation string, excluding the row
   * with the given ID.
   */
  static @NotNull List<FeatureListRow> findDuplicateRowsExcludingSelected(
      final @Nullable ModularFeatureList featureList, final int selectedRowId,
      final @NotNull String annotation) {
    if (featureList == null || annotation.isBlank()) {
      return List.of();
    }
    return featureList.getRows().stream().filter(r -> Objects.equals(
            Optional.ofNullable(LipidQcAnnotationSelectionUtils.getPreferredLipidMatch(r))
                .map(match -> match.getLipidAnnotation().getAnnotation()).orElse(null), annotation))
        .filter(r -> r.getID() != selectedRowId)
        .sorted(Comparator.comparingInt(FeatureListRow::getID)).toList();
  }
}
