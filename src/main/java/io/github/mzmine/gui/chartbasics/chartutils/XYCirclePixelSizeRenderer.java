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

package io.github.mzmine.gui.chartbasics.chartutils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.Arrays;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.event.RendererChangeEvent;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.LookupPaintScale;
import org.jfree.chart.renderer.PaintScale;
import org.jfree.chart.renderer.xy.AbstractXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.util.Args;
import org.jfree.chart.util.PublicCloneable;
import org.jfree.data.Range;
import org.jfree.data.general.DatasetUtils;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;
import io.github.mzmine.modules.visualization.kendrickmassplot.KendrickMassPlotXYDataset;
import io.github.mzmine.modules.visualization.kendrickmassplot.KendrickMassPlotXYZDataset;

/**
 * A renderer that represents data from an {@link XYZDataset} by drawing a circle at each (x,
 */
public class XYCirclePixelSizeRenderer extends AbstractXYItemRenderer
    implements XYItemRenderer, Cloneable, PublicCloneable, Serializable {
  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  /**
   * The circle width (defaults to 1.0).
   */
  private double circleWidth = 1.0;

  /**
   * The circle height (defaults to 1.0).
   */
  private double circleHeight = 1.0;


  /**
   * The anchor point used to align each circle to its (x, y) location. The default value is
   * {@code RectangleAnchor.CENTER}.
   */
  private RectangleAnchor circleAnchor = RectangleAnchor.CENTER;

  /** Temporary storage for the x-offset used to align the circle anchor. */
  private double xOffset;

  /** Temporary storage for the y-offset used to align the circle anchor. */
  private double yOffset;

  /** The paint scale. */
  private PaintScale paintScale;

  /**
   * Creates a new {@code XYCircleRenderer} instance with default attributes.
   */
  public XYCirclePixelSizeRenderer() {
    updateOffsets();
    this.paintScale = new LookupPaintScale();
  }

  /**
   * Returns the circle width, in data/axis units.
   *
   * @return The circle width.
   *
   * @see #setCircleWidth(double)
   */
  public double getCircleWidth() {
    return this.circleWidth;
  }

  /**
   * Sets the width of the circles used to represent each data item and sends a
   * {@link RendererChangeEvent} to all registered listeners.
   *
   * @param width the new width, in data/axis units (must be &gt; 0.0).
   *
   * @see #getCircleWidth()
   */
  public void setCircleWidth(double width) {
    if (width <= 0.0) {
      throw new IllegalArgumentException("The 'width' argument must be > 0.0");
    }
    this.circleWidth = width;
    updateOffsets();
    fireChangeEvent();
  }

  /**
   * Returns the circle height, in data/axis units.
   *
   * @return The circle height.
   *
   * @see #setCircleHeight(double)
   */
  public double getCircleHeight() {
    return this.circleHeight;
  }

  /**
   * Sets the height of the circles used to represent each data item and sends a
   * {@link RendererChangeEvent} to all registered listeners.
   *
   * @param height the new height, in data/axis units (must be &gt; 0.0).
   *
   * @see #getCircleHeight()
   */
  public void setCircleHeight(double height) {
    if (height <= 0.0) {
      throw new IllegalArgumentException("The 'height' argument must be > 0.0");
    }
    this.circleHeight = height;
    updateOffsets();
    fireChangeEvent();
  }


  /**
   * Returns the anchor point used to align a circle at its (x, y) location. The default values is
   * {@link RectangleAnchor#CENTER}.
   *
   * @return The anchor point (never {@code null}).
   *
   * @see #setCircleAnchor(RectangleAnchor)
   */
  public RectangleAnchor getCircleAnchor() {
    return this.circleAnchor;
  }

  /**
   * Sets the anchor point used to align a circle at its (x, y) location and sends a
   * {@link RendererChangeEvent} to all registered listeners.
   *
   * @param anchor the anchor.
   *
   * @see #getCircleAnchor()
   */
  public void setCircleAnchor(RectangleAnchor anchor) {
    Args.nullNotPermitted(anchor, "anchor");
    if (this.circleAnchor.equals(anchor)) {
      return; // no change
    }
    this.circleAnchor = anchor;
    updateOffsets();
    fireChangeEvent();
  }

  /**
   * Returns the paint scale used by the renderer.
   *
   * @return The paint scale (never {@code null}).
   *
   * @see #setPaintScale(PaintScale)
   * @since 1.0.4
   */
  public PaintScale getPaintScale() {
    return this.paintScale;
  }

  /**
   * Sets the paint scale used by the renderer and sends a {@link RendererChangeEvent} to all
   * registered listeners.
   *
   * @param scale the scale ({@code null} not permitted).
   *
   * @see #getPaintScale()
   * @since 1.0.4
   */
  public void setPaintScale(PaintScale scale) {
    Args.nullNotPermitted(scale, "scale");
    this.paintScale = scale;
    fireChangeEvent();
  }

  /**
   * Updates the offsets to take into account the circle width, height and anchor.
   */
  private void updateOffsets() {
    if (this.circleAnchor.equals(RectangleAnchor.BOTTOM_LEFT)) {
      this.xOffset = 0.0;
      this.yOffset = 0.0;
    } else if (this.circleAnchor.equals(RectangleAnchor.BOTTOM)) {
      this.xOffset = -this.circleWidth / 2.0;
      this.yOffset = 0.0;
    } else if (this.circleAnchor.equals(RectangleAnchor.BOTTOM_RIGHT)) {
      this.xOffset = -this.circleWidth;
      this.yOffset = 0.0;
    } else if (this.circleAnchor.equals(RectangleAnchor.LEFT)) {
      this.xOffset = 0.0;
      this.yOffset = -this.circleHeight / 2.0;
    } else if (this.circleAnchor.equals(RectangleAnchor.CENTER)) {
      this.xOffset = -this.circleWidth / 2.0;
      this.yOffset = -this.circleHeight / 2.0;
    } else if (this.circleAnchor.equals(RectangleAnchor.RIGHT)) {
      this.xOffset = -this.circleWidth;
      this.yOffset = -this.circleHeight / 2.0;
    } else if (this.circleAnchor.equals(RectangleAnchor.TOP_LEFT)) {
      this.xOffset = 0.0;
      this.yOffset = -this.circleHeight;
    } else if (this.circleAnchor.equals(RectangleAnchor.TOP)) {
      this.xOffset = -this.circleWidth / 2.0;
      this.yOffset = -this.circleHeight;
    } else if (this.circleAnchor.equals(RectangleAnchor.TOP_RIGHT)) {
      this.xOffset = -this.circleWidth;
      this.yOffset = -this.circleHeight;
    }
  }

  /**
   * Returns the lower and upper bounds (range) of the x-values in the specified dataset.
   *
   * @param dataset the dataset ({@code null} permitted).
   *
   * @return The range ({@code null} if the dataset is {@code null} or empty).
   *
   * @see #findRangeBounds(XYDataset)
   */
  @Override
  public Range findDomainBounds(XYDataset dataset) {
    if (dataset == null) {
      return null;
    }
    Range r = DatasetUtils.findDomainBounds(dataset, false);
    if (r == null) {
      return null;
    }
    return new Range(r.getLowerBound() + this.xOffset,
        r.getUpperBound() + this.circleWidth + this.xOffset);
  }

  /**
   * Returns the range of values the renderer requires to display all the items from the specified
   * dataset.
   *
   * @param dataset the dataset ({@code null} permitted).
   *
   * @return The range ({@code null} if the dataset is {@code null} or empty).
   *
   * @see #findDomainBounds(XYDataset)
   */
  @Override
  public Range findRangeBounds(XYDataset dataset) {
    if (dataset != null) {
      Range r = DatasetUtils.findRangeBounds(dataset, false);
      if (r == null) {
        return null;
      } else {
        return new Range(r.getLowerBound() + this.yOffset,
            r.getUpperBound() + this.circleHeight + this.yOffset);
      }
    } else {
      return null;
    }
  }

  /**
   * Draws the circle representing the specified item.
   *
   * @param g2 the graphics device.
   * @param state the state.
   * @param dataArea the data area.
   * @param info the plot rendering info.
   * @param plot the plot.
   * @param domainAxis the x-axis.
   * @param rangeAxis the y-axis.
   * @param dataset the dataset.
   * @param series the series index.
   * @param item the item index.
   * @param crosshairState the crosshair state.
   * @param pass the pass index.
   */
  @Override
  public void drawItem(Graphics2D g2, XYItemRendererState state, Rectangle2D dataArea,
      PlotRenderingInfo info, XYPlot plot, ValueAxis domainAxis, ValueAxis rangeAxis,
      XYDataset dataset, int series, int item, CrosshairState crosshairState, int pass) {

    double x = dataset.getXValue(series, item);
    double y = dataset.getYValue(series, item);
    double z = 0.0;
    double bubbleSize = 0;
    double minBubbleSize;
    double maxBubbleSize;
    Color specificDatasetColor = null;
    if (dataset instanceof KendrickMassPlotXYDataset) {
      double[] bubbleSizeValues = ((KendrickMassPlotXYDataset) dataset).getBubbleSizeValues();
      minBubbleSize = Arrays.stream(bubbleSizeValues).min().getAsDouble();
      maxBubbleSize = Arrays.stream(bubbleSizeValues).max().getAsDouble();
      bubbleSize = scaleBubbeSize(minBubbleSize, maxBubbleSize,
          ((KendrickMassPlotXYDataset) dataset).getBubbleSize(series, item));
      if (((KendrickMassPlotXYDataset) dataset).getColor() != null) {
        specificDatasetColor = ((KendrickMassPlotXYDataset) dataset).getColor();
      }
    }
    if (dataset instanceof KendrickMassPlotXYZDataset) {
      z = ((KendrickMassPlotXYZDataset) dataset).getZValue(series, item);
      double[] bubbleSizeValues = ((KendrickMassPlotXYZDataset) dataset).getBubbleSizeValues();
      minBubbleSize = Arrays.stream(bubbleSizeValues).min().getAsDouble();
      maxBubbleSize = Arrays.stream(bubbleSizeValues).max().getAsDouble();
      bubbleSize = scaleBubbeSize(minBubbleSize, maxBubbleSize,
          ((KendrickMassPlotXYZDataset) dataset).getBubbleSize(series, item));
    }

    // create new color with alpha
    Paint oldPaint = this.paintScale.getPaint(z);
    Color awtColor = (Color) oldPaint;
    Color p = new Color(awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue(), 150);
    if (specificDatasetColor != null) {
      p = new Color(specificDatasetColor.getRed(), specificDatasetColor.getGreen(),
          specificDatasetColor.getBlue(), 150);
    }

    double xx0 = domainAxis.valueToJava2D(x, dataArea, plot.getDomainAxisEdge()) - bubbleSize / 2;
    double yy0 = rangeAxis.valueToJava2D(y, dataArea, plot.getRangeAxisEdge()) - bubbleSize / 2;
    double xx1 = domainAxis.valueToJava2D(x, dataArea, plot.getDomainAxisEdge()) - bubbleSize / 2;
    double yy1 = rangeAxis.valueToJava2D(y, dataArea, plot.getRangeAxisEdge()) - bubbleSize / 2;
    Ellipse2D circle;
    PlotOrientation orientation = plot.getOrientation();
    if (orientation.equals(PlotOrientation.HORIZONTAL)) {
      circle = new Ellipse2D.Double(Math.min(yy0, yy1), Math.min(xx0, xx1), bubbleSize, bubbleSize);
    } else {
      circle = new Ellipse2D.Double(Math.min(xx0, xx1), Math.min(yy0, yy1), bubbleSize, bubbleSize);
    }
    g2.setPaint(p);
    g2.fill(circle);
    g2.setStroke(new BasicStroke(1.0f));
    g2.draw(circle);
    if (isItemLabelVisible(series, item)) {
      drawItemLabel(g2, orientation, dataset, series, item, circle.getCenterX(),
          circle.getCenterY(), y < 0.0);
    }

    int datasetIndex = plot.indexOf(dataset);
    double transX = domainAxis.valueToJava2D(x, dataArea, plot.getDomainAxisEdge());
    double transY = rangeAxis.valueToJava2D(y, dataArea, plot.getRangeAxisEdge());
    updateCrosshairValues(crosshairState, x, y, datasetIndex, transX, transY, orientation);

    EntityCollection entities = state.getEntityCollection();
    if (entities != null) {
      addEntity(entities, circle, dataset, series, item, circle.getCenterX(), circle.getCenterY());
    }

  }

  /**
   * Tests this {@code XYCircleRenderer} for equality with an arbitrary object. This method returns
   * {@code true} if and only if:
   * <ul>
   * <li>{@code obj} is an instance of {@code XYCircleRenderer} (not {@code null});</li>
   * <li>{@code obj} has the same field values as this {@code XYCircleRenderer};</li>
   * </ul>
   *
   * @param obj the object ({@code null} permitted).
   *
   * @return A boolean.
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof XYCirclePixelSizeRenderer)) {
      return false;
    }
    XYCirclePixelSizeRenderer that = (XYCirclePixelSizeRenderer) obj;
    if (this.circleHeight != that.circleHeight) {
      return false;
    }
    if (this.circleWidth != that.circleWidth) {
      return false;
    }
    if (!this.circleAnchor.equals(that.circleAnchor)) {
      return false;
    }
    if (!this.paintScale.equals(that.paintScale)) {
      return false;
    }
    return super.equals(obj);
  }

  /**
   * Returns a clone of this renderer.
   *
   * @return A clone of this renderer.
   *
   * @throws CloneNotSupportedException if there is a problem creating the clone.
   */
  @Override
  public Object clone() throws CloneNotSupportedException {
    XYCirclePixelSizeRenderer clone = (XYCirclePixelSizeRenderer) super.clone();
    if (this.paintScale instanceof PublicCloneable) {
      PublicCloneable pc = (PublicCloneable) this.paintScale;
      clone.paintScale = (PaintScale) pc.clone();
    }
    return clone;
  }

  private double scaleBubbeSize(double min, double max, double value) {
    double maximumSize = 15;
    double minimumSize = 3;
    if (min == max) {
      return 5;
    } else {
      double a = (maximumSize - minimumSize) / (max - min);
      double b = maximumSize - a * max;
      return a * value + b;
    }
  }

}
