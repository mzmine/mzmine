package io.github.mzmine.modules.visualization.ims;

import com.google.common.collect.Range;
import io.github.mzmine.gui.chartbasics.chartutils.XYBlockPixelSizePaintScales;
import io.github.mzmine.gui.chartbasics.chartutils.XYBlockPixelSizeRenderer;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.parameters.ParameterSet;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.LookupPaintScale;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.xy.XYZDataset;

import java.awt.*;
import java.util.Arrays;
import java.util.logging.Logger;

public class MobilityRetentionHeatMap extends EChartViewer {

  private XYPlot plot;
  private String paintScaleStyle;
  private JFreeChart chart3d;
  private XYZDataset dataset3d;
  private Logger logger = Logger.getLogger(this.getClass().getName());
  static final Font legendFont = new Font("SansSerif", Font.PLAIN, 12);

  public MobilityRetentionHeatMap(XYZDataset dataset, String paintScaleStyle) {

    super(
        ChartFactory.createScatterPlot(
            "",
            "retention time",
            "mobility",
            dataset,
            PlotOrientation.VERTICAL,
            true,
            true,
            true));

    chart3d = getChart();
    this.dataset3d = dataset;
    this.paintScaleStyle = paintScaleStyle;

    // copy and sort z-Values for min and max of the paint scale
    double[] copyZValues = new double[dataset3d.getItemCount(0)];
    for (int i = 0; i < dataset3d.getItemCount(0); i++) {
      copyZValues[i] = dataset3d.getZValue(0, i);
    }
    Arrays.sort(copyZValues);

    // copy and sort x-values.
    double[] copyXValues = new double[dataset3d.getItemCount(0)];
    for (int i = 0; i < dataset3d.getItemCount(0); i++) {
      copyXValues[i] = dataset3d.getXValue(0, i);
    }
    Arrays.sort(copyXValues);

    // copy and sort y-values.
    double[] copyYValues = new double[dataset3d.getItemCount(0)];
    for (int i = 0; i < dataset3d.getItemCount(0); i++) {
      copyYValues[i] = dataset3d.getYValue(0, i);
    }
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

    plot = chart3d.getXYPlot();

    // set the pixel renderer
    XYBlockPixelSizeRenderer pixelRenderer = new XYBlockPixelSizeRenderer();
    pixelRenderer.setPaintScale(scale);
    // set the block renderer
    XYBlockRenderer blockRenderer = new XYBlockRenderer();
    double retentionWidth = 0.0;
    double mobilityWidth = 0.0;

    for (int i = 0; i + 1 < copyXValues.length; i++) {
      if (copyXValues[i] != copyXValues[i + 1]) {
        retentionWidth = copyXValues[i + 1] - copyXValues[i];
        break;
      }
    }
    for (int i = 0; i + 1 < copyYValues.length; i++) {
      if (copyYValues[i] != copyYValues[i + 1]) {
        mobilityWidth = copyYValues[i + 1] - copyYValues[i];
        break;
      }
    }

    if (mobilityWidth <= 0.0 || retentionWidth <= 0.0) {
      throw new IllegalArgumentException(
          "there must be atleast two unique value of retentio time and mobility");
    }
    blockRenderer.setBlockHeight(mobilityWidth);
    blockRenderer.setBlockWidth(retentionWidth);

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
    legend.setPosition(RectangleEdge.LEFT);
    legend.getAxis().setLabelFont(legendFont);
    legend.getAxis().setTickLabelFont(legendFont);

    // Set paint scale
    blockRenderer.setPaintScale(scale);

    plot.setRenderer(blockRenderer);
    plot.setBackgroundPaint(Color.black);
    plot.setRangeGridlinePaint(Color.black);
    plot.setAxisOffset(new RectangleInsets(5, 5, 5, 5));
    plot.setOutlinePaint(Color.black);
    plot.setDomainCrosshairPaint(Color.GRAY);
    plot.setRangeCrosshairPaint(Color.GRAY);
    plot.setDomainCrosshairVisible(true);
    plot.setRangeCrosshairVisible(true);
    plot.setOutlinePaint(Color.red);
    plot.setOutlineStroke(new BasicStroke(2.5f));

    chart3d.addSubtitle(legend);
  }
}
