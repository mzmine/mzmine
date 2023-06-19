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
package io.github.mzmine.modules.visualization.spectra.spectra_stack;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.msms.DDAMsMsInfo;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.spectra.spectra_stack.pseudospectra.PseudoSpectraItemLabelGenerator;
import io.github.mzmine.modules.visualization.spectra.spectra_stack.pseudospectra.PseudoSpectraRenderer;
import io.github.mzmine.modules.visualization.spectra.spectra_stack.pseudospectra.PseudoSpectrumDataSet;
import io.github.mzmine.util.MirrorChartFactory;
import java.awt.BasicStroke;
import java.awt.Color;
import java.text.MessageFormat;
import java.text.NumberFormat;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.ui.RectangleInsets;

public class SpectrumChartFactory {

  /*public static EChartPanel createScanChartPanel(Scan scan, boolean showTitle, boolean showLegend) {
    if (scan == null)
      return null;
    PseudoSpectrumDataSet dataset = MirrorChartFactory.createMSMSDataSet(scan, "");
    JFreeChart chart =
        createChart(dataset, showTitle, showLegend, scan.getRetentionTime(), scan.getPrecursorMZ());
    return createChartPanel(chart);
  }*/

  public static EChartViewer createScanChartViewer(Scan scan, boolean showTitle,
      boolean showLegend) {
    if (scan == null) {
      return null;
    }
    PseudoSpectrumDataSet dataset = MirrorChartFactory.createMSMSDataSet(scan, "");
    double precursorMz =
        scan.getMsMsInfo() instanceof DDAMsMsInfo info ? info.getIsolationMz() : 0d;
    JFreeChart chart = createChart(dataset, showTitle, showLegend, scan.getRetentionTime(),
        precursorMz);
    return createChartViewer(chart);
  }

  public static EChartViewer createChartViewer(JFreeChart chart) {
    if (chart == null) {
      return null;
    }
    //
    EChartViewer pn = new EChartViewer(chart);
    XYItemRenderer renderer = chart.getXYPlot().getRenderer();
    PseudoSpectraItemLabelGenerator labelGenerator = new PseudoSpectraItemLabelGenerator(pn);
    renderer.setDefaultItemLabelsVisible(true);
    renderer.setDefaultItemLabelPaint(Color.BLACK);
    renderer.setSeriesItemLabelGenerator(0, labelGenerator);
    return pn;
  }

  /*public static EChartPanel createChartPanel(JFreeChart chart) {
    if (chart == null)
      return null;
    //
    EChartPanel pn = new EChartPanel(chart);
    XYItemRenderer renderer = chart.getXYPlot().getRenderer();
    PseudoSpectraItemLabelGenerator labelGenerator = new PseudoSpectraItemLabelGenerator(pn);
    renderer.setDefaultItemLabelsVisible(true);
    renderer.setDefaultItemLabelPaint(Color.BLACK);
    renderer.setSeriesItemLabelGenerator(0, labelGenerator);
    return pn;
  }*/

  public static JFreeChart createChart(PseudoSpectrumDataSet dataset, boolean showTitle,
      boolean showLegend, double rt, double precursorMZ) {
    //
    if (dataset == null) {
      return null;
    }
    //
    NumberFormat mzForm = MZmineCore.getConfiguration().getMZFormat();
    NumberFormat rtForm = MZmineCore.getConfiguration().getRTFormat();
    NumberFormat intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();

    String title = "";
    if (precursorMZ == 0) {
      title = "RT=" + mzForm.format(precursorMZ);
    } else {
      title = MessageFormat.format("MSMS for m/z={0} RT={1}", mzForm.format(precursorMZ),
          rtForm.format(rt));
    }

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

    // set the X axis (retention time) properties
    NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
    xAxis.setNumberFormatOverride(mzForm);
    xAxis.setUpperMargin(0.08);
    xAxis.setLowerMargin(0.00);
//    xAxis.setTickLabelInsets(new RectangleInsets(0, 0, 20, 20));
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
    chart.getTitle().setVisible(showTitle);
    chart.getLegend().setVisible(showLegend);
    //
    if (precursorMZ != 0) {
      addPrecursorMarker(chart, precursorMZ);
    }
    return chart;
  }

  /**
   * Precursor mass marker
   *
   * @param chart
   * @param precursorMZ
   */
  private static void addPrecursorMarker(JFreeChart chart, double precursorMZ) {
    chart.getXYPlot()
        .addDomainMarker(new ValueMarker(precursorMZ, Color.ORANGE, new BasicStroke(2f)));
  }
}
