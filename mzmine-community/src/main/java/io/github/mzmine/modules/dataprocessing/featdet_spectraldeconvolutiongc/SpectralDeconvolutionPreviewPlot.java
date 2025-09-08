/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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
package io.github.mzmine.modules.dataprocessing.featdet_spectraldeconvolutiongc;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYAnnotation;
import org.jfree.chart.annotations.XYShapeAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.Layer;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class SpectralDeconvolutionPreviewPlot extends EChartViewer {

  private final XYPlot plot;
  private int datasetIndex;
  private final XYLineAndShapeRenderer renderer;
  private final BooleanProperty showRtWidths = new SimpleBooleanProperty(false);
  private List<XYAnnotation> widthAnnotations = new ArrayList<>();

  public SpectralDeconvolutionPreviewPlot(String title, String xAxisLabel, String yAxisLabel) {
    super(null);
    XYSeriesCollection dataset = new XYSeriesCollection();
    JFreeChart chart = ChartFactory.createScatterPlot(title, xAxisLabel, yAxisLabel, dataset,
        PlotOrientation.VERTICAL, false, true, false);

    plot = chart.getXYPlot();
    plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

    setMinHeight(250);

    NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
    xAxis.setAutoRangeIncludesZero(false);

    NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
    yAxis.setAutoRangeIncludesZero(false);

    setChart(chart);
    datasetIndex = 0;
    renderer = new XYLineAndShapeRenderer(false, true);
    plot.setRenderer(renderer);

    showRtWidths.subscribe((_, show) -> handleShowRtWidthsChange(show));
  }

  private void handleShowRtWidthsChange(boolean show) {
    if (widthAnnotations == null || widthAnnotations.isEmpty()) {
      return;
    }
    final boolean oldNotify = plot.isNotify();
    plot.setNotify(false);
    plot.clearAnnotations();

    if (show) {
      for (XYAnnotation ann : widthAnnotations) {
        plot.addAnnotation(ann, false);
      }
    }

    plot.setNotify(oldNotify);
    if (oldNotify) {
      getChart().fireChartChanged();
    }
  }

  public void addDataset(List<ModularFeature> features, XYSeries series, Color color) {
    XYSeriesCollection dataset = new XYSeriesCollection();
    dataset.addSeries(series);
    plot.setDataset(datasetIndex, dataset);

    var renderer = new XYLineAndShapeRenderer(false, true);
    renderer.setSeriesPaint(0, color);
    renderer.setSeriesShape(datasetIndex, new Rectangle(0, 0, 2, 2));
    plot.setRenderer(datasetIndex, renderer);

    // way to slow to add both the dataset and also the XYAnnotations.
    // TODO translate this into the renderer and make it a XYZDataset using Z for RT width
    for (ModularFeature feature : features) {

      // Calculate the RT range (width of the box) based on the feature's RT
      double rtStart = feature.getRawDataPointsRTRange().lowerEndpoint();
      double rtEnd = feature.getRawDataPointsRTRange().upperEndpoint();
      double mz = feature.getMZ();

      // Calculate box width and height
      double boxWidth = rtEnd - rtStart;  // Width based on RT range
      double boxHeight = 0.5;  // Fixed height; adjust as needed

      // Create the rectangle centered at the feature's RT and m/z values
      Rectangle2D box = new Rectangle2D.Double(rtStart, mz - boxHeight / 2, boxWidth, boxHeight);

      // Create the annotation with the rectangle shape and color
      XYShapeAnnotation annotation = new XYShapeAnnotation(box, new BasicStroke(1.0f), color,
          color);

      widthAnnotations.add(annotation);
      // Add the annotation to the plot
      if (showRtWidths.get()) {
        plot.addAnnotation(annotation, false);
      }
    }

    datasetIndex++;
  }


  public void clearDatasets() {
    for (int i = 0; i < datasetIndex; i++) {
      plot.setDataset(i, null);
      plot.setRenderer(i, null);
    }
    datasetIndex = 0;
    plot.clearAnnotations();
    widthAnnotations.clear();
  }

  public void addIntervalMarker(Range<Float> rtRange, Color color) {

    // Create an IntervalMarker
    IntervalMarker intervalMarker = new IntervalMarker(rtRange.lowerEndpoint(),
        rtRange.upperEndpoint());
    intervalMarker.setPaint(color);
    intervalMarker.setLabelTextAnchor(TextAnchor.TOP_RIGHT);

    // Add the interval marker to the plot
    plot.addDomainMarker(intervalMarker, Layer.BACKGROUND);
  }

  public BooleanProperty showRtWidthsProperty() {
    return showRtWidths;
  }
}
