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
package io.github.mzmine.modules.visualization.spectra.spectra_stack.pseudospectra;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.scans.ScanUtils;
import java.awt.Color;
import java.text.NumberFormat;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.xy.XYSeries;

public class PseudoSpectrum {

  public static PseudoSpectrumDataSet createDataSet(FeatureListRow[] group, RawDataFile raw,
      boolean sum) {
    // data
    PseudoSpectrumDataSet series = new PseudoSpectrumDataSet(true, "pseudo");
    // add all isotopes as a second series:
    XYSeries isoSeries = new XYSeries("Isotopes", true);
    // raw isotopes in a different color
    XYSeries rawIsoSeries = new XYSeries("Raw isotope pattern", true);
    // for each row
    for (FeatureListRow row : group) {
      String annotation = null;
      // sum -> heighest peak
      if (sum) {
        series.addDP(row.getAverageMZ(), row.getBestFeature().getHeight(), annotation);
      } else {
        Feature f = raw == null ? row.getBestFeature() : row.getFeature(raw);
        if (f != null) {
          series.addDP(f.getMZ(), f.getHeight(), null);
        }
      }
      // add isotopes
      IsotopePattern pattern = row.getBestIsotopePattern();
      if (pattern != null) {
        for (DataPoint dp : ScanUtils.extractDataPoints(pattern)) {
          isoSeries.add(dp.getMZ(), dp.getIntensity());
        }
      }
    }
    series.addSeries(isoSeries);
    series.addSeries(rawIsoSeries);
    return series;
  }

  public static EChartViewer createChartViewer(FeatureListRow[] group, RawDataFile raw, boolean sum,
      String title) {
    PseudoSpectrumDataSet data = createDataSet(group, raw, sum);
    if (data == null) {
      return null;
    }
    JFreeChart chart = createChart(data, raw, sum, title);
    if (chart != null) {
      EChartViewer pn = new EChartViewer(chart);
      XYItemRenderer renderer = chart.getXYPlot().getRenderer();
      PseudoSpectraItemLabelGenerator labelGenerator = new PseudoSpectraItemLabelGenerator(pn);
      renderer.setDefaultItemLabelsVisible(true);
      renderer.setDefaultItemLabelPaint(Color.BLACK);
      renderer.setSeriesItemLabelGenerator(0, labelGenerator);
      return pn;
    }

    return null;
  }

  public static JFreeChart createChart(PseudoSpectrumDataSet dataset, RawDataFile raw, boolean sum,
      String title) {
    //
    JFreeChart chart = ChartFactory.createXYLineChart(title, // title
        "m/z", // x-axis label
        "Intensity", // y-axis label
        dataset, // data set
        PlotOrientation.VERTICAL, // orientation
        true, // isotopeFlag, // create legend?
        true, // generate tooltips?
        false // generate URLs?
    );
    chart.setBackgroundPaint(Color.white);
    chart.getTitle().setVisible(false);
    // set the plot properties
    XYPlot plot = chart.getXYPlot();
    plot.setBackgroundPaint(Color.white);
    plot.setAxisOffset(RectangleInsets.ZERO_INSETS);

    // set rendering order
    plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

    // set crosshair (selection) properties
    plot.setDomainCrosshairVisible(false);
    plot.setRangeCrosshairVisible(false);

    NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();
    NumberFormat intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();

    // set the X axis (retention time) properties
    NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
    xAxis.setNumberFormatOverride(mzFormat);
    xAxis.setUpperMargin(0.08);
    xAxis.setLowerMargin(0.00);
    xAxis.setTickLabelInsets(new RectangleInsets(0, 0, 20, 20));
    xAxis.setAutoRangeIncludesZero(true);
    xAxis.setMinorTickCount(5);

    // set the Y axis (intensity) properties
    NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
    yAxis.setNumberFormatOverride(intensityFormat);
    yAxis.setUpperMargin(0.20);

    PseudoSpectraRenderer renderer = new PseudoSpectraRenderer(Color.BLACK, false);
    plot.setRenderer(0, renderer);
    plot.setRenderer(1, renderer);
    plot.setRenderer(2, renderer);
    renderer.setSeriesVisibleInLegend(1, false);
    renderer.setSeriesPaint(2, Color.ORANGE);
    //
    return chart;
  }
}
