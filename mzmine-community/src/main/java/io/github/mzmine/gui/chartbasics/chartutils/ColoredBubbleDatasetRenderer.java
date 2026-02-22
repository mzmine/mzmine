/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

import io.github.mzmine.gui.chartbasics.simplechart.datasets.XYZBubbleDataset;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.visualization.kendrickmassplot.KendrickMassPlotXYDataset;
import io.github.mzmine.modules.visualization.kendrickmassplot.KendrickMassPlotXYZDataset;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.event.RendererChangeEvent;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.LookupPaintScale;
import org.jfree.chart.renderer.PaintScale;
import org.jfree.chart.renderer.xy.AbstractXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.text.TextUtils;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.util.Args;
import org.jfree.chart.util.PublicCloneable;
import org.jfree.data.Range;
import org.jfree.data.general.DatasetUtils;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;

/**
 * A renderer that represents data from an {@link XYZDataset} by drawing a circle at each (x,
 */
public class ColoredBubbleDatasetRenderer extends AbstractXYItemRenderer implements XYItemRenderer,
    Cloneable, PublicCloneable, Serializable {

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

  private boolean highlightAnnotated = false;
  private List<Rectangle2D> drawnLabelBounds = new ArrayList<>();
  private List<Rectangle2D> dataPointBounds = new ArrayList<>();
  private transient XYDataset cachedBubbleRangeDataset;
  private double cachedMinBubbleSize = Double.NaN;
  private double cachedMaxBubbleSize = Double.NaN;

  /**
   * Creates a new {@code XYCircleRenderer} instance with default attributes.
   */
  public ColoredBubbleDatasetRenderer() {
    updateOffsets();
    this.paintScale = new LookupPaintScale();
  }

  @Override
  public @NotNull XYItemRendererState initialise(final @NotNull Graphics2D g2,
      final @NotNull Rectangle2D dataArea, final @NotNull XYPlot plot,
      final @Nullable XYDataset data, final @Nullable PlotRenderingInfo info) {
    drawnLabelBounds.clear();
    dataPointBounds.clear();
    if (data != null) {
      updateBubbleSizeRangeCache(data);
      cacheDataPointBounds(plot, dataArea, data);
    } else {
      cachedBubbleRangeDataset = null;
      cachedMinBubbleSize = Double.NaN;
      cachedMaxBubbleSize = Double.NaN;
    }
    return super.initialise(g2, dataArea, plot, data, info);
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
  public void drawItem(final @NotNull Graphics2D g2, final @NotNull XYItemRendererState state,
      final @NotNull Rectangle2D dataArea, final @Nullable PlotRenderingInfo info,
      final @NotNull XYPlot plot, final @NotNull ValueAxis domainAxis,
      final @NotNull ValueAxis rangeAxis, final @NotNull XYDataset dataset, final int series,
      final int item, final @Nullable CrosshairState crosshairState, final int pass) {

    double x = dataset.getXValue(series, item);
    double y = dataset.getYValue(series, item);
    double z = 0.0;
    double bubbleSize = resolveBubbleSize(dataset, series, item);
    Color specificDatasetColor = null;
    boolean annotated = false;
    if (dataset instanceof KendrickMassPlotXYDataset kds) {
      if (kds.getColor() != null) {
        specificDatasetColor = kds.getColor();
      }
    }
    if (dataset instanceof XYZBubbleDataset bubbleDataset) {
      z = bubbleDataset.getZValue(series, item);
    }
    if(dataset instanceof KendrickMassPlotXYZDataset xyz) {
      annotated = xyz.isAnnotated(item);
    }

    // create new color with alpha
    Paint oldPaint = this.paintScale.getPaint(z);
    Color awtColor = (Color) oldPaint;
    Color p = new Color(awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue(), 150);
    if (specificDatasetColor != null) {
      p = new Color(specificDatasetColor.getRed(), specificDatasetColor.getGreen(),
          specificDatasetColor.getBlue(), 150);
    }

    final PlotOrientation orientation = plot.getOrientation();
    final Ellipse2D circle = createBubbleShape(x, y, bubbleSize, dataArea, plot, domainAxis,
        rangeAxis);
    if (circle == null) {
      return;
    }
    g2.setPaint(p);
    g2.fill(circle);
    g2.setStroke(new BasicStroke(1.0f));
    if(annotated && highlightAnnotated) {
      g2.setPaint(ConfigService.getDefaultColorPalette().getNegativeColorAWT());
    }
    g2.draw(circle);
    if (isItemLabelVisible(series, item) && hasSpaceForItemLabel(g2, orientation, dataset, series,
        item, circle.getCenterX(), circle.getCenterY(), y < 0.0, dataArea)) {
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
    if (!(obj instanceof ColoredBubbleDatasetRenderer)) {
      return false;
    }
    ColoredBubbleDatasetRenderer that = (ColoredBubbleDatasetRenderer) obj;
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
    ColoredBubbleDatasetRenderer clone = (ColoredBubbleDatasetRenderer) super.clone();
    if (this.paintScale instanceof PublicCloneable) {
      PublicCloneable pc = (PublicCloneable) this.paintScale;
      clone.paintScale = (PaintScale) pc.clone();
    }
    clone.drawnLabelBounds = new ArrayList<>();
    clone.dataPointBounds = new ArrayList<>();
    clone.cachedBubbleRangeDataset = null;
    clone.cachedMinBubbleSize = Double.NaN;
    clone.cachedMaxBubbleSize = Double.NaN;
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

  public boolean isHighlightAnnotated() {
    return highlightAnnotated;
  }

  public void setHighlightAnnotated(boolean highlightAnnotated) {
    this.highlightAnnotated = highlightAnnotated;
  }

  private void cacheDataPointBounds(final @NotNull XYPlot plot, final @NotNull Rectangle2D dataArea,
      final @NotNull XYDataset dataset) {
    final int datasetIndex = plot.indexOf(dataset);
    ValueAxis domainAxis = datasetIndex >= 0 ? plot.getDomainAxisForDataset(datasetIndex) : null;
    ValueAxis rangeAxis = datasetIndex >= 0 ? plot.getRangeAxisForDataset(datasetIndex) : null;
    if (domainAxis == null) {
      domainAxis = plot.getDomainAxis();
    }
    if (rangeAxis == null) {
      rangeAxis = plot.getRangeAxis();
    }
    if (domainAxis == null || rangeAxis == null) {
      return;
    }

    for (int series = 0; series < dataset.getSeriesCount(); series++) {
      for (int item = 0; item < dataset.getItemCount(series); item++) {
        final double bubbleSize = resolveBubbleSize(dataset, series, item);
        if (bubbleSize <= 0 || !Double.isFinite(bubbleSize)) {
          continue;
        }
        final double x = dataset.getXValue(series, item);
        final double y = dataset.getYValue(series, item);
        final Ellipse2D pointShape = createBubbleShape(x, y, bubbleSize, dataArea, plot,
            domainAxis, rangeAxis);
        if (pointShape != null) {
          dataPointBounds.add(pointShape.getBounds2D());
        }
      }
    }
  }

  private double resolveBubbleSize(final @NotNull XYDataset dataset, final int series,
      final int item) {
    final double rawBubbleSizeValue;
    if (dataset instanceof KendrickMassPlotXYDataset kds) {
      rawBubbleSizeValue = kds.getBubbleSize(series, item);
    } else if (dataset instanceof XYZBubbleDataset bubbleDataset) {
      rawBubbleSizeValue = bubbleDataset.getBubbleSizeValue(series, item);
    } else {
      return 0;
    }

    updateBubbleSizeRangeCache(dataset);
    if (!Double.isFinite(cachedMinBubbleSize) || !Double.isFinite(cachedMaxBubbleSize)) {
      return 0;
    }
    return scaleBubbeSize(cachedMinBubbleSize, cachedMaxBubbleSize, rawBubbleSizeValue);
  }

  private void updateBubbleSizeRangeCache(final @NotNull XYDataset dataset) {
    if (dataset == cachedBubbleRangeDataset && Double.isFinite(cachedMinBubbleSize)
        && Double.isFinite(cachedMaxBubbleSize)) {
      return;
    }

    final double[] bubbleSizeValues;
    if (dataset instanceof KendrickMassPlotXYDataset kds) {
      bubbleSizeValues = kds.getBubbleSizeValues();
    } else if (dataset instanceof XYZBubbleDataset bubbleDataset) {
      bubbleSizeValues = bubbleDataset.getBubbleSizeValues();
    } else {
      cachedBubbleRangeDataset = null;
      cachedMinBubbleSize = Double.NaN;
      cachedMaxBubbleSize = Double.NaN;
      return;
    }

    cachedBubbleRangeDataset = dataset;
    if (bubbleSizeValues.length == 0) {
      cachedMinBubbleSize = Double.NaN;
      cachedMaxBubbleSize = Double.NaN;
      return;
    }

    cachedMinBubbleSize = Arrays.stream(bubbleSizeValues).min().orElse(Double.NaN);
    cachedMaxBubbleSize = Arrays.stream(bubbleSizeValues).max().orElse(Double.NaN);
  }

  private @Nullable Ellipse2D createBubbleShape(final double x, final double y,
      final double bubbleSize, final @NotNull Rectangle2D dataArea, final @NotNull XYPlot plot,
      final @NotNull ValueAxis domainAxis, final @NotNull ValueAxis rangeAxis) {
    if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(bubbleSize)
        || bubbleSize <= 0) {
      return null;
    }

    final double transX = domainAxis.valueToJava2D(x, dataArea, plot.getDomainAxisEdge())
                          - bubbleSize / 2d;
    final double transY = rangeAxis.valueToJava2D(y, dataArea, plot.getRangeAxisEdge())
                          - bubbleSize / 2d;
    if (!Double.isFinite(transX) || !Double.isFinite(transY)) {
      return null;
    }

    if (plot.getOrientation().equals(PlotOrientation.HORIZONTAL)) {
      return new Ellipse2D.Double(transY, transX, bubbleSize, bubbleSize);
    }
    return new Ellipse2D.Double(transX, transY, bubbleSize, bubbleSize);
  }

  private boolean hasSpaceForItemLabel(final @NotNull Graphics2D g2,
      final @NotNull PlotOrientation orientation, final @NotNull XYDataset dataset,
      final int series, final int item, final double x, final double y, final boolean negative,
      final @NotNull Rectangle2D dataArea) {
    final XYItemLabelGenerator generator = getItemLabelGenerator(series, item);
    if (generator == null) {
      return false;
    }

    final String label = generator.generateLabel(dataset, series, item);
    if (label == null || label.isBlank()) {
      return false;
    }

    final ItemLabelPosition position = negative ? getNegativeItemLabelPosition(series, item)
        : getPositiveItemLabelPosition(series, item);
    final Point2D anchorPoint = calculateLabelAnchorPoint(position.getItemLabelAnchor(), x, y,
        orientation);

    final Font oldFont = g2.getFont();
    final Font labelFont = getItemLabelFont(series, item);
    g2.setFont(labelFont);
    final Shape labelShape = TextUtils.calculateRotatedStringBounds(label, g2,
        (float) anchorPoint.getX(), (float) anchorPoint.getY(), position.getTextAnchor(),
        position.getAngle(), position.getRotationAnchor());
    g2.setFont(oldFont);

    if (labelShape == null) {
      return false;
    }

    final Rectangle2D labelBounds = addPadding(labelShape.getBounds2D(), 1.0);
    if (!dataArea.contains(labelBounds)) {
      return false;
    }

    for (final Rectangle2D pointBounds : dataPointBounds) {
      if (pointBounds.intersects(labelBounds)) {
        return false;
      }
    }

    for (final Rectangle2D existingBounds : drawnLabelBounds) {
      if (existingBounds.intersects(labelBounds)) {
        return false;
      }
    }

    drawnLabelBounds.add(labelBounds);
    return true;
  }

  private @NotNull Rectangle2D addPadding(final @NotNull Rectangle2D bounds, final double padding) {
    return new Rectangle2D.Double(bounds.getX() - padding, bounds.getY() - padding,
        bounds.getWidth() + padding * 2, bounds.getHeight() + padding * 2);
  }
}
