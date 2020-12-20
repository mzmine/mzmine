/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.gui.chartbasics.gui.javafx.template;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.util.ShapeUtils;
import org.jfree.data.xy.XYDataset;


/**
 * Nasty renderer blatantly copied together from {@link XYLineAndShapeRenderer} and {@link
 * org.jfree.chart.renderer.xy.SamplingXYLineRenderer}
 */
public class ColoredXYLineRenderer extends XYLineAndShapeRenderer {

  private static final long serialVersionUID = 1L;
  private double transparency = 1.0f;

  public ColoredXYLineRenderer() {
    super(true, false);
    setDrawSeriesLineAsPath(true);
  }

  private AlphaComposite makeComposite(double alpha) {
    int type = AlphaComposite.SRC_OVER;
    return (AlphaComposite.getInstance(type, (float) alpha));
  }

  @Override
  public XYItemRendererState initialise(Graphics2D g2,
      Rectangle2D dataArea, XYPlot plot, XYDataset data,
      PlotRenderingInfo info) {

    double dpi = 100; // this seems to control the resolution of the drawn plot
    //        Integer dpiVal = (Integer) g2.getRenderingHint(HintKey.DPI);
    //        if (dpiVal != null) {
    //            dpi = dpiVal.intValue();
    //        }
    State state = new State(info);
    state.seriesPath = new GeneralPath();
    state.intervalPath = new GeneralPath();
    state.dX = 72.0 / dpi;
    return state;
  }

  @Override
  public void drawItem(Graphics2D g2, XYItemRendererState state, Rectangle2D dataArea,
      PlotRenderingInfo info, XYPlot plot, ValueAxis domainAxis, ValueAxis rangeAxis,
      XYDataset dataset, int series, int item, CrosshairState crosshairState, int pass) {

    if (!isLinePass(pass)) {
      return;
    }

    Paint seriesColor = (dataset instanceof ColoredXYDataset) ?
        ((ColoredXYDataset) dataset).getAWTColor() : getItemPaint(series, item);

    g2.setPaint(seriesColor);

    // setup for collecting optional entity info...
    EntityCollection entities = null;
    if (info != null && info.getOwner() != null) {
      entities = info.getOwner().getEntityCollection();
    }

    g2.setComposite(makeComposite(transparency));

    // do nothing if item is not visible
    if (!getItemVisible(series, item)) {
      return;
    }
    RectangleEdge xAxisLocation = plot.getDomainAxisEdge();
    RectangleEdge yAxisLocation = plot.getRangeAxisEdge();

    // get the data point...
    double x1 = dataset.getXValue(series, item);
    double y1 = dataset.getYValue(series, item);
    double transX1 = domainAxis.valueToJava2D(x1, dataArea, xAxisLocation);
    double transY1 = rangeAxis.valueToJava2D(y1, dataArea, yAxisLocation);

    State s = (State) state;
    // update path to reflect latest point
    if (!Double.isNaN(transX1) && !Double.isNaN(transY1)) {
      float x = (float) transX1;
      float y = (float) transY1;
      PlotOrientation orientation = plot.getOrientation();
      if (orientation == PlotOrientation.HORIZONTAL) {
        x = (float) transY1;
        y = (float) transX1;
      }
      if (s.lastPointGood) {
        if ((Math.abs(x - s.lastX) > s.dX)) {
          s.seriesPath.lineTo(x, y);
          if (s.lowY < s.highY) {
            s.intervalPath.moveTo((float) s.lastX, (float) s.lowY);
            s.intervalPath.lineTo((float) s.lastX, (float) s.highY);
          }
          s.lastX = x;
          s.openY = y;
          s.highY = y;
          s.lowY = y;
          s.closeY = y;

          // -----
          Shape entityArea = null;
          if (Double.isNaN(y1) || Double.isNaN(x1)) {
            return;
          }

          if (getItemShapeVisible(series, item)) {
            Shape shape = getItemShape(series, item);
            if (orientation == PlotOrientation.HORIZONTAL) {
              shape = ShapeUtils.createTranslatedShape(shape, transY1,
                  transX1);
            } else if (orientation == PlotOrientation.VERTICAL) {
              shape = ShapeUtils.createTranslatedShape(shape, transX1,
                  transY1);
            }
            entityArea = shape;
            if (shape.intersects(dataArea)) {
              g2.setPaint(seriesColor);
              g2.fill(shape);
            }
          }

          double xx = transX1;
          double yy = transY1;
          if (orientation == PlotOrientation.HORIZONTAL) {
            xx = transY1;
            yy = transX1;
          }

          // draw the item label if there is one...
          if (isItemLabelVisible(series, item)) {
            drawItemLabel(g2, orientation, dataset, series, item, xx, yy,
                (y1 < 0.0));
          }

          int datasetIndex = plot.indexOf(dataset);
          updateCrosshairValues(crosshairState, x1, y1, datasetIndex,
              transX1, transY1, orientation);

          // add an entity for the item, but only if it falls within the data
          // area...
          if (entities != null && ShapeUtils.isPointInRect(dataArea, xx, yy)) {
            addEntity(entities, entityArea, dataset, series, item, xx, yy);
          }
          // -----

        } else {
          s.highY = Math.max(s.highY, y);
          s.lowY = Math.min(s.lowY, y);
          s.closeY = y;
        }
      } else {
        s.seriesPath.moveTo(x, y);
        s.lastX = x;
        s.openY = y;
        s.highY = y;
        s.lowY = y;
        s.closeY = y;
      }
      s.lastPointGood = true;
    } else {
      s.lastPointGood = false;
    }
    // if this is the last item, draw the path ...
    if (item == s.getLastItemIndex()) {
      // draw path
      PathIterator pi = s.seriesPath.getPathIterator(null);
      int count = 0;
      while (!pi.isDone()) {
        count++;
        pi.next();
      }
      g2.setStroke(getItemStroke(series, item));
      g2.draw(s.seriesPath);
      g2.draw(s.intervalPath);
    }
  }

  protected void drawPrimaryLine(XYItemRendererState state, Graphics2D g2, XYPlot plot,
      XYDataset dataset, int pass, int series, int item, ValueAxis domainAxis, ValueAxis rangeAxis,
      Rectangle2D dataArea) {

    g2.setComposite(makeComposite(transparency));

    super.drawPrimaryLine(state, g2, plot, dataset, pass, series, item, domainAxis, rangeAxis,
        dataArea);

  }

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

  protected void drawSecondaryPass(Graphics2D g2, XYPlot plot, XYDataset dataset, int pass,
      int series, int item, ValueAxis domainAxis, Rectangle2D dataArea, ValueAxis rangeAxis,
      CrosshairState crosshairState, EntityCollection entities) {

    g2.setComposite(makeComposite(transparency));

    super.drawSecondaryPass(g2, plot, dataset, pass, series, item, domainAxis, dataArea, rangeAxis,
        crosshairState, entities);

  }

  /**
   * Records the state for the renderer.  This is used to preserve state information between calls
   * to the drawItem() method for a single chart drawing.
   */
  public static class State extends XYItemRendererState {

    /**
     * The path for the current series.
     */
    GeneralPath seriesPath;

    /**
     * A second path that draws vertical intervals to cover any extreme values.
     */
    GeneralPath intervalPath;

    /**
     * The minimum change in the x-value needed to trigger an update to the seriesPath.
     */
    double dX = 1.0;

    /**
     * The last x-coordinate visited by the seriesPath.
     */
    double lastX;

    /**
     * The initial y-coordinate for the current x-coordinate.
     */
    double openY = 0.0;

    /**
     * The highest y-coordinate for the current x-coordinate.
     */
    double highY = 0.0;

    /**
     * The lowest y-coordinate for the current x-coordinate.
     */
    double lowY = 0.0;

    /**
     * The final y-coordinate for the current x-coordinate.
     */
    double closeY = 0.0;

    /**
     * A flag that indicates if the last (x, y) point was 'good' (non-null).
     */
    boolean lastPointGood;

    /**
     * Creates a new state instance.
     *
     * @param info the plot rendering info.
     */
    public State(PlotRenderingInfo info) {
      super(info);
    }

    /**
     * This method is called by the {@link XYPlot} at the start of each series pass.  We reset the
     * state for the current series.
     *
     * @param dataset   the dataset.
     * @param series    the series index.
     * @param firstItem the first item index for this pass.
     * @param lastItem  the last item index for this pass.
     * @param pass      the current pass index.
     * @param passCount the number of passes.
     */
    @Override
    public void startSeriesPass(XYDataset dataset, int series,
        int firstItem, int lastItem, int pass, int passCount) {
      this.seriesPath.reset();
      this.intervalPath.reset();
      this.lastPointGood = false;
      super.startSeriesPass(dataset, series, firstItem, lastItem, pass,
          passCount);
    }

  }

}
