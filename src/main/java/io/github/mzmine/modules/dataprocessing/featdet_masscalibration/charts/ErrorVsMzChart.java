/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.featdet_masscalibration.charts;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ui.Layer;
import org.jfree.data.general.DatasetUtils;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import com.google.common.collect.Range;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.modules.dataprocessing.featdet_masscalibration.MassPeakMatch;
import io.github.mzmine.modules.dataprocessing.featdet_masscalibration.errormodeling.DistributionRange;

/**
 * Chart for error size vs mz ratio plots (xy scatter plot of error values vs mz ratio) with
 * additional extraction ranges and bias estimates lines shown up plus estimated trend
 */
public class ErrorVsMzChart extends EChartViewer {

  class ErrorVsMzTooltipGenerator implements XYToolTipGenerator {
    @Override
    public String generateToolTip(XYDataset dataset, int series, int item) {
      return ChartUtils.generateTooltipText(matches, item);
    }
  }

  private final XYPlot plot;

  protected List<MassPeakMatch> matches;
  protected ArrayList<ValueMarker> rangeMarkers = new ArrayList<>();
  protected XYTextAnnotation trendNameAnnotation;

  protected ErrorVsMzChart(JFreeChart chart) {
    super(chart);
    this.plot = chart.getXYPlot();
    plot.getRenderer(0).setDefaultToolTipGenerator(new ErrorVsMzTooltipGenerator());
  }

  public ErrorVsMzChart(String title) {
    this(createEmptyChart(title));
  }

  public ErrorVsMzChart() {
    this("Error size vs mz ratio");
  }

  public static JFreeChart createEmptyChart(String title) {
    NumberAxis xAxis = new NumberAxis("Measured m/z ratio");
    NumberAxis yAxis = new NumberAxis("PPM error");
    XYPlot plot = new XYPlot(null, xAxis, yAxis, null);
    plot.setRangeAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
    plot.setDomainCrosshairVisible(false);
    plot.setRangeCrosshairVisible(false);

    plot.setRenderer(0, ChartUtils.createErrorsRenderer());
    plot.setRenderer(1, ChartUtils.createTrendRenderer());
    plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

    // JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, true);
    JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, false);
    chart.setTitle((String) null);
    return chart;
  }

  public void cleanPlot() {
    ChartUtils.cleanPlot(plot);
  }

  public void cleanPlotLabels() {
    ChartUtils.cleanPlotLabels(plot);
  }

  public void updatePlot(List<MassPeakMatch> matches, Map<String, DistributionRange> errorRanges,
      double biasEstimate, Trend2D errorVsMzTrend) {
    // this.matches = matches;
    // updateChartDataset(matches, errorVsMzTrend);
    this.matches = new ArrayList<>(matches);
    Collections.sort(this.matches, MassPeakMatch.measuredMzComparator);
    rangeMarkers.clear();
    trendNameAnnotation = null;
    updateChartDataset(this.matches, errorVsMzTrend);

    for (String label : errorRanges.keySet()) {
      DistributionRange errorRange = errorRanges.get(label);
      Range<Double> errorValueRange = errorRange.getValueRange();

      ValueMarker valueMarkerLower =
          ChartUtils.createValueMarker(label + " lower", errorValueRange.lowerEndpoint());
      ValueMarker valueMarkerUpper =
          ChartUtils.createValueMarker(label + " upper", errorValueRange.upperEndpoint());
      rangeMarkers.add(valueMarkerLower);
      rangeMarkers.add(valueMarkerUpper);
    }
    rangeMarkers.add(ChartUtils.createValueMarker("Bias estimate", biasEstimate));
  }

  public void displayPlotLabels(boolean display) {
    if (display) {
      for (ValueMarker valueMarker : rangeMarkers) {
        plot.addRangeMarker(0, valueMarker, Layer.FOREGROUND, true);
      }
      if (trendNameAnnotation != null) {
        plot.addAnnotation(trendNameAnnotation, true);
      }
    }
  }

  protected void updateChartDataset(List<MassPeakMatch> matches, Trend2D trend) {
    XYSeries errorsXY = new XYSeries("PPM errors");
    for (MassPeakMatch match : matches) {
      errorsXY.add(match.getMeasuredMzRatio(), match.getMzError());
    }

    XYSeriesCollection dataset = new XYSeriesCollection(errorsXY);
    plot.setDataset(0, dataset);

    if (trend != null) {
      XYSeries trendSeries =
          DatasetUtils.sampleFunction2DToSeries(trend, dataset.getDomainLowerBound(false),
              dataset.getDomainUpperBound(false), 1000, "trend series");
      XYSeriesCollection trendDataset = new XYSeriesCollection(trendSeries);
      plot.setDataset(1, trendDataset);
      trendNameAnnotation = new XYTextAnnotation("Trend: " + trend.getName(),
          plot.getDomainAxis().getRange().getCentralValue(),
          plot.getRangeAxis().getLowerBound() + plot.getRangeAxis().getRange().getLength() / 10);
    }
  }
}
