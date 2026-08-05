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

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScaleTransform;
import io.github.mzmine.javafx.mvci.FxUpdateTask;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.visualization.dash_lipidqc.LipidQcAnnotationSelectionUtils;
import io.github.mzmine.modules.visualization.kendrickmassplot.KendrickMassPlotXYZDataset;
import java.awt.Color;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.renderer.LookupPaintScale;
import org.jfree.chart.renderer.PaintScale;

/**
 * Background task that computes Kendrick filter datasets and QC review overlays (false positives,
 * false negatives) for the {@link KendrickPane} in the lipid QC dashboard.
 */
final class KendrickFilterComputationTask extends FxUpdateTask<KendrickPane> {

  private final long requestId;
  private final @NotNull KendrickMassPlotXYZDataset baseDataset;
  private final @Nullable ModularFeatureList featureList;
  private final @NotNull Set<Integer> visibleRowIds;
  private final @NotNull Map<Integer, Color> selectedRowColors;
  private final boolean multiGroupSelection;
  private final boolean includeRetentionTimeAnalysis;
  private final @NotNull KendrickReviewMode reviewMode;
  private @NotNull KendrickFilterComputationResult result;

  KendrickFilterComputationTask(final @NotNull KendrickPane pane, final long requestId,
      final @NotNull KendrickMassPlotXYZDataset baseDataset,
      final @Nullable ModularFeatureList featureList, final @NotNull Set<Integer> visibleRowIds,
      final @NotNull Map<Integer, Color> selectedRowColors, final boolean multiGroupSelection,
      final boolean includeRetentionTimeAnalysis, final @NotNull KendrickReviewMode reviewMode) {
    super("lipidqc-kendrick-filter-update", pane);
    this.requestId = requestId;
    this.baseDataset = baseDataset;
    this.featureList = featureList;
    this.visibleRowIds = Set.copyOf(visibleRowIds);
    this.selectedRowColors = Map.copyOf(selectedRowColors);
    this.multiGroupSelection = multiGroupSelection;
    this.includeRetentionTimeAnalysis = includeRetentionTimeAnalysis;
    this.reviewMode = reviewMode;
    final KendrickSubsetDataset fallback = new KendrickSubsetDataset(baseDataset, _ -> true);
    result = new KendrickFilterComputationResult(requestId, baseDataset, reviewMode, fallback,
        null, null, createColorPaintScale(fallback), true,
        new LookupPaintScale(0d, 1d, Color.GRAY), null);
  }

  @Override
  protected void process() {
    final Predicate<FeatureListRow> visiblePredicate =
        visibleRowIds.isEmpty() ? _ -> true : row -> visibleRowIds.contains(row.getID());
    final boolean useSolidClassColoring = multiGroupSelection && !visibleRowIds.isEmpty()
        && !selectedRowColors.isEmpty();
    final KendrickSubsetDataset inDataset = useSolidClassColoring ? new KendrickSubsetDataset(
        baseDataset, visiblePredicate, row -> row.getID())
        : new KendrickSubsetDataset(baseDataset, visiblePredicate);
    final PaintScale filteredColorScale = useSolidClassColoring ? createClassPaintScale(inDataset,
        selectedRowColors) : createColorPaintScale(inDataset);
    final KendrickSubsetDataset filteredOutDataset =
        visibleRowIds.isEmpty() ? null
            : new KendrickSubsetDataset(baseDataset, row -> !visiblePredicate.test(row));
    final LookupPaintScale grayScale =
        filteredOutDataset == null ? new LookupPaintScale(0d, 1d, Color.GRAY)
            : createGrayPaintScale(filteredOutDataset);
    final HighlightResult highlightResult = computeHighlightedDataset(inDataset);
    result = new KendrickFilterComputationResult(requestId, baseDataset, reviewMode, inDataset,
        filteredOutDataset, highlightResult.highlightedDataset(), filteredColorScale,
        !useSolidClassColoring, grayScale, highlightResult.falseNegativeDetector());
  }

  private @NotNull HighlightResult computeHighlightedDataset(
      final @NotNull KendrickSubsetDataset inDataset) {
    if (featureList == null || inDataset.getItemCount(0) == 0) {
      return new HighlightResult(null, null);
    }
    return switch (reviewMode) {
      case NONE -> new HighlightResult(null, null);
      case POTENTIAL_FALSE_POSITIVE -> {
        final Set<String> allowedClassNames = visibleRowIds.isEmpty() ? Set.of()
            : collectVisibleClassNames(featureList, visibleRowIds);
        final KendrickSubsetDataset falsePositiveDataset = new KendrickSubsetDataset(baseDataset,
            row -> {
              if (KendrickFalsePositiveUtils.potentialFalsePositiveReason(featureList, row,
                  includeRetentionTimeAnalysis) == null) {
                return false;
              }
              if (visibleRowIds.isEmpty()) {
                return true;
              }
              final @Nullable MatchedLipid preferredMatch =
                  LipidQcAnnotationSelectionUtils.getPreferredLipidMatch(row);
              return preferredMatch != null && allowedClassNames.contains(
                  preferredMatch.getLipidAnnotation().getLipidClass().getName());
            });
        yield new HighlightResult(falsePositiveDataset, null);
      }
      case POTENTIAL_FALSE_NEGATIVE -> {
        final KendrickFalseNegativeDetector detector = new KendrickFalseNegativeDetector(featureList);
        final Set<String> allowedClassNames = visibleRowIds.isEmpty() ? Set.of()
            : collectVisibleClassNames(featureList, visibleRowIds);
        final KendrickSubsetDataset falseNegativeDataset = new KendrickSubsetDataset(baseDataset,
            row -> {
              final @Nullable KendrickFalseNegativeCandidate candidate = detector.detectCandidate(
                  row);
              if (candidate == null) {
                return false;
              }
              if (visibleRowIds.isEmpty()) {
                return true;
              }
              return allowedClassNames.contains(candidate.predictedClassName());
            });
        yield new HighlightResult(falseNegativeDataset, detector);
      }
    };
  }

  private static @NotNull Set<String> collectVisibleClassNames(
      final @NotNull ModularFeatureList featureList, final @NotNull Set<Integer> visibleRowIds) {
    final Set<String> classNames = new HashSet<>();
    for (final FeatureListRow row : featureList.getRows()) {
      if (!visibleRowIds.contains(row.getID())) {
        continue;
      }
      final @Nullable MatchedLipid preferredMatch = LipidQcAnnotationSelectionUtils.getPreferredLipidMatch(
          row);
      if (preferredMatch == null) {
        continue;
      }
      classNames.add(preferredMatch.getLipidAnnotation().getLipidClass().getName());
    }
    return Set.copyOf(classNames);
  }

  @Override
  protected void updateGuiModel() {
    model.applyFilterComputationResult(result);
  }

  @Override
  public @NotNull String getTaskDescription() {
    return "Calculating Kendrick filter datasets";
  }

  @Override
  public double getFinishedPercentage() {
    return 0d;
  }

  private static @NotNull LookupPaintScale createGrayPaintScale(
      final @NotNull KendrickSubsetDataset dataset) {
    final int count = dataset.getItemCount(0);
    if (count == 0) {
      final LookupPaintScale fallback = new LookupPaintScale(0d, 1d, new Color(160, 160, 160));
      fallback.add(0d, new Color(215, 215, 215));
      fallback.add(1d, new Color(105, 105, 105));
      return fallback;
    }

    double min = Double.POSITIVE_INFINITY;
    double max = Double.NEGATIVE_INFINITY;
    for (int i = 0; i < count; i++) {
      final double z = dataset.getZValue(0, i);
      if (Double.isFinite(z)) {
        min = Math.min(min, z);
        max = Math.max(max, z);
      }
    }
    if (!Double.isFinite(min) || !Double.isFinite(max) || min == max) {
      min = 0d;
      max = 1d;
    }
    final LookupPaintScale grayScale = new LookupPaintScale(min, max, new Color(160, 160, 160));
    final int steps = 8;
    for (int i = 0; i < steps; i++) {
      final double value = min + (max - min) * i / (steps - 1d);
      final int shade = 215 - (int) Math.round(120d * i / (steps - 1d));
      grayScale.add(value, new Color(shade, shade, shade, 95));
    }
    return grayScale;
  }

  private static @NotNull PaintScale createColorPaintScale(
      final @NotNull KendrickSubsetDataset dataset) {
    final int count = dataset.getItemCount(0);
    if (count == 0) {
      return new LookupPaintScale(0d, 1d, Color.GRAY);
    }
    double min = Double.POSITIVE_INFINITY;
    double max = Double.NEGATIVE_INFINITY;
    for (int i = 0; i < count; i++) {
      final double z = dataset.getZValue(0, i);
      if (Double.isFinite(z)) {
        min = Math.min(min, z);
        max = Math.max(max, z);
      }
    }
    if (!Double.isFinite(min) || !Double.isFinite(max) || min == max) {
      min = 0d;
      max = 1d;
    }
    return ConfigService.getConfiguration().getDefaultPaintScalePalette()
        .toPaintScale(PaintScaleTransform.LINEAR, Range.closed(min, max));
  }

  private static @NotNull LookupPaintScale createClassPaintScale(
      final @NotNull KendrickSubsetDataset dataset,
      final @NotNull Map<Integer, Color> selectedRowColors) {
    final Color defaultColor = ConfigService.getDefaultColorPalette().getNeutralColorAWT();
    if (dataset.getItemCount(0) == 0) {
      return new LookupPaintScale(0d, 1d, defaultColor);
    }
    final TreeMap<Double, Color> zValueToColor = new TreeMap<>();
    for (int i = 0; i < dataset.getItemCount(0); i++) {
      final FeatureListRow row = dataset.getItemObject(i);
      if (row == null) {
        continue;
      }
      final double zValue = dataset.getZValue(0, i);
      if (!Double.isFinite(zValue)) {
        continue;
      }
      zValueToColor.put(zValue, selectedRowColors.getOrDefault(row.getID(), defaultColor));
    }
    if (zValueToColor.isEmpty()) {
      return new LookupPaintScale(0d, 1d, defaultColor);
    }
    final Map.Entry<Double, Color> firstEntry = zValueToColor.firstEntry();
    final Map.Entry<Double, Color> lastEntry = zValueToColor.lastEntry();
    final double lower = firstEntry.getKey();
    final double upper = lastEntry.getKey() == lower ? lower + 1d : lastEntry.getKey();
    final LookupPaintScale paintScale = new LookupPaintScale(lower, upper, defaultColor);
    zValueToColor.forEach(paintScale::add);
    return paintScale;
  }

  private record HighlightResult(@Nullable KendrickSubsetDataset highlightedDataset,
                                 @Nullable KendrickFalseNegativeDetector falseNegativeDetector) {

  }
}
