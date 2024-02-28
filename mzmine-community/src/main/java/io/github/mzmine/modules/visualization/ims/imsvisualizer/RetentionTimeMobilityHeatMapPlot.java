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

package io.github.mzmine.modules.visualization.ims.imsvisualizer;
/*

import java.awt.Color;
import java.awt.Font;
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
import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.chartbasics.chartutils.XYBlockPixelSizeRenderer;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScale;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScaleFactory;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.main.MZmineCore;

public class RetentionTimeMobilityHeatMapPlot extends EChartViewer {

  private final XYPlot plot;
  static final Font legendFont = new Font("SansSerif", Font.PLAIN, 12);
  private PaintScaleLegend legend;
  public XYBlockPixelSizeRenderer pixelRenderer; // What does this renderer do? ~SteffenHeu
  public XYBlockRenderer blockRenderer;

  public RetentionTimeMobilityHeatMapPlot(XYZDataset dataset, PaintScale paintScale) {

    super(ChartFactory.createScatterPlot("", "retention time", "mobility", dataset,
        PlotOrientation.VERTICAL, true, true, true));

    JFreeChart chart = getChart();
    // copy and sort z-Values for min and max of the paint scale
    double[] copyZValues = new double[dataset.getItemCount(0)];
    for (int i = 0; i < dataset.getItemCount(0); i++) {
      copyZValues[i] = dataset.getZValue(0, i);
    }
    Arrays.sort(copyZValues);

    // copy and sort x-values.
    double[] copyXValues = new double[dataset.getItemCount(0)];
    for (int i = 0; i < dataset.getItemCount(0); i++) {
      copyXValues[i] = dataset.getXValue(0, i);
    }
    Arrays.sort(copyXValues);

    // copy and sort y-values.
    double[] copyYValues = new double[dataset.getItemCount(0)];
    for (int i = 0; i < dataset.getItemCount(0); i++) {
      copyYValues[i] = dataset.getYValue(0, i);
    }
    Arrays.sort(copyYValues);

    // get index in accordance to percentile windows
    int minIndexScale = 0;
    int maxIndexScale = copyZValues.length - 1;
    double min = copyZValues[minIndexScale];
    double max = copyZValues[maxIndexScale];
    PaintScaleFactory paintScaleFactoy = new PaintScaleFactory();
    paintScaleFactoy.createColorsForPaintScale(paintScale);
    // contourColors = XYBlockPixelSizePaintScales.scaleAlphaForPaintScale(contourColors);


    plot = chart.getXYPlot();
    EStandardChartTheme theme = MZmineCore.getConfiguration().getDefaultChartTheme();
    theme.apply(chart);

    // set the pixel renderer
    setPixelRenderer(copyXValues, copyYValues, paintScale);
    // set the legend
    prepareLegend(min, max, paintScale);

    blockRenderer.setPaintScale(paintScale);
    plot.setRenderer(blockRenderer);
    plot.setBackgroundPaint(Color.black);
    plot.setRangeGridlinePaint(Color.black);
    plot.setAxisOffset(new RectangleInsets(5, 5, 5, 5));
    plot.setOutlinePaint(Color.black);
    chart.addSubtitle(legend);
    plot.getRangeAxis().setStandardTickUnits(NumberAxis.createIntegerTickUnits());

  }

  void setPixelRenderer(double[] copyXValues, double[] copyYValues, LookupPaintScale scale) {
    pixelRenderer = new XYBlockPixelSizeRenderer();
    pixelRenderer.setPaintScale(scale);
    // set the block renderer
    blockRenderer = new XYBlockRenderer();
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
          "there must be at least two unique value of retention time and mobility");
    }
    blockRenderer.setBlockHeight(mobilityWidth);
    blockRenderer.setBlockWidth(retentionWidth);
  }

  void prepareLegend(double min, double max, LookupPaintScale scale) {
    NumberAxis scaleAxis = new NumberAxis("Intensity");
    scaleAxis.setRange(min, max);
    scaleAxis.setAxisLinePaint(Color.white);
    scaleAxis.setTickMarkPaint(Color.white);
    legend = new PaintScaleLegend(scale, scaleAxis);
    legend.setStripOutlineVisible(false);
    legend.setAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
    legend.setAxisOffset(5.0);
    legend.setSubdivisionCount(500);
    legend.setPosition(RectangleEdge.TOP);
    legend.getAxis().setLabelFont(legendFont);
    legend.getAxis().setTickLabelFont(legendFont);
  }

  public XYPlot getPlot() {
    return plot;
  }
}
*/