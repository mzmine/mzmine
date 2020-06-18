package io.github.mzmine.modules.visualization.ims.imsVisualizer;

import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.main.MZmineCore;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;

import java.awt.*;
import java.util.logging.Logger;

public class MobilityIntensityPlot extends EChartViewer {

  private XYPlot plot;
  private JFreeChart chart;
  private Logger logger = Logger.getLogger(this.getClass().getName());
  static final Font legendFont = new Font("SansSerif", Font.PLAIN, 12);
  private EStandardChartTheme theme;


  public MobilityIntensityPlot(XYDataset dataset) {
    super(
        ChartFactory.createXYLineChart(
            "", "mobility", "intensity", dataset, PlotOrientation.VERTICAL, true, true, false));
        chart = getChart();
        plot = chart.getXYPlot();
        theme = MZmineCore.getConfiguration().getDefaultChartTheme();
        theme.apply(chart);


    var renderer = new XYLineAndShapeRenderer();
      renderer.setSeriesPaint(0, Color.GREEN);
      renderer.setSeriesStroke(0, new BasicStroke(1.0f));

      plot.setRenderer(renderer);
      plot.setBackgroundPaint(Color.BLACK);
      plot.setRangeGridlinePaint(Color.BLACK);
      plot.setDomainGridlinePaint(Color.BLACK);
      plot.setOutlinePaint(Color.red);
      plot.setOutlineStroke(new BasicStroke(2.5f));

      chart.getLegend().setFrame(BlockBorder.NONE);


  }
}
