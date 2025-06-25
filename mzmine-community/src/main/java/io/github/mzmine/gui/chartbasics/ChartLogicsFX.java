/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.gui.chartbasics;

import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.logging.Logger;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.Axis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.AxisEntity;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.fx.ChartCanvas;
import org.jfree.chart.fx.ChartViewer;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.CombinedDomainCategoryPlot;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.CombinedRangeCategoryPlot;
import org.jfree.chart.plot.CombinedRangeXYPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.plot.Zoomable;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.Range;

/**
 * Collection of methods for JFreeCharts <br> Calculate mouseXY to plotXY <br> Calc width and height
 * for plots where domain and range axis share the same dimensions <br> Zoom and shift axes by
 * absolute or relative values
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class ChartLogicsFX {

  private static final Logger logger = Logger.getLogger(ChartLogicsFX.class.getName());

  /**
   * Translates mouse coordinates to chart coordinates (xy-axis)
   *
   * @param myChart
   * @param mouseX
   * @param mouseY
   * @return Range as chart coordinates
   */
  public static Point2D mouseXYToPlotXY(ChartViewer myChart, double mouseX, double mouseY) {
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
  public static Point2D mouseXYToPlotXY(ChartViewer myChart, int mouseX, int mouseY) {
    XYPlot plot = null;
    // find plot as parent of axis
    ChartEntity entity = findChartEntity(myChart.getCanvas(), mouseX, mouseY);
    if (entity instanceof AxisEntity) {
      Axis a = ((AxisEntity) entity).getAxis();
      if (a.getPlot() instanceof XYPlot) {
        plot = (XYPlot) a.getPlot();
      }
    }

    ChartRenderingInfo info = myChart.getRenderingInfo();
    if (info == null) {
      return null; // this should not happen if there is actually a mouse event on the chart. musst be renedered first.
    }

    int subplot = info.getPlotInfo().getSubplotIndex(new Point2D.Double(mouseX, mouseY));
    Rectangle2D dataArea = info.getPlotInfo().getDataArea();
    if (subplot != -1) {
      dataArea = info.getPlotInfo().getSubplotInfo(subplot).getDataArea();
    }

    // find subplot or plot
    if (plot == null) {
      plot = findXYSubplot(myChart.getChart(), info, mouseX, mouseY);
    }

    // coordinates
    double cx = 0;
    double cy = 0;
    if (plot != null) {
      // find axis
      ValueAxis domainAxis = plot.getDomainAxis();
      ValueAxis rangeAxis = plot.getRangeAxis();
      RectangleEdge domainAxisEdge = plot.getDomainAxisEdge();
      RectangleEdge rangeAxisEdge = plot.getRangeAxisEdge();
      // parent?
      if (domainAxis == null && plot.getParent() != null && plot.getParent() instanceof XYPlot pp) {
        domainAxis = pp.getDomainAxis();
        domainAxisEdge = pp.getDomainAxisEdge();
      }
      if (rangeAxis == null && plot.getParent() != null && plot.getParent() instanceof XYPlot pp) {
        rangeAxis = pp.getRangeAxis();
        rangeAxisEdge = pp.getRangeAxisEdge();
      }

      if (domainAxis != null) {
        cx = domainAxis.java2DToValue(mouseX, dataArea, domainAxisEdge);
      }
      if (rangeAxis != null) {
        cy = rangeAxis.java2DToValue(mouseY, dataArea, rangeAxisEdge);
      }
    }
    return new Point2D.Double(cx, cy);
  }

  /**
   * Find chartentities like JFreeChartEntity, AxisEntity, PlotEntity, TitleEntity, XY...
   *
   * @param chart
   * @return
   */
  public static ChartEntity findChartEntity(ChartCanvas chart, double mx, double my) {
    // TODO check if insets were needed
    // coordinates to find chart entities
    int x = (int) (mx / chart.getScaleX());
    int y = (int) (my / chart.getScaleY());

    ChartRenderingInfo info = chart.getRenderingInfo();
    ChartEntity entity = null;
    if (info != null) {
      EntityCollection entities = info.getEntityCollection();
      if (entities != null) {
        entity = entities.getEntity(x, y);
      }
    }
    return entity;
  }

  /**
   * Subplot or main plot at point
   *
   * @param chart
   * @param info
   * @param mouseX
   * @param mouseY
   * @return
   */
  public static XYPlot findXYSubplot(JFreeChart chart, ChartRenderingInfo info, double mouseX,
      double mouseY) {
    int subplot = info.getPlotInfo().getSubplotIndex(new Point2D.Double(mouseX, mouseY));
    XYPlot plot = null;
    if (subplot != -1) {
      if (chart.getPlot() instanceof CombinedDomainXYPlot) {
        plot = (XYPlot) ((CombinedDomainXYPlot) chart.getPlot()).getSubplots().get(subplot);
      } else if (chart.getPlot() instanceof CombinedRangeXYPlot) {
        plot = (XYPlot) ((CombinedRangeXYPlot) chart.getPlot()).getSubplots().get(subplot);
      }
    }
    if (plot == null && chart.getPlot() instanceof XYPlot) {
      plot = (XYPlot) chart.getPlot();
    }
    return plot;
  }

  /**
   * Translates screen (pixel) values to plot values
   *
   * @param myChart
   * @return width in data space for x and y
   */
  public static Point2D screenValueToPlotValue(ChartViewer myChart, int val) {
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
   * @param axis      for width calculation
   * @return
   */
  public static double calcWidthOnScreen(ChartViewer myChart, double dataWidth, ValueAxis axis,
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
   * @return
   */
  public static Dimension calcSizeForPlotWidth(ChartViewer myChart, double plotWidth) {
    return calcSizeForPlotWidth(myChart, plotWidth, 4);
  }

  /**
   * Calculates the size of a chart for a given fixed plot width Domain and Range axes need to share
   * the same unit (e.g. mm)
   *
   * @param myChart
   * @param plotWidth
   * @return
   */
  public static Dimension calcSizeForPlotWidth(ChartViewer myChart, double plotWidth,
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
   * @param myChart
   * @param plotWidth
   * @return
   */
  public static Dimension calcSizeForPlotSize(ChartViewer myChart, double plotWidth,
      double plotHeight) {
    return calcSizeForPlotSize(myChart, plotWidth, plotHeight, 4);
  }

  /**
   * Calculates the size of a chart for a given fixed plot width and height
   *
   * @param myChart
   * @param plotWidth
   * @return
   */
  public static Dimension calcSizeForPlotSize(ChartViewer myChart, double plotWidth,
      double plotHeight, int iterations) {
    makeChartResizable(myChart);

    // estimate plotwidth / height needs to be a bit bigger because plot is only part of chart
    double estimatedChartWidth = plotWidth + 200;
    double estimatedChartHeight = plotHeight + 200;

    double lastW = estimatedChartWidth;
    double lastH = estimatedChartHeight;

    // paint and get closer
    try {
      for (int i = 0; i < iterations; i++) {
        // paint on ghost panel with estimated height (if copy
        // panel==true)
        myChart.getCanvas().setWidth((int) estimatedChartWidth);
        myChart.getCanvas().setHeight((int) estimatedChartHeight);
        myChart.getCanvas().draw();

        // rendering info
        ChartRenderingInfo info = myChart.getRenderingInfo();
        Rectangle2D dataArea = info.getPlotInfo().getDataArea();
        Rectangle2D chartArea = info.getChartArea();

        // // calc title space: will be added later to the right plot
        // size
        // double titleWidth = chartArea.getWidth()-dataArea.getWidth();
        // double titleHeight =
        // chartArea.getHeight()-dataArea.getHeight();

        // calc width and height
        estimatedChartWidth = estimatedChartWidth - dataArea.getWidth() + plotWidth;
        estimatedChartHeight = estimatedChartHeight - dataArea.getHeight() + plotHeight;

        if ((int) lastW == (int) estimatedChartWidth && (int) lastH == (int) estimatedChartHeight) {
          break;
        } else {
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
   * @param chartWidth width of data
   * @return
   */
  public static double calcHeightToWidth(ChartViewer myChart, double chartWidth) {
    return calcHeightToWidth(myChart, chartWidth, chartWidth * 3, 4);
  }

  /**
   * calculates the correct height with multiple iterations Domain and Range axes need to share the
   * same unit (e.g. mm)
   *
   * @param myChart
   * @param chartWidth width of data
   * @return
   */
  public static double calcHeightToWidth(ChartViewer myChart, double chartWidth,
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
        // paint on ghost panel with estimated height (if copy
        // panel==true)
        myChart.getCanvas().setWidth((int) chartWidth);
        myChart.getCanvas().setHeight((int) estimatedHeight);
        myChart.getCanvas().draw();

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
        if ((int) lastH == (int) height) {
          break;
        } else {
          lastH = height;
        }
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
  public static void makeChartResizable(ChartViewer myChart) {
    // TODO set max and min sizes
  }

  /**
   * Domain and Range axes need to share the same unit (e.g. mm)
   *
   * @param myChart
   * @return
   */
  public static double calcWidthToHeight(ChartViewer myChart, double chartHeight) {
    makeChartResizable(myChart);

    myChart.getCanvas().draw();

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
  public static Dimension calcMaxSize(ChartViewer myChart, double chartWidth, double chartHeight) {
    makeChartResizable(myChart);
    // paint on a ghost panel
    myChart.getCanvas().draw();

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
    // if width is higher than given chartWidth then calc height for
    // chartWidth
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
   * Auto range the range axis
   *
   * @param myChart
   */
  public static void autoAxes(ChartViewer myChart) {
    XYPlot plot = (XYPlot) myChart.getChart().getPlot();
    if (plot instanceof Zoomable) {
      Zoomable z = plot;
      Point2D endPoint = new Point2D.Double(0, 0);
      // nullable but ok here
      PlotRenderingInfo pri = getPlotRenderingInfo(myChart);
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
   */
  public static void autoRangeAxis(ChartViewer myChart) {
    XYPlot plot = (XYPlot) myChart.getChart().getPlot();
    if (plot instanceof Zoomable) {
      Zoomable z = plot;
      Point2D endPoint = new Point2D.Double(0, 0);
      PlotRenderingInfo pri = getPlotRenderingInfo(myChart);
      z.zoomRangeAxes(0, pri, endPoint);
    }
  }

  /**
   * Auto range the domain axis
   *
   * @param myChart
   */
  public static void autoDomainAxis(ChartViewer myChart) {
    XYPlot plot = (XYPlot) myChart.getChart().getPlot();
    if (plot instanceof Zoomable) {
      Zoomable z = plot;
      Point2D endPoint = new Point2D.Double(0, 0);
      PlotRenderingInfo pri = getPlotRenderingInfo(myChart);
      z.zoomDomainAxes(0, pri, endPoint);
    }
  }

  @Nullable
  private static PlotRenderingInfo getPlotRenderingInfo(ChartViewer myChart) {
    PlotRenderingInfo pri = null;
    final ChartRenderingInfo renderingInfo = myChart.getRenderingInfo();
    if (renderingInfo != null) {
      pri = renderingInfo.getPlotInfo();
    }
    return pri;
  }

  /**
   * @param chart
   * @return
   */
  // TODO
  public static boolean isMouseZoomable(ChartViewer chart) {
    // return chartPanel instanceof EChartPanel ? ((EChartPanel)
    // chartPanel).isMouseZoomable()
    // : chartPanel.isRangeZoomable() && chartPanel.isDomainZoomable();
    return chart.getCanvas().isRangeZoomable() && chart.getCanvas().isDomainZoomable();
  }

  /**
   * Set margins of axes in plots
   */
  public static void setAxesMargins(final JFreeChart chart, final double margin) {
    try {
      var plot = chart.getPlot();

      if (plot instanceof CombinedDomainXYPlot cp) {
        for (final Object sub : cp.getSubplots()) {
          if (sub instanceof XYPlot xy) {
            xy.getDomainAxis().setUpperMargin(margin);
            xy.getRangeAxis().setUpperMargin(margin);
            xy.getRangeAxis().setLowerMargin(margin);
          }
        }
      }
      if (plot instanceof CombinedRangeXYPlot cp) {
        for (final Object sub : cp.getSubplots()) {
          if (sub instanceof XYPlot xy) {
            xy.getDomainAxis().setUpperMargin(margin);
            xy.getRangeAxis().setUpperMargin(margin);
            xy.getRangeAxis().setLowerMargin(margin);
          }
        }
      }

      chart.getXYPlot().getDomainAxis().setUpperMargin(margin);
    } catch (Exception ex) {
      // ignore as it only fails for combined plots or non XY plots
    }
    try {
      chart.getXYPlot().getRangeAxis().setUpperMargin(margin);
      chart.getXYPlot().getRangeAxis().setLowerMargin(margin);
    } catch (Exception ex) {
      // ignore as it only fails for combined plots or non XY plots
    }
  }

  /**
   * Set all axis of this chart to auto range or not when data is added. Auto range on axis is very
   * slow if many datasets are added
   *
   * @param state auto range on of off
   */
  public static void setAutoRangeAxis(final JFreeChart chart, final boolean state) {
    setAutoRangeAxis(chart.getPlot(), state);
  }

  public static void setAutoRangeAxis(final Plot plot, final boolean state) {
    switch (plot) {
      case CombinedDomainXYPlot cp -> {
        for (final Object sub : cp.getSubplots()) {
          if (sub instanceof Plot p) {
            setAutoRangeAxis(p, state);
          }
        }
        for (int i = 0; i < cp.getRangeAxisCount(); i++) {
          cp.getRangeAxis(i).setAutoRange(state);
        }
        for (int i = 0; i < cp.getDomainAxisCount(); i++) {
          cp.getDomainAxis(i).setAutoRange(state);
        }
      }
      case CombinedRangeXYPlot cp -> {
        for (final Object sub : cp.getSubplots()) {
          if (sub instanceof Plot p) {
            setAutoRangeAxis(p, state);
          }
        }
        for (int i = 0; i < cp.getRangeAxisCount(); i++) {
          cp.getRangeAxis(i).setAutoRange(state);
        }
        for (int i = 0; i < cp.getDomainAxisCount(); i++) {
          cp.getDomainAxis(i).setAutoRange(state);
        }
      }
      case CombinedRangeCategoryPlot cp -> {
        for (final Object sub : cp.getSubplots()) {
          if (sub instanceof Plot p) {
            setAutoRangeAxis(p, state);
          }
        }
        for (int i = 0; i < cp.getRangeAxisCount(); i++) {
          cp.getRangeAxis(i).setAutoRange(state);
        }
      }
      case CombinedDomainCategoryPlot cp -> {
        for (final Object sub : cp.getSubplots()) {
          if (sub instanceof Plot p) {
            setAutoRangeAxis(p, state);
          }
        }
        for (int i = 0; i < cp.getRangeAxisCount(); i++) {
          cp.getRangeAxis(i).setAutoRange(state);
        }
      }
      case XYPlot p -> {
        for (int i = 0; i < p.getRangeAxisCount(); i++) {
          p.getRangeAxis(i).setAutoRange(state);
        }
        for (int i = 0; i < p.getDomainAxisCount(); i++) {
          p.getDomainAxis(i).setAutoRange(state);
        }
      }
      case CategoryPlot p -> {
        for (int i = 0; i < p.getRangeAxisCount(); i++) {
          p.getRangeAxis(i).setAutoRange(state);
        }
      }
      case null, default -> {
      }
    }
  }
}
