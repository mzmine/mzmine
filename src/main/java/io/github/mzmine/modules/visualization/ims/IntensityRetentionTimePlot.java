package io.github.mzmine.modules.visualization.ims;

import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.parameters.ParameterSet;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.BorderPane;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.fx.interaction.ChartMouseEventFX;
import org.jfree.chart.fx.interaction.ChartMouseListenerFX;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;

import java.awt.*;
import java.util.logging.Logger;

public class IntensityRetentionTimePlot extends EChartViewer {

  private XYPlot plot;
  private JFreeChart chart;
  private Logger logger = Logger.getLogger(this.getClass().getName());
  static final Font legendFont = new Font("SansSerif", Font.PLAIN, 12);
  private double selectedRetention;
  private XYZDataset dataset3d;
  private ParameterSet parameterSet;
  private XYZDataset datasetMF;
  private String paintScaleStyle;


  public IntensityRetentionTimePlot(XYDataset dataset, ImsVisualizerTask imsVisualizerTask) {

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
              imsVisualizerTask.runMoblityMZHeatMap();
            }
          }

          @Override
          public void chartMouseMoved(ChartMouseEventFX event) {}
        });
  }
}
