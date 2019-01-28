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
 */package net.sf.mzmine.modules.visualization.multimsms.pseudospectra;

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
import net.sf.mzmine.chartbasics.gui.swing.EChartPanel;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.main.MZmineCore;

public class PseudoSpectrum {

  public static PseudoSpectrumDataSet createDataSet(PeakListRow[] group, RawDataFile raw,
      boolean sum) {
    // data
    PseudoSpectrumDataSet series = new PseudoSpectrumDataSet(true, "pseudo");
    // add all isotopes as a second series:
    XYSeries isoSeries = new XYSeries("Isotopes", true);
    // raw isotopes in a different color
    XYSeries rawIsoSeries = new XYSeries("Raw isotope pattern", true);
    // for each row
    for (PeakListRow row : group) {
      String annotation = null;
      // sum -> heighest peak
      if (sum)
        series.addDP(row.getAverageMZ(), row.getBestPeak().getHeight(), annotation);
      else {
        Feature f = raw == null ? row.getBestPeak() : row.getPeak(raw);
        if (f != null)
          series.addDP(f.getMZ(), f.getHeight(), null);
      }
      // add isotopes
      IsotopePattern pattern = row.getBestIsotopePattern();
      if (pattern != null) {
        for (DataPoint dp : pattern.getDataPoints())
          isoSeries.add(dp.getMZ(), dp.getIntensity());
      }
    }
    series.addSeries(isoSeries);
    series.addSeries(rawIsoSeries);
    return series;
  }

  public static EChartPanel createChartPanel(PeakListRow[] group, RawDataFile raw, boolean sum,
      String title) {
    PseudoSpectrumDataSet data = createDataSet(group, raw, sum);
    if (data == null)
      return null;
    JFreeChart chart = createChart(data, raw, sum, title);
    if (chart != null) {
      EChartPanel pn = new EChartPanel(chart);
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
