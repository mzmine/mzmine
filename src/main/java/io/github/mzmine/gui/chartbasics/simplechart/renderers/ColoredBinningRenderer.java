/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.gui.chartbasics.simplechart.renderers;

import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYZDataset;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PaintScaleProvider;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.Arrays;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import org.jfree.chart.LegendItem;
import org.jfree.chart.axis.ValueAxis;
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

/*
 * =========================================================== JFreeChart : a free chart library for
 * the Java(tm) platform ===========================================================
 *
 * (C) Copyright 2000-2017, by Object Refinery Limited and Contributors.
 *
 * Project Info: http://www.jfree.org/jfreechart/index.html
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation; either version
 * 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this library;
 * if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 *
 * [Oracle and Java are registered trademarks of Oracle and/or its affiliates. Other names may be
 * trademarks of their respective owners.]
 *
 * -------------------- XYBlockRenderer.java -------------------- (C) Copyright 2006-2017, by Object
 * Refinery Limited.
 *
 * Original Author: David Gilbert (for Object Refinery Limited); Contributor(s): -;
 *
 */

public class ColoredBinningRenderer extends AbstractXYItemRenderer
    implements XYItemRenderer, Cloneable, PublicCloneable, Serializable {

  private static final double NO_VALUE = -1d;
  private static Logger logger = Logger.getLogger(ColoredBinningRenderer.class.getName());

  private final ColoredXYZDataset binnedDataset;
  int prevItem = 1;
  /**
   * The block width (defaults to 1.0).
   */
  private double blockWidth = 1.0;
  /**
   * The block height (defaults to 1.0).
   */
  private double blockHeight = 1.0;
  /**
   * The anchor point used to align each block to its (x, y) location. The default value is {@code
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
   * The paint scale.
   */
  private PaintScale paintScale;
  /**
   * In some case a single paint scall shall be used for multiple datasets.
   */
  private boolean useDatasetPaintScale = true;
  private double[][] binnedData;

  /**
   * Creates a new {@code XYBlockRenderer} instance with default attributes. Item labels are
   * disabled by default.
   */
  public ColoredBinningRenderer(@Nonnull final ColoredXYZDataset dataset) {
    updateOffsets();
    this.paintScale = new LookupPaintScale();
    setDefaultItemLabelsVisible(false);
    binnedDataset = dataset;
    blockAnchor = RectangleAnchor.CENTER;
  }

  /**
   * Returns the block width, in data/axis units.
   *
   * @return The block width.
   */
  public double getBlockWidth() {
    return this.blockWidth;
  }

  /**
   * Returns the block height, in data/axis units.
   *
   * @return The block height.
   */
  public double getBlockHeight() {
    return this.blockHeight;
  }

  /**
   * Returns the anchor point used to align a block at its (x, y) location. The default values is
   * {@link RectangleAnchor#CENTER}.
   *
   * @return The anchor point (never {@code null}).
   */
  public RectangleAnchor getBlockAnchor() {
    return this.blockAnchor;
  }

  /**
   * Returns the paint scale used by the renderer.
   *
   * @return The paint scale (never {@code null}).
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
   * @see #getPaintScale()
   * @since 1.0.4
   */
  public void setPaintScale(PaintScale scale) {
    Args.nullNotPermitted(scale, "scale");
    this.paintScale = scale;
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
      this.xOffset = -this.blockWidth / 2.0;
      this.yOffset = 0.0;
    } else if (this.blockAnchor.equals(RectangleAnchor.BOTTOM_RIGHT)) {
      this.xOffset = -this.blockWidth;
      this.yOffset = 0.0;
    } else if (this.blockAnchor.equals(RectangleAnchor.LEFT)) {
      this.xOffset = 0.0;
      this.yOffset = -this.blockHeight / 2.0;
    } else if (this.blockAnchor.equals(RectangleAnchor.CENTER)) {
      this.xOffset = -this.blockWidth / 2.0;
      this.yOffset = -this.blockHeight / 2.0;
    } else if (this.blockAnchor.equals(RectangleAnchor.RIGHT)) {
      this.xOffset = -this.blockWidth;
      this.yOffset = -this.blockHeight / 2.0;
    } else if (this.blockAnchor.equals(RectangleAnchor.TOP_LEFT)) {
      this.xOffset = 0.0;
      this.yOffset = -this.blockHeight;
    } else if (this.blockAnchor.equals(RectangleAnchor.TOP)) {
      this.xOffset = -this.blockWidth / 2.0;
      this.yOffset = -this.blockHeight;
    } else if (this.blockAnchor.equals(RectangleAnchor.TOP_RIGHT)) {
      this.xOffset = -this.blockWidth;
      this.yOffset = -this.blockHeight;
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
    if (dataset instanceof ColoredXYDataset ds
        && ds.getStatus() == TaskStatus.FINISHED) {
      return new Range(ds.getDomainValueRange().lowerEndpoint(),
          ds.getDomainValueRange().upperEndpoint());
    }
    Range r = DatasetUtils.findDomainBounds(dataset, false);
    if (r == null) {
      return null;
    }
    return new Range(r.getLowerBound() + this.xOffset,
        r.getUpperBound() + this.blockWidth + this.xOffset);
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
      if (dataset instanceof ColoredXYZDataset ds
          && ds.getStatus() == TaskStatus.FINISHED) {
        return new Range(ds.getRangeValueRange().lowerEndpoint(),
            ds.getRangeValueRange().upperEndpoint());
      }
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
  public void drawItem(Graphics2D g2, XYItemRendererState state, Rectangle2D dataArea,
      PlotRenderingInfo info, XYPlot plot, ValueAxis domainAxis, ValueAxis rangeAxis,
      XYDataset dataset, int series, int item, CrosshairState crosshairState, int pass) {

    if (dataset != this.binnedDataset) {
      throw new IllegalArgumentException("Illegal dataset.");
    }

    boolean isNewDrawCall = false;
    if (item < prevItem) {
      binDataset(dataArea, plot, domainAxis, rangeAxis);
      isNewDrawCall = true;
    }

    prevItem = item;

    PlotOrientation orientation = plot.getOrientation();

    if (isNewDrawCall) {
      PaintScale paintScale;
      if (binnedDataset instanceof PaintScaleProvider && isUseDatasetPaintScale()
          && ((ColoredXYZDataset) dataset).getStatus() == TaskStatus.FINISHED) {
        paintScale = ((PaintScaleProvider) binnedDataset).getPaintScale();
      } else {
        paintScale = this.paintScale;
      }

      for (int x = 0; x < binnedData.length; x++) {
        for (int y = 0; y < binnedData[x].length; y++) {
          final double z = binnedData[x][y];
          if (Double.compare(z, -1d) == 0) {
            continue;
          }

          Rectangle2D block = new Rectangle2D.Double(x, y, 1, 1);
          Paint p = paintScale.getPaint(z);
          g2.setPaint(p);
          g2.fill(block);
          g2.setStroke(new BasicStroke(1.0f));
          g2.draw(block);
        }
      }
    }

    double x = dataset.getXValue(series, item);
    double y = dataset.getYValue(series, item);
    int datasetIndex = plot.indexOf(dataset);
    double transX = domainAxis.valueToJava2D(x, dataArea, plot.getDomainAxisEdge());
    double transY = rangeAxis.valueToJava2D(y, dataArea, plot.getRangeAxisEdge());
    updateCrosshairValues(crosshairState, x, y, datasetIndex, transX, transY, orientation);

//    EntityCollection entities = state.getEntityCollection();
//    if (entities != null) {
//      addEntity(entities, block, dataset, series, item, block.getCenterX(), block.getCenterY());
//    }

  }

  /**
   * Tests this {@code XYBlockRenderer} for equality with an arbitrary object. This method returns
   * {@code true} if and only if:
   * <ul>
   * <li>{@code obj} is an instance of {@code XYBlockRenderer} (not {@code null});</li>
   * <li>{@code obj} has the same field values as this {@code XYBlockRenderer};</li>
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
    if (!(obj instanceof ColoredBinningRenderer that)) {
      return false;
    }
    if (!this.blockAnchor.equals(that.blockAnchor)) {
      return false;
    }
    if (!this.paintScale.equals(that.paintScale)) {
      return false;
    }
    return super.equals(obj);
  }

  public boolean isUseDatasetPaintScale() {
    return useDatasetPaintScale;
  }

  public void setUseDatasetPaintScale(boolean useDatasetPaintScale) {
    this.useDatasetPaintScale = useDatasetPaintScale;
  }

  /**
   * Returns a clone of this renderer.
   *
   * @return A clone of this renderer.
   * @throws CloneNotSupportedException if there is a problem creating the clone.
   */
  @Override
  public Object clone() throws CloneNotSupportedException {
    ColoredBinningRenderer clone = (ColoredBinningRenderer) super.clone();
    if (this.paintScale instanceof PublicCloneable) {
      PublicCloneable pc = (PublicCloneable) this.paintScale;
      clone.paintScale = (PaintScale) pc.clone();
    }
    return clone;
  }

  @Override
  public LegendItem getLegendItem(int datasetIndex, int series) {
    return null;
//    return super.getLegendItem(datasetIndex, series);
  }

  private void binDataset(final Rectangle2D dataArea, XYPlot plot, ValueAxis domainAxis,
      ValueAxis rangeAxis) {

    final int numPixelsX = (int) Math.ceil(dataArea.getWidth() + dataArea.getX());
    final int numPixelsY = (int) Math.ceil(dataArea.getHeight() + dataArea.getY());

    if (binnedData == null || numPixelsX != binnedData.length
        || numPixelsY != binnedData[0].length) {
      binnedData = new double[numPixelsX][numPixelsY];
    }
    for (double[] array : binnedData) {
      Arrays.fill(array, NO_VALUE);
    }

    final double w = binnedDataset.getBoxHeight();
    final double h = binnedDataset.getBoxWidth();

    blockWidth = w;
    blockHeight = h;
    updateOffsets();

    for (int i = 0; i < binnedDataset.getItemCount(0); i++) {
      final double cx = binnedDataset.getXValue(0, i);
      final double cy = binnedDataset.getYValue(0, i);
      final double z = binnedDataset.getZValue(0, i);

      // transform from dataset xy to java 2d
      final int transformedDomain0 = (int) Math.round(domainAxis
          .valueToJava2D(cx + xOffset, dataArea, plot.getDomainAxisEdge()));
      final int transformedRange0 = (int) Math.round(rangeAxis
          .valueToJava2D(cy + yOffset, dataArea, plot.getRangeAxisEdge()));

      final int transformedDomain1 = (int) Math.round(domainAxis
          .valueToJava2D(cx + xOffset + w, dataArea, plot.getDomainAxisEdge()));
      final int transformedRange1 = (int) Math.round(rangeAxis
          .valueToJava2D(cy + yOffset + h, dataArea, plot.getRangeAxisEdge()));

      final int minX = Math.min(transformedDomain0, transformedDomain1);
      final int maxX = Math.max(transformedDomain0, transformedDomain1);
      final int minY = Math.min(transformedRange0, transformedRange1);
      final int maxY = Math.max(transformedRange0, transformedRange1);

      // paint the pixels
      if (minX < numPixelsX && minY < numPixelsY
          && minX >= 0 && minY >= 0
          && maxX < numPixelsX && maxY < numPixelsY
          && maxX >= 0 && maxY >= 0) {
        // only keep the highest values per pixel
        for (int x = minX; x <= maxX; x++) {
          for (int y = minY; y <= maxY; y++) {
            binnedData[x][y] = Math.max(binnedData[x][y], z);
          }
        }
      }
    }
  }
}

