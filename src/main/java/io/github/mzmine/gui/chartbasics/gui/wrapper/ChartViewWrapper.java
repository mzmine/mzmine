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

package io.github.mzmine.gui.chartbasics.gui.wrapper;

import java.awt.event.MouseListener;
import java.awt.geom.Point2D;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.fx.ChartViewer;
import org.jfree.chart.plot.XYPlot;

import io.github.mzmine.gui.chartbasics.ChartLogics;
import io.github.mzmine.gui.chartbasics.ChartLogicsFX;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.chartbasics.gui.swing.ChartGestureMouseAdapter;
import io.github.mzmine.gui.chartbasics.gui.swing.EChartPanel;
import io.github.mzmine.gui.chartbasics.listener.ZoomHistory;

public class ChartViewWrapper {

  // only one is initialised
  private ChartPanel cp;
  private ChartViewer cc;

  public ChartViewWrapper(ChartPanel cp) {
    super();
    this.cp = cp;
  }

  public ChartViewWrapper(ChartViewer cc) {
    super();
    this.cc = cc;
  }

  public ChartViewer getChartFX() {
    return cc;
  }

  public ChartPanel getChartSwing() {
    return cp;
  }

  public JFreeChart getChart() {
    return cp != null ? cp.getChart() : cc.getChart();
  }

  public boolean isFX() {
    return cc != null;
  }

  public boolean isSwing() {
    return !isFX();
  }

  public ChartRenderingInfo getRenderingInfo() {
    return cp != null ? cp.getChartRenderingInfo() : cc.getRenderingInfo();
  }

  // logics
  public Point2D mouseXYToPlotXY(double x, double y) {
    try {
      return cp != null ? ChartLogics.mouseXYToPlotXY(cp, x, y)
          : ChartLogicsFX.mouseXYToPlotXY(cc, x, y);
    } catch (Exception e) {
      e.printStackTrace();
      return new Point2D.Double(0, 0);
    }
  }

  public boolean isMouseZoomable() {
    return cp != null ? ChartLogics.isMouseZoomable(cp) : ChartLogicsFX.isMouseZoomable(cc);
  }

  public void setMouseZoomable(boolean zoomable) {
    if (cp != null)
      cp.setMouseZoomable(zoomable);
    else {
      if (cc instanceof EChartViewer)
        ((EChartViewer) cc).setMouseZoomable(zoomable);
      else {
        cc.getCanvas().setDomainZoomable(zoomable);
        cc.getCanvas().setRangeZoomable(zoomable);
      }
    }
  }

  /**
   * Auto range the range axis
   * 
   */
  public void autoAxes() {
    if (cp != null)
      ChartLogics.autoAxes(cp);
    else
      ChartLogicsFX.autoAxes(cc);
  }

  /**
   * Auto range the range axis
   * 
   */
  public void autoRangeAxis() {
    if (cp != null)
      ChartLogics.autoRangeAxis(cp);
    else
      ChartLogicsFX.autoRangeAxis(cc);
  }

  /**
   * Auto range the range axis
   * 
   */
  public void autoDomainAxis() {
    if (cp != null)
      ChartLogics.autoDomainAxis(cp);
    else
      ChartLogicsFX.autoDomainAxis(cc);
  }

  /**
   * The ZoomHistory of this ChartPanel/ChartCanvas
   * 
   * @return
   */
  public ZoomHistory getZoomHistory() {
    if (cp != null) {
      if (cp instanceof EChartPanel)
        return ((EChartPanel) cp).getZoomHistory();
      else {
        Object o = cp.getClientProperty(ZoomHistory.PROPERTY_NAME);
        if (o != null && o instanceof ZoomHistory)
          return (ZoomHistory) o;
        else
          return null;
      }
    } else {
      // fx TODO
      if (cc instanceof EChartViewer)
        return ((EChartViewer) cc).getZoomHistory();
      else
        return null;
    }
  }

  public void setZoomHistory(ZoomHistory h) {
    if (cp != null) {
      if (cp instanceof EChartPanel)
        ((EChartPanel) cp).setZoomHistory(h);
      else {
        cp.putClientProperty(h, ZoomHistory.PROPERTY_NAME);
      }
    } else {
      // TODO

      if (cc instanceof EChartViewer)
        ((EChartViewer) cc).setZoomHistory(h);
    }
  }

  /**
   * Subplot or main plot at point
   * 
   * @param mouseX
   * @param mouseY
   * @return
   */
  public XYPlot findXYSubplot(double mouseX, double mouseY) {
    return cp != null ? ChartLogics.findXYSubplot(getChart(), getRenderingInfo(), mouseX, mouseY)
        : ChartLogicsFX.findXYSubplot(getChart(), getRenderingInfo(), mouseX, mouseY);
  }

  /**
   * Find chartentities like JFreeChartEntity, AxisEntity, PlotEntity, TitleEntity, XY...
   * 
   * @param mx mouse coordinates
   * @param my mouse coordinates
   * @return
   */
  public ChartEntity findChartEntity(double mx, double my) {
    return cp != null ? ChartLogics.findChartEntity(cp, mx, my)
        : ChartLogicsFX.findChartEntity(cc.getCanvas(), mx, my);
  }

  /**
   * Get the chart gesture mouse adapter
   * 
   * @return
   */
  public GestureMouseAdapter getGestureAdapter() {
    if (cp != null) {
      if (cp instanceof EChartPanel)
        return ((EChartPanel) cp).getGestureAdapter();
      else
        for (MouseListener l : cp.getMouseListeners())
          if (ChartGestureMouseAdapter.class.isInstance(l))
            return (ChartGestureMouseAdapter) l;
      // none
      return null;
    } else if (cc != null) {
      if (cc instanceof EChartViewer)
        return ((EChartViewer) cc).getGestureAdapter();
      else
        return null;
      // TODO export adapter for normal ChartViewer
    } else
      return null;
  }
}
