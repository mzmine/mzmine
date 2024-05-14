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

package io.github.mzmine.gui.chartbasics.gestures;

import java.awt.geom.Point2D;
import java.util.function.Consumer;
import java.util.logging.Logger;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.entity.AxisEntity;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.plot.PlotOrientation;

import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.GestureButton;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.Entity;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.Event;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.Key;
import io.github.mzmine.gui.chartbasics.gui.wrapper.ChartViewWrapper;
import io.github.mzmine.gui.chartbasics.gui.wrapper.MouseEventWrapper;

/**
 * The {@link ChartGestureDragDiffHandler} consumes primary mouse events to generate
 * {@link ChartGestureDragDiffEvent}s. These events are then processed by one or multiple
 * {@link Consumer}s. Each Consumer has a specific {@link Key} filter. Key and Consumer array have
 * to be sorted accordingly.
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class ChartGestureDragDiffHandler extends ChartGestureHandler {
  private Logger logger = Logger.getLogger(this.getClass().getName());

  public enum Orientation {
    VERTICAL, HORIZONTAL;
  }

  protected Key[] key;
  protected Consumer<ChartGestureDragDiffEvent> dragDiffHandler[];
  // default orientation
  protected Orientation orient = Orientation.HORIZONTAL;

  public ChartGestureDragDiffHandler(ChartGesture.Entity entity, GestureButton button, Key[] key,
      Consumer<ChartGestureDragDiffEvent> dragDiffHandler) {
    this(entity, button, key, new Consumer[] {dragDiffHandler});
  }

  public ChartGestureDragDiffHandler(ChartGesture.Entity entity, GestureButton button, Key[] key,
      Consumer<ChartGestureDragDiffEvent> dragDiffHandler[]) {
    this(entity, button, key, dragDiffHandler, Orientation.HORIZONTAL);
  }

  public ChartGestureDragDiffHandler(ChartGesture.Entity entity, GestureButton button, Key[] key,
      Consumer<ChartGestureDragDiffEvent> dragDiffHandler[], Orientation defaultOrientation) {
    super(new ChartGesture(entity, new Event[] {Event.RELEASED, Event.PRESSED, Event.DRAGGED},
        button, Key.ALL));
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
  public Orientation getOrientation(ChartGestureEvent event) {
    ChartEntity ce = event.getEntity();
    if (ce instanceof AxisEntity) {
      JFreeChart chart = event.getChart();
      PlotOrientation plotorient = PlotOrientation.HORIZONTAL;
      if (chart.getXYPlot() != null)
        plotorient = chart.getXYPlot().getOrientation();
      else if (chart.getCategoryPlot() != null)
        plotorient = chart.getCategoryPlot().getOrientation();

      Entity entity = event.getGesture().getEntity();
      if ((entity.equals(Entity.DOMAIN_AXIS) && plotorient.equals(PlotOrientation.VERTICAL))
          || (entity.equals(Entity.RANGE_AXIS) && plotorient.equals(PlotOrientation.HORIZONTAL)))
        orient = Orientation.HORIZONTAL;
      else
        orient = Orientation.VERTICAL;
    }
    return orient;
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
            Point2D released = chartPanel.mouseXYToPlotXY(e.getX(), e.getY());
            if (released != null) {
              double offset = 0;
              double start = 0;
              // scroll x
              if (getOrientation(event).equals(Orientation.HORIZONTAL)) {
                offset = -(released.getX() - last.getX());
                start = first.getX();
              }
              // scroll y
              else {
                offset = -(released.getY() - last.getY());
                start = first.getY();
              }

              // new dragdiff event
              ChartGestureDragDiffEvent dragEvent = new ChartGestureDragDiffEvent(startEvent,
                  lastEvent, event, start, offset, orient);
              // scroll / zoom / do anything with this new event
              // choose handler by key filter
              for (int i = 0; i < dragDiffHandler.length; i++)
                if (key[i].filter(event.getMouseEvent()))
                  dragDiffHandler[i].accept(dragEvent);
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
}
