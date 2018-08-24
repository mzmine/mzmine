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

package net.sf.mzmine.chartbasics.gui.wrapper;

import java.awt.geom.Point2D;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.fx.ChartViewer;
import net.sf.mzmine.chartbasics.ChartLogics;
import net.sf.mzmine.chartbasics.ChartLogicsFX;
import net.sf.mzmine.chartbasics.gui.javafx.EChartViewer;
import net.sf.mzmine.chartbasics.gui.swing.EChartPanel;
import net.sf.mzmine.chartbasics.listener.ZoomHistory;

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
    return cp != null ? ChartLogics.mouseXYToPlotXY(cp, x, y)
        : ChartLogicsFX.mouseXYToPlotXY(cc, x, y);
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
}
