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

package io.github.mzmine.gui.chartbasics.gui.swing;

import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.EntityCollection;

import io.github.mzmine.gui.chartbasics.gestures.ChartGesture;
import io.github.mzmine.gui.chartbasics.gestures.ChartGestureEvent;
import io.github.mzmine.gui.chartbasics.gestures.ChartGestureHandler;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.GestureButton;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.Entity;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.Event;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.Key;
import io.github.mzmine.gui.chartbasics.gestures.ChartGestureDragDiffHandler.Orientation;
import io.github.mzmine.gui.chartbasics.gestures.ChartGestureHandler.DragHandler;
import io.github.mzmine.gui.chartbasics.gestures.ChartGestureHandler.Handler;
import io.github.mzmine.gui.chartbasics.gui.wrapper.GestureMouseAdapter;

/**
 * Handles all MouseEvents (like a MouseAdapter) and transforms them into {@link ChartGestureEvent}s
 * which are then handled by a list of {@link ChartGestureHandler}s.
 * <p>
 * Add this adapter to a ChartPanel as a mouse and mousewheel listener.
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class ChartGestureMouseAdapter extends MouseAdapter implements GestureMouseAdapter {

  public static final Logger logger = Logger.getLogger(ChartGestureMouseAdapter.class.getName());

  private int lastEntityX = -1, lastEntityY = -1;
  private ChartEntity lastEntity = null;
  private ChartGestureEvent lastDragEvent = null;

  // handle gestures
  private List<ChartGestureHandler> gestureHandlers;
  // listen for
  private HashMap<Event, Boolean> listensFor = new HashMap<Event, Boolean>(Event.values().length);

  /**
   * True if Event.ALL or e was added as part of a gesture handler
   * 
   * @param e
   * @return
   */
  private boolean listensFor(Event e) {
    return listensFor.get(Event.ALL) || listensFor.get(e);
  }

  /**
   * Call all handlers for a specific Gesture Also call all parent handlers (AXIS is parent entity
   * of RANGE_AXIS entity ALL is parent of all events/entities)
   * 
   * @param e
   */
  private void handleEvent(final ChartGestureEvent e) {
    if (gestureHandlers != null)
      gestureHandlers.stream().filter(handler -> handler.getGesture().filter(e.getGesture()))
          .forEach(handler -> handler.accept(e));
  }

  /**
   * Add drag handlers for each key (key and handler have to be ordered)
   * 
   * @param g
   * @param handler
   */
  public void addDragGestureHandler(DragHandler[] handler, Key[] key, Entity entity, GestureButton button,
      Orientation orient, Object[] param) {
    ChartGestureHandler h =
        ChartGestureHandler.createDragDiffHandler(handler, key, entity, button, orient, param);
    addGestureHandler(h);
  }

  /**
   * Add a preset handler for specific gestures and ChartMouseGestureEvents
   * 
   * @param g
   * @param handler
   */
  public void addGestureHandler(Handler handler, Entity entity, Event[] event, GestureButton button,
      Key key, Object[] param) {
    ChartGestureHandler h = ChartGestureHandler.createHandler(handler,
        new ChartGesture(entity, event, button, key), param);
    addGestureHandler(h);
  }

  /**
   * Add a handler for specific gestures and ChartMouseGestureEvents
   * 
   * @param g
   * @param handler
   */
  public void addGestureHandler(ChartGestureHandler handler) {
    if (gestureHandlers == null) {
      gestureHandlers = new ArrayList<ChartGestureHandler>();
      for (Event e : Event.values())
        listensFor.put(e, false);
    }

    gestureHandlers.add(handler);
    for (Event e : handler.getGesture().getEvent())
      listensFor.replace(e, true);
  }

  /**
   * Add a handler for specific gestures and ChartMouseGestureEvents
   * 
   * @param g
   * @param handler
   */
  public void removeGestureHandler(ChartGestureHandler handler) {
    if (gestureHandlers == null)
      return;

    if (gestureHandlers.remove(handler)) {
      // reset listenFor
      for (Event e : Event.values())
        listensFor.replace(e, false);

      // set all again
      for (ChartGestureHandler gh : gestureHandlers)
        for (Event e : gh.getGesture().getEvent())
          listensFor.replace(e, true);
    }
  }

  /**
   * Find chartentities like JFreeChartEntity, AxisEntity, PlotEntity, TitleEntity, XY...
   * 
   * @param chartPanel
   * @param x
   * @param y
   * @return
   */
  private ChartEntity findChartEntity(ChartPanel chartPanel, MouseEvent e) {
    // coordinates to find chart entities
    Insets insets = chartPanel.getInsets();
    int x = (int) ((e.getX() - insets.left) / chartPanel.getScaleX());
    int y = (int) ((e.getY() - insets.top) / chartPanel.getScaleY());

    if (lastEntity != null && x == lastEntityX && y == lastEntityY)
      return lastEntity;
    else {
      ChartRenderingInfo info = chartPanel.getChartRenderingInfo();
      ChartEntity entity = null;
      if (info != null) {
        EntityCollection entities = info.getEntityCollection();
        if (entities != null) {
          entity = entities.getEntity(x, y);
        }
      }
      return entity;
    }
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    if (gestureHandlers == null || gestureHandlers.isEmpty()
        || !(listensFor(Event.CLICK) || listensFor(Event.DOUBLE_CLICK)))
      return;

    if (e.getComponent() instanceof ChartPanel) {
      ChartPanel chartPanel = (ChartPanel) e.getComponent();
      ChartEntity entity = findChartEntity(chartPanel, e);
      ChartGesture.Entity gestureEntity = ChartGesture.getGestureEntity(entity);
      GestureButton button = GestureButton.getButton(e.getButton());

      if (!e.isConsumed()) {
        // double clicked
        if (e.getClickCount() == 2) {
          // reset click count to handle double clicks quickly
          e.consume();
          // handle event
          handleEvent(new ChartGestureEvent(chartPanel, e, entity,
              new ChartGesture(gestureEntity, Event.DOUBLE_CLICK, button)));
        } else if (e.getClickCount() == 1) {
          // handle event
          handleEvent(new ChartGestureEvent(chartPanel, e, entity,
              new ChartGesture(gestureEntity, Event.CLICK, button)));
        }
      }
    }
  }

  @Override
  public void mouseReleased(MouseEvent e) {
    if (gestureHandlers == null || gestureHandlers.isEmpty() || !listensFor(Event.RELEASED))
      return;

    if (e.getComponent() instanceof ChartPanel) {
      ChartPanel chartPanel = (ChartPanel) e.getComponent();
      ChartEntity entity = findChartEntity(chartPanel, e);
      ChartGesture.Entity gestureEntity = ChartGesture.getGestureEntity(entity);
      GestureButton button = GestureButton.getButton(e.getButton());

      // last gesture was dragged? keep the same chartEntity
      if (lastDragEvent != null) {
        entity = lastDragEvent.getEntity();
        gestureEntity = lastDragEvent.getGesture().getEntity();
      }
      // handle event
      handleEvent(new ChartGestureEvent(chartPanel, e, entity,
          new ChartGesture(gestureEntity, Event.RELEASED, button)));

      // reset drag
      lastDragEvent = null;
    }
  }

  @Override
  public void mousePressed(MouseEvent e) {
    if (gestureHandlers == null || gestureHandlers.isEmpty() || !listensFor(Event.PRESSED))
      return;

    if (e.getComponent() instanceof ChartPanel) {
      ChartPanel chartPanel = (ChartPanel) e.getComponent();
      ChartEntity entity = findChartEntity(chartPanel, e);
      ChartGesture.Entity gestureEntity = ChartGesture.getGestureEntity(entity);
      GestureButton button = GestureButton.getButton(e.getButton());
      // handle event
      lastDragEvent = new ChartGestureEvent(chartPanel, e, entity,
          new ChartGesture(gestureEntity, Event.PRESSED, button));
      handleEvent(lastDragEvent);
    }
  }

  @Override
  public void mouseDragged(MouseEvent e) {
    if (gestureHandlers == null || gestureHandlers.isEmpty() || !listensFor(Event.DRAGGED))
      return;

    if (e.getComponent() instanceof ChartPanel) {
      ChartPanel chartPanel = (ChartPanel) e.getComponent();
      // keep the same chartEntity
      ChartEntity entity = lastDragEvent.getEntity();
      ChartGesture.Entity gestureEntity = lastDragEvent.getGesture().getEntity();
      GestureButton button = lastDragEvent.getGesture().getButton();

      // handle event
      lastDragEvent = new ChartGestureEvent(chartPanel, e, entity,
          new ChartGesture(gestureEntity, Event.DRAGGED, button));
      handleEvent(lastDragEvent);
    }
  }

  @Override
  public void mouseMoved(MouseEvent e) {
    if (gestureHandlers == null || gestureHandlers.isEmpty() || !listensFor(Event.MOVED))
      return;

    if (e.getComponent() instanceof ChartPanel) {
      ChartPanel chartPanel = (ChartPanel) e.getComponent();
      ChartEntity entity = findChartEntity(chartPanel, e);
      ChartGesture.Entity gestureEntity = ChartGesture.getGestureEntity(entity);
      GestureButton button = GestureButton.getButton(e.getButton());

      // handle event
      handleEvent(new ChartGestureEvent(chartPanel, e, entity,
          new ChartGesture(gestureEntity, Event.MOVED, button)));
    }
  }

  @Override
  public void mouseWheelMoved(MouseWheelEvent e) {
    if (gestureHandlers == null || gestureHandlers.isEmpty() || !listensFor(Event.MOUSE_WHEEL))
      return;

    if (e.getComponent() instanceof ChartPanel) {
      ChartPanel chartPanel = (ChartPanel) e.getComponent();
      ChartEntity entity = findChartEntity(chartPanel, e);
      ChartGesture.Entity gestureEntity = ChartGesture.getGestureEntity(entity);
      GestureButton button = GestureButton.getButton(e.getButton());

      // handle event
      handleEvent(new ChartGestureEvent(chartPanel, e, entity,
          new ChartGesture(gestureEntity, Event.MOUSE_WHEEL, button)));
    }
  }

  @Override
  public void mouseEntered(MouseEvent e) {
    if (gestureHandlers == null || gestureHandlers.isEmpty() || !listensFor(Event.ENTERED))
      return;

    if (e.getComponent() instanceof ChartPanel) {
      ChartPanel chartPanel = (ChartPanel) e.getComponent();
      ChartEntity entity = findChartEntity(chartPanel, e);
      ChartGesture.Entity gestureEntity = ChartGesture.getGestureEntity(entity);
      GestureButton button = GestureButton.getButton(e.getButton());

      // handle event
      handleEvent(new ChartGestureEvent(chartPanel, e, entity,
          new ChartGesture(gestureEntity, Event.ENTERED, button)));
    }
  }

  @Override
  public void mouseExited(MouseEvent e) {
    if (gestureHandlers == null || gestureHandlers.isEmpty() || !listensFor(Event.EXITED))
      return;

    if (e.getComponent() instanceof ChartPanel) {
      ChartPanel chartPanel = (ChartPanel) e.getComponent();
      ChartEntity entity = findChartEntity(chartPanel, e);
      ChartGesture.Entity gestureEntity = ChartGesture.getGestureEntity(entity);
      GestureButton button = GestureButton.getButton(e.getButton());

      // handle event
      handleEvent(new ChartGestureEvent(chartPanel, e, entity,
          new ChartGesture(gestureEntity, Event.EXITED, button)));
    }
  }

  /**
   * Example how to create a new handler handles all events and prints them
   */
  public void addDebugHandler() {
    addGestureHandler(ChartGestureHandler.createHandler(Handler.DEBUG, // a
                                                                       // preset
                                                                       // handler
        ChartGesture.ALL, // no gesture filters
        null) // no parameters for this handler
    );
  }

  public void clearHandlers() {
    if (gestureHandlers != null)
      gestureHandlers.clear();
  }

  public List<ChartGestureHandler> getGestureHandlers() {
    return gestureHandlers;
  }
}
