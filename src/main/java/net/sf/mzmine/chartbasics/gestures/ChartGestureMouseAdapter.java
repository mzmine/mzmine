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

import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.EntityCollection;
import net.sf.mzmine.chartbasics.gestures.ChartGesture.Button;
import net.sf.mzmine.chartbasics.gestures.ChartGesture.Entity;
import net.sf.mzmine.chartbasics.gestures.ChartGesture.Event;
import net.sf.mzmine.chartbasics.gestures.ChartGesture.Key;
import net.sf.mzmine.chartbasics.gestures.ChartGestureDragDiffHandler.Orientation;
import net.sf.mzmine.chartbasics.gestures.ChartGestureHandler.DragHandler;
import net.sf.mzmine.chartbasics.gestures.ChartGestureHandler.Handler;

/**
 * Handles all MouseEvents (like a MouseAdapter) and transforms them into {@link ChartGestureEvent}s
 * which are then handled by a list of {@link ChartGestureHandler}s.
 * <p>
 * Add this adapter to a ChartPanel as a mouse and mousewheel listener.
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class ChartGestureMouseAdapter extends MouseAdapter {
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
  public void addDragGestureHandler(DragHandler[] handler, Key[] key, Entity entity, Button button,
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
  public void addGestureHandler(Handler handler, Entity entity, Event[] event, Button button,
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
      Button button = Button.getButton(e.getButton());

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
      Button button = Button.getButton(e.getButton());

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
      Button button = Button.getButton(e.getButton());
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
      Button button = lastDragEvent.getGesture().getButton();

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
      Button button = Button.getButton(e.getButton());

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
      Button button = Button.getButton(e.getButton());

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
      Button button = Button.getButton(e.getButton());

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
      Button button = Button.getButton(e.getButton());

      // handle event
      handleEvent(new ChartGestureEvent(chartPanel, e, entity,
          new ChartGesture(gestureEntity, Event.EXITED, button)));
    }
  }


  /**
   * Example how to create a new handler handles all events and prints them
   */
  public void addDebugHandler() {
    addGestureHandler(ChartGestureHandler.createHandler(Handler.DEBUG, // a preset handler
        ChartGesture.ALL, // no gesture filters
        null) // no parameters for this handler
    );
  }

  public void clearHandlers() {
    if (gestureHandlers != null)
      gestureHandlers.clear();
  }

}
