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
import static io.github.mzmine.modules.visualization.dash_lipidqc.LipidQcAnnotationSelectionUtils.extractCarbons;
import static io.github.mzmine.modules.visualization.dash_lipidqc.LipidQcAnnotationSelectionUtils.extractDbe;
import static io.github.mzmine.modules.visualization.dash_lipidqc.LipidQcAnnotationSelectionUtils.getPreferredOrPotentialLipidMatch;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Background task that computes ECN and DBE retention trend datasets for the
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
      final @Nullable FeatureListRow selectedRow, final @NotNull List<FeatureListRow> rowsWithLipidIds,
      final @Nullable RetentionTrendMode mode, final boolean showAllLipidsOfSelectedClass) {
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
    @Nullable MatchedLipid selectedMatch =
        getPreferredOrPotentialLipidMatch(selectedRow);
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
      case ECN_CARBON_TREND -> {
        if (showAllLipidsOfSelectedClass) {
          result = buildTotalLipidClassResult(mode, selectedMatch, selectedClass);
          return;
        }
        final int dbe = extractDbe(selectedMatch.getLipidAnnotation());
        if (dbe < 0) {
          result = new RetentionComputationResult(
              "Cannot determine DBE for selected lipid annotation.", null);
          return;
        }
        final int matchCount = (int) rowsWithLipidIds.stream()
            .filter(row -> hasMatchingClassAndDbeAnnotation(row, selectedClass, dbe))
            .count();
        if (matchCount < 3) {
          result = new RetentionComputationResult(
              "Not enough lipids for ECN model (need at least 3 rows in same class/DBE group).",
              null);
          return;
        }
        result = new RetentionComputationResult(null,
            new EcnRetentionPayload(mode, selectedMatch, rowsWithLipidIds, selectedClass, dbe,
                matchCount));
      }
      case DBE_TREND -> {
        if (showAllLipidsOfSelectedClass) {
          result = buildTotalLipidClassResult(mode, selectedMatch, selectedClass);
          return;
        }
        final int carbons = extractCarbons(
            selectedMatch.getLipidAnnotation());
        final int selectedDbe = extractDbe(
            selectedMatch.getLipidAnnotation());
        if (carbons < 0 || selectedDbe < 0) {
          result = new RetentionComputationResult(
              "Cannot determine carbon/DBE values for selected lipid annotation.", null);
          return;
        }
        final RetentionTrendDataset trendDataset = new RetentionTrendDataset(rowsWithLipidIds,
            match -> match.getLipidAnnotation().getLipidClass().equals(selectedClass)
                && extractCarbons(match.getLipidAnnotation()) == carbons,
            match -> extractDbe(match.getLipidAnnotation()));
        if (trendDataset.getItemCount(0) < 3) {
          result = new RetentionComputationResult(
              "Not enough lipids for DBE trend model (need at least 3 rows in same class/carbon group).",
              null);
          return;
        }
        result = new RetentionComputationResult(null,
            new DbeRetentionPayload(mode, selectedMatch, selectedClass, carbons, selectedDbe,
                trendDataset));
      }
      case COMBINED_CARBON_DBE_TRENDS -> {
        final int selectedCarbons = extractCarbons(
            selectedMatch.getLipidAnnotation());
        final int selectedDbe = extractDbe(
            selectedMatch.getLipidAnnotation());
        if (selectedCarbons < 0 || selectedDbe < 0) {
          result = new RetentionComputationResult(
              "Cannot determine carbon/DBE values for selected lipid annotation.", null);
          return;
        }
        final RetentionTrendDataset carbonTrendDataset = new RetentionTrendDataset(
            rowsWithLipidIds,
            match -> match.getLipidAnnotation().getLipidClass().equals(selectedClass)
                && extractDbe(match.getLipidAnnotation()) == selectedDbe,
            match -> extractCarbons(match.getLipidAnnotation()));
        final RetentionTrendDataset dbeTrendDataset = new RetentionTrendDataset(
            rowsWithLipidIds,
            match -> match.getLipidAnnotation().getLipidClass().equals(selectedClass)
                && extractCarbons(
                match.getLipidAnnotation()) == selectedCarbons,
            match -> extractDbe(match.getLipidAnnotation()));
        final boolean hasCarbonTrend = carbonTrendDataset.getItemCount(0) >= 2;
        final boolean hasDbeTrend = dbeTrendDataset.getItemCount(0) >= 2;
        if (!hasCarbonTrend && !hasDbeTrend) {
          result = new RetentionComputationResult(
              "Not enough lipids for combined trend model (need at least 2 rows in trend group).",
              null);
          return;
        }
        result = new RetentionComputationResult(null,
            new CombinedRetentionPayload(mode, selectedMatch, selectedClass, selectedCarbons,
                selectedDbe, hasCarbonTrend ? carbonTrendDataset : null,
                hasDbeTrend ? dbeTrendDataset : null));
      }
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

  private static boolean hasMatchingClassAndDbeAnnotation(final @NotNull FeatureListRow row,
      final @NotNull ILipidClass selectedClass, final int dbe) {
    for (final MatchedLipid match : row.getLipidMatches()) {
      if (!match.getLipidAnnotation().getLipidClass().equals(selectedClass)) {
        continue;
      }
      if (extractDbe(match.getLipidAnnotation()) == dbe) {
        return true;
      }
    }
    return false;
  }

  private @NotNull RetentionComputationResult buildTotalLipidClassResult(
      final @NotNull RetentionTrendMode mode, final @NotNull MatchedLipid selectedMatch,
      final @NotNull ILipidClass selectedClass) {
    final long matchCount = rowsWithLipidIds.stream().flatMap(row -> row.getLipidMatches().stream())
        .filter(match -> match.getLipidAnnotation().getLipidClass().equals(selectedClass))
        .count();
    if (matchCount == 0L) {
      return new RetentionComputationResult(
          "No lipid annotations available for the selected lipid class.", null);
    }
    return new RetentionComputationResult(null,
        new TotalLipidClassRetentionPayload(mode, selectedMatch, rowsWithLipidIds, selectedClass));
  }
}
