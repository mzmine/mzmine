/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.visualization.otherdetectors.chromatogramplot;

import io.github.mzmine.gui.chartbasics.gestures.ChartGesture;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.Entity;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.Event;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.GestureButton;
import io.github.mzmine.gui.chartbasics.gestures.ChartGestureHandler;
import io.github.mzmine.gui.chartbasics.gui.javafx.ChartGestureMouseAdapterFX;
import io.github.mzmine.gui.chartbasics.simplechart.PlotCursorPosition;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.generators.SeriesKeyAtMaxLabelGenerator;
import io.github.mzmine.gui.chartbasics.simplechart.generators.SimpleToolTipGenerator;
import io.github.mzmine.gui.chartbasics.simplechart.generators.XYLabelCollisionResolver;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.main.ConfigService;
import java.awt.BasicStroke;
import java.awt.Stroke;
import java.util.List;
import java.util.Map;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import org.jfree.chart.annotations.XYAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.entity.LegendItemEntity;
import org.jfree.chart.event.ChartProgressEvent;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;

public class ChromatogramPlotBuilder extends FxViewBuilder<ChromatogramPlotModel> {

  // Stroke widths used to visually distinguish the selected dataset from the rest. Shared across
  // all renderers added through the model; subclasses (e.g. dashed strokes) are not preserved.
  private static final Stroke DEFAULT_STROKE = new BasicStroke(1.0f);
  private static final Stroke SELECTED_STROKE = new BasicStroke(2.5f);

  protected ChromatogramPlotBuilder(ChromatogramPlotModel model) {
    super(model);
  }

  @Override
  public Region build() {

    BorderPane pane = new BorderPane();
    final SimpleXYChart<PlotXYDataProvider> chart = model.getChart();
    pane.setCenter(chart);

    chart.setMinHeight(100);
    chart.setMinWidth(100);

    chart.getXYPlot().setDomainCrosshairVisible(true);
    chart.getXYPlot().setRangeCrosshairVisible(true);
    chart.getXYPlot().setDomainCrosshairPaint(new java.awt.Color(0, 0, 0, 100));
    chart.getXYPlot().setRangeCrosshairPaint(new java.awt.Color(0, 0, 0, 100));
    chart.getXYPlot().setDomainCrosshairStroke(new BasicStroke(0.5f));
    chart.getXYPlot().setRangeCrosshairStroke(new BasicStroke(0.5f));

    chart.setOnMouseClicked(e -> {
      final PlotCursorPosition pos = chart.getCursorPosition();
      if (pos != null) {
        model.setCursorPosition(pos);
      }
    });

    chart.getXYPlot().getRangeAxis().setUpperMargin(0.08);

    model.rangeStickyZeroProperty().subscribe(sticky -> {
      if (sticky) {
        // otherwise there is a margin on bottom
        chart.getXYPlot().getRangeAxis().setLowerMargin(0.0);
      }
      chart.setStickyZeroRangeAxis(sticky);
    });

    // Clear the drawn-label-bounds cache at the start of every chart draw pass. Using
    // ChartProgressEvent.DRAWING_STARTED (instead of PlotChangeListener) covers every cause of a
    // repaint — zoom, dataset add/remove, renderer swap AND window/SplitPane resize, the latter of
    // which does not fire a PlotChangeEvent because the plot's own state hasn't changed.
    chart.getChart().addProgressListener(event -> {
      if (event.getType() == ChartProgressEvent.DRAWING_STARTED) {
        model.clearDrawnLabelBounds();
      }
    });

    initializeDatasetRendererListener(chart);
    initializeAnnotationListener(chart);
    initializeValueListener(chart);
    initializeSelectedDatasetHandling(chart);

    model.cursorPositionProperty().bindBidirectional(chart.cursorPositionProperty());
    model.titleProperty().subscribe(title -> {
      chart.getChart().setTitle(title);
      var theme = ConfigService.getConfiguration().getDefaultChartTheme();
      theme.applyToTitles(model.getChart().getChart());
    });
    model.domainLabelProperty().subscribe(label -> {
      if (model.isShowDomainAxisLabel()) {
        chart.getXYPlot().getDomainAxis().setLabel(label);
      }
    });
    model.rangeLabelProperty().subscribe(label -> {
      if (model.isShowRangeAxisLabel()) {
        chart.getXYPlot().getRangeAxis().setLabel(label);
      }
    });

    model.showDomainAxisLabelProperty().subscribe(show -> {
      chart.getXYPlot().getDomainAxis().setLabel(show ? model.getDomainLabel() : null);
    });
    model.showRangeAxisLabelProperty().subscribe(show -> {
      chart.getXYPlot().getRangeAxis().setLabel(show ? model.getRangeLabel() : null);
    });

    model.rangeAxisFormatProperty().subscribe(
        format -> ((NumberAxis) chart.getXYPlot().getRangeAxis()).setNumberFormatOverride(format));
    model.domainAxisFormatProperty().subscribe(
        format -> ((NumberAxis) chart.getXYPlot().getDomainAxis()).setNumberFormatOverride(format));

    return pane;
  }

  private void initializeDatasetRendererListener(SimpleXYChart<PlotXYDataProvider> chart) {
    // shared across renderers: the generator caches max-item indices per dataset reference.
    // The collision resolver drops labels whose screen-space rectangle would overlap an already
    // accepted label — useful when several chromatograms peak close to each other on the time axis.
    final XYLabelCollisionResolver collisionResolver = new XYLabelCollisionResolver(
        model::getXYPlot, model::getDrawnLabelBounds, model::getBottomLabelMargin);
    final SeriesKeyAtMaxLabelGenerator seriesLabelGenerator = new SeriesKeyAtMaxLabelGenerator(
        collisionResolver);
    final SimpleToolTipGenerator tooltipGenerator = new SimpleToolTipGenerator();

    model.datasetRenderersProperty()
        .addListener((MapChangeListener<XYDataset, XYItemRenderer>) change -> {
          if (change.wasAdded()) {
            final XYItemRenderer added = change.getValueAdded();
            final XYDataset key = change.getKey();
            if (model.isShowSeriesLabel()) {
              applySeriesLabel(added, seriesLabelGenerator, tooltipGenerator);
            }
            chart.addDataset(key, added);
          }
          if (change.wasRemoved()) {
            if (change.getMap().isEmpty()) {
              chart.removeAllDatasets();
            } else {
              final XYItemRenderer removed = change.getValueRemoved();
              final XYDataset key = change.getKey();
              chart.getAllDatasets().entrySet().stream().filter(e -> e.getValue() == key)
                  .findFirst().ifPresent(e -> chart.removeDataSet(e.getKey()));
            }
          }
        });

    // Late toggles re-configure existing renderers so the flag works even after datasets exist.
    model.showSeriesLabelProperty().subscribe(show -> {
      if (!show) {
        return;
      }
      chart.applyWithNotifyChanges(false,
          () -> model.getDatasetRenderers().values()
              .forEach(r -> applySeriesLabel(r, seriesLabelGenerator, tooltipGenerator)));
    });
  }

  private static void applySeriesLabel(final XYItemRenderer renderer,
      final SeriesKeyAtMaxLabelGenerator labelGenerator,
      final SimpleToolTipGenerator tooltipGenerator) {
    renderer.setDefaultItemLabelGenerator(labelGenerator);
    renderer.setDefaultItemLabelsVisible(true);
    // Custom renderers added through the model don't get the chart's default tooltip generator,
    // so install one here too — without it the IonTimeSeriesToXYProvider tooltip never shows.
    if (renderer.getDefaultToolTipGenerator() == null) {
      renderer.setDefaultToolTipGenerator(tooltipGenerator);
    }
  }

  private void initializeValueListener(SimpleXYChart<PlotXYDataProvider> chart) {
    model.domainAxisMarkersProperty().addListener((ListChangeListener<ValueMarker>) c -> {
      chart.applyWithNotifyChanges(false, () -> {
        while (c.next()) {
          if (c.wasAdded()) {
            final List<? extends ValueMarker> added = c.getAddedSubList();
            added.forEach(m -> chart.getXYPlot().addDomainMarker(m));
          }
          if (c.wasRemoved()) {
            final List<? extends ValueMarker> removed = c.getRemoved();
            removed.forEach(m -> chart.getXYPlot().removeDomainMarker(m));
          }
        }
      });
    });
  }

  /**
   * Wires the selected-dataset property to (1) the legend-item click that toggles it, and (2) the
   * renderer stroke that visualises which dataset is currently selected. The handler is attached to
   * the chart's mouse adapter through {@link ChartGestureMouseAdapterFX}, in line with the existing
   * mzmine ChartGestures pattern.
   */
  private void initializeSelectedDatasetHandling(SimpleXYChart<PlotXYDataProvider> chart) {
    // Toggle selectedDataset on left-click of a legend item.
    final ChartGestureMouseAdapterFX adapter = chart.getGestureAdapter();
    if (adapter != null) {
      adapter.addGestureHandler(new ChartGestureHandler(
          new ChartGesture(Entity.LEGEND_ITEM, Event.CLICK, GestureButton.BUTTON1), e -> {
        if (!(e.getEntity() instanceof LegendItemEntity legend)) {
          return;
        }
        // LegendItemEntity#getDataset() returns the dataset the clicked legend item belongs to.
        // Toggle: same dataset clicked again -> deselect.
        final Object ds = legend.getDataset();
        if (ds instanceof XYDataset clicked) {
          final XYDataset current = model.getSelectedDataset();
          model.setSelectedDataset(current == clicked ? null : clicked);
        }
      }));
    }

    // Re-apply strokes when the selection changes or new datasets land.
    model.selectedDatasetProperty().subscribe(_ -> applySelectedDatasetHighlight(chart));
    model.datasetRenderersProperty().addListener(
        (MapChangeListener<XYDataset, XYItemRenderer>) _ -> {
          // If the selected dataset was removed, clear the selection.
          final XYDataset selected = model.getSelectedDataset();
          if (selected != null && !model.getDatasetRenderers().containsKey(selected)) {
            model.setSelectedDataset(null);
          } else {
            applySelectedDatasetHighlight(chart);
          }
        });
  }

  private void applySelectedDatasetHighlight(SimpleXYChart<PlotXYDataProvider> chart) {
    final XYDataset selected = model.getSelectedDataset();
    chart.applyWithNotifyChanges(false, () -> {
      for (final Map.Entry<XYDataset, XYItemRenderer> e : model.getDatasetRenderers().entrySet()) {
        final Stroke stroke = e.getKey() == selected ? SELECTED_STROKE : DEFAULT_STROKE;
        e.getValue().setDefaultStroke(stroke);
      }
    });
  }

  private void initializeAnnotationListener(SimpleXYChart<PlotXYDataProvider> chart) {
    model.annotationsProperty().addListener((ListChangeListener<XYAnnotation>) c -> {
      chart.applyWithNotifyChanges(false, () -> {
        while (c.next()) {
          if (c.wasAdded()) {
            final List<? extends XYAnnotation> added = c.getAddedSubList();
            added.forEach(a -> chart.getXYPlot().addAnnotation(a));
          }
          if (c.wasRemoved()) {
            final List<? extends XYAnnotation> removed = c.getRemoved();
            removed.forEach(a -> chart.getXYPlot().removeAnnotation(a));
          }
        }
      });
    });
  }
}
