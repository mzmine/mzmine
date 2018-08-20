/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.chartbasics.gestures;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.TitleEntity;
import org.jfree.data.Range;
import net.sf.mzmine.chartbasics.ChartLogics;
import net.sf.mzmine.chartbasics.gestures.ChartGesture.Button;
import net.sf.mzmine.chartbasics.gestures.ChartGesture.Entity;
import net.sf.mzmine.chartbasics.gestures.ChartGesture.Event;
import net.sf.mzmine.chartbasics.gestures.ChartGesture.Key;
import net.sf.mzmine.chartbasics.gestures.ChartGestureDragDiffHandler.Orientation;
import net.sf.mzmine.chartbasics.gestures.interf.GestureHandlerFactory;
import net.sf.mzmine.chartbasics.gestures.standard.DragGestureHandlerDef;
import net.sf.mzmine.chartbasics.gestures.standard.GestureHandlerDef;
import net.sf.mzmine.chartbasics.javafx.mouse.MouseEventWrapper;
import net.sf.mzmine.chartbasics.listener.ZoomHistory;

/**
 * The handler processes {@link ChartGestureEvent}s in a {@link Consumer}. Pre-defined handlers and
 * drag-difference handlers can be generated. It also provides a static list of standard gesture
 * handler definitions.
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class ChartGestureHandler {
  /**
   * Some standard handlers
   */
  public enum Handler {
    DEBUG, // Prints out the gesture
    PREVIOUS_ZOOM_HISTORY, // Jump back in the zoom history
    NEXT_ZOOM_HISTORY, // Jump forward in the zoom history
    TITLE_REMOVER, // Remove titles (setVisible false)
    AUTO_ZOOM_AXIS, // Auto zoom axis
    AUTO_ZOOM_OPPOSITE_AXIS, // Auto zoom opposite axis (domain<->range axis)
    SCROLL_AXIS, // Scroll an axis while retaining the zoom
    SCROLL_AXIS_AND_AUTO_ZOOM, // Scroll an axis and auto zoom the other
    ZOOM_AXIS_INCLUDE_ZERO, // Zoom an axis while holding the lowerBound
    ZOOM_AXIS_CENTER; // Zoom and axis centered to the start gesture

    @Override
    public String toString() {
      return super.toString().replaceAll("_", " ");
    }
  }
  /**
   * Some DragDiff standard handlers
   */
  public enum DragHandler {
    AUTO_ZOOM_AXIS, // Auto zoom axis
    AUTO_ZOOM_OPPOSITE_AXIS, // Auto zoom opposite axis (domain<->range axis)
    SCROLL_AXIS, // Scroll an axis while retaining the zoom
    SCROLL_AXIS_AND_AUTO_ZOOM, // Scroll an axis and auto zoom the other
    ZOOM_AXIS_INCLUDE_ZERO, // Zoom an axis while holding the lowerBound
    ZOOM_AXIS_CENTER; // Zoom and axis centered to the start gesture

    @Override
    public String toString() {
      return super.toString().replaceAll("_", " ");
    }
  }

  // static list of standard chartgestures as GestureHandlerFactories
  // initialise + getter
  private static List<GestureHandlerFactory> standardGestures = null;

  // non static section
  private ChartGesture gesture;
  private Consumer<ChartGestureEvent> handler;

  public ChartGestureHandler(ChartGesture gesture, Consumer<ChartGestureEvent> handler) {
    this.gesture = gesture;
    this.handler = handler;
  }

  public ChartGestureHandler(ChartGesture gesture) {
    this.gesture = gesture;
  }

  public void setConsumer(Consumer<ChartGestureEvent> handler) {
    this.handler = handler;
  }

  public void accept(ChartGestureEvent e) {
    if (handler != null)
      handler.accept(e);
  }

  public ChartGesture getGesture() {
    return gesture;
  }

  public Consumer<ChartGestureEvent> getHandler() {
    return handler;
  }


  /**
   * The drag diff handler listens for PRESSED, DRAGGED and RELEASED events and is called for every
   * range difference between two events. is a range difference between two drag events
   * 
   * @param handler A list of DragHandlers (predefined consumers)
   * @param key A list of Key filters which are connected to the handler list
   * @param entity The Entity filter
   * @param button The Button filter
   * @param orient Orientation of drag events (Horizontal or Vertical)
   * @param param Parameters for specific handlers
   */
  public static ChartGestureHandler createDragDiffHandler(DragHandler[] handler, Key[] key,
      Entity entity, Button button, Orientation orient, Object[] param) {
    Consumer<ChartGestureDragDiffEvent>[] consumer =
        (Consumer<ChartGestureDragDiffEvent>[]) new Consumer[handler.length];
    // create all consumers for all keys
    try {
      for (int i = 0; i < consumer.length; i++) {
        consumer[i] = createDragDiffConsumer(handler[i], param);
      }
      return new ChartGestureDragDiffHandler(entity, button, key, consumer);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Creates a predefined consumer for drag diff events
   * 
   * @param handler The predefined consumer
   * @param param Parameters for specific handlers
   * @return
   * @throws Exception
   */
  public static Consumer<ChartGestureDragDiffEvent> createDragDiffConsumer(DragHandler handler,
      Object[] param) throws Exception {
    switch (handler) {
      case SCROLL_AXIS:
        return e -> {
          ValueAxis axis = e.getAxis();
          if (axis != null)
            ChartLogics.offsetAxisAbsolute(axis, e.getDiff());
        };
      case SCROLL_AXIS_AND_AUTO_ZOOM:
        return de -> {
          ValueAxis axis = de.getAxis();
          if (axis != null) {
            ChartLogics.offsetAxisAbsolute(axis, de.getDiff());
            if (de.getEntity().equals(Entity.DOMAIN_AXIS))
              ChartLogics.autoRangeAxis(de.getChartPanel());
            else
              ChartLogics.autoDomainAxis(de.getChartPanel());
          }
        };
      case AUTO_ZOOM_AXIS:
        return de -> {
          ValueAxis axis = de.getAxis();
          if (axis != null) {
            if (de.getEntity().equals(Entity.DOMAIN_AXIS))
              ChartLogics.autoDomainAxis(de.getChartPanel());
            else
              ChartLogics.autoRangeAxis(de.getChartPanel());
          }
        };
      case AUTO_ZOOM_OPPOSITE_AXIS:
        return de -> {
          ValueAxis axis = de.getAxis();
          if (axis != null) {
            if (de.getEntity().equals(Entity.DOMAIN_AXIS))
              ChartLogics.autoRangeAxis(de.getChartPanel());
            else
              ChartLogics.autoDomainAxis(de.getChartPanel());
          }
        };
      case ZOOM_AXIS_INCLUDE_ZERO:
        return de -> {
          ValueAxis axis = de.getAxis();
          if (axis != null) {
            double diff = de.getDiff() / axis.getRange().getLength() * 4;
            ChartLogics.zoomAxis(axis, diff, true);
          }
        };
      case ZOOM_AXIS_CENTER:
        return de -> {
          ValueAxis axis = de.getAxis();
          if (axis != null) {
            double diff = de.getDiff() / axis.getRange().getLength() * 4;
            ChartLogics.zoomAxis(axis, diff, de.getStart());
          }
        };
      default:
        throw new Exception("DragHandler not specified");
    }
  }

  /**
   * Create preset handlers
   * 
   * @param handler
   * @param g
   * @return
   */
  public static ChartGestureHandler createHandler(Handler handler, ChartGesture g) {
    return createHandler(handler, g, null);
  }

  /**
   * Create preset handlers
   * 
   * @param handler
   * @param g
   * @param param Parameters for specific handlers <br>
   * @return
   */
  public static ChartGestureHandler createHandler(Handler handler, final ChartGesture g,
      Object[] param) {
    Consumer<ChartGestureEvent> newHandler = null;
    switch (handler) {
      case DEBUG:
        newHandler = e -> System.out.println(e.toString());
        break;
      case PREVIOUS_ZOOM_HISTORY:
        newHandler = e -> {
          Object o = e.getChartPanel().getClientProperty(ZoomHistory.PROPERTY_NAME);
          if (o != null && o instanceof ZoomHistory) {
            ZoomHistory h = (ZoomHistory) o;
            Range[] range = h.setPreviousPoint();
            if (range != null && range.length > 0 && range[0] != null) {
              ValueAxis dom = e.getChartPanel().getChart().getXYPlot().getDomainAxis();
              ValueAxis ran = e.getChartPanel().getChart().getXYPlot().getRangeAxis();
              ChartLogics.setZoomAxis(dom, range[0]);
              ChartLogics.setZoomAxis(ran, range[1]);
            }
          }
        };
        break;
      case NEXT_ZOOM_HISTORY:
        newHandler = e -> {
          Object o = e.getChartPanel().getClientProperty(ZoomHistory.PROPERTY_NAME);
          if (o != null && o instanceof ZoomHistory) {
            ZoomHistory h = (ZoomHistory) o;
            Range[] range = h.setNextPoint();
            if (range != null && range.length > 0 && range[0] != null) {
              ValueAxis dom = e.getChartPanel().getChart().getXYPlot().getDomainAxis();
              ValueAxis ran = e.getChartPanel().getChart().getXYPlot().getRangeAxis();
              ChartLogics.setZoomAxis(dom, range[0]);
              ChartLogics.setZoomAxis(ran, range[1]);
            }
          }
        };
        break;
      case TITLE_REMOVER:
        newHandler = e -> {
          if (e.getEntity() instanceof TitleEntity) {
            TitleEntity te = (TitleEntity) e.getEntity();
            te.getTitle().setVisible(false);
          }
        };
        break;
      case AUTO_ZOOM_AXIS:
        newHandler = e -> {
          if (e.getGesture().getEntity().equals(Entity.AXIS)) {
            ChartLogics.autoRangeAxis(e.getChartPanel());
            ChartLogics.autoDomainAxis(e.getChartPanel());
          } else if (e.getGesture().getEntity().equals(Entity.RANGE_AXIS))
            ChartLogics.autoRangeAxis(e.getChartPanel());
          else
            ChartLogics.autoDomainAxis(e.getChartPanel());
        };
        break;
      case AUTO_ZOOM_OPPOSITE_AXIS:
        newHandler = e -> {
          if (e.getGesture().getEntity().equals(Entity.AXIS)) {
            ChartLogics.autoRangeAxis(e.getChartPanel());
            ChartLogics.autoDomainAxis(e.getChartPanel());
          } else if (e.getGesture().getEntity().equals(Entity.DOMAIN_AXIS))
            ChartLogics.autoRangeAxis(e.getChartPanel());
          else
            ChartLogics.autoDomainAxis(e.getChartPanel());
        };
        break;
      case SCROLL_AXIS:
        newHandler = e -> {
          ValueAxis axis = e.getAxis();
          if (axis != null) {
            double diff = 0.03;
            if (e.getMouseEvent().isMouseWheelEvent()) {
              diff = -0.10 * e.getMouseEvent().getWheelRotation();
            }
            ChartLogics.offsetAxis(axis, diff);
          }
        };
        break;
      case SCROLL_AXIS_AND_AUTO_ZOOM:
        newHandler = e -> {
          ValueAxis axis = e.getAxis();
          if (axis != null) {
            double diff = 0.03;
            if (e.getMouseEvent().isMouseWheelEvent()) {
              diff = -0.10 * e.getMouseEvent().getWheelRotation();
            }
            ChartLogics.offsetAxis(axis, diff);

            if (e.getGesture().getEntity().equals(Entity.DOMAIN_AXIS))
              ChartLogics.autoRangeAxis(e.getChartPanel());
            else
              ChartLogics.autoDomainAxis(e.getChartPanel());
          }
        };
        break;
      case ZOOM_AXIS_INCLUDE_ZERO:
        newHandler = e -> {
          ValueAxis axis = e.getAxis();
          if (axis != null) {
            double diff = 0.05;
            if (e.getMouseEvent().isMouseWheelEvent()) {
              diff = -0.10 * e.getMouseEvent().getWheelRotation();
            }
            ChartLogics.zoomAxis(axis, diff, true);
          }
        };
        break;
      case ZOOM_AXIS_CENTER:
        newHandler = e -> {
          ValueAxis axis = e.getAxis();
          if (axis != null) {
            MouseEventWrapper p = e.getMouseEvent();
            double diff = 0.05;
            if (e.getMouseEvent().isMouseWheelEvent()) {
              diff = -0.10 * p.getWheelRotation();
            }

            // get data space coordinates
            Point2D point = e.getCoordinates(e.getChartPanel(), p.getX(), p.getY());
            if (point != null) {
              // vertical ?
              Boolean orient = e.isVerticalAxis(e.getChartPanel(), axis);
              if (orient == null)
                return;
              else if (orient)
                ChartLogics.zoomAxis(axis, diff, point.getY());
              else
                ChartLogics.zoomAxis(axis, diff, point.getX());
            }
          }
        };
        break;
      default:
        break;
    }
    if (newHandler == null)
      return null;
    else
      return new ChartGestureHandler(g, newHandler);
  }

  /**
   * A list of standard gestures
   * 
   * @see {@link GestureHandlerDef} {@link DragGestureHandlerDef}
   * @return
   */
  public static List<GestureHandlerFactory> getStandardGestures() {
    if (standardGestures == null)
      initStandardGestures(true, true, true, true, true);
    return standardGestures;
  }

  /**
   * Generates a list of standard chart gesture
   * 
   * @param axisDrag
   * @param axisWheel
   * @param titleRemover
   * @param zoomHistory
   * @param axisAutoRange
   * @return
   */
  public static List<GestureHandlerFactory> initStandardGestures(boolean axisDrag,
      boolean axisWheel, boolean titleRemover, boolean zoomHistory, boolean axisAutoRange) {
    standardGestures = new ArrayList<GestureHandlerFactory>();
    if (axisDrag) {
      // adds multiple gestures to one domain axis drag handler
      // Scroll axis: DRAG mouse over domain axis
      // Scroll + auto zoom: SHIFT + DRAG
      // Zoom axis centered: CTRL + DRAG
      // Zoom + auto zoom range axis: CTRL + SHIFT + DRAG
      standardGestures.add(new DragGestureHandlerDef(
          new DragHandler[] {DragHandler.SCROLL_AXIS, DragHandler.SCROLL_AXIS_AND_AUTO_ZOOM,
              DragHandler.ZOOM_AXIS_CENTER, DragHandler.ZOOM_AXIS_CENTER,
              DragHandler.AUTO_ZOOM_OPPOSITE_AXIS},
          new Key[] {Key.NONE, Key.SHIFT, Key.CTRL, Key.CTRL_SHIFT, Key.CTRL_SHIFT},
          Entity.DOMAIN_AXIS, Button.BUTTON1, null, null));

      // Zoom range axis (include zero): DRAG
      standardGestures
          .add(new DragGestureHandlerDef(new DragHandler[] {DragHandler.ZOOM_AXIS_INCLUDE_ZERO},
              new Key[] {Key.ALL}, Entity.RANGE_AXIS, Button.BUTTON1, null, null));
    }
    if (axisWheel) {
      // MOUSE WHEEL on domain axis
      // Scroll axis: WHEEL over domain axis
      // Scroll + auto zoom: SHIFT + WHEEL
      // Zoom axis centered: CTRL + WHEEL
      // Zoom + auto zoom range axis: CTRL + SHIFT + WHEEL
      standardGestures.add(new GestureHandlerDef(Handler.SCROLL_AXIS, Entity.DOMAIN_AXIS,
          new Event[] {Event.MOUSE_WHEEL}, null, Key.NONE, null));
      standardGestures.add(new GestureHandlerDef(Handler.SCROLL_AXIS_AND_AUTO_ZOOM,
          Entity.DOMAIN_AXIS, new Event[] {Event.MOUSE_WHEEL}, null, Key.SHIFT, null));
      standardGestures.add(new GestureHandlerDef(Handler.ZOOM_AXIS_CENTER, Entity.DOMAIN_AXIS,
          new Event[] {Event.MOUSE_WHEEL}, null, Key.CTRL, null));
      standardGestures.add(new GestureHandlerDef(Handler.ZOOM_AXIS_CENTER, Entity.DOMAIN_AXIS,
          new Event[] {Event.MOUSE_WHEEL}, null, Key.CTRL_SHIFT, null));
      standardGestures.add(new GestureHandlerDef(Handler.AUTO_ZOOM_OPPOSITE_AXIS,
          Entity.DOMAIN_AXIS, new Event[] {Event.MOUSE_WHEEL}, null, Key.CTRL_SHIFT, null));
      // Zoom range axis (include zero): MOUSE WHEEL
      standardGestures.add(new GestureHandlerDef(Handler.ZOOM_AXIS_INCLUDE_ZERO, Entity.RANGE_AXIS,
          new Event[] {Event.MOUSE_WHEEL}, null, Key.ALL, null));
    }
    if (zoomHistory) {
      // Previous zoom history: DOUBLE CLICK on plot
      // Next zoom history: CTRL + DOUBLE CLICK on plot
      standardGestures.add(new GestureHandlerDef(Handler.PREVIOUS_ZOOM_HISTORY, Entity.PLOT,
          new Event[] {Event.DOUBLE_CLICK}, Button.BUTTON1, Key.NONE, null));
      standardGestures.add(new GestureHandlerDef(Handler.NEXT_ZOOM_HISTORY, Entity.PLOT,
          new Event[] {Event.DOUBLE_CLICK}, Button.BUTTON1, Key.CTRL, null));
    }
    if (titleRemover) {
      // Remove titles, legends: CTRL + CLICK on titles
      standardGestures.add(new GestureHandlerDef(Handler.TITLE_REMOVER, Entity.TITLE,
          new Event[] {Event.CLICK}, Button.BUTTON1, Key.CTRL, null));
    }
    if (axisAutoRange) {
      // Auto zoom axes: DOUBLE CLICK on axis
      standardGestures.add(new GestureHandlerDef(Handler.AUTO_ZOOM_AXIS, Entity.DOMAIN_AXIS,
          new Event[] {Event.DOUBLE_CLICK}, Button.BUTTON1, null, null));
      standardGestures.add(new GestureHandlerDef(Handler.AUTO_ZOOM_AXIS, Entity.RANGE_AXIS,
          new Event[] {Event.DOUBLE_CLICK}, Button.BUTTON1, null, null));
    }
    return standardGestures;
  }
}
