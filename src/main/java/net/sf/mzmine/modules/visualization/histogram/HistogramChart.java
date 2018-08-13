/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.visualization.histogram;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.text.NumberFormat;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.SeriesRenderingOrder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.ClusteredXYBarRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.RectangleInsets;
import net.sf.mzmine.chartbasics.EChartPanel;
import net.sf.mzmine.chartbasics.listener.ZoomHistory;
import net.sf.mzmine.main.MZmineCore;

public class HistogramChart extends EChartPanel {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  // grid color
  private static final Color gridColor = Color.lightGray;

  // titles
  private static final Font titleFont = new Font("SansSerif", Font.BOLD, 12);
  private static final Font subTitleFont = new Font("SansSerif", Font.PLAIN, 11);
  private TextTitle chartTitle, chartSubTitle;

  // legend
  private static final Font legendFont = new Font("SansSerif", Font.PLAIN, 11);

  // margin
  private static final double marginSize = 0.05;

  private JFreeChart chart;
  private XYPlot plot;
  private HistogramPlotDataset dataSet;

  public HistogramChart() {
    super(null, true);

    // initialize the chart by default time series chart from factory
    chart = ChartFactory.createHistogram("", // title
        "", // x-axis label
        "", // y-axis label
        null, // data set
        PlotOrientation.VERTICAL, // orientation
        true, // create legend
        false, // generate tooltips
        false // generate URLs
    );

    // title
    chartTitle = chart.getTitle();
    chartTitle.setFont(titleFont);
    chartTitle.setMargin(5, 0, 0, 0);

    chartSubTitle = new TextTitle();
    chartSubTitle.setFont(subTitleFont);
    chartSubTitle.setMargin(5, 0, 0, 0);
    chart.addSubtitle(chartSubTitle);

    // legend constructed by ChartFactory
    LegendTitle legend = chart.getLegend();
    legend.setItemFont(legendFont);
    legend.setFrame(BlockBorder.NONE);

    chart.setBackgroundPaint(Color.white);
    setChart(chart);

    // disable maximum size (we don't want scaling)
    setMaximumDrawWidth(Integer.MAX_VALUE);
    setMaximumDrawHeight(Integer.MAX_VALUE);

    // set the plot properties
    plot = chart.getXYPlot();
    plot.setBackgroundPaint(Color.white);
    plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
    plot.setDatasetRenderingOrder(DatasetRenderingOrder.REVERSE);
    plot.setSeriesRenderingOrder(SeriesRenderingOrder.FORWARD);

    // set grid properties
    plot.setDomainGridlinePaint(gridColor);
    plot.setRangeGridlinePaint(gridColor);

    // set crosshair (selection) properties
    plot.setDomainCrosshairVisible(false);
    plot.setRangeCrosshairVisible(true);

    // set the logarithmic axis
    NumberAxis axisDomain = new HistogramDomainAxis();
    axisDomain.setMinorTickCount(1);
    axisDomain.setAutoRange(true);

    NumberAxis axisRange = new NumberAxis();
    axisRange.setMinorTickCount(1);
    axisRange.setAutoRange(true);

    plot.setDomainAxis(axisDomain);
    plot.setRangeAxis(axisRange);

    ClusteredXYBarRenderer renderer = new ClusteredXYBarRenderer();
    renderer.setMargin(marginSize);
    renderer.setShadowVisible(false);
    plot.setRenderer(renderer);

    this.setMinimumSize(new Dimension(400, 400));
    this.setDismissDelay(Integer.MAX_VALUE);
    this.setInitialDelay(0);


    // reset zoom history
    ZoomHistory history = getZoomHistory();
    if (history != null)
      history.clear();
  }

  synchronized public void addDataset(HistogramPlotDataset newSet, HistogramDataType dataType) {
    dataSet = newSet;
    setAxisNumberFormat(dataType);

    double lower = dataSet.getMinimum();
    double upper = dataSet.getMaximum();

    HistogramDomainAxis axis = (HistogramDomainAxis) plot.getDomainAxis();
    axis.setAutoRange(true);
    axis.setAutoRangeIncludesZero(false);
    axis.setLabel(dataType.toString());
    axis.setRange(lower, upper);
    axis.setLowerTickValue(lower);
    axis.setUpperTickValue(upper);
    axis.setVisibleTickCount(dataSet.getNumberOfBins() + 1);
    axis.setAutoTickUnitSelection(false);
    axis.setTickUnit(new NumberTickUnit(dataSet.getBinWidth()));

    if (dataSet.getItemCount(0) > 6)
      axis.setVerticalTickLabels(true);

    plot.getRangeAxis().setLabel("Number of peaks");
    plot.setDataset(0, newSet);
    setTitle(dataSet.getPeakList().getName(), "Histogram of peaks's " + dataType);
  }

  public void setAxisNumberFormat(HistogramDataType dataType) {

    NumberFormat formatter = null;
    switch (dataType) {
      case AREA:
        formatter = MZmineCore.getConfiguration().getIntensityFormat();
        break;
      case MASS:
        formatter = MZmineCore.getConfiguration().getMZFormat();
        break;
      case HEIGHT:
        formatter = MZmineCore.getConfiguration().getIntensityFormat();
        break;
      case RT:
        formatter = MZmineCore.getConfiguration().getRTFormat();
        break;
    }
    ((NumberAxis) plot.getDomainAxis()).setNumberFormatOverride(formatter);

  }

  void setTitle(String titleText, String subTitleText) {
    chartTitle.setText(titleText);
    chartSubTitle.setText(subTitleText);
  }

  XYPlot getXYPlot() {
    return plot;
  }

}
