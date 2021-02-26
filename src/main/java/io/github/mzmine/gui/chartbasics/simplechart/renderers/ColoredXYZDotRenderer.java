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

package io.github.mzmine.gui.chartbasics.simplechart.renderers;

import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYZDataset;
import io.github.mzmine.gui.chartbasics.simplechart.generators.SimpleToolTipGenerator;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.HashSet;
import java.util.Set;
import javafx.application.Platform;
import javafx.util.Pair;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.renderer.xy.XYShapeRenderer;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.util.ShapeUtils;
import org.jfree.data.Range;
import org.jfree.data.xy.XYDataset;

public class ColoredXYZDotRenderer extends XYShapeRenderer {

  private static final Shape dataPointsShape = new Ellipse2D.Double(0, 0, 7, 7);

  public boolean pointsReduction = false;
  private final Set<Pair<Integer, Integer>> pointsCoordinates = new HashSet<>();

  public ColoredXYZDotRenderer() {
    super();

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
  public XYItemRendererState initialise(Graphics2D var1, Rectangle2D var2, XYPlot var3, XYDataset var4, PlotRenderingInfo var5) {
    XYItemRendererState state = super.initialise(var1, var2, var3, var4, var5);

    // Do not call drawItem, if item is not visible(e.g. after zoom)
    state.setProcessVisibleItemsOnly(true);

    // Clear saved coordinated, when the renderer is initialized
    pointsCoordinates.clear();

    if (pointsReduction) {
      Platform.runLater(() -> System.out
          .println("[DEBUG] Number of \"unique\" data points: " + pointsCoordinates.size()));
    }

    return state;
  }

  @Override
  public void drawItem(Graphics2D g2, XYItemRendererState state,
      Rectangle2D dataArea, PlotRenderingInfo info, XYPlot plot,
      ValueAxis domainAxis, ValueAxis rangeAxis, XYDataset dataset,
      int series, int item, CrosshairState crosshairState, int pass) {

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

    // Do not show points, that are close to each other
    if (pointsReduction) {

      int roundX = (int) Math.round(transX);
      int roundY = (int) Math.round(transY);
      if (roundX % 2 == 0) {
        roundX++;
      }
      if (roundY % 2 == 0) {
        roundY++;
      }
      Pair<Integer, Integer> pair = new Pair<>(roundX, roundY);

      if (pointsCoordinates.contains(pair)) {
        return;
      }

      pointsCoordinates.add(pair);
    }

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

}
