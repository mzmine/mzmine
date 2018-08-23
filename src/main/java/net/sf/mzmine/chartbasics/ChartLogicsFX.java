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

package net.sf.mzmine.chartbasics;

import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.logging.Logger;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.fx.ChartCanvas;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.plot.Zoomable;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.Range;

/**
 * Collection of methods for JFreeCharts <br>
 * Calculate mouseXY to plotXY <br>
 * Calc width and height for plots where domain and range axis share the same dimensions <br>
 * Zoom and shift axes by absolute or relative values
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class ChartLogicsFX {
  private static Logger logger = Logger.getLogger(ChartLogicsFX.class.getName());

  /**
   * Translates mouse coordinates to chart coordinates (xy-axis)
   * 
   * @param myChart
   * @param mouseX
   * @param mouseY
   * @return Range as chart coordinates
   */
  public static Point2D mouseXYToPlotXY(ChartCanvas myChart, double mouseX, double mouseY) {
    return mouseXYToPlotXY(myChart, (int) mouseX, (int) mouseY);
  }

  /**
   * Translates mouse coordinates to chart coordinates (xy-axis)
   * 
   * @param myChart
   * @param mouseX
   * @param mouseY
   * @return Range as chart coordinates
   */
  public static Point2D mouseXYToPlotXY(ChartCanvas myChart, int mouseX, int mouseY) {
    XYPlot plot = (XYPlot) myChart.getChart().getPlot();
    ChartRenderingInfo info = myChart.getRenderingInfo();
    Rectangle2D dataArea = info.getPlotInfo().getDataArea();

    ValueAxis domainAxis = plot.getDomainAxis();
    ValueAxis rangeAxis = plot.getRangeAxis();
    if (domainAxis != null && rangeAxis != null) {
      RectangleEdge domainAxisEdge = plot.getDomainAxisEdge();
      RectangleEdge rangeAxisEdge = plot.getRangeAxisEdge();
      double chartX = domainAxis.java2DToValue(mouseX, dataArea, domainAxisEdge);
      double chartY = rangeAxis.java2DToValue(mouseY, dataArea, rangeAxisEdge);

      return new Point2D.Double(chartX, chartY);
    } else
      return null;
  }

  /**
   * Translates screen (pixel) values to plot values
   * 
   * @param myChart
   * @return width in data space for x and y
   */
  public static Point2D screenValueToPlotValue(ChartCanvas myChart, int val) {
    Point2D p = mouseXYToPlotXY(myChart, 0, 0);
    Point2D p2 = mouseXYToPlotXY(myChart, val, val);
    // inverted y
    return new Point2D.Double(p2.getX() - p.getX(), p.getY() - p2.getY());
  }


  /**
   * Data width to pixel width on screen
   * 
   * @param myChart
   * @param dataWidth width of data
   * @param axis for width calculation
   * @return
   */
  public static double calcWidthOnScreen(ChartCanvas myChart, double dataWidth, ValueAxis axis,
      RectangleEdge axisEdge) {
    XYPlot plot = (XYPlot) myChart.getChart().getPlot();
    ChartRenderingInfo info = myChart.getRenderingInfo();
    Rectangle2D dataArea = info.getPlotInfo().getDataArea();

    // width 2D
    return axis.lengthToJava2D(dataWidth, dataArea, axisEdge);
  }


  /**
   * Calculates the size of a chart for a given fixed plot width Domain and Range axes need to share
   * the same unit (e.g. mm)
   * 
   * @param chart
   * @param width
   * @return
   */
  public static Dimension calcSizeForPlotWidth(ChartCanvas myChart, double plotWidth) {
    return calcSizeForPlotWidth(myChart, plotWidth, 4);
  }

  /**
   * Calculates the size of a chart for a given fixed plot width Domain and Range axes need to share
   * the same unit (e.g. mm)
   * 
   * @param chart
   * @param plotWidth
   * @return
   */
  public static Dimension calcSizeForPlotWidth(ChartCanvas myChart, double plotWidth,
      int iterations) {
    // ranges
    XYPlot plot = (XYPlot) myChart.getChart().getPlot();
    ValueAxis domainAxis = plot.getDomainAxis();
    Range x = domainAxis.getRange();
    ValueAxis rangeAxis = plot.getRangeAxis();
    Range y = rangeAxis.getRange();

    // plot height is fixed
    double plotHeight = plotWidth / x.getLength() * y.getLength();
    return calcSizeForPlotSize(myChart, plotWidth, plotHeight, iterations);
  }

  /**
   * Calculates the size of a chart for a given fixed plot width and height
   * 
   * @param chart
   * @param plotWidth
   * @return
   */
  public static Dimension calcSizeForPlotSize(ChartCanvas myChart, double plotWidth,
      double plotHeight) {
    return calcSizeForPlotSize(myChart, plotWidth, plotHeight, 4);
  }

  /**
   * Calculates the size of a chart for a given fixed plot width and height
   * 
   * @param chart
   * @param plotWidth
   * @return
   */
  public static Dimension calcSizeForPlotSize(ChartCanvas myChart, double plotWidth,
      double plotHeight, int iterations) {
    makeChartResizable(myChart);

    // estimate plotwidth / height
    double estimatedChartWidth = plotWidth + 200;
    double estimatedChartHeight = plotHeight + 200;

    double lastW = estimatedChartWidth;
    double lastH = estimatedChartHeight;

    // paint and get closer
    try {
      for (int i = 0; i < iterations; i++) {
        // paint on ghost panel with estimated height (if copy panel==true)
        myChart.setWidth((int) estimatedChartWidth);
        myChart.setHeight((int) estimatedChartHeight);
        myChart.draw();

        // rendering info
        ChartRenderingInfo info = myChart.getRenderingInfo();
        Rectangle2D dataArea = info.getPlotInfo().getDataArea();
        Rectangle2D chartArea = info.getChartArea();

        // // calc title space: will be added later to the right plot size
        // double titleWidth = chartArea.getWidth()-dataArea.getWidth();
        // double titleHeight = chartArea.getHeight()-dataArea.getHeight();

        // calc width and height
        estimatedChartWidth = estimatedChartWidth - dataArea.getWidth() + plotWidth;
        estimatedChartHeight = estimatedChartHeight - dataArea.getHeight() + plotHeight;

        if ((int) lastW == (int) estimatedChartWidth && (int) lastH == (int) estimatedChartHeight)
          break;
        else {
          lastW = estimatedChartWidth;
          lastH = estimatedChartHeight;
        }
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    return new Dimension((int) estimatedChartWidth, (int) estimatedChartHeight);
  }

  /**
   * calls this method twice (2 iterations) with an estimated chartHeight of 3*chartWidth Domain and
   * Range axes need to share the same unit (e.g. mm)
   * 
   * @param myChart
   * @param dataWidth width of data
   * @param axis for width calculation
   * @return
   */
  public static double calcHeightToWidth(ChartCanvas myChart, double chartWidth) {
    return calcHeightToWidth(myChart, chartWidth, chartWidth * 3, 4);
  }

  /**
   * calculates the correct height with multiple iterations Domain and Range axes need to share the
   * same unit (e.g. mm)
   * 
   * @param myChart
   * @param dataWidth width of data
   * @param axis for width calculation
   * @return
   */
  public static double calcHeightToWidth(ChartCanvas myChart, double chartWidth,
      double estimatedHeight, int iterations) {
    // if(myChart.getChartRenderingInfo()==null ||
    // myChart.getChartRenderingInfo().getChartArea()==null ||
    // myChart.getChartRenderingInfo().getChartArea().getWidth()==0)
    // result
    double height = estimatedHeight;
    double lastH = height;

    makeChartResizable(myChart);

    try {
      for (int i = 0; i < iterations; i++) {
        // paint on ghost panel with estimated height (if copy panel==true)
        myChart.setWidth((int) chartWidth);
        myChart.setHeight((int) estimatedHeight);
        myChart.draw();

        XYPlot plot = (XYPlot) myChart.getChart().getPlot();
        ChartRenderingInfo info = myChart.getRenderingInfo();
        Rectangle2D dataArea = info.getPlotInfo().getDataArea();
        Rectangle2D chartArea = info.getChartArea();

        // calc title space: will be added later to the right plot size
        double titleWidth = chartArea.getWidth() - dataArea.getWidth();
        double titleHeight = chartArea.getHeight() - dataArea.getHeight();

        // calc right plot size with axis dim.
        // real plot width is given by factor;
        double realPW = chartWidth - titleWidth;

        // ranges
        ValueAxis domainAxis = plot.getDomainAxis();
        org.jfree.data.Range x = domainAxis.getRange();
        ValueAxis rangeAxis = plot.getRangeAxis();
        org.jfree.data.Range y = rangeAxis.getRange();

        // real plot height can be calculated by
        double realPH = realPW / x.getLength() * y.getLength();

        // the real height
        height = realPH + titleHeight;

        // for next iteration
        estimatedHeight = height;
        if ((int) lastH == (int) height)
          break;
        else
          lastH = height;
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    return height;
  }

  /**
   * Removes draw size restrictions
   * 
   * @param myChart
   */
  public static void makeChartResizable(ChartCanvas myChart) {
    // TODO set max and min sizes
  }

  /**
   * 
   * Domain and Range axes need to share the same unit (e.g. mm)
   * 
   * @param myChart
   * @return
   */
  public static double calcWidthToHeight(ChartCanvas myChart, double chartHeight) {
    makeChartResizable(myChart);

    myChart.draw();

    XYPlot plot = (XYPlot) myChart.getChart().getPlot();
    ChartRenderingInfo info = myChart.getRenderingInfo();
    Rectangle2D dataArea = info.getPlotInfo().getDataArea();
    Rectangle2D chartArea = info.getChartArea();


    // calc title space: will be added later to the right plot size
    double titleWidth = chartArea.getWidth() - dataArea.getWidth();
    double titleHeight = chartArea.getHeight() - dataArea.getHeight();

    // calc right plot size with axis dim.
    // real plot width is given by factor;
    double realPH = chartHeight - titleHeight;

    // ranges
    ValueAxis domainAxis = plot.getDomainAxis();
    org.jfree.data.Range x = domainAxis.getRange();
    ValueAxis rangeAxis = plot.getRangeAxis();
    org.jfree.data.Range y = rangeAxis.getRange();

    // real plot height can be calculated by
    double realPW = realPH / y.getLength() * x.getLength();

    double width = realPW + titleWidth;

    return width;
  }

  /**
   * Returns dimensions for limiting factor width or height
   * 
   * @param myChart
   * @return
   */
  public static Dimension calcMaxSize(ChartCanvas myChart, double chartWidth, double chartHeight) {
    makeChartResizable(myChart);
    // paint on a ghost panel
    myChart.draw();

    XYPlot plot = (XYPlot) myChart.getChart().getPlot();
    ChartRenderingInfo info = myChart.getRenderingInfo();
    Rectangle2D dataArea = info.getPlotInfo().getDataArea();
    Rectangle2D chartArea = info.getChartArea();


    // calc title space: will be added later to the right plot size
    double titleWidth = chartArea.getWidth() - dataArea.getWidth();
    double titleHeight = chartArea.getHeight() - dataArea.getHeight();

    // calculatig width for max height

    // calc right plot size with axis dim.
    // real plot width is given by factor;
    double realPH = chartHeight - titleHeight;

    // ranges
    ValueAxis domainAxis = plot.getDomainAxis();
    org.jfree.data.Range x = domainAxis.getRange();
    ValueAxis rangeAxis = plot.getRangeAxis();
    org.jfree.data.Range y = rangeAxis.getRange();

    // real plot height can be calculated by
    double realPW = realPH / y.getLength() * x.getLength();

    double width = realPW + titleWidth;
    // if width is higher than given chartWidth then calc height for chartWidth
    if (width > chartWidth) {
      // calc right plot size with axis dim.
      // real plot width is given by factor;
      realPW = chartWidth - titleWidth;

      // real plot height can be calculated by
      realPH = realPW / x.getLength() * y.getLength();

      double height = realPH + titleHeight;
      // Return size
      return new Dimension((int) chartWidth, (int) height);
    } else {
      // Return size
      return new Dimension((int) width, (int) chartHeight);
    }
  }



  /**
   * 
   * @param myChart
   * @return Range the domainAxis zoom (X-axis)
   */
  public static Range getZoomDomainAxis(ChartCanvas myChart) {
    XYPlot plot = (XYPlot) myChart.getChart().getPlot();
    ValueAxis domainAxis = plot.getDomainAxis();

    return new Range(domainAxis.getLowerBound(), domainAxis.getUpperBound());
  }

  /**
   * Zoom into a chart panel
   * 
   * @param myChart
   * @param zoom
   * @param autoRangeY if true the range (Y) axis auto bounds will be restored
   */
  public static void setZoomDomainAxis(ChartCanvas myChart, Range zoom, boolean autoRangeY) {
    XYPlot plot = (XYPlot) myChart.getChart().getPlot();
    ValueAxis domainAxis = plot.getDomainAxis();
    setZoomAxis(domainAxis, keepRangeWithinAutoBounds(domainAxis, zoom));

    if (autoRangeY) {
      autoRangeAxis(myChart);
    }
  }

  /**
   * Zoom into a chart panel
   * 
   * @param myChart
   * @param zoom
   * @param autoRangeY if true the range (Y) axis auto bounds will be restored
   */
  public static void setZoomAxis(ValueAxis axis, Range zoom) {
    axis.setRange(zoom);
  }


  /**
   * Auto range the range axis
   * 
   * @param myChart
   * @param zoom
   * @param autoRangeY if true the range (Y) axis auto bounds will be restored
   */
  public static void autoAxes(ChartCanvas myChart) {
    XYPlot plot = (XYPlot) myChart.getChart().getPlot();
    if (plot instanceof Zoomable) {
      Zoomable z = (Zoomable) plot;
      Point2D endPoint = new Point2D.Double(0, 0);
      PlotRenderingInfo pri = myChart.getRenderingInfo().getPlotInfo();
      boolean saved = plot.isNotify();
      plot.setNotify(false);
      z.zoomDomainAxes(0, pri, endPoint);
      z.zoomRangeAxes(0, pri, endPoint);
      plot.setNotify(saved);
    }
  }

  /**
   * Auto range the range axis
   * 
   * @param myChart
   * @param zoom
   * @param autoRangeY if true the range (Y) axis auto bounds will be restored
   */
  public static void autoRangeAxis(ChartCanvas myChart) {
    XYPlot plot = (XYPlot) myChart.getChart().getPlot();
    if (plot instanceof Zoomable) {
      Zoomable z = (Zoomable) plot;
      Point2D endPoint = new Point2D.Double(0, 0);
      PlotRenderingInfo pri = myChart.getRenderingInfo().getPlotInfo();
      z.zoomRangeAxes(0, pri, endPoint);
    }
  }

  /**
   * Auto range the range axis
   * 
   * @param myChart
   * @param zoom
   * @param autoRangeY if true the range (Y) axis auto bounds will be restored
   */
  public static void autoDomainAxis(ChartCanvas myChart) {
    XYPlot plot = (XYPlot) myChart.getChart().getPlot();
    if (plot instanceof Zoomable) {
      Zoomable z = (Zoomable) plot;
      Point2D endPoint = new Point2D.Double(0, 0);
      PlotRenderingInfo pri = myChart.getRenderingInfo().getPlotInfo();
      z.zoomDomainAxes(0, pri, endPoint);
    }
  }

  /**
   * Move a chart by a percentage x-offset if xoffset is <0 the shift will be negativ (xoffset>0
   * results in a positive shift)
   * 
   * @param myChart
   * @param xoffset in percent
   * @param autoRangeY if true the range (Y) axis auto bounds will be restored
   */
  public static void offsetDomainAxis(ChartCanvas myChart, double xoffset, boolean autoRangeY) {
    XYPlot plot = (XYPlot) myChart.getChart().getPlot();
    ValueAxis domainAxis = plot.getDomainAxis();
    // apply offset on x
    double distance = (domainAxis.getUpperBound() - domainAxis.getLowerBound()) * xoffset;

    Range range =
        new Range(domainAxis.getLowerBound() + distance, domainAxis.getUpperBound() + distance);
    setZoomDomainAxis(myChart, keepRangeWithinAutoBounds(domainAxis, range), autoRangeY);
  }

  /**
   * Apply an absolute offset to domain (x) axis and move it
   * 
   * @param myChart
   * @param xoffset
   * @param autoRangeY
   */
  public static void offsetDomainAxisAbsolute(ChartCanvas myChart, double xoffset,
      boolean autoRangeY) {
    XYPlot plot = (XYPlot) myChart.getChart().getPlot();
    ValueAxis domainAxis = plot.getDomainAxis();
    // apply offset on x

    Range range =
        new Range(domainAxis.getLowerBound() + xoffset, domainAxis.getUpperBound() + xoffset);
    setZoomDomainAxis(myChart, keepRangeWithinAutoBounds(domainAxis, range), autoRangeY);
  }

  /**
   * Apply an absolute offset to an axis and move it
   * 
   * @param myChart
   * @param offset
   */
  public static void offsetAxisAbsolute(ValueAxis axis, double offset) {
    Range range = new Range(axis.getLowerBound() + offset, axis.getUpperBound() + offset);
    setZoomAxis(axis, keepRangeWithinAutoBounds(axis, range));
  }

  /**
   * Apply an relative offset to an axis and move it. LowerBound and UpperBound are defined by
   * {@link ValueAxis#getDefaultAutoRange()}
   * 
   * @param myChart
   * @param offset percentage
   */
  public static void offsetAxis(ValueAxis axis, double offset) {
    double distance = (axis.getUpperBound() - axis.getLowerBound()) * offset;
    Range range = new Range(axis.getLowerBound() + distance, axis.getUpperBound() + distance);
    setZoomAxis(axis, keepRangeWithinAutoBounds(axis, range));
  }

  public static Range keepRangeWithinAutoBounds(ValueAxis axis, Range range) {
    // keep within auto range bounds
    // Range auto = axis.getDefaultAutoRange();
    // if(range.getLowerBound()<auto.getLowerBound()){
    // double negative = range.getLowerBound()-auto.getLowerBound();
    // range = new Range(auto.getLowerBound(), range.getUpperBound()-negative);
    // }
    // if(range.getUpperBound()>auto.getUpperBound()) {
    // double positive = range.getUpperBound()-auto.getUpperBound();
    // range = new Range(range.getLowerBound()-positive, auto.getUpperBound());
    // }
    return range;
  }

  /**
   * Zoom in (negative yzoom) or zoom out of range axis.
   * 
   * @param myChart
   * @param yzoom percentage zoom factor
   * @param holdLowerBound if true only the upper bound will be zoomed
   */
  public static void zoomRangeAxis(ChartCanvas myChart, double yzoom, boolean holdLowerBound) {
    XYPlot plot = (XYPlot) myChart.getChart().getPlot();
    ValueAxis rangeAxis = plot.getRangeAxis();

    double lower = rangeAxis.getLowerBound();
    double upper = rangeAxis.getUpperBound();
    double dist = upper - lower;

    if (holdLowerBound) {
      upper += dist * yzoom;
    } else {
      lower -= dist * yzoom / 2;
      upper += dist * yzoom / 2;
    }

    if (lower < upper) {
      Range range = new Range(lower, upper);
      setZoomAxis(rangeAxis, keepRangeWithinAutoBounds(rangeAxis, range));
    }
  }

  /**
   * Zoom in (negative zoom) or zoom out of axis.
   * 
   * @param myChart
   * @param zoom percentage zoom factor
   * @param holdLowerBound if true only the upper bound will be zoomed
   */
  public static void zoomAxis(ValueAxis axis, double zoom, boolean holdLowerBound) {
    double lower = axis.getLowerBound();
    double upper = axis.getUpperBound();
    double dist = upper - lower;

    if (holdLowerBound) {
      if (zoom == 0)
        return;
      upper += dist * zoom;
    } else {
      lower -= dist * zoom / 2;
      upper += dist * zoom / 2;
    }

    if (lower < upper) {
      logger.info("Set zoom:" + lower + ", " + upper + " (keep lower:" + holdLowerBound + ")");
      Range range = new Range(lower, upper);
      setZoomAxis(axis, keepRangeWithinAutoBounds(axis, range));
    }
  }

  /**
   * Zoom in (negative zoom) or zoom out of axis.
   * 
   * @param myChart
   * @param zoom percentage zoom factor
   * @param start point on this range (first click/pressed event), used as center
   */
  public static void zoomAxis(ValueAxis axis, double zoom, double start) {
    double lower = axis.getLowerBound();
    double upper = axis.getUpperBound();
    double dist = upper - lower;
    double f = (start - lower) / dist;

    lower -= dist * zoom * f;
    upper += dist * zoom * (1 - f);

    if (lower < upper) {
      Range range = new Range(lower, upper);
      setZoomAxis(axis, keepRangeWithinAutoBounds(axis, range));
    }
  }

  /**
   * 
   * @param ChartCanvas
   * @return
   */
  // TODO
  public static boolean isMouseZoomable(ChartCanvas chart) {
    // return chartPanel instanceof EChartPanel ? ((EChartPanel) chartPanel).isMouseZoomable()
    // : chartPanel.isRangeZoomable() && chartPanel.isDomainZoomable();
    return chart.isRangeZoomable() && chart.isDomainZoomable();
  }
}
