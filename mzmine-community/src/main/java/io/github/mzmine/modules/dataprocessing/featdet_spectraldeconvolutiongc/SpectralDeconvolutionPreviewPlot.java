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
package io.github.mzmine.modules.dataprocessing.featdet_spectraldeconvolutiongc;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.List;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
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

  public SpectralDeconvolutionPreviewPlot(String title, String xAxisLabel, String yAxisLabel) {
    super(null);
    XYSeriesCollection dataset = new XYSeriesCollection();
    JFreeChart chart = ChartFactory.createScatterPlot(title, xAxisLabel, yAxisLabel, dataset,
        PlotOrientation.VERTICAL, false, true, false);

    plot = chart.getXYPlot();
    plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

    setMinHeight(300);

    NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
    xAxis.setAutoRangeIncludesZero(false);

    NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
    yAxis.setAutoRangeIncludesZero(false);

    setChart(chart);
    datasetIndex = 0;
    renderer = new XYLineAndShapeRenderer(false, true);
    plot.setRenderer(renderer);
  }

  public void addDataset(List<ModularFeature> features, XYSeries series, Color color) {
    XYSeriesCollection dataset = new XYSeriesCollection();
    dataset.addSeries(series);
    plot.setDataset(datasetIndex, dataset);

    renderer.setSeriesPaint(datasetIndex, color);
    renderer.setSeriesShape(datasetIndex, new Rectangle(0, 0, 1, 1));
    plot.setRenderer(datasetIndex, renderer);
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

      // Add the annotation to the plot
      plot.addAnnotation(annotation);
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

}
