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
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.RunOption;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidAnnotationLevel;
import io.github.mzmine.modules.visualization.dash_lipidqc.LipidAnnotationQCDashboardModel;
import io.github.mzmine.modules.visualization.dash_lipidqc.LipidQcAnnotationSelectionUtils;
import io.github.mzmine.modules.visualization.dash_lipidqc.kendrick.KendrickFalseNegativeCandidate;
import io.github.mzmine.modules.visualization.dash_lipidqc.kendrick.KendrickFalseNegativeDetector;
import io.github.mzmine.modules.visualization.dash_lipidqc.kendrick.KendrickFalsePositiveUtils;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.Range;
import org.jfree.data.general.DatasetUtils;
import org.jfree.data.xy.XYDataset;

/**
 * Builds immutable chart specs from neutral retention computation payloads.
 */
final class RetentionChartSpecFactory {

  private static final @NotNull Color SELECTED_POINT_COLOR = ConfigService.getDefaultColorPalette()
      .getPositiveColorAWT();

  private final @NotNull LipidAnnotationQCDashboardModel model;

  RetentionChartSpecFactory(final @NotNull LipidAnnotationQCDashboardModel model) {
    this.model = model;
  }

  @Nullable RetentionChartSpec build(final @NotNull RetentionComputationPayload payload) {
    return switch (payload) {
      case EcnRetentionPayload ecnPayload -> buildEcnSpec(ecnPayload);
      case DbeRetentionPayload dbePayload -> buildDbeSpec(dbePayload);
      case CombinedRetentionPayload combinedPayload -> buildCombinedSpec(combinedPayload);
      case TotalLipidClassRetentionPayload totalPayload -> buildTotalLipidClassSpec(totalPayload);
    };
  }

  private @Nullable RetentionChartSpec buildEcnSpec(final @NotNull EcnRetentionPayload payload) {
    final List<RetentionPointRef> primaryPoints = createPrimaryPointRefs(payload.series());
    if (primaryPoints.isEmpty()) {
      return null;
    }

    final List<RetentionDatasetSpec> datasets = new ArrayList<>();
    final Color seriesColor = ecnTrendDatasetColor();
    final ColoredXYDataset primaryDataset = createDataset(payload.series().seriesKey(), seriesColor,
        primaryPoints);
    datasets.add(new RetentionDatasetSpec(primaryDataset,
        createPointRenderer(primaryDataset, seriesColor,
            new Ellipse2D.Double(-3.2d, -3.2d, 6.4d, 6.4d), false), 0,
        RetentionDatasetRole.PRIMARY_POINTS));

    final double[] regression = calculateLinearRegression(primaryDataset);
    if (hasValidRegression(primaryDataset, regression)) {
      datasets.add(new RetentionDatasetSpec(createDataset("Regression", seriesColor,
          createRegressionPoints(regression, primaryDataset)),
          createRegressionRenderer(seriesColor, new BasicStroke(1.6f), false), 0,
          RetentionDatasetRole.REGRESSION));
    }

    final @Nullable SelectionQualityFlag selectionQualityFlag =
        model.getRow() == null ? null : determineSelectionQualityFlag(model.getRow());
    final List<RetentionPointRef> falsePositivePoints = createFalsePositiveOverlayPoints(
        primaryPoints, false);
    if (!falsePositivePoints.isEmpty()) {
      datasets.add(new RetentionDatasetSpec(
          createDataset("Potential false positives", falsePositiveColor(), falsePositivePoints),
          createOutlinedOverlayRenderer(falsePositiveColor(),
              new Ellipse2D.Double(-6d, -6d, 12d, 12d), null), 0,
          RetentionDatasetRole.FALSE_POSITIVE));
    }

    final int selectedCarbons = LipidQcAnnotationSelectionUtils.extractCarbons(
        payload.selectedMatch().getLipidAnnotation());
    addSelectedAndFalseNegativeDatasets(datasets, 0, model.getRow(), selectedCarbons,
        selectionQualityFlag);

    final AxisBounds domainBounds = calculateDomainBounds(datasets);
    final AxisBounds rangeBounds = calculateRangeBounds(datasets, 0);
    final String title =
        "Retention time analysis: " + payload.selectedClass().getAbbr() + " ECN DBE="
            + payload.dbe() + " (R2 " + ConfigService.getConfiguration().getScoreFormat()
            .format(regression[2]) + ", n=" + payload.matchCount() + ")" + formatQualityIndicator(
            falsePositivePoints.size(), selectionQualityFlag);
    return new RetentionChartSpec(payload.mode(), title,
        new RetentionAxisSpec(0, "Retention time", domainBounds.min(), domainBounds.max(), true),
        List.of(new RetentionAxisSpec(0, "Number of carbons", rangeBounds.min(), rangeBounds.max(),
            false)), datasets, null, false, false);
  }

  private @Nullable RetentionChartSpec buildDbeSpec(final @NotNull DbeRetentionPayload payload) {
    final List<RetentionPointRef> primaryPoints = createPrimaryPointRefs(payload.series());
    if (primaryPoints.isEmpty()) {
      return null;
    }

    final List<RetentionDatasetSpec> datasets = new ArrayList<>();
    final Color seriesColor = dbeTrendDatasetColor();
    final ColoredXYDataset primaryDataset = createDataset(payload.series().seriesKey(), seriesColor,
        primaryPoints);
    datasets.add(new RetentionDatasetSpec(primaryDataset,
        createPointRenderer(primaryDataset, seriesColor, new Ellipse2D.Double(-3d, -3d, 6d, 6d),
            false), 0, RetentionDatasetRole.PRIMARY_POINTS));

    final double[] regression = calculateLinearRegression(primaryDataset);
    if (hasValidRegression(primaryDataset, regression)) {
      datasets.add(new RetentionDatasetSpec(createDataset("Regression", seriesColor,
          createRegressionPoints(regression, primaryDataset)),
          createRegressionRenderer(seriesColor, new BasicStroke(1.6f), false), 0,
          RetentionDatasetRole.REGRESSION));
    }

    final @Nullable SelectionQualityFlag selectionQualityFlag =
        model.getRow() == null ? null : determineSelectionQualityFlag(model.getRow());
    final List<RetentionPointRef> falsePositivePoints = createFalsePositiveOverlayPoints(
        primaryPoints, false);
    if (!falsePositivePoints.isEmpty()) {
      datasets.add(new RetentionDatasetSpec(
          createDataset("Potential false positives", falsePositiveColor(), falsePositivePoints),
          createOutlinedOverlayRenderer(falsePositiveColor(),
              new Ellipse2D.Double(-6d, -6d, 12d, 12d), null), 0,
          RetentionDatasetRole.FALSE_POSITIVE));
    }

    addSelectedAndFalseNegativeDatasets(datasets, 0, model.getRow(), payload.selectedDbe(),
        selectionQualityFlag);

    final AxisBounds domainBounds = calculateDomainBounds(datasets);
    final AxisBounds rangeBounds = calculateRangeBounds(datasets, 0);
    final String title = "Retention time analysis: " + payload.selectedClass().getAbbr() + " DBE C="
        + payload.carbons() + " (R2 " + ConfigService.getConfiguration().getScoreFormat()
        .format(regression[2]) + ", n=" + payload.series().points().size() + ")"
        + formatQualityIndicator(falsePositivePoints.size(), selectionQualityFlag);
    return new RetentionChartSpec(payload.mode(), title,
        new RetentionAxisSpec(0, "Retention time", domainBounds.min(), domainBounds.max(), true),
        List.of(new RetentionAxisSpec(0, "Number of DBEs", rangeBounds.min(), rangeBounds.max(),
            false)), datasets, null, false, false);
  }

  private @Nullable RetentionChartSpec buildCombinedSpec(
      final @NotNull CombinedRetentionPayload payload) {
    final List<RetentionDatasetSpec> datasets = new ArrayList<>();
    final List<RetentionAxisSpec> rangeAxes = new ArrayList<>();
    final @Nullable SelectionQualityFlag selectionQualityFlag =
        model.getRow() == null ? null : determineSelectionQualityFlag(model.getRow());

    final @Nullable RetentionSeriesData carbonSeries = payload.carbonsSeries();
    final @Nullable RetentionSeriesData dbeSeries = payload.dbeSeries();
    final boolean hasCarbon = carbonSeries != null && !carbonSeries.points().isEmpty();
    final boolean hasDbe = dbeSeries != null && !dbeSeries.points().isEmpty();
    if (!hasCarbon && !hasDbe) {
      return null;
    }

    final int carbonAxisIndex = hasCarbon ? 0 : -1;
    final int dbeAxisIndex = hasDbe ? (hasCarbon ? 1 : 0) : -1;

    int falsePositiveCount = 0;
    if (hasCarbon) {
      final List<RetentionPointRef> primaryPoints = createPrimaryPointRefs(carbonSeries);
      final ColoredXYDataset primaryDataset = createDataset(carbonSeries.seriesKey(),
          ecnTrendDatasetColor(), primaryPoints);
      datasets.add(new RetentionDatasetSpec(primaryDataset,
          createPointRenderer(primaryDataset, ecnTrendDatasetColor(),
              new Ellipse2D.Double(-3.2d, -3.2d, 6.4d, 6.4d), false), carbonAxisIndex,
          RetentionDatasetRole.PRIMARY_POINTS));
      final double[] regression = calculateLinearRegression(primaryDataset);
      if (hasValidRegression(primaryDataset, regression)) {
        datasets.add(new RetentionDatasetSpec(
            createDataset("Carbon regression", ecnTrendDatasetColor(),
                createRegressionPoints(regression, primaryDataset)),
            createRegressionRenderer(ecnTrendDatasetColor(), new BasicStroke(1.6f), false),
            carbonAxisIndex, RetentionDatasetRole.REGRESSION));
      }

      final List<RetentionPointRef> falsePositivePoints = createFalsePositiveOverlayPoints(
          primaryPoints, false);
      falsePositiveCount += falsePositivePoints.size();
      if (!falsePositivePoints.isEmpty()) {
        datasets.add(new RetentionDatasetSpec(
            createDataset("Carbon false positives", falsePositiveColor(), falsePositivePoints),
            createOutlinedOverlayRenderer(falsePositiveColor(),
                new Ellipse2D.Double(-6d, -6d, 12d, 12d), null), carbonAxisIndex,
            RetentionDatasetRole.FALSE_POSITIVE));
      }
    }
    if (hasDbe) {
      final List<RetentionPointRef> primaryPoints = createPrimaryPointRefs(dbeSeries);
      final ColoredXYDataset primaryDataset = createDataset(dbeSeries.seriesKey(),
          dbeTrendDatasetColor(), primaryPoints);
      datasets.add(new RetentionDatasetSpec(primaryDataset,
          createPointRenderer(primaryDataset, dbeTrendDatasetColor(),
              new Rectangle2D.Double(-3d, -3d, 6d, 6d), false), dbeAxisIndex,
          RetentionDatasetRole.PRIMARY_POINTS));
      final double[] regression = calculateLinearRegression(primaryDataset);
      if (hasValidRegression(primaryDataset, regression)) {
        datasets.add(new RetentionDatasetSpec(
            createDataset("DBE regression", dbeTrendDatasetColor(),
                createRegressionPoints(regression, primaryDataset)),
            createRegressionRenderer(dbeTrendDatasetColor(), new BasicStroke(1.6f), false),
            dbeAxisIndex, RetentionDatasetRole.REGRESSION));
      }

      final List<RetentionPointRef> falsePositivePoints = createFalsePositiveOverlayPoints(
          primaryPoints, false);
      falsePositiveCount += falsePositivePoints.size();
      if (!falsePositivePoints.isEmpty()) {
        datasets.add(new RetentionDatasetSpec(
            createDataset("DBE false positives", falsePositiveColor(), falsePositivePoints),
            createOutlinedOverlayRenderer(falsePositiveColor(),
                new Ellipse2D.Double(-6d, -6d, 12d, 12d), null), dbeAxisIndex,
            RetentionDatasetRole.FALSE_POSITIVE));
      }
    }

    final @Nullable FeatureListRow selectedRow = model.getRow();
    final @Nullable Float selectedRt = selectedRow == null ? null : selectedRow.getAverageRT();
    if (selectedRt != null && hasCarbon) {
      addSelectedPointDataset(datasets, carbonAxisIndex, selectedRt.doubleValue(),
          payload.selectedCarbons(), "Selected carbon");
      if (selectionQualityFlag == SelectionQualityFlag.POTENTIAL_FALSE_NEGATIVE) {
        addFalseNegativeDataset(datasets, carbonAxisIndex, selectedRt.doubleValue(),
            payload.selectedCarbons());
      }
    }
    if (selectedRt != null && hasDbe) {
      addSelectedPointDataset(datasets, dbeAxisIndex, selectedRt.doubleValue(),
          payload.selectedDbe(), "Selected DBE");
      if (selectionQualityFlag == SelectionQualityFlag.POTENTIAL_FALSE_NEGATIVE) {
        addFalseNegativeDataset(datasets, dbeAxisIndex, selectedRt.doubleValue(),
            payload.selectedDbe());
      }
    }

    if (hasCarbon) {
      final AxisBounds carbonBounds = calculateRangeBounds(datasets, carbonAxisIndex);
      rangeAxes.add(new RetentionAxisSpec(carbonAxisIndex, "Number of carbons", carbonBounds.min(),
          carbonBounds.max(), false));
    }
    if (hasDbe) {
      final AxisBounds dbeBounds = calculateRangeBounds(datasets, dbeAxisIndex);
      rangeAxes.add(
          new RetentionAxisSpec(dbeAxisIndex, "Number of DBEs", dbeBounds.min(), dbeBounds.max(),
              false));
    }

    final @Nullable RetentionAxisSyncSpec axisSyncSpec =
        hasCarbon && hasDbe && selectedRt != null ? new RetentionAxisSyncSpec(carbonAxisIndex,
            dbeAxisIndex, payload.selectedCarbons(), payload.selectedDbe()) : null;
    final AxisBounds domainBounds = calculateDomainBounds(datasets);
    final String title = "Retention time analysis: " + payload.selectedClass().getAbbr() + " "
        + payload.selectedCarbons() + ":" + payload.selectedDbe() + formatQualityIndicator(
        falsePositiveCount, selectionQualityFlag);
    return new RetentionChartSpec(payload.mode(), title,
        new RetentionAxisSpec(0, "Retention time", domainBounds.min(), domainBounds.max(), true),
        rangeAxes, datasets, axisSyncSpec, false, true);
  }

  private @Nullable RetentionChartSpec buildTotalLipidClassSpec(
      final @NotNull TotalLipidClassRetentionPayload payload) {
    if (payload.series().isEmpty()) {
      return null;
    }

    final List<RetentionDatasetSpec> datasets = new ArrayList<>();
    final List<Color> trendColors = createDistinctTrendColors(payload.series().size());
    int trendCount = 0;
    for (int i = 0; i < payload.series().size(); i++) {
      final RetentionSeriesData series = payload.series().get(i);
      final Color color = trendColors.get(i);
      final List<RetentionPointRef> primaryPoints = createPrimaryPointRefs(series);
      if (primaryPoints.isEmpty()) {
        continue;
      }
      final ColoredXYDataset primaryDataset = createDataset(series.seriesKey(), color,
          primaryPoints);
      datasets.add(new RetentionDatasetSpec(primaryDataset,
          createPointRenderer(primaryDataset, color, new Ellipse2D.Double(-3d, -3d, 6d, 6d), false),
          0, RetentionDatasetRole.PRIMARY_POINTS));

      final double[] regression = calculateLinearRegression(primaryDataset);
      if (hasValidRegression(primaryDataset, regression)) {
        datasets.add(new RetentionDatasetSpec(createDataset(series.seriesKey(), color,
            createRegressionPoints(regression, primaryDataset)),
            createRegressionRenderer(color, groupedRegressionStroke(), true), 0,
            RetentionDatasetRole.REGRESSION));
        trendCount++;
      }
    }
    if (datasets.isEmpty()) {
      return null;
    }

    final @Nullable SelectionQualityFlag selectionQualityFlag =
        model.getRow() == null ? null : determineSelectionQualityFlag(model.getRow());
    final List<RetentionPointRef> falsePositivePoints = new ArrayList<>();
    final Set<String> addedCoordinates = new HashSet<>();
    for (final RetentionSeriesData series : payload.series()) {
      for (final RetentionPointRef point : createPrimaryPointRefs(series)) {
        if (point.row() == null || !isPotentialFalsePositive(point.row())) {
          continue;
        }
        final String coordinateKey = point.x() + ":" + point.y();
        if (!addedCoordinates.add(coordinateKey)) {
          continue;
        }
        falsePositivePoints.add(
            new RetentionPointRef(null, null, point.x(), point.y(), null, null));
      }
    }
    if (!falsePositivePoints.isEmpty()) {
      datasets.add(new RetentionDatasetSpec(
          createDataset("Potential false positives", falsePositiveColor(), falsePositivePoints),
          createOutlinedOverlayRenderer(falsePositiveColor(),
              new Ellipse2D.Double(-6d, -6d, 12d, 12d), null), 0,
          RetentionDatasetRole.FALSE_POSITIVE));
    }

    if (model.getRow() != null) {
      final int selectedYValue = payload.mode() == RetentionTrendMode.ECN_CARBON_TREND
          ? LipidQcAnnotationSelectionUtils.extractCarbons(
          payload.selectedMatch().getLipidAnnotation())
          : LipidQcAnnotationSelectionUtils.extractDbe(
              payload.selectedMatch().getLipidAnnotation());
      addSelectedAndFalseNegativeDatasets(datasets, 0, model.getRow(), selectedYValue,
          selectionQualityFlag);
    }

    final AxisBounds domainBounds = calculateDomainBounds(datasets);
    final AxisBounds rangeBounds = calculateRangeBounds(datasets, 0);
    final boolean showEcnTrend = payload.mode() == RetentionTrendMode.ECN_CARBON_TREND;
    final String title =
        "Retention time analysis: " + payload.selectedClass().getAbbr() + " " + (showEcnTrend
            ? "ECN carbon trend" : "DBE trend") + " all class lipids (n="
            + payload.annotationCount() + ", " + (showEcnTrend ? "DBE lines=" : "carbon lines=")
            + trendCount + ")" + formatQualityIndicator(falsePositivePoints.size(),
            selectionQualityFlag);
    return new RetentionChartSpec(payload.mode(), title,
        new RetentionAxisSpec(0, "Retention time", domainBounds.min(), domainBounds.max(), true),
        List.of(new RetentionAxisSpec(0, showEcnTrend ? "Number of carbons" : "Number of DBEs",
            rangeBounds.min(), rangeBounds.max(), false)), datasets, null, trendCount > 0, false);
  }

  private void addSelectedAndFalseNegativeDatasets(
      final @NotNull List<RetentionDatasetSpec> datasets, final int rangeAxisIndex,
      final @Nullable FeatureListRow selectedRow, final double selectedYValue,
      final @Nullable SelectionQualityFlag selectionQualityFlag) {
    if (selectedRow == null || selectedRow.getAverageRT() == null || !Double.isFinite(
        selectedYValue)) {
      return;
    }
    final double selectedRt = selectedRow.getAverageRT();
    addSelectedPointDataset(datasets, rangeAxisIndex, selectedRt, selectedYValue, "Selected lipid");
    if (selectionQualityFlag == SelectionQualityFlag.POTENTIAL_FALSE_NEGATIVE) {
      addFalseNegativeDataset(datasets, rangeAxisIndex, selectedRt, selectedYValue);
    }
  }

  private void addSelectedPointDataset(final @NotNull List<RetentionDatasetSpec> datasets,
      final int rangeAxisIndex, final double xValue, final double yValue,
      final @NotNull String seriesKey) {
    datasets.add(new RetentionDatasetSpec(createDataset(seriesKey, SELECTED_POINT_COLOR,
        List.of(new RetentionPointRef(null, null, xValue, yValue, null, null))),
        createSelectedOverlayRenderer(SELECTED_POINT_COLOR,
            new Ellipse2D.Double(-5d, -5d, 10d, 10d)), rangeAxisIndex,
        RetentionDatasetRole.SELECTED_POINT));
  }

  private void addFalseNegativeDataset(final @NotNull List<RetentionDatasetSpec> datasets,
      final int rangeAxisIndex, final double xValue, final double yValue) {
    datasets.add(new RetentionDatasetSpec(
        createDataset("Potential false negative", falseNegativeColor(),
            List.of(new RetentionPointRef(null, null, xValue, yValue, null, null))),
        createOutlinedOverlayRenderer(falseNegativeColor(), falseNegativeMarkerShape(), "FN"),
        rangeAxisIndex, RetentionDatasetRole.FALSE_NEGATIVE));
  }

  private @NotNull List<RetentionPointRef> createPrimaryPointRefs(
      final @NotNull RetentionSeriesData series) {
    final LipidAnnotationLevel level = model.getPreferredLipidLevel();
    final List<RetentionPointRef> refs = new ArrayList<>(series.points().size());
    for (int i = 0; i < series.points().size(); i++) {
      final RetentionSeriesPoint point = series.points().get(i);
      refs.add(new RetentionPointRef(point.row(), point.match(), point.x(), point.y(),
          determinePointLabel(series.points(), i, level),
          point.match().getLipidAnnotation().getAnnotation(level)));
    }
    return refs;
  }

  private @Nullable String determinePointLabel(final @NotNull List<RetentionSeriesPoint> points,
      final int index, final @NotNull LipidAnnotationLevel level) {
    final RetentionSeriesPoint point = points.get(index);
    final double yValue = point.y();
    final double score = point.match().getMsMsScore() == null ? 0d : point.match().getMsMsScore();
    for (int i = 0; i < points.size(); i++) {
      if (i == index || Double.compare(points.get(i).y(), yValue) != 0) {
        continue;
      }
      final MatchedLipid otherMatch = points.get(i).match();
      final double otherScore = otherMatch.getMsMsScore() == null ? 0d : otherMatch.getMsMsScore();
      if (score < otherScore) {
        return null;
      }
    }
    return point.match().getLipidAnnotation().getAnnotation(level);
  }

  private @NotNull List<RetentionPointRef> createRegressionPoints(
      final @NotNull double[] regression, final @NotNull XYDataset dataset) {
    final double minX = minDatasetX(dataset);
    final double maxX = maxDatasetX(dataset);
    final double minY = regression[0] * minX + regression[1];
    final double maxY = regression[0] * maxX + regression[1];
    return List.of(new RetentionPointRef(null, null, minX, minY, null, null),
        new RetentionPointRef(null, null, maxX, maxY, null, null));
  }

  private @NotNull List<RetentionPointRef> createFalsePositiveOverlayPoints(
      final @NotNull List<RetentionPointRef> primaryPoints, final boolean deduplicateCoordinates) {
    final List<RetentionPointRef> points = new ArrayList<>();
    final Set<String> addedCoordinates = deduplicateCoordinates ? new HashSet<>() : null;
    for (final RetentionPointRef point : primaryPoints) {
      if (point.row() == null || !isPotentialFalsePositive(point.row())) {
        continue;
      }
      if (addedCoordinates != null) {
        final String coordinateKey = point.x() + ":" + point.y();
        if (!addedCoordinates.add(coordinateKey)) {
          continue;
        }
      }
      points.add(new RetentionPointRef(null, null, point.x(), point.y(), null, null));
    }
    return points;
  }

  private @NotNull ColoredXYDataset createDataset(final @NotNull String seriesKey,
      final @NotNull Color color, final @NotNull List<RetentionPointRef> points) {
    return new ColoredXYDataset(new RetentionPointProvider(seriesKey, color, points),
        RunOption.THIS_THREAD);
  }

  private @NotNull XYLineAndShapeRenderer createPointRenderer(
      final @NotNull ColoredXYDataset dataset, final @NotNull Paint seriesPaint,
      final @NotNull Shape seriesShape, final boolean showLegend) {
    final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(false, true);
    renderer.setSeriesPaint(0, seriesPaint);
    renderer.setSeriesShape(0, seriesShape);
    renderer.setSeriesVisibleInLegend(0, showLegend);
    renderer.setDefaultItemLabelGenerator((xyDataset, series, item) -> dataset.getLabel(item));
    renderer.setDefaultItemLabelsVisible(true);
    renderer.setDefaultItemLabelPaint(retentionLabelPaint());
    renderer.setDefaultToolTipGenerator((xyDataset, series, item) -> dataset.getToolTipText(item));
    return renderer;
  }

  private @NotNull XYLineAndShapeRenderer createRegressionRenderer(final @NotNull Paint linePaint,
      final @NotNull BasicStroke stroke, final boolean showLegend) {
    final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
    renderer.setSeriesPaint(0, linePaint);
    renderer.setSeriesStroke(0, stroke);
    renderer.setSeriesVisibleInLegend(0, showLegend);
    return renderer;
  }

  private @NotNull XYLineAndShapeRenderer createSelectedOverlayRenderer(
      final @NotNull Paint strokePaint, final @NotNull Shape marker) {
    final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(false, true);
    renderer.setSeriesPaint(0, strokePaint);
    renderer.setSeriesStroke(0, new BasicStroke(2f));
    renderer.setSeriesShape(0, marker);
    renderer.setDefaultShapesFilled(true);
    renderer.setUseOutlinePaint(true);
    renderer.setSeriesOutlinePaint(0,
        ConfigService.getConfiguration().isDarkMode() ? Color.WHITE : Color.BLACK);
    renderer.setSeriesOutlineStroke(0, new BasicStroke(1.1f));
    renderer.setSeriesVisibleInLegend(0, false);
    return renderer;
  }

  private @NotNull XYLineAndShapeRenderer createOutlinedOverlayRenderer(final @NotNull Color color,
      final @NotNull Shape shape, final @Nullable String label) {
    final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(false, true);
    renderer.setSeriesPaint(0, color);
    renderer.setSeriesStroke(0, new BasicStroke(2.2f));
    renderer.setSeriesShape(0, shape);
    renderer.setDefaultShapesFilled(false);
    renderer.setUseOutlinePaint(true);
    renderer.setSeriesOutlinePaint(0, color);
    renderer.setSeriesOutlineStroke(0, new BasicStroke(2.2f));
    renderer.setSeriesVisibleInLegend(0, false);
    if (label != null) {
      renderer.setDefaultItemLabelGenerator((xyDataset, series, item) -> label);
      renderer.setDefaultItemLabelsVisible(true);
      renderer.setDefaultItemLabelPaint(color);
      renderer.setDefaultPositiveItemLabelPosition(
          new ItemLabelPosition(ItemLabelAnchor.OUTSIDE12, TextAnchor.BOTTOM_CENTER));
    }
    return renderer;
  }

  private @Nullable SelectionQualityFlag determineSelectionQualityFlag(
      final @Nullable FeatureListRow row) {
    if (row == null) {
      return SelectionQualityFlag.NONE;
    }
    if (!(row.getFeatureList() instanceof ModularFeatureList modularFeatureList)) {
      return SelectionQualityFlag.NONE;
    }
    if (row.getLipidMatches().isEmpty()) {
      final @Nullable KendrickFalseNegativeCandidate candidate = new KendrickFalseNegativeDetector(
          modularFeatureList).detectCandidate(row);
      return candidate != null ? SelectionQualityFlag.POTENTIAL_FALSE_NEGATIVE
          : SelectionQualityFlag.NONE;
    }
    final @Nullable String falsePositiveReason = KendrickFalsePositiveUtils.potentialFalsePositiveReason(
        modularFeatureList, row, true);
    return falsePositiveReason != null ? SelectionQualityFlag.POTENTIAL_FALSE_POSITIVE
        : SelectionQualityFlag.NONE;
  }

  private boolean isPotentialFalsePositive(final @NotNull FeatureListRow row) {
    if (!(row.getFeatureList() instanceof ModularFeatureList modularFeatureList)) {
      return false;
    }
    return KendrickFalsePositiveUtils.potentialFalsePositiveReason(modularFeatureList, row, true)
        != null;
  }

  private @NotNull String formatQualityIndicator(final int falsePositiveCount,
      final @Nullable SelectionQualityFlag selectionQualityFlag) {
    final StringBuilder suffixBuilder = new StringBuilder();
    if (falsePositiveCount > 0) {
      suffixBuilder.append(" | FP outlined: ").append(falsePositiveCount);
    }
    if (selectionQualityFlag == SelectionQualityFlag.POTENTIAL_FALSE_NEGATIVE) {
      suffixBuilder.append(" | FN outlined");
    } else if (selectionQualityFlag == SelectionQualityFlag.POTENTIAL_FALSE_POSITIVE
        && falsePositiveCount == 0) {
      suffixBuilder.append(" | FP outlined");
    }
    return suffixBuilder.toString();
  }

  private @NotNull AxisBounds calculateDomainBounds(
      final @NotNull List<RetentionDatasetSpec> datasets) {
    double min = Double.POSITIVE_INFINITY;
    double max = Double.NEGATIVE_INFINITY;
    for (final RetentionDatasetSpec datasetSpec : datasets) {
      min = Math.min(min, minDatasetX(datasetSpec.dataset()));
      max = Math.max(max, maxDatasetX(datasetSpec.dataset()));
    }
    return sanitizeBounds(min, max);
  }

  private @NotNull AxisBounds calculateRangeBounds(
      final @NotNull List<RetentionDatasetSpec> datasets, final int axisIndex) {
    double min = Double.POSITIVE_INFINITY;
    double max = Double.NEGATIVE_INFINITY;
    for (final RetentionDatasetSpec datasetSpec : datasets) {
      if (datasetSpec.rangeAxisIndex() != axisIndex) {
        continue;
      }
      min = Math.min(min, minDatasetY(datasetSpec.dataset()));
      max = Math.max(max, maxDatasetY(datasetSpec.dataset()));
    }
    return sanitizeBounds(min, max);
  }

  private @NotNull AxisBounds sanitizeBounds(final double min, final double max) {
    if (!Double.isFinite(min) || !Double.isFinite(max)) {
      return new AxisBounds(0d, 1d);
    }
    return new AxisBounds(min, max);
  }

  private @NotNull double[] calculateLinearRegression(final @NotNull XYDataset dataset) {
    final int itemCount = dataset.getItemCount(0);
    if (itemCount < 2) {
      return new double[]{Double.NaN, Double.NaN, Double.NaN};
    }
    double sumX = 0d;
    double sumY = 0d;
    double sumXX = 0d;
    double sumXY = 0d;
    for (int i = 0; i < itemCount; i++) {
      final double x = dataset.getXValue(0, i);
      final double y = dataset.getYValue(0, i);
      sumX += x;
      sumY += y;
      sumXX += x * x;
      sumXY += x * y;
    }
    final double denominator = itemCount * sumXX - sumX * sumX;
    if (Math.abs(denominator) < 1e-10d) {
      return new double[]{Double.NaN, Double.NaN, Double.NaN};
    }
    final double slope = (itemCount * sumXY - sumX * sumY) / denominator;
    final double intercept = (sumY - slope * sumX) / itemCount;
    final double meanY = sumY / itemCount;
    double ssr = 0d;
    double sse = 0d;
    for (int i = 0; i < itemCount; i++) {
      final double x = dataset.getXValue(0, i);
      final double y = dataset.getYValue(0, i);
      final double predicted = slope * x + intercept;
      ssr += (predicted - meanY) * (predicted - meanY);
      sse += (y - predicted) * (y - predicted);
    }
    final double r2 = (ssr + sse) > 0d ? ssr / (ssr + sse) : Double.NaN;
    return new double[]{slope, intercept, r2};
  }

  private boolean hasValidRegression(final @NotNull XYDataset dataset,
      final @NotNull double[] regression) {
    return dataset.getItemCount(0) > 1 && Double.isFinite(regression[0]) && Double.isFinite(
        regression[1]);
  }

  private double minDatasetY(final @NotNull XYDataset dataset) {
    final Range range = DatasetUtils.findRangeBounds(dataset, false);
    return range == null ? Double.POSITIVE_INFINITY : range.getLowerBound();
  }

  private double maxDatasetY(final @NotNull XYDataset dataset) {
    final Range range = DatasetUtils.findRangeBounds(dataset, false);
    return range == null ? Double.NEGATIVE_INFINITY : range.getUpperBound();
  }

  private double minDatasetX(final @NotNull XYDataset dataset) {
    final Range range = DatasetUtils.findDomainBounds(dataset, false);
    return range == null ? Double.POSITIVE_INFINITY : range.getLowerBound();
  }

  private double maxDatasetX(final @NotNull XYDataset dataset) {
    final Range range = DatasetUtils.findDomainBounds(dataset, false);
    return range == null ? Double.NEGATIVE_INFINITY : range.getUpperBound();
  }

  private @NotNull Shape falseNegativeMarkerShape() {
    final Path2D.Double diamond = new Path2D.Double();
    diamond.moveTo(0d, -6.5d);
    diamond.lineTo(6.5d, 0d);
    diamond.lineTo(0d, 6.5d);
    diamond.lineTo(-6.5d, 0d);
    diamond.closePath();
    return diamond;
  }

  private @NotNull List<Color> createDistinctTrendColors(final int colorCount) {
    final List<Color> colors = new ArrayList<>(colorCount);
    final Set<Integer> usedColorCodes = new HashSet<>();
    final var palette = ConfigService.getConfiguration().getDefaultColorPalette();
    final int paletteSize = Math.max(0, palette.size());
    int colorIndex = 0;
    while (colors.size() < colorCount) {
      int overflowIndex = Math.max(0, colorIndex - paletteSize);
      Color color = colorIndex < paletteSize ? palette.getAWT(colorIndex)
          : generateDistinctOverflowColor(overflowIndex);
      while (usedColorCodes.contains(color.getRGB())) {
        color = generateDistinctOverflowColor(++overflowIndex);
      }
      usedColorCodes.add(color.getRGB());
      colors.add(color);
      colorIndex++;
    }
    return colors;
  }

  private @NotNull Color generateDistinctOverflowColor(final int index) {
    final double hue = (index * 0.6180339887498949d) % 1d;
    return Color.getHSBColor((float) hue, 0.72f, 0.88f);
  }

  private @NotNull BasicStroke groupedRegressionStroke() {
    return new BasicStroke(1.8f);
  }

  private @NotNull Paint retentionLabelPaint() {
    return ConfigService.getConfiguration().isDarkMode() ? new Color(230, 230, 230)
        : new Color(35, 35, 35);
  }

  private @NotNull Color ecnTrendDatasetColor() {
    return ConfigService.getDefaultColorPalette().getPositiveColorAWT();
  }

  private @NotNull Color dbeTrendDatasetColor() {
    return ConfigService.getDefaultColorPalette().getNeutralColorAWT();
  }

  private @NotNull Color falsePositiveColor() {
    return ConfigService.getDefaultColorPalette().getNegativeColorAWT();
  }

  private @NotNull Color falseNegativeColor() {
    return ConfigService.getDefaultColorPalette().getPositiveColorAWT();
  }

  private record AxisBounds(double min, double max) {

  }
}
