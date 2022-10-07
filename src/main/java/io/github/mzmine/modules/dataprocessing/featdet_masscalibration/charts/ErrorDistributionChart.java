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


import com.google.common.collect.Range;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.modules.dataprocessing.featdet_masscalibration.MassPeakMatch;
import io.github.mzmine.modules.dataprocessing.featdet_masscalibration.errormodeling.DistributionRange;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.event.PlotChangeEvent;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ui.Layer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Chart for error distribution (xy scatter plot of error values vs match number, sorted by error size)
 * with additional extraction ranges and bias estimates lines shown up
 */
public class ErrorDistributionChart extends EChartViewer {

  class ErrorDistributionTooltipGenerator implements XYToolTipGenerator {
    @Override
    public String generateToolTip(XYDataset dataset, int series, int item) {
      return ChartUtils.generateTooltipText(matches, item);
    }
  }

  private final JFreeChart distributionChart;

  protected List<MassPeakMatch> matches;
  protected ArrayList<ValueMarker> rangeMarkers = new ArrayList<>();

  protected ErrorDistributionChart(JFreeChart chart) {
    super(chart);
    distributionChart = chart;
    distributionChart.getXYPlot().getRenderer(0).setDefaultToolTipGenerator(new ErrorDistributionTooltipGenerator());
  }

  public ErrorDistributionChart(String title) {
    this(createEmptyDistributionChart(title));
  }

  public ErrorDistributionChart() {
    this("Error distribution");
  }

  public static JFreeChart createEmptyDistributionChart(String title) {
    NumberAxis xAxis = new NumberAxis("Match number");
    NumberAxis yAxis = new NumberAxis("PPM error");
    XYPlot plot = new XYPlot(null, xAxis, yAxis, ChartUtils.createErrorsRenderer());
    plot.setRangeAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
    plot.setDomainCrosshairVisible(false);
    plot.setRangeCrosshairVisible(false);

//    JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, true);
    JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, false);
    chart.setTitle((String) null);
    return chart;
  }

  public void cleanDistributionPlot() {
    XYPlot distributionPlot = distributionChart.getXYPlot();
    ChartUtils.cleanPlot(distributionPlot);
  }

  public void cleanPlotLabels() {
    ChartUtils.cleanPlotLabels(distributionChart.getXYPlot());
  }

  public void updateDistributionPlot(List<MassPeakMatch> matches, List<Double> errors,
                                     Map<String, DistributionRange> errorRanges, double biasEstimate) {
    XYPlot distributionPlot = distributionChart.getXYPlot();

    this.matches = new ArrayList<>(matches);
    Collections.sort(this.matches, MassPeakMatch.mzErrorComparator);
    rangeMarkers.clear();

    XYDataset dataset = createDistributionDataset(errors);
    distributionPlot.setDataset(dataset);

    for (String label : errorRanges.keySet()) {
      DistributionRange errorRange = errorRanges.get(label);
      Range<Double> errorValueRange = errorRange.getValueRange();

      ValueMarker valueMarkerLower = ChartUtils.createValueMarker(label + " lower", errorValueRange.lowerEndpoint());
      ValueMarker valueMarkerUpper = ChartUtils.createValueMarker(label + " upper", errorValueRange.upperEndpoint());
      rangeMarkers.add(valueMarkerLower);
      rangeMarkers.add(valueMarkerUpper);
    }
    rangeMarkers.add(ChartUtils.createValueMarker("Bias estimate", biasEstimate));
  }

  public void displayPlotLabels(boolean display) {
    cleanPlotLabels();
    if (display) {
      for (ValueMarker valueMarker : rangeMarkers) {
//        distributionChart.getXYPlot().addRangeMarker(valueMarker);
        distributionChart.getXYPlot().addRangeMarker(0, valueMarker, Layer.FOREGROUND, false);
      }
      distributionChart.getXYPlot().notifyListeners(new PlotChangeEvent(distributionChart.getXYPlot()));
    }
  }

  protected XYDataset createDistributionDataset(List<Double> errors) {
    XYSeries errorsXY = new XYSeries("PPM errors");
    for (int i = 0; i < errors.size(); i++) {
      errorsXY.add(i + 1, errors.get(i));
    }

    return new XYSeriesCollection(errorsXY);
  }
}
