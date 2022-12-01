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
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYZDataset;
import io.github.mzmine.gui.chartbasics.simplechart.generators.SimpleToolTipGenerator;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;
import javafx.util.Pair;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.RendererUtils;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.renderer.xy.XYShapeRenderer;
import org.jfree.chart.util.ShapeUtils;
import org.jfree.chart.util.SortOrder;
import org.jfree.data.xy.XYDataset;

/**
 * Renderer for the points of scatter plot, optimized not to render overlapping points and
 * supports z axis ordering.
 */
public class ColoredXYZDotRenderer extends XYShapeRenderer {

  private static final Shape dataPointsShape = new Ellipse2D.Double(0, 0, 7, 7);

  /**
   * Order of z axis(i. e. darker/lighter colors on top)
   */
  private SortOrder zOrder;

  /**
   * Mapping of point's coordinates represented as a single integer to point's index in dataset
   * and z value
   */
  private final Map<Integer, Pair<Integer, Double>> uniqueCoords = new HashMap<>();

  /**
   * Array of booleans: true means, that data point corresponding to index need to be drawn,
   * false means, that the data point is redundant, because it overlaps with other one
   */
  private boolean[] pointsToDraw;

  public ColoredXYZDotRenderer() {
    super();

    SimpleChartUtility.tryApplyDefaultChartThemeToRenderer(this);
    SimpleToolTipGenerator toolTipGenerator = new SimpleToolTipGenerator();
    setDefaultToolTipGenerator(toolTipGenerator);

    setDefaultItemLabelsVisible(false);
    setSeriesVisibleInLegend(0, false);
    setSeriesItemLabelsVisible(0, false);

    setSeriesShape(0, dataPointsShape);
  }

  @Override
  public int getPassCount() {
    // One renderer pass is enough for the scatter plot
    return 1;
  }

  @Override
  public XYItemRendererState initialise(Graphics2D g2, Rectangle2D dataArea, XYPlot plot, XYDataset dataset, PlotRenderingInfo info) {
    XYItemRendererState state = super.initialise(g2, dataArea, plot, dataset, info);

    // Clear saved coordinated, when the renderer is initialized
    uniqueCoords.clear();

    // TODO: for loop over series?
    int series = 0;

    pointsToDraw = new boolean[dataset.getItemCount(series)];

    // Set to false, because following lines do the same as true
    // Non visible dots will not be considered, for example after zoom
    state.setProcessVisibleItemsOnly(false);
    int firstItem = 0;
    int lastItem = dataset.getItemCount(series) - 1;
    if (lastItem == -1) {
      return state;
    }
    int[] itemBounds = RendererUtils.findLiveItems(
        dataset, series, plot.getDomainAxis().getLowerBound(),
        plot.getDomainAxis().getUpperBound());
    firstItem = Math.max(itemBounds[0] - 1, 0);
    lastItem = Math.min(itemBounds[1] + 1, lastItem);

    // Loop through all data points and find "unique" points
    for (int item = firstItem; item <= lastItem; item++) {

      // Get all values
      double x = dataset.getXValue(series, item);
      double y = dataset.getYValue(series, item);
      double z = ((ColoredXYZDataset) dataset).getZValue(series, item);
      if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z)) {
        continue;
      }

      // Calculate x and y coordinates relative to the data area
      double transX = plot.getDomainAxis().valueToJava2D(x, dataArea,
          plot.getDomainAxisEdge());
      double transY = plot.getRangeAxis().valueToJava2D(y, dataArea,
          plot.getRangeAxisEdge());

      // Sparse the coordinates by rounding and making them odd
      int roundX = (int) Math.round(transX);
      int roundY = (int) Math.round(transY);
      if (roundX % 2 == 0) {
        roundX++;
      }
      if (roundY % 2 == 0) {
        roundY++;
      }

      // Cantor pairing function(injection of roundX and roundY to single integer)
      int coordinate = ((roundX + roundY) * (roundX + roundY + 1) + roundY) / 2;

      // If point's coordinate is already present and z value is not visible according
      // to the z values order, do nothing
      if (uniqueCoords.containsKey(coordinate)) {
        if (zOrder == null
            || zOrder == SortOrder.ASCENDING && z <= uniqueCoords.get(coordinate).getValue()
            || zOrder == SortOrder.DESCENDING && z >= uniqueCoords.get(coordinate).getValue()) {
          continue;
        }
      }

      // Add new coordinate
      uniqueCoords.put(coordinate, new Pair<>(item, z));
    }

    // Loop through all unique coordinates and save indexes of points to be drawn
    for (Pair<Integer, Double> pair : uniqueCoords.values()) {
      pointsToDraw[pair.getKey()] = true;
    }

    return state;
  }

  @Override
  public void drawItem(Graphics2D g2, XYItemRendererState state,
      Rectangle2D dataArea, PlotRenderingInfo info, XYPlot plot,
      ValueAxis domainAxis, ValueAxis rangeAxis, XYDataset dataset,
      int series, int item, CrosshairState crosshairState, int pass) {

    // Test if point is need to be drawn
    if (!pointsToDraw[item]) {
      return;
    }

    EntityCollection entities = null;
    if (info != null) {
      entities = info.getOwner().getEntityCollection();
    }

    double x = dataset.getXValue(series, item);
    double y = dataset.getYValue(series, item);
    if (Double.isNaN(x) || Double.isNaN(y)) {
      return;
    }

    double transX = domainAxis.valueToJava2D(x, dataArea,
        plot.getDomainAxisEdge());
    double transY = rangeAxis.valueToJava2D(y, dataArea,
        plot.getRangeAxisEdge());

    PlotOrientation orientation = plot.getOrientation();

    Shape shape;
    if (orientation == PlotOrientation.HORIZONTAL) {
      shape = ShapeUtils.createTranslatedShape(dataPointsShape, transY, transX);
    } else /* if (orientation == PlotOrientation.VERTICAL) */ {
      shape = ShapeUtils.createTranslatedShape(dataPointsShape, transX, transY);
    }

    if (shape.intersects(dataArea)) {
      g2.setPaint(getPaint(dataset, series, item));
      g2.fill(shape);
    }

    int datasetIndex = plot.indexOf(dataset);
    updateCrosshairValues(crosshairState, x, y, datasetIndex, transX, transY, orientation);

    if (entities != null) {
        addEntity(entities, shape, dataset, series, item, 0.0, 0.0);
    }
  }

  @Override
  protected Paint getPaint(XYDataset dataset, int series, int item) {
    return ((ColoredXYZDataset) dataset).getPaintScale()
        .getPaint(((ColoredXYZDataset) dataset).getZValue(series, item));
  }

  public void setZOrder(SortOrder zOrder) {
    this.zOrder = zOrder;
  }

}
