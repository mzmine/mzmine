/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */
package net.sf.mzmine.modules.visualization.spectra.multimsms;

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
import net.sf.mzmine.chartbasics.gui.swing.EChartPanel;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.spectra.multimsms.pseudospectra.PseudoSpectraItemLabelGenerator;
import net.sf.mzmine.modules.visualization.spectra.multimsms.pseudospectra.PseudoSpectraRenderer;
import net.sf.mzmine.modules.visualization.spectra.multimsms.pseudospectra.PseudoSpectrumDataSet;

public class SpectrumChartFactory {

  public static Scan getMSMSScan(PeakListRow row, RawDataFile raw, boolean alwaysShowBest,
      boolean useBestForMissingRaw) {
    Scan scan = null;
    if (alwaysShowBest || raw == null) {
      scan = row.getBestFragmentation();
    } else if (raw != null) {
      Feature peak = row.getPeak(raw);
      if (peak != null)
        scan = raw.getScan(peak.getMostIntenseFragmentScanNumber());
    }
    if (scan == null && useBestForMissingRaw)
      scan = row.getBestFragmentation();
    return scan;
  }

  public static PseudoSpectrumDataSet createMSMSDataSet(Scan scan) {
    NumberFormat mzForm = MZmineCore.getConfiguration().getMZFormat();
    NumberFormat rtForm = MZmineCore.getConfiguration().getRTFormat();

    if (scan != null) {
      // data
      PseudoSpectrumDataSet series =
          new PseudoSpectrumDataSet(true, MessageFormat.format("MSMS for m/z={0} RT={1}",
              mzForm.format(scan.getPrecursorMZ()), rtForm.format(scan.getRetentionTime())));
      // for each row
      for (DataPoint dp : scan.getDataPoints()) {
        series.addDP(dp.getMZ(), dp.getIntensity(), null);
      }
      return series;
    } else
      return null;
  }

  /**
   * Two scans as a mirror comparison
   * 
   * @param scan
   * @param mirror gets reflected by *-1
   * @return
   */
  public static PseudoSpectrumDataSet createMirrorDataSet(Scan scan, Scan mirror) {
    NumberFormat mzForm = MZmineCore.getConfiguration().getMZFormat();
    NumberFormat rtForm = MZmineCore.getConfiguration().getRTFormat();

    if (scan != null && mirror != null) {
      String label1 = MessageFormat.format("MSMS for m/z={0} RT={1}",
          mzForm.format(scan.getPrecursorMZ()), rtForm.format(scan.getRetentionTime()));
      String label2 = MessageFormat.format("MSMS for m/z={0} RT={1}",
          mzForm.format(mirror.getPrecursorMZ()), rtForm.format(mirror.getRetentionTime()));
      // data
      PseudoSpectrumDataSet data = new PseudoSpectrumDataSet(true, label1, label2);
      // for each row
      for (DataPoint dp : scan.getDataPoints())
        data.addDP(0, dp.getMZ(), dp.getIntensity(), null);
      for (DataPoint dp : mirror.getDataPoints())
        data.addDP(1, dp.getMZ(), -dp.getIntensity(), null);

      return data;
    } else
      return null;
  }


  /**
   * Also adds the label gen
   * 
   * @param row
   * @param raw
   * @param showTitle
   * @param showLegend
   * @return
   */
  public static EChartPanel createMirrorChartPanel(Scan scan, Scan mirror, boolean showTitle,
      boolean showLegend) {
    if (scan == null || mirror == null)
      return null;
    PseudoSpectrumDataSet dataset = createMirrorDataSet(scan, mirror);
    JFreeChart chart = createChart(dataset, showTitle, showLegend, scan.getRetentionTime(), 0);
    return createChartPanel(chart);
  }

  public static EChartPanel createScanChartPanel(Scan scan, boolean showTitle, boolean showLegend) {
    if (scan == null)
      return null;
    PseudoSpectrumDataSet dataset = createMSMSDataSet(scan);
    JFreeChart chart =
        createChart(dataset, showTitle, showLegend, scan.getRetentionTime(), scan.getPrecursorMZ());
    return createChartPanel(chart);
  }

  public static EChartPanel createChartPanel(JFreeChart chart) {
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
  }

  /**
   * Also adds the label gen
   * 
   * @param row
   * @param raw
   * @param showTitle
   * @param showLegend
   * @param useBestForMissingRaw
   * @param alwaysShowBest
   * @return
   */
  public static EChartPanel createMSMSChartPanel(PeakListRow row, RawDataFile raw,
      boolean showTitle, boolean showLegend, boolean alwaysShowBest, boolean useBestForMissingRaw) {
    Scan scan = getMSMSScan(row, raw, alwaysShowBest, useBestForMissingRaw);
    if (scan == null)
      return null;
    return createScanChartPanel(scan, showTitle, showLegend);
  }

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
   * @param scan
   */
  private static void addPrecursorMarker(JFreeChart chart, double precursorMZ) {
    chart.getXYPlot()
        .addDomainMarker(new ValueMarker(precursorMZ, Color.ORANGE, new BasicStroke(1.5f)));
  }
}
