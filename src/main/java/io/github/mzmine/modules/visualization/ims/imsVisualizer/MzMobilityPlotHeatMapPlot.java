package io.github.mzmine.modules.visualization.ims.imsVisualizer;

import com.google.common.collect.Range;
import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.chartbasics.chartutils.XYBlockPixelSizePaintScales;
import io.github.mzmine.gui.chartbasics.chartutils.XYBlockPixelSizeRenderer;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.CombinedRangeXYPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.LookupPaintScale;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.logging.Logger;

public class MzMobilityPlotHeatMapPlot extends EChartViewer {

  private XYPlot plot3d;
  private XYPlot plot2d;
  private XYPlot getPlot3d;
  private String paintScaleStyle;
  private JFreeChart chart;
  private XYZDataset dataset3d;
  private XYDataset dataset2d;
  private Logger logger = Logger.getLogger(this.getClass().getName());
  static final Font legendFont = new Font("SansSerif", Font.PLAIN, 12);
  private EStandardChartTheme theme;
  private ParameterSet parameterSet;
  private double selectedRetention;

  public MzMobilityPlotHeatMapPlot(
      ParameterSet parameterSet, String paintScaleStyle, double selectedRetention) {

    this.parameterSet = parameterSet;
    this.dataset3d = new MobilityFrameXYZDataset(parameterSet, selectedRetention);
    this.dataset2d = new MobilityIntensityXYDataset(parameterSet);
    this.paintScaleStyle = paintScaleStyle;
    this.selectedRetention = selectedRetention;

    // create 2d plot.
    var renderer2d = new XYLineAndShapeRenderer();
    renderer2d.setSeriesPaint(0, Color.GREEN);
    renderer2d.setSeriesStroke(0, new BasicStroke(1.0f));

    NumberAxis domain2d = new NumberAxis("intensity");
    plot2d = new XYPlot(dataset2d, domain2d, null, renderer2d);
    plot2d.setRenderer(renderer2d);
    plot2d.setBackgroundPaint(Color.BLACK);
    plot2d.setRangeGridlinePaint(Color.BLACK);
    plot2d.setDomainGridlinePaint(Color.BLACK);
    plot2d.setOutlinePaint(Color.red);
    plot2d.setOutlineStroke(new BasicStroke(2.5f));

    // add 3d plot to the combined plots.

    double[] copyZValues = new double[dataset3d.getItemCount(0)];
    double[] copyXValues = new double[dataset3d.getItemCount(0)];
    double[] copyYValues = new double[dataset3d.getItemCount(0)];
    for (int i = 0; i < dataset3d.getItemCount(0); i++) {
      copyZValues[i] = dataset3d.getZValue(0, i);
      copyXValues[i] = dataset3d.getXValue(0, i);
      copyYValues[i] = dataset3d.getYValue(0, i);
    }
    Arrays.sort(copyZValues);
    Arrays.sort(copyXValues);
    Arrays.sort(copyYValues);

    // get index in accordance to percentile windows
    int minIndexScale = 0;
    int maxIndexScale = copyZValues.length - 1;
    double min = copyZValues[minIndexScale];
    double max = copyZValues[maxIndexScale];
    Paint[] contourColors =
        XYBlockPixelSizePaintScales.getPaintColors(
            "percentile", Range.closed(min, max), paintScaleStyle);
    LookupPaintScale scale = new LookupPaintScale(min, max, Color.BLACK);

    double[] scaleValues = new double[contourColors.length];
    double delta = (max - min) / (contourColors.length - 1);
    double value = min;
    for (int i = 0; i < contourColors.length; i++) {
      scaleValues[i] = value;
      scale.add(value, contourColors[i]);
      value = value + delta;
    }
    
    // set the block renderer renderer
    XYBlockRenderer blockRenderer = new XYBlockRenderer();
    double mzWidth = 0.0;
    double mobilityWidth = 0.0;

    for (int i = 0; i + 1 < copyYValues.length; i++) {
      if (copyYValues[i] != copyYValues[i + 1]) {
        mobilityWidth = copyYValues[i + 1] - copyYValues[i];
        break;
      }
    }
    ArrayList<Double> deltas = new ArrayList<>();
    for (int i = 0; i + 1 < copyXValues.length; i++) {
      if (copyXValues[i] != copyXValues[i + 1]) {
        deltas.add(copyXValues[i + 1] - copyXValues[i]);
      }
    }

    Collections.sort(deltas);
    mzWidth = deltas.get(deltas.size() / 2);

    if (mobilityWidth <= 0.0 || mzWidth <= 0.0) {
      throw new IllegalArgumentException(
          "there must be atleast two unique value of retentio time and mobility");
    }

    blockRenderer.setBlockHeight(mobilityWidth);
    blockRenderer.setBlockWidth(mzWidth);
    NumberAxis domain3d = new NumberAxis("mz");
    plot3d = new XYPlot(dataset3d, domain3d,null,blockRenderer);

    // Legend
    NumberAxis scaleAxis = new NumberAxis("Intensity");
    scaleAxis.setRange(min, max);
    scaleAxis.setAxisLinePaint(Color.white);
    scaleAxis.setTickMarkPaint(Color.white);
    PaintScaleLegend legend = new PaintScaleLegend(scale, scaleAxis);

    legend.setStripOutlineVisible(false);
    legend.setAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
    legend.setAxisOffset(5.0);
    legend.setMargin(new RectangleInsets(5, 5, 5, 5));
    legend.setFrame(new BlockBorder(Color.white));
    legend.setPadding(new RectangleInsets(10, 10, 10, 10));
    legend.setStripWidth(10);
    legend.setPosition(RectangleEdge.RIGHT);
    legend.getAxis().setLabelFont(legendFont);
    legend.getAxis().setTickLabelFont(legendFont);

    // Set paint scale
    blockRenderer.setPaintScale(scale);
    plot3d.setRenderer(blockRenderer);
    plot3d.setBackgroundPaint(Color.black);
    plot3d.setRangeGridlinePaint(Color.black);
    plot3d.setAxisOffset(new RectangleInsets(5, 5, 5, 5));
    plot3d.setOutlinePaint(Color.black);
    plot3d.setDomainCrosshairPaint(Color.GRAY);
    plot3d.setRangeCrosshairPaint(Color.GRAY);
    plot3d.setDomainCrosshairVisible(true);
    plot3d.setRangeCrosshairVisible(true);
    plot3d.setOutlinePaint(Color.red);
    plot3d.setOutlineStroke(new BasicStroke(2.5f));

    CombinedRangeXYPlot plot = new CombinedRangeXYPlot(new NumberAxis("mobility"));
    //add all plots.
    plot.add(plot2d);
    plot.add(plot3d);

    chart = new JFreeChart("", JFreeChart.DEFAULT_TITLE_FONT, plot, true);
    chart.addSubtitle(legend);

    setChart(chart);
  }
}
