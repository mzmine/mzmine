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

package io.github.mzmine.gui.chartbasics.simplechart.renderers;

import io.github.mzmine.gui.chartbasics.simplechart.SimpleChartUtility;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.providers.ColorProvider;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;
import org.jfree.chart.LegendItem;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.chart.labels.XYSeriesLabelGenerator;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.DrawingSupplier;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYDataset;

public class ColoredXYBarRenderer extends XYBarRenderer {

  public static final float TRANSPARENCY = 0.8f;
  public static final AlphaComposite alphaComp =
      AlphaComposite.getInstance(AlphaComposite.SRC_OVER, TRANSPARENCY);
  private static final long serialVersionUID = 1L;

  private boolean isTransparent;
  private XYDataset currentDataset;

  public ColoredXYBarRenderer(boolean isTransparent) {
    this.isTransparent = isTransparent;
    setShadowVisible(false);
    // Set the tooltip generator
    setBarPainter(new StandardXYBarPainter());
    setDefaultItemLabelsVisible(true);

    SimpleChartUtility.tryApplyDefaultChartThemeToRenderer(this);
  }

  /**
   * This method returns null, because we don't want to change the colors dynamically.
   */
  public DrawingSupplier getDrawingSupplier() {
    return null;
  }

  @Override
  public void drawItem(Graphics2D g2, XYItemRendererState state,
      Rectangle2D dataArea, PlotRenderingInfo info, XYPlot plot,
      ValueAxis domainAxis, ValueAxis rangeAxis, XYDataset dataset,
      int series, int item, CrosshairState crosshairState, int pass) {

    if (!getItemVisible(series, item)) {
      return;
    }

    currentDataset = dataset;

    IntervalXYDataset intervalDataset = (IntervalXYDataset) dataset;

    double value0;
    double value1;
    if (super.getUseYInterval()) {
      value0 = intervalDataset.getStartYValue(series, item);
      value1 = intervalDataset.getEndYValue(series, item);
    } else {
      value0 = getBase();
      value1 = intervalDataset.getYValue(series, item);
    }
    if (Double.isNaN(value0) || Double.isNaN(value1)) {
      return;
    }
    if (value0 <= value1) {
      if (!rangeAxis.getRange().intersects(value0, value1)) {
        return;
      }
    } else {
      if (!rangeAxis.getRange().intersects(value1, value0)) {
        return;
      }
    }

    double translatedValue0 = rangeAxis.valueToJava2D(value0, dataArea,
        plot.getRangeAxisEdge());
    double translatedValue1 = rangeAxis.valueToJava2D(value1, dataArea,
        plot.getRangeAxisEdge());
    double bottom = Math.min(translatedValue0, translatedValue1);
    double top = Math.max(translatedValue0, translatedValue1);

    double startX = intervalDataset.getStartXValue(series, item);
    if (Double.isNaN(startX)) {
      return;
    }
    double endX = intervalDataset.getEndXValue(series, item);
    if (Double.isNaN(endX)) {
      return;
    }
    if (startX <= endX) {
      if (!domainAxis.getRange().intersects(startX, endX)) {
        return;
      }
    } else {
      if (!domainAxis.getRange().intersects(endX, startX)) {
        return;
      }
    }

    // is there an alignment adjustment to be made?
    if (getBarAlignmentFactor() >= 0.0 && getBarAlignmentFactor() <= 1.0) {
      double x = intervalDataset.getXValue(series, item);
      double interval = endX - startX;
      startX = x - interval * getBarAlignmentFactor();
      endX = startX + interval;
    }

    RectangleEdge location = plot.getDomainAxisEdge();
    double translatedStartX = domainAxis.valueToJava2D(startX, dataArea,
        location);
    double translatedEndX = domainAxis.valueToJava2D(endX, dataArea,
        location);

    double translatedWidth = Math.max(1, Math.abs(translatedEndX
        - translatedStartX));

    double left = Math.min(translatedStartX, translatedEndX);
    if (getMargin() > 0.0) {
      double cut = translatedWidth * getMargin();
      translatedWidth = translatedWidth - cut;
      left = left + cut / 2;
    }

    Rectangle2D bar = null;
    PlotOrientation orientation = plot.getOrientation();
    if (orientation.isHorizontal()) {
      // clip left and right bounds to data area
      bottom = Math.max(bottom, dataArea.getMinX());
      top = Math.min(top, dataArea.getMaxX());
      bar = new Rectangle2D.Double(
          bottom, left, top - bottom, translatedWidth);
    } else if (orientation.isVertical()) {
      // clip top and bottom bounds to data area
      bottom = Math.max(bottom, dataArea.getMinY());
      top = Math.min(top, dataArea.getMaxY());
      bar = new Rectangle2D.Double(left, bottom, translatedWidth,
          top - bottom);
    }

    boolean positive = (value1 > 0.0);
    boolean inverted = rangeAxis.isInverted();
    RectangleEdge barBase;
    if (orientation.isHorizontal()) {
      if (positive && inverted || !positive && !inverted) {
        barBase = RectangleEdge.RIGHT;
      } else {
        barBase = RectangleEdge.LEFT;
      }
    } else {
      if (positive && !inverted || !positive && inverted) {
        barBase = RectangleEdge.BOTTOM;
      } else {
        barBase = RectangleEdge.TOP;
      }
    }

    if (state.getElementHinting()) {
      beginElementGroup(g2, dataset.getSeriesKey(series), item);
    }
    if (getShadowsVisible()) {
      getBarPainter().paintBarShadow(g2, this, series, item, bar, barBase,
          !getUseYInterval());
    }
    if (currentDataset instanceof ColoredXYDataset) {
      g2.setPaint(((ColoredXYDataset) currentDataset).getAWTColor()); // set color
    }
    getBarPainter().paintBar(g2, this, series, item, bar, barBase);
    if (state.getElementHinting()) {
      endElementGroup(g2);
    }

    if (isItemLabelVisible(series, item)) {
      XYItemLabelGenerator generator = getItemLabelGenerator(series,
          item);
      drawItemLabel(g2, dataset, series, item, plot, generator, bar,
          value1 < 0.0);
    }

    // update the crosshair point
    double x1 = (startX + endX) / 2.0;
    double y1 = dataset.getYValue(series, item);
    double transX1 = domainAxis.valueToJava2D(x1, dataArea, location);
    double transY1 = rangeAxis.valueToJava2D(y1, dataArea,
        plot.getRangeAxisEdge());
    int datasetIndex = plot.indexOf(dataset);
    updateCrosshairValues(crosshairState, x1, y1, datasetIndex,
        transX1, transY1, plot.getOrientation());

    EntityCollection entities = state.getEntityCollection();
    if (entities != null) {
      addEntity(entities, bar, dataset, series, item, 0.0, 0.0);
    }
  }

  @Override
  public Paint getItemPaint(int row, int column) {
    if (currentDataset instanceof ColorProvider) {
      return ((ColorProvider) currentDataset).getAWTColor();
    }
    return super.getItemPaint(row, column);
  }

  @Override
  public LegendItem getLegendItem(int datasetIndex, int series) {
    XYPlot xyplot = getPlot();
    if (xyplot == null) {
      return null;
    }
    XYDataset dataset = xyplot.getDataset(datasetIndex);
    currentDataset = dataset;
    if (dataset == null) {
      return null;
    }
    LegendItem result;
    XYSeriesLabelGenerator lg = getLegendItemLabelGenerator();
    String label = lg.generateLabel(dataset, series);
    String description = label;
    String toolTipText = null;
    if (getLegendItemToolTipGenerator() != null) {
      toolTipText = getLegendItemToolTipGenerator().generateLabel(
          dataset, series);
    }
    String urlText = null;
    if (getLegendItemURLGenerator() != null) {
      urlText = getLegendItemURLGenerator().generateLabel(dataset,
          series);
    }
    Shape shape = getLegendBar();
    Paint paint = lookupSeriesPaint(series);
    Paint outlinePaint = lookupSeriesOutlinePaint(series);
    Stroke outlineStroke = lookupSeriesOutlineStroke(series);
    if (isDrawBarOutline()) {
      result = new LegendItem(label, description, toolTipText,
          urlText, shape, paint, outlineStroke, outlinePaint);
    } else {
      result = new LegendItem(label, description, toolTipText, urlText,
          shape, paint);
    }
    result.setLabelFont(lookupLegendTextFont(series));
    Paint labelPaint = lookupLegendTextPaint(series);
    if (labelPaint != null) {
      result.setLabelPaint(labelPaint);
    }
    result.setDataset(dataset);
    result.setDatasetIndex(datasetIndex);
    result.setSeriesKey(dataset.getSeriesKey(series));
    result.setSeriesIndex(series);
    if (getGradientPaintTransformer() != null) {
      result.setFillPaintTransformer(getGradientPaintTransformer());
    }
    return result;
  }

  @Override
  public Paint lookupSeriesPaint(int series) {
    if (currentDataset instanceof ColorProvider) {
      return ((ColorProvider) currentDataset).getAWTColor();
    }
    return super.lookupSeriesPaint(series);
  }
}
