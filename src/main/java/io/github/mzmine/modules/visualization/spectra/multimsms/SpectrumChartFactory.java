/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */
package io.github.mzmine.modules.visualization.spectra.multimsms;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.msms.DDAMsMsInfo;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.spectra.multimsms.pseudospectra.PseudoSpectraItemLabelGenerator;
import io.github.mzmine.modules.visualization.spectra.multimsms.pseudospectra.PseudoSpectraRenderer;
import io.github.mzmine.modules.visualization.spectra.multimsms.pseudospectra.PseudoSpectrumDataSet;
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

  public static EChartViewer createScanChartViewer(Scan scan, boolean showTitle, boolean showLegend) {
    if (scan == null)
      return null;
    PseudoSpectrumDataSet dataset = MirrorChartFactory.createMSMSDataSet(scan, "");
    double precursorMz = scan.getMsMsInfo() instanceof DDAMsMsInfo info ? info.getIsolationMz() : 0d;
    JFreeChart chart =
        createChart(dataset, showTitle, showLegend, scan.getRetentionTime(), precursorMz);
    return createChartViewer(chart);
  }

  public static EChartViewer createChartViewer(JFreeChart chart) {
    if (chart == null)
      return null;
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
    if (dataset == null)
      return null;
    //
    NumberFormat mzForm = MZmineCore.getConfiguration().getMZFormat();
    NumberFormat rtForm = MZmineCore.getConfiguration().getRTFormat();
    NumberFormat intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();

    String title = "";
    if (precursorMZ == 0)
      title = "RT=" + mzForm.format(precursorMZ);
    else
      title = MessageFormat.format("MSMS for m/z={0} RT={1}", mzForm.format(precursorMZ),
          rtForm.format(rt));

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
    chart.getTitle().setVisible(showTitle);
    chart.getLegend().setVisible(showLegend);
    //
    if (precursorMZ != 0)
      addPrecursorMarker(chart, precursorMZ);
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
