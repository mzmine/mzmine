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
import io.github.mzmine.gui.chartbasics.chartutils.ColoredBubbleDatasetRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.providers.XYItemObjectProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.AlphaBubbleDatasetRenderer;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidAnnotationLevel;
import io.github.mzmine.modules.visualization.dash_lipidqc.DashboardComputationPane;
import io.github.mzmine.modules.visualization.dash_lipidqc.DashboardFilterState;
import io.github.mzmine.modules.visualization.dash_lipidqc.LipidAnnotationQCDashboardModel;
import io.github.mzmine.modules.visualization.dash_lipidqc.LipidQcAnnotationSelectionUtils;
import io.github.mzmine.modules.visualization.kendrickmassplot.KendrickMassPlotChart;
import io.github.mzmine.modules.visualization.kendrickmassplot.KendrickMassPlotParameters;
import io.github.mzmine.modules.visualization.kendrickmassplot.KendrickMassPlotXYZDataset;
import io.github.mzmine.modules.visualization.kendrickmassplot.KendrickPlotDataTypes;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureTableFXUtil;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.awt.geom.Ellipse2D;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Accordion;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TitledPane;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.fx.interaction.ChartMouseEventFX;
import org.jfree.chart.fx.interaction.ChartMouseListenerFX;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.LookupPaintScale;
import org.jfree.chart.renderer.PaintScale;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.chart.title.Title;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * Dashboard panel displaying a Kendrick mass plot with optional QC review overlays that highlight
 * potential false-positive or false-negative lipid annotations.
 */
public class KendrickPane extends DashboardComputationPane {

  private static final double KMD_MIN = -1d;
  private static final double KMD_MAX = 1d;
  private static final int FILTERED_OUT_DATASET_INDEX = 0;
  private static final int FILTERED_IN_DATASET_INDEX = 1;
  private static final int TREND_OVERLAY_DATASET_INDEX = 2;
  private static final int OUTLIER_OVERLAY_DATASET_INDEX = 3;
  private static final int SELECTION_OVERLAY_DATASET_INDEX = 4;

  private final @NotNull LipidAnnotationQCDashboardModel model;
  private final @NotNull DashboardFilterState filterState;
  private final @NotNull ComboBox<KendrickReviewMode> reviewModeSelector = new ComboBox<>(
      FXCollections.observableArrayList(KendrickReviewMode.values()));
  private @Nullable KendrickMassPlotXYZDataset baseDataset;
  private @Nullable KendrickMassPlotChart chart;
  private @Nullable ColoredBubbleDatasetRenderer colorRenderer;
  private @Nullable ColoredBubbleDatasetRenderer filteredOutRenderer;
  private @Nullable KendrickFalseNegativeDetector falseNegativeDetector;
  private @NotNull KendrickReviewMode reviewMode = KendrickReviewMode.NONE;
  private @Nullable Consumer<KendrickReviewMode> onReviewModeChanged;
  private @Nullable Consumer<List<FeatureListRow>> onOutlierRowsChanged;
  private long filterRequestId;

  public KendrickPane(final @NotNull LipidAnnotationQCDashboardModel model,
      final @NotNull DashboardFilterState filterState) {
    super("Select a feature list to build Kendrick plot.");
    this.model = model;
    this.filterState = filterState;
    model.featureListProperty().subscribe(this::onFeatureListChanged);
    model.rowProperty().subscribe(_ -> updateSelectionOverlay());
    model.retentionTimeAnalysisEnabledProperty().subscribe(_ -> requestUpdate());
    reviewModeSelector.getSelectionModel().select(reviewMode);
    reviewModeSelector.valueProperty().addListener((_, _, mode) -> {
      if (mode == null || mode == reviewMode) {
        return;
      }
      reviewMode = mode;
      requestUpdate();
      if (onReviewModeChanged != null) {
        onReviewModeChanged.accept(mode);
      }
    });
    final HBox modeControls = new HBox(6, FxLabels.newLabel("Mode:"), reviewModeSelector);
    modeControls.setAlignment(Pos.CENTER_LEFT);
    final TitledPane modePane = new TitledPane("Annotation review mode", modeControls);
    modePane.setCollapsible(true);
    final Accordion modeAccordion = new Accordion(modePane);
    modeAccordion.setExpandedPane(null);
    setBottom(modeAccordion);
  }

  public void setOnReviewModeChanged(
      final @Nullable Consumer<KendrickReviewMode> onReviewModeChanged) {
    this.onReviewModeChanged = onReviewModeChanged;
    if (onReviewModeChanged != null) {
      onReviewModeChanged.accept(reviewMode);
    }
  }

  public void setOnOutlierRowsChanged(
      final @Nullable Consumer<List<FeatureListRow>> onOutlierRowsChanged) {
    this.onOutlierRowsChanged = onOutlierRowsChanged;
  }

  public @NotNull KendrickReviewMode getReviewMode() {
    return reviewMode;
  }

  private void onFeatureListChanged(final @NotNull ModularFeatureList featureList) {
    if (baseDataset != null && baseDataset.getStatus() == TaskStatus.PROCESSING) {
      baseDataset.cancel();
    }
    discardChart();
    if (featureList.getNumberOfRows() == 0) {
      showPlaceholder("Feature list has no rows.");
      return;
    }

    showPlaceholder("Loading Kendrick mass plot...");
    final KendrickMassPlotParameters params = (KendrickMassPlotParameters) new KendrickMassPlotParameters().cloneParameterSet();
    params.setParameter(KendrickMassPlotParameters.featureList,
        new FeatureListsSelection(featureList));
    params.setParameter(KendrickMassPlotParameters.xAxisValues, KendrickPlotDataTypes.MZ);
    params.setParameter(KendrickMassPlotParameters.yAxisValues,
        KendrickPlotDataTypes.KENDRICK_MASS_DEFECT);
    params.setParameter(KendrickMassPlotParameters.yAxisCustomKendrickMassBase, "CH2");
    params.setParameter(KendrickMassPlotParameters.colorScaleValues,
        KendrickPlotDataTypes.RETENTION_TIME);
    params.setParameter(KendrickMassPlotParameters.bubbleSizeValues,
        KendrickPlotDataTypes.INTENSITY);

    final KendrickMassPlotXYZDataset dataset = new KendrickMassPlotXYZDataset(params, 1, 1);
    baseDataset = dataset;
    dataset.addTaskStatusListener((_, newStatus, _) -> {
      if (dataset != baseDataset) {
        return;
      }
      if (newStatus == TaskStatus.FINISHED) {
        Platform.runLater(() -> buildChart(dataset));
      } else if (newStatus == TaskStatus.ERROR || newStatus == TaskStatus.CANCELED) {
        Platform.runLater(() -> showPlaceholder("Kendrick mass plot could not be created."));
      }
    });
  }

  public void requestUpdate() {
    if (chart == null || baseDataset == null || colorRenderer == null
        || filteredOutRenderer == null) {
      return;
    }
    final boolean filterActive = !filterState.getBarSelectedRowIds().isEmpty();
    final Set<Integer> visibleIds = filterActive ? Set.copyOf(filterState.getBarSelectedRowIds())
        : Set.of();
    final boolean multiGroupSelection = filterState.getBarSelectedGroups().size() > 1;
    final long requestId = ++filterRequestId;
    scheduleUpdate(new KendrickFilterComputationTask(this, requestId,
        Objects.requireNonNull(baseDataset), model.getFeatureList(), visibleIds,
        filterState.getBarSelectedRowColors(), multiGroupSelection,
        model.isRetentionTimeAnalysisEnabled(), reviewMode));
  }

  void applyFilterComputationResult(final @NotNull KendrickFilterComputationResult result) {
    if (result.requestId() != filterRequestId || chart == null || baseDataset == null
        || colorRenderer == null || filteredOutRenderer == null
        || result.baseDataset() != baseDataset) {
      return;
    }

    falseNegativeDetector = result.falseNegativeDetector();
    final XYPlot plot = chart.getChart().getXYPlot();
    final @Nullable AxisRangeState domainAxisState =
        plot.getDomainAxis() instanceof NumberAxis domainAxis ? captureAxisState(domainAxis) : null;
    final @Nullable AxisRangeState rangeAxisState =
        plot.getRangeAxis() instanceof NumberAxis rangeAxis ? captureAxisState(rangeAxis) : null;
    colorRenderer.setPaintScale(result.filteredColorScale());
    if (result.showColorScaleLegend()) {
      updateColorScaleLegend(result.filteredColorScale());
    } else {
      removeColorScaleLegend();
    }

    if (result.filteredOutDataset() != null) {
      filteredOutRenderer.setPaintScale(result.grayScale());
      plot.setDataset(FILTERED_OUT_DATASET_INDEX, result.filteredOutDataset());
      plot.setRenderer(FILTERED_OUT_DATASET_INDEX, filteredOutRenderer);
      plot.setDataset(FILTERED_IN_DATASET_INDEX, result.inDataset());
      plot.setRenderer(FILTERED_IN_DATASET_INDEX, colorRenderer);
    } else {
      plot.setDataset(FILTERED_OUT_DATASET_INDEX, result.inDataset());
      plot.setRenderer(FILTERED_OUT_DATASET_INDEX, colorRenderer);
      plot.setDataset(FILTERED_IN_DATASET_INDEX, null);
      plot.setRenderer(FILTERED_IN_DATASET_INDEX, null);
    }

    updateOutlierOverlay(result.outlierDataset(), result.reviewMode());
    notifyOutlierRowsChanged(result.outlierDataset());
    updateSelectionOverlay();
    if (plot.getDomainAxis() instanceof NumberAxis domainAxis && domainAxisState != null) {
      restoreAxisState(domainAxis, domainAxisState);
    }
    if (plot.getRangeAxis() instanceof NumberAxis rangeAxis && rangeAxisState != null) {
      restoreAxisState(rangeAxis, rangeAxisState);
    }
    chart.getChart().fireChartChanged();
  }

  private void updateColorScaleLegend(final @NotNull PaintScale scale) {
    if (chart == null) {
      return;
    }
    final JFreeChart jChart = chart.getChart();
    removeColorScaleLegend();

    final XYPlot xyPlot = jChart.getXYPlot();
    final NumberAxis scaleAxis = new NumberAxis(null);
    scaleAxis.setRange(scale.getLowerBound(),
        Math.max(scale.getUpperBound(), scale.getLowerBound()));
    final Paint axisPaint = xyPlot.getDomainAxis().getAxisLinePaint();
    scaleAxis.setAxisLinePaint(axisPaint);
    scaleAxis.setTickMarkPaint(axisPaint);
    scaleAxis.setNumberFormatOverride(new DecimalFormat("0.#"));
    scaleAxis.setLabelFont(xyPlot.getDomainAxis().getLabelFont());
    scaleAxis.setLabelPaint(axisPaint);
    scaleAxis.setTickLabelFont(xyPlot.getDomainAxis().getTickLabelFont());
    scaleAxis.setTickLabelPaint(axisPaint);
    scaleAxis.setLabel("Retention time");

    final PaintScaleLegend legend = new PaintScaleLegend(scale, scaleAxis);
    legend.setPadding(5, 0, 5, 0);
    legend.setStripOutlineVisible(false);
    legend.setAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
    legend.setAxisOffset(5.0);
    legend.setSubdivisionCount(500);
    legend.setPosition(RectangleEdge.RIGHT);
    legend.setBackgroundPaint(new Color(0, 0, 0, 0));
    jChart.addSubtitle(legend);
  }

  private void removeColorScaleLegend() {
    if (chart == null) {
      return;
    }
    final JFreeChart jChart = chart.getChart();
    Title existingLegend = null;
    for (int i = 0; i < jChart.getSubtitleCount(); i++) {
      final Title subtitle = jChart.getSubtitle(i);
      if (subtitle instanceof PaintScaleLegend) {
        existingLegend = subtitle;
        break;
      }
    }
    if (existingLegend != null) {
      jChart.removeSubtitle(existingLegend);
    }
  }

  private void buildChart(final @NotNull KendrickMassPlotXYZDataset dataset) {
    final KendrickMassPlotChart newChart = new KendrickMassPlotChart("", "m/z",
        "Kendrick mass defect (CH2)", "Retention time", dataset);
    newChart.getChart().setTitle((String) null);
    final XYPlot plot = newChart.getChart().getXYPlot();
    plot.setDomainCrosshairVisible(false);
    plot.setRangeCrosshairVisible(false);
    plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
    if (plot.getDomainAxis() instanceof NumberAxis domainAxis) {
      domainAxis.setAutoRangeIncludesZero(false);
      domainAxis.setAutoRangeStickyZero(false);
    }
    if (plot.getRangeAxis() instanceof NumberAxis rangeAxis) {
      rangeAxis.setAutoRangeIncludesZero(false);
      rangeAxis.setAutoRangeStickyZero(false);
    }

    final var baseRenderer = plot.getRenderer();
    final var tooltipGenerator =
        baseRenderer != null ? baseRenderer.getDefaultToolTipGenerator() : null;
    final var itemLabelPaint = baseRenderer != null ? baseRenderer.getDefaultItemLabelPaint()
        : null;
    final boolean itemLabelsVisible = baseRenderer != null && Boolean.TRUE.equals(
        baseRenderer.getDefaultItemLabelsVisible());
    final PaintScale colorScale =
        baseRenderer instanceof ColoredBubbleDatasetRenderer colored ? colored.getPaintScale()
            : new LookupPaintScale(0d, 1d, Color.GRAY);

    // decision: replace the base renderer's label generator with a level-aware one so the
    // preferred lipid level property is read at render time, not captured at chart-build time
    final var levelAwareLabelGenerator = buildLevelAwareLabelGenerator();

    colorRenderer = new AlphaBubbleDatasetRenderer(1f);
    colorRenderer.setPaintScale(colorScale);
    if (tooltipGenerator != null) {
      colorRenderer.setDefaultToolTipGenerator(tooltipGenerator);
    }
    colorRenderer.setDefaultItemLabelGenerator(levelAwareLabelGenerator);
    if (itemLabelPaint != null) {
      colorRenderer.setDefaultItemLabelPaint(itemLabelPaint);
    }
    colorRenderer.setDefaultItemLabelsVisible(itemLabelsVisible);

    filteredOutRenderer = new AlphaBubbleDatasetRenderer(0.35f);
    filteredOutRenderer.setPaintScale(new LookupPaintScale(0d, 1d, Color.GRAY));
    if (tooltipGenerator != null) {
      filteredOutRenderer.setDefaultToolTipGenerator(tooltipGenerator);
    }
    filteredOutRenderer.setDefaultItemLabelGenerator(levelAwareLabelGenerator);
    if (itemLabelPaint != null) {
      filteredOutRenderer.setDefaultItemLabelPaint(itemLabelPaint);
    }
    filteredOutRenderer.setDefaultItemLabelsVisible(itemLabelsVisible);

    newChart.addChartMouseListener(new ChartMouseListenerFX() {
      @Override
      public void chartMouseClicked(final ChartMouseEventFX event) {
        if (event.getTrigger() == null || !event.getTrigger().isStillSincePress()
            || event.getTrigger().getButton() != MouseButton.PRIMARY) {
          return;
        }
        if (!(event.getEntity() instanceof XYItemEntity entity)) {
          return;
        }
        final FeatureListRow clickedRow = resolveClickedRow(entity.getDataset(),
            entity.getItem());
        if (clickedRow == null || Objects.equals(clickedRow, model.getRow())) {
          return;
        }
        model.setRow(clickedRow);
        FeatureTableFXUtil.selectAndScrollTo(clickedRow, model.getFeatureTableFx());
      }

      @Override
      public void chartMouseMoved(final ChartMouseEventFX event) {
      }
    });

    chart = newChart;
    setCenter(newChart);
    optimizeVisibleKendrickAxes(plot);
    requestUpdate();
  }

  private static void optimizeVisibleKendrickAxes(final @NotNull XYPlot plot) {
    final KendrickAxisExtrema xExtrema = collectVisibleExtrema(plot, true);
    final KendrickAxisExtrema yExtrema = collectVisibleExtrema(plot, false);
    if (plot.getDomainAxis() instanceof NumberAxis domainAxis && xExtrema.available()) {
      applyAxisExtrema(domainAxis, xExtrema);
    }
    if (plot.getRangeAxis() instanceof NumberAxis rangeAxis && yExtrema.available()) {
      applyAxisExtrema(rangeAxis, yExtrema);
    }
  }

  private static @NotNull KendrickAxisExtrema collectVisibleExtrema(final @NotNull XYPlot plot,
      final boolean xAxis) {
    double min = Double.POSITIVE_INFINITY;
    double max = Double.NEGATIVE_INFINITY;
    for (final int datasetIndex : new int[]{FILTERED_OUT_DATASET_INDEX, FILTERED_IN_DATASET_INDEX}) {
      final XYDataset dataset = plot.getDataset(datasetIndex);
      if (dataset == null || dataset.getSeriesCount() == 0) {
        continue;
      }
      for (int series = 0; series < dataset.getSeriesCount(); series++) {
        final int itemCount = dataset.getItemCount(series);
        for (int item = 0; item < itemCount; item++) {
          final double value =
              xAxis ? dataset.getXValue(series, item) : dataset.getYValue(series, item);
          if (!Double.isFinite(value)) {
            continue;
          }
          min = Math.min(min, value);
          max = Math.max(max, value);
        }
      }
    }
    return new KendrickAxisExtrema(min, max, Double.isFinite(min) && Double.isFinite(max));
  }

  private static void applyAxisExtrema(final @NotNull NumberAxis axis,
      final @NotNull KendrickAxisExtrema extrema) {
    if (!extrema.available()) {
      return;
    }
    axis.setAutoRange(false);
    axis.setAutoRangeIncludesZero(false);
    axis.setAutoRangeStickyZero(false);
    if (extrema.max() <= extrema.min()) {
      final double delta = Math.max(Math.abs(extrema.min()) * 0.02d, 0.05d);
      axis.setRange(extrema.min() - delta, extrema.max() + delta);
      return;
    }
    final double span = extrema.max() - extrema.min();
    final double padding = span * 0.02d;
    axis.setRange(extrema.min() - padding, extrema.max() + padding);
  }

  private static @NotNull AxisRangeState captureAxisState(final @NotNull NumberAxis axis) {
    return new AxisRangeState(axis.isAutoRange(), axis.getLowerBound(), axis.getUpperBound());
  }

  private static void restoreAxisState(final @NotNull NumberAxis axis,
      final @NotNull AxisRangeState axisState) {
    if (axisState.autoRange()) {
      axis.setAutoRange(true);
      return;
    }
    if (!Double.isFinite(axisState.lowerBound()) || !Double.isFinite(axisState.upperBound())) {
      return;
    }
    if (axisState.upperBound() <= axisState.lowerBound()) {
      final double delta = Math.max(Math.abs(axisState.lowerBound()) * 0.02d, 0.05d);
      axis.setRange(axisState.lowerBound() - delta, axisState.upperBound() + delta);
      return;
    }
    axis.setAutoRange(false);
    axis.setAutoRangeIncludesZero(false);
    axis.setAutoRangeStickyZero(false);
    axis.setRange(axisState.lowerBound(), axisState.upperBound());
  }

  private void updateSelectionOverlay() {
    if (chart == null || baseDataset == null) {
      return;
    }
    final XYPlot plot = chart.getChart().getXYPlot();
    plot.setDataset(TREND_OVERLAY_DATASET_INDEX, null);
    plot.setRenderer(TREND_OVERLAY_DATASET_INDEX, null);
    plot.setDataset(SELECTION_OVERLAY_DATASET_INDEX, null);
    plot.setRenderer(SELECTION_OVERLAY_DATASET_INDEX, null);
    final @Nullable FeatureListRow selectedRow = model.getRow();
    if (selectedRow == null) {
      return;
    }

    int selectedIndex = -1;
    for (int i = 0; i < baseDataset.getItemCount(0); i++) {
      final FeatureListRow row = baseDataset.getItemObject(i);
      if (row != null && Objects.equals(row.getID(), selectedRow.getID())) {
        selectedIndex = i;
        break;
      }
    }
    if (selectedIndex < 0) {
      return;
    }

    final double x = baseDataset.getXValue(0, selectedIndex);
    final double y = baseDataset.getYValue(0, selectedIndex);
    if (!Double.isFinite(x) || !Double.isFinite(y)) {
      return;
    }
    final XYSeries selectedSeries = new XYSeries("Selected lipid");
    selectedSeries.add(x, y);
    final XYSeriesCollection selectedDataset = new XYSeriesCollection();
    selectedDataset.addSeries(selectedSeries);
    plot.setDataset(SELECTION_OVERLAY_DATASET_INDEX, selectedDataset);
    plot.setRenderer(SELECTION_OVERLAY_DATASET_INDEX,
        new SelectedLipidOverlayRenderer(getSelectedLabel(selectedRow)));
    updateFalseNegativeTrendOverlay(plot, selectedRow);
  }

  private void updateFalseNegativeTrendOverlay(final @NotNull XYPlot plot,
      final @NotNull FeatureListRow selectedRow) {
    if (reviewMode != KendrickReviewMode.POTENTIAL_FALSE_NEGATIVE
        || falseNegativeDetector == null) {
      return;
    }
    final @Nullable KendrickFalseNegativeCandidate candidate = falseNegativeDetector.detectCandidate(
        selectedRow);
    if (candidate == null) {
      return;
    }
    final KendrickAxisExtrema visibleMzExtrema = collectVisibleExtrema(plot, true);
    if (!visibleMzExtrema.available()) {
      return;
    }
    final double minMz = visibleMzExtrema.min();
    final double maxMz = visibleMzExtrema.max();
    if (!Double.isFinite(minMz) || !Double.isFinite(maxMz)) {
      return;
    }

    final XYSeriesCollection trendDataset = new XYSeriesCollection();
    final XYLineAndShapeRenderer trendRenderer = new XYLineAndShapeRenderer(true, false);
    trendRenderer.setDefaultShapesVisible(false);
    trendRenderer.setAutoPopulateSeriesStroke(false);
    trendRenderer.setAutoPopulateSeriesPaint(false);
    int seriesIndex = 0;

    final @Nullable Double ch2TrendKmd = candidate.ch2TrendKmd();
    if (ch2TrendKmd != null && Double.isFinite(ch2TrendKmd)) {
      final XYSeries ch2Series = new XYSeries("CH2 trend");
      ch2Series.add(minMz, ch2TrendKmd);
      ch2Series.add(maxMz, ch2TrendKmd);
      trendDataset.addSeries(ch2Series);
      trendRenderer.setSeriesPaint(seriesIndex,
          ConfigService.getDefaultColorPalette().getPositiveColorAWT());
      trendRenderer.setSeriesStroke(seriesIndex, new BasicStroke(1.6f,
          BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f,
          new float[]{5f, 4f}, 0f));
      trendRenderer.setSeriesVisibleInLegend(seriesIndex, false);
      seriesIndex++;
    }

    final @Nullable Double dbeTrendSlope = candidate.dbeTrendSlope();
    final @Nullable Double dbeTrendIntercept = candidate.dbeTrendIntercept();
    if (dbeTrendSlope != null && dbeTrendIntercept != null && Double.isFinite(dbeTrendSlope)
        && Double.isFinite(dbeTrendIntercept)) {
      final @Nullable TrendSegment clippedSegment = clipLineToKmdBounds(dbeTrendSlope,
          dbeTrendIntercept, minMz, maxMz);
      if (clippedSegment != null) {
        final XYSeries dbeSeries = new XYSeries("DBE trend");
        dbeSeries.add(clippedSegment.x1(), clippedSegment.y1());
        dbeSeries.add(clippedSegment.x2(), clippedSegment.y2());
        trendDataset.addSeries(dbeSeries);
        trendRenderer.setSeriesPaint(seriesIndex,
            ConfigService.getDefaultColorPalette().getNeutralColorAWT());
        trendRenderer.setSeriesStroke(seriesIndex, new BasicStroke(1.6f,
            BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f,
            new float[]{5f, 4f}, 0f));
        trendRenderer.setSeriesVisibleInLegend(seriesIndex, false);
        seriesIndex++;
      }
    }

    if (seriesIndex == 0) {
      return;
    }
    plot.setDataset(TREND_OVERLAY_DATASET_INDEX, trendDataset);
    plot.setRenderer(TREND_OVERLAY_DATASET_INDEX, trendRenderer);
  }

  private void notifyOutlierRowsChanged(final @Nullable KendrickSubsetDataset outlierDataset) {
    if (onOutlierRowsChanged == null) {
      return;
    }
    final List<FeatureListRow> rows = new ArrayList<>();
    if (outlierDataset != null) {
      for (int i = 0; i < outlierDataset.getItemCount(0); i++) {
        final FeatureListRow row = outlierDataset.getItemObject(i);
        if (row != null) {
          rows.add(row);
        }
      }
    }
    onOutlierRowsChanged.accept(rows);
  }

  private void updateOutlierOverlay(final @Nullable KendrickSubsetDataset outlierDataset,
      final @NotNull KendrickReviewMode reviewMode) {
    if (chart == null) {
      return;
    }
    final XYPlot plot = chart.getChart().getXYPlot();
    plot.setDataset(OUTLIER_OVERLAY_DATASET_INDEX, null);
    plot.setRenderer(OUTLIER_OVERLAY_DATASET_INDEX, null);
    if (outlierDataset == null || outlierDataset.getItemCount(0) == 0) {
      return;
    }

    final XYLineAndShapeRenderer outlierRenderer = new XYLineAndShapeRenderer(false, true);
    final Color overlayColor = switch (reviewMode) {
      case NONE -> ConfigService.getDefaultColorPalette().getNeutralColorAWT();
      case POTENTIAL_FALSE_POSITIVE -> ConfigService.getDefaultColorPalette().getNegativeColorAWT();
      case POTENTIAL_FALSE_NEGATIVE -> ConfigService.getDefaultColorPalette().getPositiveColorAWT();
    };
    outlierRenderer.setSeriesPaint(0, overlayColor);
    outlierRenderer.setSeriesStroke(0, new BasicStroke(1.9f));
    outlierRenderer.setSeriesShape(0, new Ellipse2D.Double(-5.5, -5.5, 11, 11));
    outlierRenderer.setDefaultShapesFilled(false);
    outlierRenderer.setSeriesVisibleInLegend(0, false);
    plot.setDataset(OUTLIER_OVERLAY_DATASET_INDEX, outlierDataset);
    plot.setRenderer(OUTLIER_OVERLAY_DATASET_INDEX, outlierRenderer);
  }

  private static @Nullable FeatureListRow resolveClickedRow(final @NotNull XYDataset dataset,
      final int item) {
    if (dataset instanceof XYItemObjectProvider<?> provider) {
      final Object obj = provider.getItemObject(item);
      if (obj instanceof FeatureListRow row) {
        return row;
      }
    }
    return null;
  }

  private @NotNull XYItemLabelGenerator buildLevelAwareLabelGenerator() {
    return (xyDataset, series, item) -> {
      if (!(xyDataset instanceof XYItemObjectProvider<?> provider)) {
        return null;
      }
      final Object obj = provider.getItemObject(item);
      if (!(obj instanceof FeatureListRow row)) {
        return null;
      }
      final @Nullable MatchedLipid match = LipidQcAnnotationSelectionUtils.getPreferredLipidMatch(
          row);
      if (match == null) {
        return null;
      }
      final LipidAnnotationLevel level = model.getPreferredLipidLevel();
      final String annotation = match.getLipidAnnotation().getAnnotation(level);
      return annotation.length() > 52 ? annotation.substring(0, 49) + "..." : annotation;
    };
  }

  private @NotNull String getSelectedLabel(final @NotNull FeatureListRow row) {
    final @Nullable MatchedLipid selectedMatch = LipidQcAnnotationSelectionUtils.getPreferredLipidMatch(
        row);
    if (selectedMatch == null) {
      return "Row " + row.getID();
    }
    final String annotation = selectedMatch.getLipidAnnotation()
        .getAnnotation(model.getPreferredLipidLevel());
    return annotation.length() > 52 ? annotation.substring(0, 49) + "..." : annotation;
  }

  private static @Nullable TrendSegment clipLineToKmdBounds(final double slope,
      final double intercept, final double minX, final double maxX) {
    if (!Double.isFinite(slope) || !Double.isFinite(intercept) || !Double.isFinite(minX)
        || !Double.isFinite(maxX) || maxX <= minX) {
      return null;
    }

    if (Math.abs(slope) < 1e-12d) {
      final double y = intercept;
      if (!Double.isFinite(y) || y < KMD_MIN || y > KMD_MAX) {
        return null;
      }
      return new TrendSegment(minX, y, maxX, y);
    }

    final double xAtMinKmd = (KMD_MIN - intercept) / slope;
    final double xAtMaxKmd = (KMD_MAX - intercept) / slope;
    final double minAllowedX = Math.min(xAtMinKmd, xAtMaxKmd);
    final double maxAllowedX = Math.max(xAtMinKmd, xAtMaxKmd);
    final double clippedMinX = Math.max(minX, minAllowedX);
    final double clippedMaxX = Math.min(maxX, maxAllowedX);
    if (!Double.isFinite(clippedMinX) || !Double.isFinite(clippedMaxX) || clippedMaxX <= clippedMinX) {
      return null;
    }

    final double y1 = clampKmd(slope * clippedMinX + intercept);
    final double y2 = clampKmd(slope * clippedMaxX + intercept);
    return new TrendSegment(clippedMinX, y1, clippedMaxX, y2);
  }

  private static double clampKmd(final double kmd) {
    return Math.max(KMD_MIN, Math.min(KMD_MAX, kmd));
  }

  private void discardChart() {
    cancelScheduledTasks();
    falseNegativeDetector = null;
    if (chart == null) {
      baseDataset = null;
      return;
    }
    final XYPlot plot = chart.getChart().getXYPlot();
    for (int i = 0; i < Math.max(5, plot.getDatasetCount()); i++) {
      plot.setDataset(i, null);
      plot.setRenderer(i, null);
    }
    setCenter(placeholder);
    chart = null;
    baseDataset = null;
  }

  private record AxisRangeState(boolean autoRange, double lowerBound, double upperBound) {

  }

  private record TrendSegment(double x1, double y1, double x2, double y2) {

  }
}
