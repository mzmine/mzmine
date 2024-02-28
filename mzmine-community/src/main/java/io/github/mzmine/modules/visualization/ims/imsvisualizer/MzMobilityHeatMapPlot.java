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
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.fx.interaction.ChartMouseEventFX;
import org.jfree.chart.fx.interaction.ChartMouseListenerFX;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.LookupPaintScale;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.xy.XYZDataset;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.chartbasics.chartutils.XYBlockPixelSizeRenderer;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScale;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScaleFactory;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.ims.ImsVisualizerTask;

public class MzMobilityHeatMapPlot extends EChartViewer {

  private XYPlot plot;
  private JFreeChart chart;
  private XYZDataset dataset3d;
  static final Font legendFont = new Font("SansSerif", Font.PLAIN, 12);
  private EStandardChartTheme theme;
  private final double selectedRetentionTime;
  private PaintScaleLegend legend;
  private XYBlockPixelSizeRenderer pixelRenderer;
  private XYBlockRenderer blockRenderer;

  public MzMobilityHeatMapPlot(XYZDataset dataset, PaintScale paintScale, ImsVisualizerTask imsTask,
      IntensityMobilityPlot implot) {

    super(ChartFactory.createScatterPlot("", "m/z", "mobility", dataset, PlotOrientation.VERTICAL,
        true, true, true));

    chart = getChart();
    this.dataset3d = dataset;
    this.selectedRetentionTime = imsTask.getSelectedRetentionTime();

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
    PaintScaleFactory paintScaleFactoy = new PaintScaleFactory();
    paintScaleFactoy.createColorsForPaintScale(paintScale);
    // contourColors = XYBlockPixelSizePaintScales.scaleAlphaForPaintScale(contourColors);

    plot = chart.getXYPlot();
    theme = MZmineCore.getConfiguration().getDefaultChartTheme();
    theme.apply(chart);

    // set the pixel renderer
    setPixelRenderer(copyXValues, copyYValues, paintScale);
    // Legend
    prepareLegend(min, max, paintScale);

    // Set paint scale
    blockRenderer.setPaintScale(paintScale);

    plot.setRenderer(blockRenderer);
    plot.setBackgroundPaint(Color.black);
    plot.setRangeGridlinePaint(Color.black);
    plot.setAxisOffset(new RectangleInsets(5, 5, 5, 5));
    plot.setOutlinePaint(Color.black);
    chart.addSubtitle(legend);

    // mouse listener.
    addChartMouseListener(new ChartMouseListenerFX() {
      @Override
      public void chartMouseClicked(ChartMouseEventFX event) {
        ChartEntity chartEntity = event.getEntity();

        if (event.getTrigger().getClickCount() == 2) {
          // if chartintity is not selected any valid point find it's nearest.
          if (!(chartEntity instanceof XYItemEntity)) {
            int x = (int) ((event.getTrigger().getX() - getInsets().getLeft()) / getScaleX());
            int y = (int) ((event.getTrigger().getY() - getInsets().getRight()) / getScaleY());
            Point2D point2d = new Point2D.Double(x, y);
            double minDistance = Integer.MAX_VALUE;
            Collection entities = getRenderingInfo().getEntityCollection().getEntities();
            for (Iterator iter = entities.iterator(); iter.hasNext();) {
              ChartEntity element = (ChartEntity) iter.next();

              if (element instanceof XYItemEntity) {
                Rectangle rect = element.getArea().getBounds();
                Point2D centerPoint = new Point2D.Double(rect.getCenterX(), rect.getCenterY());

                if (point2d.distance(centerPoint) < minDistance) {
                  minDistance = point2d.distance(centerPoint);
                  chartEntity = element;
                }
              }
            }
          }

          if (chartEntity instanceof XYItemEntity) {
            XYItemEntity entity = (XYItemEntity) chartEntity;
            int serindex = entity.getSeriesIndex();
            int itemindex = entity.getItem();
            double mobility = 0;
            mobility = dataset.getYValue(serindex, itemindex);
            List<Scan> selectedScan = imsTask.getSelectedScans();
            for (int i = 0; i < imsTask.getScans().length; i++) {
              if (selectedScan.get(i).getMobility() == mobility
                  && selectedRetentionTime == selectedScan.get(i).getRetentionTime()) {
                break;
              }
            }
            implot.showSelectedScan();
          }
        }
      }

      @Override
      public void chartMouseMoved(ChartMouseEventFX event) {}
    });

  }

  void setPixelRenderer(double[] copyXValues, double[] copyYValues, LookupPaintScale scale) {
    pixelRenderer = new XYBlockPixelSizeRenderer();
    pixelRenderer.setPaintScale(scale);

    // set the block renderer renderer
    blockRenderer = new XYBlockRenderer();
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
  }

  public void prepareLegend(double min, double max, LookupPaintScale scale) {
    NumberAxis scaleAxis = new NumberAxis("Intensity");
    scaleAxis.setRange(min, max);
    scaleAxis.setAxisLinePaint(Color.white);
    scaleAxis.setTickMarkPaint(Color.white);
    legend = new PaintScaleLegend(scale, scaleAxis);
    legend.setStripOutlineVisible(false);
    legend.setAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
    legend.setAxisOffset(5.0);
    legend.setSubdivisionCount(500);
    legend.setPosition(RectangleEdge.RIGHT);
    legend.getAxis().setLabelFont(legendFont);
    legend.getAxis().setTickLabelFont(legendFont);
  }

}*/
