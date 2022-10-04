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
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYZPieDataset;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jfree.chart.LegendItem;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.AbstractXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.util.PublicCloneable;
import org.jfree.data.Range;
import org.jfree.data.general.DatasetUtils;
import org.jfree.data.xy.XYDataset;

/**
 * Copied from {@link org.jfree.chart.renderer.xy.XYBlockRenderer}. Modified to use the data set
 * color and draw pie charts based on {@link io.github.mzmine.gui.chartbasics.simplechart.providers.PieXYZDataProvider}s.
 *
 * @author https://github.com/SteffenHeu
 */
public class ColoredXYZPieRenderer extends AbstractXYItemRenderer
    implements XYItemRenderer, Cloneable, PublicCloneable, Serializable {

  private static final Logger logger = Logger.getLogger(ColoredXYZPieRenderer.class.getName());

  private ColoredXYZPieDataset<?> currentDataset = null;

  /**
   * Creates a new {@code ColoredXYZPieRenderer} instance with default attributes.
   */
  public ColoredXYZPieRenderer() {
    super();
    SimpleChartUtility.tryApplyDefaultChartThemeToRenderer(this);
    // default
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
    return new Range(r.getLowerBound(),
        r.getUpperBound());
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
        return new Range(r.getLowerBound(), r.getUpperBound());
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

    if (!(dataset instanceof ColoredXYZPieDataset<?> pieDataset) || series > 0) {
      // we draw all series at once, because we need all values at once to draw the pie correctly.
      return;
    }

    currentDataset = pieDataset;

    final double x = dataset.getXValue(series, item);
    final double y = dataset.getYValue(series, item);
    final double z = pieDataset.getZValue(item);
    final double pieDiameter = pieDataset.getPieDiameter(item);

    double cx = domainAxis.valueToJava2D(x, dataArea,
        plot.getDomainAxisEdge()) - pieDiameter / 2;
    double cy = rangeAxis.valueToJava2D(y, dataArea,
        plot.getRangeAxisEdge()) - pieDiameter / 2;

    int startAngle = 0;
    for (int seriesIndex = 0; seriesIndex < dataset.getSeriesCount(); seriesIndex++) {
      try {
        final double value = pieDataset.getZValue(seriesIndex, item);
        final int endAngle = (int) (value * 360 / z);

        g2.setPaint(pieDataset.getSliceColor(seriesIndex));
        g2.fill(new Arc2D.Double(cx, cy, pieDiameter,
            pieDataset.getPieDiameter(item), startAngle, endAngle, Arc2D.PIE));
        startAngle += endAngle;

      } catch (ClassCastException e) {
        e.printStackTrace();
        logger.log(Level.WARNING, e,
            () -> "The identifier delivered by the dataset could not be cast to the required generic type.");
        return;
      }
    }

    g2.setPaint(Color.BLACK);
    g2.draw(new Arc2D.Double(cx, cy, pieDiameter,
        pieDiameter, 0, 360, Arc2D.OPEN));

    final PlotOrientation orientation = plot.getOrientation();

    if (isItemLabelVisible(series, item)) {
      drawItemLabel(g2, orientation, dataset, series, item,
          cx, y + pieDiameter, y < 0.0);
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
      addEntity(entities, new Ellipse2D.Double(cx, cy, pieDiameter, pieDiameter), dataset, series, item,
          cx + pieDiameter / 2, cy + pieDiameter / 2);
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
    if (!(obj instanceof ColoredXYZPieRenderer)) {
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
    ColoredXYZPieRenderer clone = new ColoredXYZPieRenderer();
    return clone;
  }

  @Override
  public LegendItem getLegendItem(int datasetIndex, int series) {
    XYDataset dataset = getPlot().getDataset(datasetIndex);
    if ((dataset instanceof ColoredXYZPieDataset<?> ds)) {
      currentDataset = ds;
    }
    return super.getLegendItem(datasetIndex, series);
  }

  @Override
  public Paint lookupSeriesPaint(int series) {
    if (currentDataset != null) {
      return currentDataset.getSliceColor(series);
    }
    return super.lookupSeriesPaint(series);
  }
}

