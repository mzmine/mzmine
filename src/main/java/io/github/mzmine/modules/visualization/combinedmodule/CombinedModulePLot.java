package io.github.mzmine.modules.visualization.combinedmodule;

import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import java.awt.Color;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.SeriesRenderingOrder;
import org.jfree.chart.plot.XYPlot;

public class CombinedModulePLot extends EChartViewer {

  private JFreeChart chart;

  private XYPlot plot;

  public CombinedModulePLot() {
    super(ChartFactory.createXYLineChart("", "", "", null, PlotOrientation.VERTICAL, true, true,
        false), true, true, false, false, true);
    setMouseZoomable(false);

    chart = getChart();
    chart.setBackgroundPaint(Color.white);
    plot = chart.getXYPlot();
    plot.setBackgroundPaint(Color.white);
    plot.setSeriesRenderingOrder(SeriesRenderingOrder.FORWARD);
  }

}
