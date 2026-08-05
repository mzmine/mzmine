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
import io.github.mzmine.gui.chartbasics.gui.javafx.model.PlotCursorUtils;
import io.github.mzmine.gui.chartbasics.simplechart.PlotCursorPosition;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.XYItemObjectProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.XYValueProvider;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.visualization.dash_lipidqc.LipidAnnotationQCDashboardModel;
import io.github.mzmine.util.FeatureTableFXUtil;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.axis.NumberAxis;
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

  private @Nullable RetentionChartSpec currentSpec;

  RetentionChartController(final @NotNull EquivalentCarbonNumberPane pane,
      final @NotNull LipidAnnotationQCDashboardModel model,
      final @NotNull SimpleXYChart<PlotXYDataProvider> chart) {
    this.pane = pane;
    this.model = model;
    this.chart = chart;
    specFactory = new RetentionChartSpecFactory(model);
    chart.setMinSize(250d, 120d);
    chart.setShowCrosshair(true);
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
    final @Nullable PlotCursorPosition previousCursorPosition = chart.getCursorPosition();
    final @Nullable RetentionPointRef previousCursorPoint = previousCursorPosition == null ? null
        : extractPointRef(previousCursorPosition.getDataset(),
            previousCursorPosition.getValueIndex());
    final @Nullable FeatureListRow previousCursorRow =
        previousCursorPoint == null ? null : previousCursorPoint.row();
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

      pane.updatePaneTitle(spec.title());
      pane.showRetentionChart(chart);

      restoreCursorPosition(previousCursorPosition, previousCursorRow);
      // need to reapply chart theme since axis are being rebuild.
      ConfigService.getConfiguration().getDefaultChartTheme().apply(plot.getChart());
    });
  }

  private void resetChart() {
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
    if (newPosition == null || currentSpec == null) {
      return;
    }
    final @Nullable RetentionPointRef pointRef = extractPointRef(newPosition.getDataset(),
        newPosition.getValueIndex());
    if (pointRef == null || pointRef.row() == null) {
      return;
    }
    selectRow(pointRef.row());
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
    if (!Objects.equals(row, model.getRow())) {
      model.setRow(row);
    }
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

  private void restoreCursorPosition(final @Nullable PlotCursorPosition previousCursorPosition,
      final @Nullable FeatureListRow previousCursorRow) {
    if (previousCursorPosition != null && previousCursorRow != null && Objects.equals(
        previousCursorRow, model.getRow())) {
      final List<XYDataset> datasets = List.copyOf(chart.getAllDatasets().values());
      final PlotCursorPosition restoredPosition = PlotCursorUtils.moveCursorFindInData(
          previousCursorPosition, datasets, previousCursorPosition.getDomainValue(),
          previousCursorPosition.getRangeValue());
      if (restoredPosition.getDataset() != null) {
        chart.setCursorPosition(restoredPosition);
        return;
      }
    }

    final @Nullable FeatureListRow selectedRow = model.getRow();
    if (selectedRow == null) {
      return;
    }
    for (final XYDataset dataset : chart.getAllDatasets().values()) {
      if (!(dataset instanceof ColoredXYDataset coloredDataset)) {
        continue;
      }
      for (int item = 0; item < coloredDataset.getItemCount(0); item++) {
        final @Nullable RetentionPointRef pointRef = extractPointRef(dataset, item);
        if (pointRef == null || !Objects.equals(pointRef.row(), selectedRow)) {
          continue;
        }
        chart.setCursorPosition(new PlotCursorPosition(coloredDataset.getXValue(0, item),
            coloredDataset.getYValue(0, item), item, dataset));
        return;
      }
    }
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
