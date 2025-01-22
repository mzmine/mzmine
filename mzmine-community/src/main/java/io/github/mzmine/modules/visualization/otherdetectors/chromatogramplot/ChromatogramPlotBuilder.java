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

import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import java.util.List;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import org.jfree.chart.annotations.XYAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;

public class ChromatogramPlotBuilder extends FxViewBuilder<ChromatogramPlotModel> {

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
    chart.setStickyZeroRangeAxis(false);

    initializeDatasetRendererListener(chart);
    initializeAnnotationListener(chart);
    initializeValueListener(chart);

    model.cursorPositionProperty().bindBidirectional(chart.cursorPositionProperty());
    model.titleProperty().subscribe(title -> chart.getChart().setTitle(title));
    model.domainLabelProperty()
        .subscribe(label -> chart.getXYPlot().getDomainAxis().setLabel(label));
    model.rangeLabelProperty().subscribe(label -> chart.getXYPlot().getRangeAxis().setLabel(label));
    model.rangeAxisFormatProperty().subscribe(
        format -> ((NumberAxis) chart.getXYPlot().getRangeAxis()).setNumberFormatOverride(format));
    model.domainAxisFormatProperty().subscribe(
        format -> ((NumberAxis) chart.getXYPlot().getDomainAxis()).setNumberFormatOverride(format));

    return pane;
  }

  private void initializeDatasetRendererListener(SimpleXYChart<PlotXYDataProvider> chart) {
    model.datasetRenderersProperty()
        .addListener((MapChangeListener<XYDataset, XYItemRenderer>) change -> {
          if (change.wasAdded()) {
            final XYItemRenderer added = change.getValueAdded();
            final XYDataset key = change.getKey();
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
