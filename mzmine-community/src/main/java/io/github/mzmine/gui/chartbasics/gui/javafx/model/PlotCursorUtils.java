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

package io.github.mzmine.gui.chartbasics.gui.javafx.model;

import io.github.mzmine.gui.chartbasics.ChartLogicsFX;
import io.github.mzmine.gui.chartbasics.RenderedValueAxis;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.Entity;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.Event;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.GestureButton;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.Key;
import io.github.mzmine.gui.chartbasics.gestures.ChartGestureEvent;
import io.github.mzmine.gui.chartbasics.gestures.ChartGestureHandler;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.chartbasics.simplechart.PlotCursorPosition;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.util.MathUtils;
import io.github.mzmine.util.maths.Precision;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.function.Consumer;
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

    // mabye there is a more efficient way of searching for the selected value index.
    int itemIndex = -1;
    @Nullable XYDataset bestDataset = null;
    double dist = Double.MAX_VALUE;

    double bestX = -1;
    double bestY = -1;

    for (int dsIndex = 0; dsIndex < plot.getDatasetCount(); dsIndex++) {
      final XYDataset dataset = plot.getDataset(dsIndex);
      if (dataset == null || !isCursorSelectable(dataset)) {
        continue;
      }

      final RenderedValueAxis domainAxis = RenderedValueAxis.domainOfDataset(plot, dataset);
      final RenderedValueAxis rangeAxis = RenderedValueAxis.rangeOfDataset(plot, dataset);
      if (rangeAxis == null || domainAxis == null) {
        continue;
      }

      for (int series = 0; series < dataset.getSeriesCount(); series++) {
        final int numDP = dataset.getItemCount(series);
        for (int i = 0; i < numDP; i++) {
          final double itemX = dataset.getXValue(series, i);
          final double itemY = dataset.getYValue(series, i);
          final double screenX = domainAxis.valueToJava2D(itemX, dataArea);
          final double screenY = rangeAxis.valueToJava2D(itemY, dataArea);
          // assumption: selectable datasets are single-series in the current cursor model.
          final double newDist = MathUtils.getDistance(searchX, searchY, screenX, screenY);
          if (newDist < dist) {
            dist = newDist;
            itemIndex = i;
            bestDataset = dataset;
            bestX = itemX;
            bestY = itemY;
          }
        }
      }
    }
    if (itemIndex == -1) {
      cursorPositionProperty.set(null);
    } else {
      cursorPositionProperty.set(new PlotCursorPosition(bestX, bestY, itemIndex, bestDataset,
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
   * Adds timeline-style scrubbing to an XY plot. Pressing selects the closest data point and locks
   * the drag to that dataset. Horizontal dragging then selects the point nearest the cursor's
   * domain value, independent of the cursor's vertical position.
   *
   * <p>Rectangle zoom is suspended only for the duration of an active scrub and restored on mouse
   * release. Modifier-assisted chart gestures remain available because scrubbing requires no
   * modifier key.</p>
   */
  public static void addMouseScrubListener(EChartViewer viewer, XYPlot plot,
      ObjectProperty<@Nullable PlotCursorPosition> cursorPositionProperty) {
    viewer.getMouseAdapter().addGestureHandler(new ChartGestureHandler(
        new ChartGesture(Entity.ALL_PLOT_AND_DATA,
            new Event[]{Event.PRESSED, Event.DRAGGED, Event.RELEASED},
            GestureButton.BUTTON1, Key.NONE), new Consumer<>() {
      private boolean scrubbing;
      private boolean wasMouseZoomable;
      private @Nullable XYDataset scrubDataset;

      @Override
      public void accept(ChartGestureEvent event) {
        if (event.checkEvent(Event.PRESSED)) {
          findSetCursorPosition(event, viewer.getRenderingInfo(), plot, cursorPositionProperty);
          final PlotCursorPosition position = cursorPositionProperty.get();
          scrubDataset = position == null ? null : position.getDataset();
          if (scrubDataset != null) {
            wasMouseZoomable = viewer.isMouseZoomable();
            viewer.setMouseZoomable(false);
            scrubbing = true;
          }
        } else if (event.checkEvent(Event.DRAGGED) && scrubbing) {
          findSetDomainCursorPosition(event, viewer.getRenderingInfo(), plot, scrubDataset,
              cursorPositionProperty);
        } else if (event.checkEvent(Event.RELEASED) && scrubbing) {
          findSetDomainCursorPosition(event, viewer.getRenderingInfo(), plot, scrubDataset,
              cursorPositionProperty);
          viewer.setMouseZoomable(wasMouseZoomable);
          scrubDataset = null;
          scrubbing = false;
        }
      }
    }));
  }

  /** Selects the point nearest the mouse's domain coordinate in one dataset. */
  private static void findSetDomainCursorPosition(ChartGestureEvent event,
      @Nullable ChartRenderingInfo renderInfo, XYPlot plot, @Nullable XYDataset dataset,
      ObjectProperty<@Nullable PlotCursorPosition> cursorPositionProperty) {
    if (renderInfo == null || dataset == null || !isCursorSelectable(dataset)) {
      return;
    }

    final double mouseX = event.getMouseEvent().getX();
    final double mouseY = event.getMouseEvent().getY();
    final Rectangle2D dataArea = ChartLogicsFX.getDataArea(mouseX, mouseY, renderInfo);
    final RenderedValueAxis domainAxis = RenderedValueAxis.domainOfDataset(plot, dataset);
    if (domainAxis == null) {
      return;
    }

    final double domainValue = domainAxis.java2DToValue(mouseX, dataArea);
    int bestIndex = -1;
    double bestDistance = Double.POSITIVE_INFINITY;
    double bestX = Double.NaN;
    double bestY = Double.NaN;

    for (int series = 0; series < dataset.getSeriesCount(); series++) {
      for (int item = 0; item < dataset.getItemCount(series); item++) {
        final double itemX = dataset.getXValue(series, item);
        final double distance = Math.abs(itemX - domainValue);
        if (distance < bestDistance) {
          bestDistance = distance;
          bestIndex = item;
          bestX = itemX;
          bestY = dataset.getYValue(series, item);
        }
      }
    }

    if (bestIndex >= 0) {
      cursorPositionProperty.set(new PlotCursorPosition(bestX, bestY, bestIndex, dataset,
          event.getMouseEvent()));
    }
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
    if (pos != null && Precision.equalFloatSignificance(domain, pos.getDomainValue())
        && Precision.equalFloatSignificance(range, pos.getRangeValue())) {
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
    if (pos != null && Precision.equalFloatSignificance(value, pos.getDomainValue())) {
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
    if (pos != null && Precision.equalFloatSignificance(value, pos.getRangeValue())) {
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
    if (!isCursorSelectable(dataset)) {
      return null;
    }
    for (int series = 0; series < dataset.getSeriesCount(); series++) {
      for (int i = 0; i < dataset.getItemCount(series); i++) {
        final double x = dataset.getXValue(series, i);
        final double y = dataset.getYValue(series, i);
        if (Double.compare(x, domain) == 0 && Double.compare(y, range) == 0) {
          return new PlotCursorPosition(x, y, i, dataset);
        }
      }
    }
    return null;
  }

  @Nullable
  private static PlotCursorPosition findItemInDatasetByDomain(@NotNull XYDataset dataset,
      double domain) {
    if (dataset.getSeriesCount() == 0 || !isCursorSelectable(dataset)) {
      return null;
    }
    for (int series = 0; series < dataset.getSeriesCount(); series++) {
      for (int i = 0; i < dataset.getItemCount(series); i++) {
        final double x = dataset.getXValue(series, i);
        final double y = dataset.getYValue(series, i);
        if (Double.compare(x, domain) == 0) {
          return new PlotCursorPosition(x, y, i, dataset);
        }
      }
    }
    return null;
  }

  @Nullable
  private static PlotCursorPosition findItemInDatasetByRange(@NotNull XYDataset dataset,
      double range) {
    if (dataset.getSeriesCount() == 0 || !isCursorSelectable(dataset)) {
      return null;
    }
    for (int series = 0; series < dataset.getSeriesCount(); series++) {
      for (int i = 0; i < dataset.getItemCount(series); i++) {
        final double x = dataset.getXValue(series, i);
        final double y = dataset.getYValue(series, i);
        if (Double.compare(y, range) == 0) {
          return new PlotCursorPosition(x, y, i, dataset);
        }
      }
    }
    return null;
  }

  private static boolean isCursorSelectable(final @Nullable XYDataset dataset) {
    return !(dataset instanceof ColoredXYDataset coloredDataset)
        || coloredDataset.isCursorSelectable();
  }
}
