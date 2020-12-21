package io.github.mzmine.modules.dataprocessing.featdet_imagebuilder.imageplot;

import java.awt.Color;
import java.awt.Font;
import java.text.DecimalFormat;
import java.util.Arrays;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.LookupPaintScale;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.xy.XYZDataset;
import com.google.common.collect.Range;
import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.chartbasics.chartutils.XYBlockPixelSizePaintScales;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.main.MZmineCore;

public class ImageHeatMapPlot extends EChartViewer {

  private final XYPlot plot;
  static final Font legendFont = new Font("SansSerif", Font.PLAIN, 8);
  private PaintScaleLegend legend;
  private XYBlockRenderer blockRenderer;
  private double dataPointHeight;
  private double dataPointWidth;

  public ImageHeatMapPlot(XYZDataset dataset, String paintScaleStyle, double dataPointWidth,
      double dataPointHeight) {

    super(ChartFactory.createScatterPlot("", "[\u00B5m]", "[\u00B5m]", dataset,
        PlotOrientation.VERTICAL, true, true, true));

    this.dataPointWidth = dataPointWidth;
    this.dataPointHeight = dataPointHeight;

    JFreeChart chart = getChart();
    // copy and sort z-Values for min and max of the paint scale
    double[] copyZValues = new double[dataset.getItemCount(0)];
    for (int i = 0; i < dataset.getItemCount(0); i++) {
      copyZValues[i] = dataset.getZValue(0, i);
    }
    Arrays.sort(copyZValues);

    // get index in accordance to percentile windows
    int minIndexScale = 0;
    int maxIndexScale = copyZValues.length - 1;
    double min = copyZValues[minIndexScale];
    double max = copyZValues[maxIndexScale];
    Color[] contourColors =
        XYBlockPixelSizePaintScales.getPaintColors("", Range.closed(min, max), paintScaleStyle);
    // contourColors = XYBlockPixelSizePaintScales.scaleAlphaForPaintScale(contourColors);
    LookupPaintScale scale = new LookupPaintScale(min, max, Color.BLACK);

    double[] scaleValues = new double[contourColors.length];
    double delta = (max - min) / (contourColors.length - 1);
    double value = min;
    for (int i = 0; i < contourColors.length; i++) {
      scaleValues[i] = value;
      scale.add(value, contourColors[i]);
      value = value + delta;
    }

    plot = chart.getXYPlot();
    EStandardChartTheme theme = MZmineCore.getConfiguration().getDefaultChartTheme();
    theme.apply(chart);
    ((NumberAxis) chart.getXYPlot().getRangeAxis())
        .setNumberFormatOverride(new DecimalFormat("0.0E0"));
    ((NumberAxis) chart.getXYPlot().getDomainAxis())
        .setNumberFormatOverride(new DecimalFormat("0.0E0"));
    setPixelRenderer();
    prepareLegend(min, max, scale);

    blockRenderer.setPaintScale(scale);
    plot.setRenderer(blockRenderer);
    plot.setBackgroundPaint(Color.black);
    plot.setRangeGridlinePaint(Color.black);
    plot.setAxisOffset(new RectangleInsets(5, 5, 5, 5));
    plot.setOutlinePaint(Color.black);
    chart.addSubtitle(legend);

  }

  private void setPixelRenderer() {
    blockRenderer = new XYBlockRenderer();
    blockRenderer.setBlockHeight(dataPointHeight);
    blockRenderer.setBlockWidth(dataPointWidth);
  }

  private void prepareLegend(double min, double max, LookupPaintScale scale) {
    NumberAxis scaleAxis = new NumberAxis(null);
    scaleAxis.setNumberFormatOverride(new DecimalFormat("0.0E0"));
    scaleAxis.setRange(min, max);
    scaleAxis.setAxisLinePaint(Color.white);
    scaleAxis.setTickMarkPaint(Color.white);
    legend = new PaintScaleLegend(scale, scaleAxis);
    legend.setPadding(5, 0, 5, 0);
    legend.setStripOutlineVisible(false);
    legend.setAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
    legend.setAxisOffset(5.0);
    legend.setSubdivisionCount(500);
    legend.setPosition(RectangleEdge.RIGHT);
    legend.getAxis().setLabelFont(legendFont);
    legend.getAxis().setTickLabelFont(legendFont);
  }

  public XYPlot getPlot() {
    return plot;
  }
}
