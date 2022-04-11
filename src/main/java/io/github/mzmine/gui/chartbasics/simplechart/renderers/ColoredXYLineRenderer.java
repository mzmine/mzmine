/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.gui.chartbasics.simplechart.renderers;

import io.github.mzmine.gui.chartbasics.simplechart.SimpleChartUtility;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
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
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;


/**
 * The standard line renderer for {@link SimpleXYChart}s.
 * <p></p>
 * This renderer has been modified to draw a dataset, generate labels and legend items based on the
 * color specified by the dataset.
 * <p>
 * Todo: pregenerate item labels to speed up gui. Maybe store all maxima and just compare those?
 *
 * @author https://github.com/SteffenHeu
 */
public class ColoredXYLineRenderer extends XYLineAndShapeRenderer {

  private static final long serialVersionUID = 1L;
  private double transparency = 1.0f;

  private XYDataset currentDataset;

  public ColoredXYLineRenderer() {
    super(true, false);
    setDrawSeriesLineAsPath(true);
    SimpleChartUtility.tryApplyDefaultChartThemeToRenderer(this);
  }

  private AlphaComposite makeComposite(double alpha) {
    int type = AlphaComposite.SRC_OVER;
    return (AlphaComposite.getInstance(type, (float) alpha));
  }

  @Override
  protected void drawItemLabel(Graphics2D g2, PlotOrientation orientation, XYDataset dataset,
      int series, int item, double x, double y, boolean negative) {
    super.drawItemLabel(g2, orientation, dataset, series, item, x, y, negative);
  }

  @Override
  public void drawItem(Graphics2D g2, XYItemRendererState state, Rectangle2D dataArea,
      PlotRenderingInfo info, XYPlot plot, ValueAxis domainAxis, ValueAxis rangeAxis,
      XYDataset dataset, int series, int item, CrosshairState crosshairState, int pass) {

    g2.setComposite(makeComposite(transparency));
    currentDataset = dataset;

    super.drawItem(g2, state, dataArea, info, plot, domainAxis, rangeAxis, dataset, series, item,
        crosshairState, pass);

  }

  @Override
  protected void drawPrimaryLine(XYItemRendererState state, Graphics2D g2, XYPlot plot,
      XYDataset dataset, int pass, int series, int item, ValueAxis domainAxis, ValueAxis rangeAxis,
      Rectangle2D dataArea) {

    g2.setComposite(makeComposite(transparency));

    super.drawPrimaryLine(state, g2, plot, dataset, pass, series, item, domainAxis, rangeAxis,
        dataArea);

  }

  @Override
  protected void drawFirstPassShape(Graphics2D g2, int pass, int series, int item, Shape shape) {
    g2.setComposite(makeComposite(transparency));
    g2.setStroke(getItemStroke(series, item));
    g2.setPaint(getItemPaint(series, item));
    g2.draw(shape);
  }

  protected void drawPrimaryLineAsPath(XYItemRendererState state, Graphics2D g2, XYPlot plot,
      XYDataset dataset, int pass, int series, int item, ValueAxis domainAxis, ValueAxis rangeAxis,
      Rectangle2D dataArea) {

    g2.setComposite(makeComposite(transparency));

    super.drawPrimaryLineAsPath(state, g2, plot, dataset, pass, series, item, domainAxis, rangeAxis,
        dataArea);

  }

  @Override
  protected void drawSecondaryPass(Graphics2D g2, XYPlot plot, XYDataset dataset, int pass,
      int series, int item, ValueAxis domainAxis, Rectangle2D dataArea, ValueAxis rangeAxis,
      CrosshairState crosshairState, EntityCollection entities) {

    g2.setComposite(makeComposite(transparency));

    super.drawSecondaryPass(g2, plot, dataset, pass, series, item, domainAxis, dataArea, rangeAxis,
        crosshairState, entities);

  }

  /**
   * Returns the paint used to fill data items as they are drawn.  The default implementation passes
   * control to the {@link #lookupSeriesFillPaint(int)} method - you can override this method if you
   * require different behaviour.
   *
   * @param row    the row (or series) index (zero-based).
   * @param column the column (or category) index (zero-based).
   * @return The paint (never {@code null}).
   */
  @Override
  public Paint getItemFillPaint(int row, int column) {
    if (currentDataset instanceof ColorProvider) {
      return ((ColorProvider) currentDataset).getAWTColor();
    }
    return super.getItemFillPaint(row, column);
  }

  /**
   * Returns a legend item for the specified series.
   *
   * @param datasetIndex the dataset index (zero-based).
   * @param series       the series index (zero-based).
   * @return A legend item for the series (possibly {@code null}).
   */
  @Override
  public LegendItem getLegendItem(int datasetIndex, int series) {
    XYPlot plot = getPlot();
    if (plot == null) {
      return null;
    }

    XYDataset dataset = getPlot().getDataset(datasetIndex);
    if (dataset == null) {
      return null;
    }

    if (!getItemVisible(series, 0)) {
      return null;
    }
    String label = getLegendItemLabelGenerator().generateLabel(dataset,
        series);
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
    boolean shapeIsVisible = getItemShapeVisible(series, 0);
    Shape shape = lookupLegendShape(series);
    boolean shapeIsFilled = getItemShapeFilled(series, 0);
    Paint fillPaint = (super.getUseFillPaint() ? lookupSeriesFillPaint(series)
        : lookupSeriesPaint(series));
    fillPaint = (dataset instanceof ColorProvider) ?
        ((ColorProvider) dataset).getAWTColor() : fillPaint;

    boolean shapeOutlineVisible = super.getDrawOutlines();
    Paint outlinePaint = (super.getUseOutlinePaint() ? lookupSeriesOutlinePaint(
        series) : lookupSeriesPaint(series));

    Stroke outlineStroke = lookupSeriesOutlineStroke(series);
    boolean lineVisible = getItemLineVisible(series, 0);
    Stroke lineStroke = lookupSeriesStroke(series);
    Paint linePaint = lookupSeriesPaint(series);
    linePaint = (dataset instanceof ColorProvider) ?
        ((ColorProvider) dataset).getAWTColor() : linePaint;

    LegendItem result = new LegendItem(label, description, toolTipText,
        urlText, shapeIsVisible, shape, shapeIsFilled, fillPaint,
        shapeOutlineVisible, outlinePaint, outlineStroke, lineVisible,
        super.getLegendLine(), lineStroke, linePaint);

    result.setLabelFont(lookupLegendTextFont(series));
    Paint labelPaint = lookupLegendTextPaint(series);
    if (labelPaint != null) {
      result.setLabelPaint(labelPaint);
    }
    result.setSeriesKey(dataset.getSeriesKey(series));
    result.setSeriesIndex(series);
    result.setDataset(dataset);
    result.setDatasetIndex(datasetIndex);

    return result;
  }

  @Override
  public Paint getItemPaint(int row, int column) {
    if (currentDataset instanceof ColorProvider) {
      return ((ColorProvider) currentDataset).getAWTColor();
    }
    return lookupSeriesPaint(row);
  }

}
