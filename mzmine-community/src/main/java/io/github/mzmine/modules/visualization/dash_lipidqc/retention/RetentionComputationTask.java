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

package io.github.mzmine.modules.visualization.dash_lipidqc.retention;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.javafx.mvci.FxUpdateTask;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.ILipidClass;
import io.github.mzmine.modules.visualization.dash_lipidqc.LipidQcAnnotationSelectionUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Background task that computes neutral retention series data for the
 * {@link EquivalentCarbonNumberPane} in the lipid QC dashboard.
 */
final class RetentionComputationTask extends FxUpdateTask<EquivalentCarbonNumberPane> {

  private final @Nullable FeatureListRow selectedRow;
  private final @NotNull List<FeatureListRow> rowsWithLipidIds;
  private final @Nullable RetentionTrendMode mode;
  private final boolean showAllLipidsOfSelectedClass;
  private @NotNull RetentionComputationResult result =
      new RetentionComputationResult("Select a row with lipid annotations.", null);

  RetentionComputationTask(final @NotNull EquivalentCarbonNumberPane pane,
      final @Nullable FeatureListRow selectedRow,
      final @NotNull List<FeatureListRow> rowsWithLipidIds, final @Nullable RetentionTrendMode mode,
      final boolean showAllLipidsOfSelectedClass) {
    super("lipidqc-retention-update", pane);
    this.selectedRow = selectedRow;
    this.rowsWithLipidIds = List.copyOf(rowsWithLipidIds);
    this.mode = mode;
    this.showAllLipidsOfSelectedClass = showAllLipidsOfSelectedClass;
  }

  @Override
  protected void process() {
    if (selectedRow == null) {
      result = new RetentionComputationResult("Select a row with lipid annotations.", null);
      return;
    }

    final @Nullable MatchedLipid selectedMatch = LipidQcAnnotationSelectionUtils.getPreferredOrPotentialLipidMatch(
        selectedRow);
    if (selectedMatch == null) {
      result = new RetentionComputationResult(
          "No lipid annotations available for selected row.", null);
      return;
    }
    if (rowsWithLipidIds.isEmpty()) {
      result = new RetentionComputationResult(
          "No lipid annotations available in this feature list.", null);
      return;
    }
    if (mode == null) {
      result = new RetentionComputationResult("Select a retention time trend.", null);
      return;
    }

    final ILipidClass selectedClass = selectedMatch.getLipidAnnotation().getLipidClass();

    switch (mode) {
      case ECN_CARBON_TREND -> processEcnTrend(selectedMatch, selectedClass);
      case DBE_TREND -> processDbeTrend(selectedMatch, selectedClass);
      case COMBINED_CARBON_DBE_TRENDS -> processCombinedTrend(selectedMatch, selectedClass);
    }
  }

  @Override
  protected void updateGuiModel() {
    model.applyComputationResult(result);
  }

  @Override
  public @NotNull String getTaskDescription() {
    return "Calculating retention time analysis";
  }

  @Override
  public double getFinishedPercentage() {
    return 0d;
  }

  private void processEcnTrend(final @NotNull MatchedLipid selectedMatch,
      final @NotNull ILipidClass selectedClass) {
    if (showAllLipidsOfSelectedClass) {
      result = buildTotalLipidClassResult(RetentionTrendMode.ECN_CARBON_TREND, selectedMatch,
          selectedClass);
      return;
    }

    final int dbe = LipidQcAnnotationSelectionUtils.extractDbe(selectedMatch.getLipidAnnotation());
    if (dbe < 0) {
      result = new RetentionComputationResult("Cannot determine DBE for selected lipid annotation.",
          null);
      return;
    }

    final int matchCount = (int) rowsWithLipidIds.stream()
        .filter(row -> hasMatchingClassAndDbeAnnotation(row, selectedClass, dbe)).count();
    if (matchCount < 3) {
      result = new RetentionComputationResult(
          "Not enough lipids for ECN model (need at least 3 rows in same class/DBE group).", null);
      return;
    }

    final RetentionSeriesData series = buildSeriesData(rowsWithLipidIds,
        match -> match.getLipidAnnotation().getLipidClass().equals(selectedClass)
            && LipidQcAnnotationSelectionUtils.extractDbe(match.getLipidAnnotation()) == dbe,
        match -> LipidQcAnnotationSelectionUtils.extractCarbons(match.getLipidAnnotation()),
        "Retention trend");
    result = new RetentionComputationResult(null,
        new EcnRetentionPayload(RetentionTrendMode.ECN_CARBON_TREND, selectedMatch, selectedClass,
            dbe, matchCount, series));
  }

  private void processDbeTrend(final @NotNull MatchedLipid selectedMatch,
      final @NotNull ILipidClass selectedClass) {
    if (showAllLipidsOfSelectedClass) {
      result = buildTotalLipidClassResult(RetentionTrendMode.DBE_TREND, selectedMatch,
          selectedClass);
      return;
    }

    final int carbons = LipidQcAnnotationSelectionUtils.extractCarbons(
        selectedMatch.getLipidAnnotation());
    final int selectedDbe = LipidQcAnnotationSelectionUtils.extractDbe(
        selectedMatch.getLipidAnnotation());
    if (carbons < 0 || selectedDbe < 0) {
      result = new RetentionComputationResult(
          "Cannot determine carbon/DBE values for selected lipid annotation.", null);
      return;
    }

    final RetentionSeriesData series = buildSeriesData(rowsWithLipidIds,
        match -> match.getLipidAnnotation().getLipidClass().equals(selectedClass)
            && LipidQcAnnotationSelectionUtils.extractCarbons(match.getLipidAnnotation())
            == carbons,
        match -> LipidQcAnnotationSelectionUtils.extractDbe(match.getLipidAnnotation()),
        "Retention trend");
    if (series.points().size() < 3) {
      result = new RetentionComputationResult(
          "Not enough lipids for DBE trend model (need at least 3 rows in same class/carbon group).",
          null);
      return;
    }

    result = new RetentionComputationResult(null,
        new DbeRetentionPayload(RetentionTrendMode.DBE_TREND, selectedMatch, selectedClass, carbons,
            selectedDbe, series));
  }

  private void processCombinedTrend(final @NotNull MatchedLipid selectedMatch,
      final @NotNull ILipidClass selectedClass) {
    final int selectedCarbons = LipidQcAnnotationSelectionUtils.extractCarbons(
        selectedMatch.getLipidAnnotation());
    final int selectedDbe = LipidQcAnnotationSelectionUtils.extractDbe(
        selectedMatch.getLipidAnnotation());
    if (selectedCarbons < 0 || selectedDbe < 0) {
      result = new RetentionComputationResult(
          "Cannot determine carbon/DBE values for selected lipid annotation.", null);
      return;
    }

    final RetentionSeriesData carbonSeries = buildSeriesData(rowsWithLipidIds,
        match -> match.getLipidAnnotation().getLipidClass().equals(selectedClass)
            && LipidQcAnnotationSelectionUtils.extractDbe(match.getLipidAnnotation())
            == selectedDbe,
        match -> LipidQcAnnotationSelectionUtils.extractCarbons(match.getLipidAnnotation()),
        "Carbon trend");
    final RetentionSeriesData dbeSeries = buildSeriesData(rowsWithLipidIds,
        match -> match.getLipidAnnotation().getLipidClass().equals(selectedClass)
            && LipidQcAnnotationSelectionUtils.extractCarbons(match.getLipidAnnotation())
            == selectedCarbons,
        match -> LipidQcAnnotationSelectionUtils.extractDbe(match.getLipidAnnotation()),
        "DBE trend");
    final boolean hasCarbonTrend = carbonSeries.points().size() >= 2;
    final boolean hasDbeTrend = dbeSeries.points().size() >= 2;
    if (!hasCarbonTrend && !hasDbeTrend) {
      result = new RetentionComputationResult(
          "Not enough lipids for combined trend model (need at least 2 rows in trend group).",
          null);
      return;
    }

    result = new RetentionComputationResult(null,
        new CombinedRetentionPayload(RetentionTrendMode.COMBINED_CARBON_DBE_TRENDS, selectedMatch,
            selectedClass, selectedCarbons, selectedDbe, hasCarbonTrend ? carbonSeries : null,
            hasDbeTrend ? dbeSeries : null));
  }

  private @NotNull RetentionComputationResult buildTotalLipidClassResult(
      final @NotNull RetentionTrendMode trendMode, final @NotNull MatchedLipid selectedMatch,
      final @NotNull ILipidClass selectedClass) {
    final boolean showEcnTrend = trendMode == RetentionTrendMode.ECN_CARBON_TREND;
    final List<RetentionSeriesData> series = buildGroupedSeriesData(rowsWithLipidIds, selectedClass,
        showEcnTrend ? match -> LipidQcAnnotationSelectionUtils.extractDbe(
            match.getLipidAnnotation())
            : match -> LipidQcAnnotationSelectionUtils.extractCarbons(match.getLipidAnnotation()),
        showEcnTrend ? match -> LipidQcAnnotationSelectionUtils.extractCarbons(
            match.getLipidAnnotation())
            : match -> LipidQcAnnotationSelectionUtils.extractDbe(match.getLipidAnnotation()),
        showEcnTrend ? dbe -> "DBE " + dbe : carbons -> "C " + carbons);
    final int annotationCount = series.stream().mapToInt(seriesData -> seriesData.points().size())
        .sum();
    if (annotationCount == 0) {
      return new RetentionComputationResult(
          "No lipid annotations available for the selected lipid class.", null);
    }

    return new RetentionComputationResult(null,
        new TotalLipidClassRetentionPayload(trendMode, selectedMatch, selectedClass, series,
            annotationCount));
  }

  private static @NotNull RetentionSeriesData buildSeriesData(
      final @NotNull List<FeatureListRow> rows, final @NotNull Predicate<MatchedLipid> predicate,
      final @NotNull ToDoubleFunction<MatchedLipid> yValueExtractor,
      final @NotNull String seriesKey) {
    final List<RetentionSeriesPoint> points = new ArrayList<>();
    for (final FeatureListRow row : rows) {
      final Float rowRt = row.getAverageRT();
      if (rowRt == null || !Float.isFinite(rowRt)) {
        continue;
      }
      final List<MatchedLipid> rowMatches = row.getLipidMatches();
      if (rowMatches.isEmpty()) {
        continue;
      }

      final Set<Double> addedYValues = new HashSet<>();
      for (final MatchedLipid match : rowMatches) {
        if (!predicate.test(match)) {
          continue;
        }
        final double yValue = yValueExtractor.applyAsDouble(match);
        if (!Double.isFinite(yValue) || !addedYValues.add(yValue)) {
          continue;
        }
        points.add(new RetentionSeriesPoint(row, match, rowRt.doubleValue(), yValue));
      }
    }
    return new RetentionSeriesData(seriesKey, points);
  }

  private static @NotNull List<RetentionSeriesData> buildGroupedSeriesData(
      final @NotNull List<FeatureListRow> rows, final @NotNull ILipidClass selectedClass,
      final @NotNull ToIntFunction<MatchedLipid> seriesKeyExtractor,
      final @NotNull ToIntFunction<MatchedLipid> yValueExtractor,
      final @NotNull IntFunction<String> seriesLabelFormatter) {
    final Map<Integer, List<RetentionSeriesPoint>> groupedPoints = new TreeMap<>();
    for (final FeatureListRow row : rows) {
      final Float rowRt = row.getAverageRT();
      if (rowRt == null || !Float.isFinite(rowRt)) {
        continue;
      }

      final Map<Integer, Set<Integer>> addedYValuesPerSeries = new HashMap<>();
      for (final MatchedLipid match : row.getLipidMatches()) {
        if (!match.getLipidAnnotation().getLipidClass().equals(selectedClass)) {
          continue;
        }
        final int seriesKey = seriesKeyExtractor.applyAsInt(match);
        final int yValue = yValueExtractor.applyAsInt(match);
        if (seriesKey < 0 || yValue < 0) {
          continue;
        }

        final Set<Integer> addedYValues = addedYValuesPerSeries.computeIfAbsent(seriesKey,
            ignored -> new HashSet<>());
        if (!addedYValues.add(yValue)) {
          continue;
        }

        groupedPoints.computeIfAbsent(seriesKey, ignored -> new ArrayList<>())
            .add(new RetentionSeriesPoint(row, match, rowRt.doubleValue(), yValue));
      }
    }

    final List<RetentionSeriesData> series = new ArrayList<>(groupedPoints.size());
    for (final Map.Entry<Integer, List<RetentionSeriesPoint>> entry : groupedPoints.entrySet()) {
      final List<RetentionSeriesPoint> points = new ArrayList<>(entry.getValue());
      points.sort(Comparator.comparingDouble(RetentionSeriesPoint::x)
          .thenComparingDouble(RetentionSeriesPoint::y));
      series.add(new RetentionSeriesData(seriesLabelFormatter.apply(entry.getKey()), points));
    }
    return series;
  }

  private static boolean hasMatchingClassAndDbeAnnotation(final @NotNull FeatureListRow row,
      final @NotNull ILipidClass selectedClass, final int dbe) {
    for (final MatchedLipid match : row.getLipidMatches()) {
      if (!match.getLipidAnnotation().getLipidClass().equals(selectedClass)) {
        continue;
      }
      if (LipidQcAnnotationSelectionUtils.extractDbe(match.getLipidAnnotation()) == dbe) {
        return true;
      }
    }
    return false;
  }
}
