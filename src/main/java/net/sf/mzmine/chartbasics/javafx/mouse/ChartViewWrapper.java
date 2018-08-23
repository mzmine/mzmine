package net.sf.mzmine.chartbasics.javafx.mouse;

import java.awt.geom.Point2D;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.fx.ChartCanvas;
import net.sf.mzmine.chartbasics.ChartLogics;
import net.sf.mzmine.chartbasics.ChartLogicsFX;

public class ChartViewWrapper {

  // only one is initialised
  private ChartPanel cp;
  private ChartCanvas cc;

  public ChartViewWrapper(ChartPanel cp) {
    super();
    this.cp = cp;
  }

  public ChartViewWrapper(ChartCanvas cc) {
    super();
    this.cc = cc;
  }

  public ChartCanvas getChartFX() {
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
      cc.setDomainZoomable(zoomable);
      cc.setRangeZoomable(zoomable);
    }
  }



  /**
   * Auto range the range axis
   * 
   * @param myChart
   * @param zoom
   * @param autoRangeY if true the range (Y) axis auto bounds will be restored
   */
  public void autoAxes(ChartCanvas myChart) {
    if (cp != null)
      ChartLogics.autoAxes(cp);
    else
      ChartLogicsFX.autoAxes(cc);
  }

  /**
   * Auto range the range axis
   * 
   * @param myChart
   * @param zoom
   * @param autoRangeY if true the range (Y) axis auto bounds will be restored
   */
  public void autoRangeAxis(ChartCanvas myChart) {
    if (cp != null)
      ChartLogics.autoRangeAxis(cp);
    else
      ChartLogicsFX.autoRangeAxis(cc);
  }

  /**
   * Auto range the range axis
   * 
   * @param myChart
   * @param zoom
   * @param autoRangeY if true the range (Y) axis auto bounds will be restored
   */
  public void autoDomainAxis(ChartCanvas myChart) {
    if (cp != null)
      ChartLogics.autoDomainAxis(cp);
    else
      ChartLogicsFX.autoDomainAxis(cc);
  }
}
