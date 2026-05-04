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
import io.github.mzmine.gui.chartbasics.gui.javafx.model.FxXYPlot;
import io.github.mzmine.gui.chartbasics.simplechart.PlotCursorPosition;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.XYItemObjectProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.XYValueProvider;
import io.github.mzmine.modules.visualization.dash_lipidqc.LipidAnnotationQCDashboardModel;
import io.github.mzmine.util.FeatureTableFXUtil;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.fx.interaction.ChartMouseEventFX;
import org.jfree.chart.fx.interaction.ChartMouseListenerFX;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;

/**
 * Applies immutable retention chart specs to one persistent {@link SimpleXYChart} instance and
 * bridges chart selection back to the dashboard model.
 */
final class RetentionChartController {

  private final @NotNull EquivalentCarbonNumberPane pane;
  private final @NotNull LipidAnnotationQCDashboardModel model;
  private final @NotNull SimpleXYChart<PlotXYDataProvider> chart;
  private final @NotNull RetentionChartSpecFactory specFactory;
  private final @NotNull ChartMouseListenerFX manualCombinedSelectionListener = new ChartMouseListenerFX() {
    @Override
    public void chartMouseClicked(final @NotNull ChartMouseEventFX event) {
      if (!(event.getEntity() instanceof XYItemEntity entity)) {
        return;
      }
      final @Nullable RetentionPointRef pointRef = extractPointRef(entity.getDataset(),
          entity.getItem());
      if (pointRef == null || pointRef.row() == null) {
        return;
      }
      selectRow(pointRef.row());
    }

    @Override
    public void chartMouseMoved(final @NotNull ChartMouseEventFX event) {
    }
  };

  private @Nullable RetentionChartSpec currentSpec;
  private boolean manualCombinedSelectionInstalled;

  RetentionChartController(final @NotNull EquivalentCarbonNumberPane pane,
      final @NotNull LipidAnnotationQCDashboardModel model,
      final @NotNull SimpleXYChart<PlotXYDataProvider> chart) {
    this.pane = pane;
    this.model = model;
    this.chart = chart;
    specFactory = new RetentionChartSpecFactory(model);
    chart.setMinSize(250d, 120d);
    chart.setShowCrosshair(false);
    chart.cursorPositionProperty()
        .addListener((_, _, newPosition) -> handleCursorSelection(newPosition));
  }

  void apply(final @NotNull RetentionComputationResult result) {
    pane.updatePaneTitle("Retention time analysis");
    if (result.placeholderText() != null) {
      applyPlaceholder(result.placeholderText());
      return;
    }

    final @Nullable RetentionComputationPayload payload = result.payload();
    if (payload == null) {
      applyPlaceholder("No retention trend data available.");
      return;
    }

    final @Nullable RetentionChartSpec spec = specFactory.build(payload);
    if (spec == null) {
      applyPlaceholder("No retention trend data available.");
      return;
    }
    applySpec(spec);
  }

  void applyPlaceholder(final @NotNull String text) {
    resetChart();
    pane.showRetentionPlaceholder(text);
  }

  private void applySpec(final @NotNull RetentionChartSpec spec) {
    chart.applyWithNotifyChanges(false, () -> {
      resetChart();
      currentSpec = spec;
      configureAxes(spec);
      chart.setLegendItemsVisible(spec.showLegend());
      if (chart.getChart().getLegend() != null) {
        chart.getChart().getLegend().setVisible(spec.showLegend());
      }

      final FxXYPlot plot = chart.getXYPlot();
      for (int i = 0; i < spec.datasets().size(); i++) {
        final RetentionDatasetSpec datasetSpec = spec.datasets().get(i);
        plot.setDataset(i, datasetSpec.dataset());
        plot.setRenderer(i, datasetSpec.renderer());
        plot.mapDatasetToRangeAxis(i, datasetSpec.rangeAxisIndex());
      }

      if (spec.axisSyncSpec() != null) {
        installAxisSynchronizer(plot, spec.axisSyncSpec());
      }
      if (spec.manualSelection()) {
        installManualCombinedSelection();
      }

      pane.updatePaneTitle(spec.title());
      pane.showRetentionChart(chart);
    });
  }

  private void resetChart() {
    removeManualCombinedSelection();
    currentSpec = null;
    chart.setCursorPosition(null);
    chart.setLegendItemsVisible(false);
    if (chart.getChart().getLegend() != null) {
      chart.getChart().getLegend().setVisible(false);
    }

    final FxXYPlot plot = chart.getXYPlot();
    chart.removeAllDatasets();
    for (int i = 0; i < plot.getRendererCount(); i++) {
      plot.setRenderer(i, null);
    }
    plot.clearDomainMarkers();
    plot.clearRangeMarkers();

    final int rangeAxisCount = plot.getRangeAxisCount();
    for (int i = 1; i < rangeAxisCount; i++) {
      plot.setRangeAxis(i, null);
    }
  }

  private void configureAxes(final @NotNull RetentionChartSpec spec) {
    final FxXYPlot plot = chart.getXYPlot();
    plot.setDomainAxis(new NumberAxis(spec.domainAxis().label()));
    if (plot.getDomainAxis() instanceof NumberAxis domainAxis) {
      applyAxisSpec(domainAxis, spec.domainAxis());
    }

    for (final RetentionAxisSpec axisSpec : spec.rangeAxes()) {
      final NumberAxis rangeAxis = new NumberAxis(axisSpec.label());
      applyAxisSpec(rangeAxis, axisSpec);
      plot.setRangeAxis(axisSpec.axisIndex(), rangeAxis);
    }
  }

  private void applyAxisSpec(final @NotNull NumberAxis axis,
      final @NotNull RetentionAxisSpec spec) {
    axis.setLabel(spec.label());
    setAxisRangeToData(axis, spec.min(), spec.max(), spec.lockLowerToFirstPoint());
  }

  private void handleCursorSelection(final @Nullable PlotCursorPosition newPosition) {
    if (newPosition == null || currentSpec == null || currentSpec.manualSelection()) {
      return;
    }
    final @Nullable RetentionPointRef pointRef = extractPointRef(newPosition.getDataset(),
        newPosition.getValueIndex());
    if (pointRef == null || pointRef.row() == null) {
      return;
    }
    selectRow(pointRef.row());
  }

  private void installManualCombinedSelection() {
    if (manualCombinedSelectionInstalled) {
      return;
    }
    chart.addChartMouseListener(manualCombinedSelectionListener);
    manualCombinedSelectionInstalled = true;
  }

  private void removeManualCombinedSelection() {
    if (!manualCombinedSelectionInstalled) {
      return;
    }
    chart.removeChartMouseListener(manualCombinedSelectionListener);
    manualCombinedSelectionInstalled = false;
  }

  private void installAxisSynchronizer(final @NotNull XYPlot plot,
      final @NotNull RetentionAxisSyncSpec syncSpec) {
    if (!(plot.getRangeAxis(syncSpec.primaryAxisIndex()) instanceof NumberAxis primaryAxis)
        || !(plot.getRangeAxis(
        syncSpec.secondaryAxisIndex()) instanceof NumberAxis secondaryAxis)) {
      return;
    }
    final SelectedPointAxisSynchronizer synchronizer = new SelectedPointAxisSynchronizer(
        primaryAxis, secondaryAxis, syncSpec.primaryValue(), syncSpec.secondaryValue(),
        primaryAxis.getLowerBound(), primaryAxis.getUpperBound(), secondaryAxis.getLowerBound(),
        secondaryAxis.getUpperBound());
    synchronizer.install();
    synchronizer.syncSecondaryToPrimary();
  }

  private void selectRow(final @NotNull FeatureListRow row) {
    if (Objects.equals(row, model.getRow())) {
      FeatureTableFXUtil.selectAndScrollTo(row, model.getFeatureTableFx());
      return;
    }
    model.setRow(row);
    FeatureTableFXUtil.selectAndScrollTo(row, model.getFeatureTableFx());
  }

  private @Nullable RetentionPointRef extractPointRef(final @Nullable XYDataset dataset,
      final int itemIndex) {
    if (!(dataset instanceof ColoredXYDataset coloredDataset) || itemIndex < 0) {
      return null;
    }
    final XYValueProvider valueProvider = coloredDataset.getValueProvider();
    if (!(valueProvider instanceof XYItemObjectProvider<?> itemObjectProvider)) {
      return null;
    }
    final Object itemObject = itemObjectProvider.getItemObject(itemIndex);
    return itemObject instanceof RetentionPointRef pointRef ? pointRef : null;
  }

  private void setAxisRangeToData(final @NotNull NumberAxis axis, final double min,
      final double max, final boolean lockLowerToFirstPoint) {
    if (!Double.isFinite(min) || !Double.isFinite(max)) {
      return;
    }
    axis.setAutoRange(false);
    axis.setAutoRangeIncludesZero(false);
    axis.setAutoRangeStickyZero(false);

    if (max <= min) {
      final double delta = Math.max(Math.abs(min) * 0.05d, 0.2d);
      final double lower = lockLowerToFirstPoint ? min - delta * 0.5d : min - delta;
      axis.setRange(lower, min + delta * 1.6d);
      return;
    }

    final double span = max - min;
    final double lowerPadding = lockLowerToFirstPoint ? span * 0.16d : span * 0.18d;
    final double upperPadding = span * 0.30d;
    axis.setRange(min - lowerPadding, max + upperPadding);
  }
}
