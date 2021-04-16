package io.github.mzmine.gui.chartbasics.simplechart.renderers;

import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYZPieDataset;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.event.RendererChangeEvent;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.AbstractXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.util.Args;
import org.jfree.chart.util.PublicCloneable;
import org.jfree.data.Range;
import org.jfree.data.general.DatasetUtils;
import org.jfree.data.xy.XYDataset;

/**
 * Copied from {@link org.jfree.chart.renderer.xy.XYBlockRenderer}. Modified to use the data set
 * color and draw pie charts.
 */
public class ColoredXYZPieRenderer extends AbstractXYItemRenderer
    implements XYItemRenderer, Cloneable, PublicCloneable, Serializable {

  private static final Logger logger = Logger.getLogger(ColoredXYZPieRenderer.class.getName());

  /**
   * The block width (defaults to 1.0).
   */
  private double pieDiameter = 1.0;

  /**
   * The anchor point used to align each block to its (x, y) location.  The default value is {@code
   * RectangleAnchor.CENTER}.
   */
  private RectangleAnchor blockAnchor = RectangleAnchor.CENTER;

  /**
   * Temporary storage for the x-offset used to align the block anchor.
   */
  private double xOffset;

  /**
   * Temporary storage for the y-offset used to align the block anchor.
   */
  private double yOffset;

  /**
   * Creates a new {@code XYBlockRenderer} instance with default attributes.
   */
  public ColoredXYZPieRenderer() {
    updateOffsets();
  }

  /**
   * Returns the block width, in data/axis units.
   *
   * @return The block width.
   * @see #setPieDiameter(double)
   */
  public double getPieDiameter() {
    return this.pieDiameter;
  }

  /**
   * Sets the width of the blocks used to represent each data item and sends a {@link
   * RendererChangeEvent} to all registered listeners.
   *
   * @param width the new width, in data/axis units (must be &gt; 0.0).
   * @see #getPieDiameter()
   */
  public void setPieDiameter(double width) {
    if (width <= 0.0) {
      throw new IllegalArgumentException(
          "The 'width' argument must be > 0.0");
    }
    this.pieDiameter = width;
    updateOffsets();
    fireChangeEvent();
  }

  /**
   * Returns the block height, in data/axis units.
   *
   * @return The block height.
   * @see #setBlockHeight(double)
   */
  public double getBlockHeight() {
    return this.pieDiameter;
  }

  /**
   * Sets the height of the blocks used to represent each data item and sends a {@link
   * RendererChangeEvent} to all registered listeners.
   *
   * @param height the new height, in data/axis units (must be &gt; 0.0).
   * @see #getBlockHeight()
   */
  public void setBlockHeight(double height) {
    if (height <= 0.0) {
      throw new IllegalArgumentException(
          "The 'height' argument must be > 0.0");
    }
    this.blockHeight = height;
    updateOffsets();
    fireChangeEvent();
  }

  /**
   * Returns the anchor point used to align a block at its (x, y) location. The default values is
   * {@link RectangleAnchor#CENTER}.
   *
   * @return The anchor point (never {@code null}).
   * @see #setBlockAnchor(RectangleAnchor)
   */
  public RectangleAnchor getBlockAnchor() {
    return this.blockAnchor;
  }

  /**
   * Sets the anchor point used to align a block at its (x, y) location and sends a {@link
   * RendererChangeEvent} to all registered listeners.
   *
   * @param anchor the anchor.
   * @see #getBlockAnchor()
   */
  public void setBlockAnchor(RectangleAnchor anchor) {
    Args.nullNotPermitted(anchor, "anchor");
    if (this.blockAnchor.equals(anchor)) {
      return;  // no change
    }
    this.blockAnchor = anchor;
    updateOffsets();
    fireChangeEvent();
  }

  /**
   * Updates the offsets to take into account the block width, height and anchor.
   */
  private void updateOffsets() {
    if (this.blockAnchor.equals(RectangleAnchor.BOTTOM_LEFT)) {
      this.xOffset = 0.0;
      this.yOffset = 0.0;
    } else if (this.blockAnchor.equals(RectangleAnchor.BOTTOM)) {
      this.xOffset = -this.pieDiameter / 2.0;
      this.yOffset = 0.0;
    } else if (this.blockAnchor.equals(RectangleAnchor.BOTTOM_RIGHT)) {
      this.xOffset = -this.pieDiameter;
      this.yOffset = 0.0;
    } else if (this.blockAnchor.equals(RectangleAnchor.LEFT)) {
      this.xOffset = 0.0;
      this.yOffset = -this.pieDiameter / 2.0;
    } else if (this.blockAnchor.equals(RectangleAnchor.CENTER)) {
      this.xOffset = -this.pieDiameter / 2.0;
      this.yOffset = -this.pieDiameter / 2.0;
    } else if (this.blockAnchor.equals(RectangleAnchor.RIGHT)) {
      this.xOffset = -this.pieDiameter;
      this.yOffset = -this.pieDiameter / 2.0;
    } else if (this.blockAnchor.equals(RectangleAnchor.TOP_LEFT)) {
      this.xOffset = 0.0;
      this.yOffset = -this.pieDiameter;
    } else if (this.blockAnchor.equals(RectangleAnchor.TOP)) {
      this.xOffset = -this.pieDiameter / 2.0;
      this.yOffset = -this.pieDiameter;
    } else if (this.blockAnchor.equals(RectangleAnchor.TOP_RIGHT)) {
      this.xOffset = -this.pieDiameter;
      this.yOffset = -this.pieDiameter;
    }
  }

  /**
   * Returns the lower and upper bounds (range) of the x-values in the specified dataset.
   *
   * @param dataset the dataset ({@code null} permitted).
   * @return The range ({@code null} if the dataset is {@code null} or empty).
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
        r.getUpperBound() + this.pieDiameter + this.xOffset);
  }

  /**
   * Returns the range of values the renderer requires to display all the items from the specified
   * dataset.
   *
   * @param dataset the dataset ({@code null} permitted).
   * @return The range ({@code null} if the dataset is {@code null} or empty).
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
            r.getUpperBound() + this.blockHeight + this.yOffset);
      }
    } else {
      return null;
    }
  }

  /**
   * Draws the block representing the specified item.
   *
   * @param g2             the graphics device.
   * @param state          the state.
   * @param dataArea       the data area.
   * @param info           the plot rendering info.
   * @param plot           the plot.
   * @param domainAxis     the x-axis.
   * @param rangeAxis      the y-axis.
   * @param dataset        the dataset.
   * @param series         the series index.
   * @param item           the item index.
   * @param crosshairState the crosshair state.
   * @param pass           the pass index.
   */
  @Override
  public void drawItem(Graphics2D g2, XYItemRendererState state,
      Rectangle2D dataArea, PlotRenderingInfo info, XYPlot plot,
      ValueAxis domainAxis, ValueAxis rangeAxis, XYDataset dataset,
      int series, int item, CrosshairState crosshairState, int pass) {

    if (!(dataset instanceof ColoredXYZPieDataset<?> pieDataset)) {
      return;
    }

    double x = dataset.getXValue(series, item);
    double y = dataset.getYValue(series, item);
    double z = pieDataset.getZValue(series, item);

    double xx0 = domainAxis.valueToJava2D(x + this.xOffset, dataArea,
        plot.getDomainAxisEdge());
    double yy0 = rangeAxis.valueToJava2D(y + this.yOffset, dataArea,
        plot.getRangeAxisEdge());
    double xx1 = domainAxis.valueToJava2D(x + this.pieDiameter
        + this.xOffset, dataArea, plot.getDomainAxisEdge());
    double yy1 = rangeAxis.valueToJava2D(y + this.pieDiameter
        + this.yOffset, dataArea, plot.getRangeAxisEdge());
    Rectangle2D block;
    PlotOrientation orientation = plot.getOrientation();
    if (orientation.equals(PlotOrientation.HORIZONTAL)) {
      block = new Rectangle2D.Double(Math.min(yy0, yy1),
          Math.min(xx0, xx1), Math.abs(yy1 - yy0),
          Math.abs(xx0 - xx1));
    } else {
      block = new Rectangle2D.Double(Math.min(xx0, xx1),
          Math.min(yy0, yy1), Math.abs(xx1 - xx0),
          Math.abs(yy1 - yy0));
    }

    for (Object slice : pieDataset.getPieDataProvider().getSliceIdentifiers()) {
      double percentage = pieDataset.getZValue(item, slice) / z;
      try {
        g2.setPaint(pieDataset.getSliceColor(slice));
      } catch (ClassCastException e) {
        e.printStackTrace();
        logger.log(Level.WARNING, e,
            () -> "The identifier delivered by the dataset could not be cast to the required generic type.");
        return;
      }
      g2.drawArc(xx0, yy0, pieDataset.getPieWidth(item), );
    }

    g2.setPaint(p);
    g2.fill(block);
    g2.setStroke(new BasicStroke(1.0f));
    g2.draw(block);

    if (isItemLabelVisible(series, item)) {
      drawItemLabel(g2, orientation, dataset, series, item,
          block.getCenterX(), block.getCenterY(), y < 0.0);
    }

    int datasetIndex = plot.indexOf(dataset);
    double transX = domainAxis.valueToJava2D(x, dataArea,
        plot.getDomainAxisEdge());
    double transY = rangeAxis.valueToJava2D(y, dataArea,
        plot.getRangeAxisEdge());
    updateCrosshairValues(crosshairState, x, y, datasetIndex,
        transX, transY, orientation);

    EntityCollection entities = state.getEntityCollection();
    if (entities != null) {
      addEntity(entities, block, dataset, series, item,
          block.getCenterX(), block.getCenterY());
    }

  }

  /**
   * Tests this {@code XYBlockRenderer} for equality with an arbitrary object.  This method returns
   * {@code true} if and only if:
   * <ul>
   * <li>{@code obj} is an instance of {@code XYBlockRenderer} (not
   *     {@code null});</li>
   * <li>{@code obj} has the same field values as this
   *     {@code XYBlockRenderer};</li>
   * </ul>
   *
   * @param obj the object ({@code null} permitted).
   * @return A boolean.
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof org.jfree.chart.renderer.xy.XYBlockRenderer)) {
      return false;
    }
    org.jfree.chart.renderer.xy.XYBlockRenderer that = (org.jfree.chart.renderer.xy.XYBlockRenderer) obj;
    if (this.pieDiameter != that.getBlockHeight()) {
      return false;
    }
    if (this.pieDiameter != that.getBlockWidth()) {
      return false;
    }
    if (!this.blockAnchor.equals(that.getBlockAnchor())) {
      return false;
    }
    if (!this.paintScale.equals(that.getPaintScale())) {
      return false;
    }
    return super.equals(obj);
  }

  /**
   * Returns a clone of this renderer.
   *
   * @return A clone of this renderer.
   * @throws CloneNotSupportedException if there is a problem creating the clone.
   */
  @Override
  public Object clone() throws CloneNotSupportedException {
    org.jfree.chart.renderer.xy.XYBlockRenderer clone = (org.jfree.chart.renderer.xy.XYBlockRenderer) super
        .clone();
    return clone;
  }

}

