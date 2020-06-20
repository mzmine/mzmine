package io.github.mzmine.modules.visualization.ims.imsVisualizer;

import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.ims.ImsVisualizerTask;
import io.github.mzmine.parameters.ParameterSet;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.fx.interaction.ChartMouseEventFX;
import org.jfree.chart.fx.interaction.ChartMouseListenerFX;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.xy.XYDataset;

import java.awt.*;
import java.util.logging.Logger;

public class IntensityRetentionTimePlot extends EChartViewer {

  private XYPlot plot;
  private JFreeChart chart;
  private Logger logger = Logger.getLogger(this.getClass().getName());
  static final Font legendFont = new Font("SansSerif", Font.PLAIN, 12);
  private double selectedRetention;
  private ParameterSet parameterSet;
  private ValueMarker marker;
  private EStandardChartTheme theme;

  public IntensityRetentionTimePlot(
      XYDataset dataset,
      ImsVisualizerTask imsVisualizerTask,
      MobilityRetentionHeatMapPlot mobilityRetentionHeatMapPlot) {

    super(
        ChartFactory.createXYLineChart(
            null,
            "retention time",
            "intensity",
            dataset,
            PlotOrientation.VERTICAL,
            true,
            true,
            false));
    this.parameterSet = parameterSet;
    chart = getChart();
    theme = MZmineCore.getConfiguration().getDefaultChartTheme();
    theme.apply(chart);
    plot = chart.getXYPlot();
    var renderer = new XYLineAndShapeRenderer();
    renderer.setSeriesPaint(0, Color.GREEN);
    renderer.setSeriesStroke(0, new BasicStroke(1.0f));

    plot.setRenderer(renderer);
    plot.setBackgroundPaint(Color.BLACK);
    plot.setRangeGridlinePaint(Color.RED);
    plot.setDomainGridlinePaint(Color.RED);
    plot.setOutlinePaint(Color.red);
    plot.setOutlineStroke(new BasicStroke(2.5f));

    chart.getLegend().setFrame(BlockBorder.NONE);
    addChartMouseListener(
        new ChartMouseListenerFX() {
          @Override
          public void chartMouseClicked(ChartMouseEventFX event) {
            ChartEntity chartEntity = event.getEntity();
            if (chartEntity instanceof XYItemEntity) {
              XYItemEntity entity = (XYItemEntity) chartEntity;
              int serindex = entity.getSeriesIndex();
              int itemindex = entity.getItem();
              selectedRetention = dataset.getXValue(serindex, itemindex);
              // Get controller
              imsVisualizerTask.setSelectedRetentionTime(selectedRetention);
              //imsVisualizerTask.runMoblityMZHeatMap();

              // setting the marker at seleted range.
              plot.clearDomainMarkers();
              marker = new ValueMarker(selectedRetention);
              marker.setLabel("Selected RT");
              marker.setPaint(Color.red);
              marker.setLabelFont(legendFont);
              marker.setStroke(new BasicStroke(2));
              marker.setLabelAnchor(RectangleAnchor.TOP_RIGHT);
              marker.setLabelTextAnchor(TextAnchor.BASELINE_CENTER);
              plot.addDomainMarker(marker);

              // marker to the mobility-retention time heatmap plot.
              //mobilityRetentionHeatMapPlot.getPlot().clearDomainMarkers();
              //mobilityRetentionHeatMapPlot.getPlot().addDomainMarker(marker);
            }
          }

          @Override
          public void chartMouseMoved(ChartMouseEventFX event) {}
        });
  }
}
