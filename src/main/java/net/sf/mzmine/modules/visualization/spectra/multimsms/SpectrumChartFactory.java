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
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
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

  public static PseudoSpectrumDataSet createMSMSDataSet(Scan scan, String label) {
    if (scan != null) {
      return createMSMSDataSet(scan.getPrecursorMZ(), scan.getRetentionTime(), scan.getDataPoints(),
          label);
    } else
      return null;
  }

  public static PseudoSpectrumDataSet createMSMSDataSet(double precursorMZ, double rt,
      DataPoint[] dps, String label) {
    NumberFormat mzForm = MZmineCore.getConfiguration().getMZFormat();
    NumberFormat rtForm = MZmineCore.getConfiguration().getRTFormat();

    if (label == null)
      label = "";
    else if (!label.isEmpty())
      label = " (" + label + ")";
    // data
    PseudoSpectrumDataSet series =
        new PseudoSpectrumDataSet(true, MessageFormat.format("MSMS for m/z={0} RT={1}{2}",
            mzForm.format(precursorMZ), rtForm.format(rt), label));
    // for each row
    for (DataPoint dp : dps) {
      series.addDP(dp.getMZ(), dp.getIntensity(), null);
    }
    return series;
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
  public static EChartPanel createMirrorChartPanel(Scan scan, Scan mirror, String labelA,
      String labelB, boolean showTitle, boolean showLegend) {
    if (scan == null || mirror == null)
      return null;

    return createMirrorChartPanel(labelA, scan.getPrecursorMZ(), scan.getRetentionTime(),
        scan.getDataPoints(), labelB, mirror.getPrecursorMZ(), mirror.getRetentionTime(),
        mirror.getDataPoints(), showTitle, showLegend);
  }

  public static EChartPanel createMirrorChartPanel(String labelA, double precursorMZA, double rtA,
      DataPoint[] dpsA, String labelB, double precursorMZB, double rtB, DataPoint[] dpsB,
      boolean showTitle, boolean showLegend) {
    PseudoSpectrumDataSet data =
        dpsA == null ? null : createMSMSDataSet(precursorMZA, rtA, dpsA, labelA);
    PseudoSpectrumDataSet dataMirror =
        dpsB == null ? null : createMSMSDataSet(precursorMZB, rtB, dpsB, labelB);

    NumberFormat mzForm = MZmineCore.getConfiguration().getMZFormat();
    NumberFormat intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();

    // set the X axis (retention time) properties
    NumberAxis xAxis = new NumberAxis("m/z");
    xAxis.setNumberFormatOverride(mzForm);
    xAxis.setUpperMargin(0.08);
    xAxis.setLowerMargin(0.00);
    xAxis.setTickLabelInsets(new RectangleInsets(0, 0, 20, 20));
    xAxis.setAutoRangeIncludesZero(false);
    xAxis.setMinorTickCount(5);

    PseudoSpectraRenderer renderer1 = new PseudoSpectraRenderer(Color.BLACK, false);
    PseudoSpectraRenderer renderer2 = new PseudoSpectraRenderer(Color.BLACK, false);

    // create subplot 1...
    final NumberAxis rangeAxis1 = new NumberAxis("Intensity");
    final XYPlot subplot1 = new XYPlot(data, null, rangeAxis1, renderer1);
    subplot1.setRangeAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
    rangeAxis1.setNumberFormatOverride(intensityFormat);
    rangeAxis1.setUpperMargin(0.20);
    rangeAxis1.setAutoRangeIncludesZero(true);
    rangeAxis1.setAutoRangeStickyZero(true);

    // create subplot 2...
    final NumberAxis rangeAxis2 = new NumberAxis("Intensity");
    rangeAxis2.setNumberFormatOverride(intensityFormat);
    rangeAxis2.setUpperMargin(0.20);
    rangeAxis2.setAutoRangeIncludesZero(true);
    rangeAxis2.setAutoRangeStickyZero(true);
    rangeAxis2.setInverted(true);
    final XYPlot subplot2 = new XYPlot(dataMirror, null, rangeAxis2, renderer2);
    subplot2.setRangeAxisLocation(AxisLocation.TOP_OR_LEFT);

    // parent plot...
    final CombinedDomainXYPlot plot = new CombinedDomainXYPlot(new NumberAxis("Domain"));
    plot.setGap(0);

    // add the subplots...
    plot.add(subplot1, 1);
    plot.add(subplot2, 1);
    plot.setOrientation(PlotOrientation.VERTICAL);


    // set the plot properties
    plot.setBackgroundPaint(Color.white);
    plot.setAxisOffset(RectangleInsets.ZERO_INSETS);

    // set rendering order
    plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

    // set crosshair (selection) properties
    plot.setDomainCrosshairVisible(false);
    plot.setRangeCrosshairVisible(false);

    // return a new chart containing the overlaid plot...
    JFreeChart chart = new JFreeChart("", JFreeChart.DEFAULT_TITLE_FONT, plot, true);
    chart.setBackgroundPaint(Color.white);
    chart.getTitle().setVisible(false);

    // chart.getXYPlot().setRangeZeroBaselineVisible(true);
    chart.getTitle().setVisible(showTitle);
    chart.getLegend().setVisible(showLegend);

    return new EChartPanel(chart);
  }

  public static EChartPanel createScanChartPanel(Scan scan, boolean showTitle, boolean showLegend) {
    if (scan == null)
      return null;
    PseudoSpectrumDataSet dataset = createMSMSDataSet(scan, "");
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
