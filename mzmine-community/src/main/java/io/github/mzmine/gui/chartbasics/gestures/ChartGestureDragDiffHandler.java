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

package io.github.mzmine.gui.chartbasics.gestures;

import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.Entity;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.Event;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.GestureButton;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.Key;
import io.github.mzmine.gui.chartbasics.gui.wrapper.ChartViewWrapper;
import io.github.mzmine.gui.chartbasics.gui.wrapper.MouseEventWrapper;
import java.awt.geom.Point2D;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.AxisEntity;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ui.RectangleEdge;

/**
 * The {@link ChartGestureDragDiffHandler} consumes primary mouse events to generate
 * {@link ChartGestureDragDiffEvent}s. These events are then processed by one or multiple
 * {@link Consumer}s. Each Consumer has a specific {@link Key} filter. Key and Consumer array have
 * to be sorted accordingly.
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class ChartGestureDragDiffHandler extends ChartGestureHandler {

  public enum Orientation {
    VERTICAL, HORIZONTAL;
  }

  protected Key[] key;
  protected Consumer<ChartGestureDragDiffEvent> dragDiffHandler[];
  // default orientation
  protected Orientation orient = Orientation.HORIZONTAL;

  public ChartGestureDragDiffHandler(ChartGesture.Entity entity, GestureButton button, Key[] key,
      Consumer<ChartGestureDragDiffEvent> dragDiffHandler) {
    this(entity, button, key, new Consumer[]{dragDiffHandler});
  }

  public ChartGestureDragDiffHandler(ChartGesture.Entity entity, GestureButton button, Key[] key,
      Consumer<ChartGestureDragDiffEvent> dragDiffHandler[]) {
    this(entity, button, key, dragDiffHandler, Orientation.HORIZONTAL);
  }

  public ChartGestureDragDiffHandler(ChartGesture.Entity entity, GestureButton button, Key[] key,
      Consumer<ChartGestureDragDiffEvent> dragDiffHandler[], Orientation defaultOrientation) {
    super(
        new ChartGesture(entity, new Event[]{Event.RELEASED, Event.PRESSED, Event.DRAGGED}, button,
            Key.ALL));
    /**
     * Handles PRESSED, DRAGGED, RELEASED Events Fires the correct DragDiffHandlers for the Key
     * filter
     */
    setConsumer(createConsumer());
    // super() finished
    this.key = key;
    this.dragDiffHandler = dragDiffHandler;
    this.orient = defaultOrientation;
  }

  /**
   * use default orientation or orientation of axis
   *
   * @param event
   * @return
   */
  public @NotNull Orientation getOrientation(final @NotNull ChartGestureEvent event) {
    final Orientation axisOrientation = getAxisOrientation(event);
    if (axisOrientation != null) {
      orient = axisOrientation;
      return orient;
    }

    ChartEntity ce = event.getEntity();
    if (ce instanceof AxisEntity) {
      JFreeChart chart = event.getChart();
      PlotOrientation plotorient = PlotOrientation.HORIZONTAL;
      if (chart.getXYPlot() != null) {
        plotorient = chart.getXYPlot().getOrientation();
      } else if (chart.getCategoryPlot() != null) {
        plotorient = chart.getCategoryPlot().getOrientation();
      }

      Entity entity = event.getGesture().getEntity();
      if ((entity.equals(Entity.DOMAIN_AXIS) && plotorient.equals(PlotOrientation.VERTICAL)) || (
          entity.equals(Entity.RANGE_AXIS) && plotorient.equals(PlotOrientation.HORIZONTAL))) {
        orient = Orientation.HORIZONTAL;
      } else {
        orient = Orientation.VERTICAL;
      }
    }
    return orient;
  }

  private @Nullable Orientation getAxisOrientation(final @NotNull ChartGestureEvent event) {
    final ValueAxis axis = event.getAxis();
    if (axis == null || !(axis.getPlot() instanceof XYPlot plot)) {
      return null;
    }

    for (int i = 0; i < plot.getDomainAxisCount(); i++) {
      if (axis.equals(plot.getDomainAxis(i))) {
        final RectangleEdge edge = plot.getDomainAxisEdge(i);
        return RectangleEdge.isTopOrBottom(edge) ? Orientation.HORIZONTAL : Orientation.VERTICAL;
      }
    }
    for (int i = 0; i < plot.getRangeAxisCount(); i++) {
      if (axis.equals(plot.getRangeAxis(i))) {
        final RectangleEdge edge = plot.getRangeAxisEdge(i);
        return RectangleEdge.isTopOrBottom(edge) ? Orientation.HORIZONTAL : Orientation.VERTICAL;
      }
    }
    return null;
  }

  /**
   * Handle PRESSED, DRAGGED, RELEASED events to generate drag diff events
   *
   * @return
   */
  private Consumer<ChartGestureEvent> createConsumer() {
    return new Consumer<ChartGestureEvent>() {
      // variables
      boolean wasMouseZoomable = false;
      Point2D last = null, first = null;
      ChartGestureEvent startEvent = null, lastEvent = null;

      @Override
      public void accept(ChartGestureEvent event) {
        ChartViewWrapper chartPanel = event.getChartWrapper();
        JFreeChart chart = chartPanel.getChart();
        MouseEventWrapper e = event.getMouseEvent();

        // released?
        if (event.checkEvent(Event.RELEASED)) {
          chartPanel.setMouseZoomable(wasMouseZoomable);
          last = null;
        } else if (event.checkEvent(Event.PRESSED)) {
          // get data space coordinates
          last = chartPanel.mouseXYToPlotXY(e.getX(), e.getY());
          first = last;
          startEvent = event;
          lastEvent = event;
          if (last != null) {
            wasMouseZoomable = chartPanel.isMouseZoomable();
            chartPanel.setMouseZoomable(false);
          }
        } else if (event.checkEvent(Event.DRAGGED)) {
          if (last != null) {
            // get data space coordinates
            final Point2D released = chartPanel.mouseXYToPlotXY(e.getX(), e.getY());
            if (released != null) {
              final Orientation dragOrientation = getOrientation(event);
              final double offset = -(getAxisCoordinate(event, released, dragOrientation)
                  - getAxisCoordinate(event, last, dragOrientation));
              final double start = getAxisCoordinate(startEvent, first, dragOrientation);

              // new dragdiff event
              ChartGestureDragDiffEvent dragEvent = new ChartGestureDragDiffEvent(startEvent,
                  lastEvent, event, start, offset, dragOrientation);
              // scroll / zoom / do anything with this new event
              // choose handler by key filter
              for (int i = 0; i < dragDiffHandler.length; i++) {
                if (key[i].filter(event.getMouseEvent())) {
                  dragDiffHandler[i].accept(dragEvent);
                }
              }
              // set last event
              lastEvent = event;
              // save updated last
              last = chartPanel.mouseXYToPlotXY(e.getX(), e.getY());
            }
          }
        }
      }
    };
  }

  private double getAxisCoordinate(final @NotNull ChartGestureEvent event,
      final @NotNull Point2D point, final @NotNull Orientation axisOrientation) {
    final Entity entity = event.getGesture().getEntity();
    if (Entity.DOMAIN_AXIS.equals(entity)) {
      return point.getX();
    }
    if (Entity.RANGE_AXIS.equals(entity)) {
      return point.getY();
    }
    // assumption: for generic axis gestures use the rendered axis direction as fallback.
    return axisOrientation.equals(Orientation.HORIZONTAL) ? point.getX() : point.getY();
  }
}
