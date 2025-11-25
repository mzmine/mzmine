/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.gui.chartbasics.gui.javafx.model;

import io.github.mzmine.gui.chartbasics.ChartLogicsFX;
import io.github.mzmine.gui.chartbasics.RenderedValueAxis;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.Entity;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.Event;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.GestureButton;
import io.github.mzmine.gui.chartbasics.gestures.ChartGestureEvent;
import io.github.mzmine.gui.chartbasics.gestures.ChartGestureHandler;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.chartbasics.simplechart.PlotCursorPosition;
import io.github.mzmine.util.MathUtils;
import io.github.mzmine.util.maths.Precision;
import java.awt.geom.Rectangle2D;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;

public class PlotCursorUtils {


  public static void findSetCursorPosition(ChartGestureEvent event,
      @Nullable ChartRenderingInfo renderInfo, XYPlot plot,
      ObjectProperty<@Nullable PlotCursorPosition> cursorPositionProperty) {
    if (renderInfo == null) {
      // should not happen usually when this originates from mouse event
      return;
    }

    // use mouse coordinates and then search closest dp
    final double searchX = event.getMouseEvent().getX();
    final double searchY = event.getMouseEvent().getY();

    final Rectangle2D dataArea = ChartLogicsFX.getDataArea(searchX, searchY, renderInfo);

    // find axis
    RenderedValueAxis domainAxis = RenderedValueAxis.domainOf(plot);
    RenderedValueAxis rangeAxis = RenderedValueAxis.rangeOf(plot);
    if (rangeAxis == null || domainAxis == null) {
      throw new UnsupportedOperationException(
          "No axis found for plot (maybe category or combined axis plot?)" + plot);
    }

    // mabye there is a more efficient way of searching for the selected value index.
    int itemIndex = -1;
    int datasetIndex = -1;
    double dist = Double.MAX_VALUE;

    double bestX = -1;
    double bestY = -1;

    for (int dsIndex = 0; dsIndex < plot.getDatasetCount(); dsIndex++) {
      XYDataset dataset = plot.getDataset(dsIndex);
      if (dataset == null) {
        continue;
      }

      final int numDP = dataset.getItemCount(0);
      for (int i = 0; i < numDP; i++) {
        final double itemX = dataset.getXValue(0, i);
        final double itemY = dataset.getYValue(0, i);
        final double screenX = domainAxis.valueToJava2D(itemX, dataArea);
        final double screenY = rangeAxis.valueToJava2D(itemY, dataArea);
        // compare on pixel basis
        double newDist = MathUtils.getDistance(searchX, searchY, screenX, screenY);
        if (newDist < dist) {
          dist = newDist;
          itemIndex = i;
          datasetIndex = dsIndex;
          bestX = itemX;
          bestY = itemY;
        }
      }
    }
    if (itemIndex == -1) {
      cursorPositionProperty.set(null);
    } else {
      cursorPositionProperty.set(
          new PlotCursorPosition(bestX, bestY, itemIndex, plot.getDataset(datasetIndex),
              event.getMouseEvent()));
    }
  }

  /**
   * Adds a mouse listener to the chart that will handle cursor positions on click
   */
  public static void addMouseListener(EChartViewer viewer, XYPlot plot,
      ObjectProperty<@Nullable PlotCursorPosition> cursorPositionProperty) {
    viewer.getMouseAdapter().addGestureHandler(new ChartGestureHandler(
        new ChartGesture(Entity.ALL_PLOT_AND_DATA, Event.CLICK, GestureButton.BUTTON1), e -> {
      PlotCursorUtils.findSetCursorPosition(e, viewer.getRenderingInfo(), plot,
          cursorPositionProperty);
    }));
  }

  /**
   *
   * @param pos
   * @param datasets
   * @param domain
   * @param range
   * @return the original position if the xy coordinates are the same, or the moved position with
   * dataset if found exactly the same data point, or a position without the dataset if there is no
   * dataset with exactly these coordinates.
   */
  @NotNull
  public static PlotCursorPosition moveCursorFindInData(@Nullable PlotCursorPosition pos,
      @NotNull List<? extends XYDataset> datasets, double domain, double range) {
    if (pos != null && Precision.equalDoubleSignificance(domain, pos.getDomainValue())
        && Precision.equalDoubleSignificance(range, pos.getRangeValue())) {
      // skip for the same values
      return pos;
    }

    if (pos != null && pos.getDataset() != null) {
      // check old dataset first
      var newPos = findItemInDataset(pos.getDataset(), domain, range);
      if (newPos != null) {
        return newPos;
      }
    }

    // find first dataset that contains these values
    for (XYDataset dataset : datasets) {
      var newPos = findItemInDataset(dataset, domain, range);
      if (newPos != null) {
        return newPos;
      }
    }
    return new PlotCursorPosition(domain, range);
  }

  /**
   *
   * @return the original position if the xy coordinates are the same, or the moved position with
   * dataset if found exactly the same data point, or a position without the dataset if there is no
   * dataset with exactly these coordinates.
   */
  @NotNull
  public static PlotCursorPosition moveDomainCursorFindInData(@Nullable PlotCursorPosition pos,
      List<? extends XYDataset> datasets, double value) {
    if (pos != null && Precision.equalDoubleSignificance(value, pos.getDomainValue())) {
      // skip for the same values
      return pos;
    }

    if (pos != null && pos.getDataset() != null) {
      // check old dataset first
      var newPos = findItemInDatasetByDomain(pos.getDataset(), value);
      if (newPos != null) {
        return newPos;
      }
    }

    // find first dataset that contains these values
    for (XYDataset dataset : datasets) {
      var newPos = findItemInDatasetByDomain(dataset, value);
      if (newPos != null) {
        return newPos;
      }
    }
    return new PlotCursorPosition(value, null);
  }

  /**
   *
   * @return the original position if the xy coordinates are the same, or the moved position with
   * dataset if found exactly the same data point, or a position without the dataset if there is no
   * dataset with exactly these coordinates.
   */
  @NotNull
  public static PlotCursorPosition moveRangeCursorFindInData(@Nullable PlotCursorPosition pos,
      List<? extends XYDataset> datasets, double value) {
    if (pos != null && Precision.equalDoubleSignificance(value, pos.getRangeValue())) {
      // skip for the same values
      return pos;
    }

    if (pos != null && pos.getDataset() != null) {
      // check old dataset first
      var newPos = findItemInDatasetByRange(pos.getDataset(), value);
      if (newPos != null) {
        return newPos;
      }
    }

    // find first dataset that contains these values
    for (XYDataset dataset : datasets) {
      var newPos = findItemInDatasetByRange(dataset, value);
      if (newPos != null) {
        return newPos;
      }
    }
    return new PlotCursorPosition(value, null);
  }

  @Nullable
  private static PlotCursorPosition findItemInDataset(@NotNull XYDataset dataset, double domain,
      double range) {
    for (int i = 0; i < dataset.getItemCount(0); i++) {
      final double x = dataset.getXValue(0, i);
      final double y = dataset.getYValue(0, i);
      if (Double.compare(x, domain) == 0 && Double.compare(y, range) == 0) {
        return new PlotCursorPosition(x, y, i, dataset);
      }
    }
    return null;
  }

  @Nullable
  private static PlotCursorPosition findItemInDatasetByDomain(@NotNull XYDataset dataset,
      double domain) {
    if (dataset.getSeriesCount() == 0) {
      return null;
    }
    for (int i = 0; i < dataset.getItemCount(0); i++) {
      final double x = dataset.getXValue(0, i);
      final double y = dataset.getYValue(0, i);
      if (Double.compare(x, domain) == 0) {
        return new PlotCursorPosition(x, y, i, dataset);
      }
    }
    return null;
  }

  @Nullable
  private static PlotCursorPosition findItemInDatasetByRange(@NotNull XYDataset dataset,
      double range) {
    if (dataset.getSeriesCount() == 0) {
      return null;
    }
    for (int i = 0; i < dataset.getItemCount(0); i++) {
      final double x = dataset.getXValue(0, i);
      final double y = dataset.getYValue(0, i);
      if (Double.compare(y, range) == 0) {
        return new PlotCursorPosition(x, y, i, dataset);
      }
    }
    return null;
  }
}
