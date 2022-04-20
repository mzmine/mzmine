/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.gui.chartbasics.gui.javafx;

import io.github.mzmine.gui.chartbasics.gestures.ChartGesture;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.Entity;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.Event;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.GestureButton;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.Key;
import io.github.mzmine.gui.chartbasics.gestures.ChartGestureDragDiffHandler.Orientation;
import io.github.mzmine.gui.chartbasics.gestures.ChartGestureEvent;
import io.github.mzmine.gui.chartbasics.gestures.ChartGestureHandler;
import io.github.mzmine.gui.chartbasics.gestures.ChartGestureHandler.DragHandler;
import io.github.mzmine.gui.chartbasics.gestures.ChartGestureHandler.Handler;
import io.github.mzmine.gui.chartbasics.gui.wrapper.ChartViewWrapper;
import io.github.mzmine.gui.chartbasics.gui.wrapper.GestureMouseAdapter;
import io.github.mzmine.gui.chartbasics.gui.wrapper.MouseEventWrapper;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.logging.Logger;
import javafx.geometry.Insets;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.fx.ChartCanvas;
import org.jfree.chart.fx.ChartViewer;
import org.jfree.chart.fx.interaction.MouseHandlerFX;

/**
 * Handles drag zooming of charts on a {@link ChartCanvas}. This handler should be configured with
 * the required modifier keys and installed as a live handler (not an auxiliary handler). This
 * handler only works for a <b>ChartCanvas</b> that is embedded in a {@link ChartViewer}, since it
 * relies on the <b>ChartViewer</b> for drawing the zoom rectangle.
 */
public class ChartGestureMouseAdapterFX implements GestureMouseAdapter, MouseHandlerFX {

  public static final Logger logger = Logger.getLogger(ChartGestureMouseAdapterFX.class.getName());

  private ChartViewer chartViewer;
  private ChartViewWrapper cw;
  private int lastEntityX = -1, lastEntityY = -1;
  private ChartEntity lastEntity = null;
  private ChartGestureEvent lastDragEvent = null;

  // handle gestures
  private List<ChartGestureHandler> gestureHandlers;
  // listen for
  private EnumMap<Event, Boolean> listensFor = new EnumMap<Event, Boolean>(Event.class);
  //
  private String id;
  private boolean isEnabled = true;

  /**
   * Creates a new instance with no modifier keys required.
   *
   * @param id the handler ID ({@code null} not permitted).
   * @param chartViewer the chart viewer.
   */
  public ChartGestureMouseAdapterFX(String id, ChartViewer chartViewer) {
    this.id = id;
    this.chartViewer = chartViewer;
    cw = new ChartViewWrapper(chartViewer);
  }

  @Override
  public boolean hasMatchingModifiers(MouseEvent e) {
    return true;
  }

  @Override
  public String getID() {
    return id;
  }

  @Override
  public boolean isEnabled() {
    return isEnabled;
  }

  public void setEnabled(boolean isEnabled) {
    this.isEnabled = isEnabled;
  }

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
          .forEach(handler -> {
            handler.accept(e);
          });
  }

  /**
   * Add drag handlers for each key (key and handler have to be ordered)
   *
   * @param handler
   */
  @Override
  public void addDragGestureHandler(DragHandler[] handler, Key[] key, Entity entity,
      GestureButton button, Orientation orient, Object[] param) {
    ChartGestureHandler h =
        ChartGestureHandler.createDragDiffHandler(handler, key, entity, button, orient, param);
    addGestureHandler(h);
  }

  /**
   * Add a preset handler for specific gestures and ChartMouseGestureEvents
   *
   * @param handler
   */
  @Override
  public void addGestureHandler(Handler handler, Entity entity, Event[] event, GestureButton button,
      Key key, Object[] param) {
    ChartGestureHandler h = ChartGestureHandler.createHandler(handler,
        new ChartGesture(entity, event, button, key), param);
    addGestureHandler(h);
  }

  /**
   * Add a handler for specific gestures and ChartMouseGestureEvents
   *
   * @param handler
   */
  @Override
  public void addGestureHandler(ChartGestureHandler handler) {
    if (gestureHandlers == null) {
      gestureHandlers = new ArrayList<>();
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
   * @param handler
   */
  @Override
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
   * @return
   */
  private ChartEntity findChartEntity(MouseEventWrapper e) {
    // coordinates to find chart entities
    Insets insets = chartViewer.getInsets();
    int x = (int) ((e.getX() - insets.getLeft()) / chartViewer.getCanvas().getScaleX());
    int y = (int) ((e.getY() - insets.getTop()) / chartViewer.getCanvas().getScaleY());

    if (lastEntity != null && x == lastEntityX && y == lastEntityY) {
      return lastEntity;
    } else {
      ChartRenderingInfo info = chartViewer.getCanvas().getRenderingInfo();
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

  /**
   * Handles a mouse moved event. This implementation does nothing, override the method if required.
   *
   * @param canvas the canvas ({@code null} not permitted).
   * @param eOrig the event ({@code null} not permitted).
   */
  @Override
  public void handleMouseMoved(ChartCanvas canvas, MouseEvent eOrig) {
    if (gestureHandlers == null || gestureHandlers.isEmpty() || !listensFor(Event.MOVED))
      return;

    MouseEventWrapper e = new MouseEventWrapper(eOrig);
    ChartViewWrapper cw = new ChartViewWrapper(chartViewer);
    ChartEntity entity = findChartEntity(e);
    ChartGesture.Entity gestureEntity = ChartGesture.getGestureEntity(entity);
    GestureButton button = GestureButton.getButton(e.getButton());

    // handle event
    handleEvent(
        new ChartGestureEvent(cw, e, entity, new ChartGesture(gestureEntity, Event.MOVED, button)));
  }

  /**
   * Handles a mouse clicked event. This implementation does nothing, override the method if
   * required.
   *
   * @param canvas the canvas ({@code null} not permitted).
   * @param eOrig the event ({@code null} not permitted).
   */
  @Override
  public void handleMouseClicked(ChartCanvas canvas, MouseEvent eOrig) {
    // if mouse was moved during click - do not count click
    if (!eOrig.isStillSincePress() || gestureHandlers == null || gestureHandlers.isEmpty() || !(
        listensFor(Event.CLICK) || listensFor(Event.DOUBLE_CLICK))) {
      return;
    }

    MouseEventWrapper e = new MouseEventWrapper(eOrig);
    ChartEntity entity = findChartEntity(e);
    ChartGesture.Entity gestureEntity = ChartGesture.getGestureEntity(entity);
    GestureButton button = GestureButton.getButton(e.getButton());

    if (!e.isConsumed()) {
      // double clicked
      if (e.getClickCount() == 2) {
        // reset click count to handle double clicks quickly
        e.consume();
        // handle event
        handleEvent(new ChartGestureEvent(cw, e, entity,
            new ChartGesture(gestureEntity, Event.DOUBLE_CLICK, button)));
      } else if (e.getClickCount() == 1) {
        // handle event
        handleEvent(new ChartGestureEvent(cw, e, entity,
            new ChartGesture(gestureEntity, Event.CLICK, button)));
      }
    }
  }

  /**
   *
   * @param canvas the canvas ({@code null} not permitted).
   * @param eOrig the event ({@code null} not permitted).
   */
  @Override
  public void handleMouseReleased(ChartCanvas canvas, MouseEvent eOrig) {
    if (gestureHandlers == null || gestureHandlers.isEmpty() || !listensFor(Event.RELEASED))
      return;

    MouseEventWrapper e = new MouseEventWrapper(eOrig);
    ChartEntity entity = findChartEntity(e);
    ChartGesture.Entity gestureEntity = ChartGesture.getGestureEntity(entity);
    GestureButton button = GestureButton.getButton(e.getButton());

    // last gesture was dragged? keep the same chartEntity
    if (lastDragEvent != null) {
      entity = lastDragEvent.getEntity();
      gestureEntity = lastDragEvent.getGesture().getEntity();
    }
    // handle event
    handleEvent(new ChartGestureEvent(cw, e, entity,
        new ChartGesture(gestureEntity, Event.RELEASED, button)));

    // reset drag
    lastDragEvent = null;
  }

  /**
   * Handles a mouse pressed event. This implementation does nothing, override the method if
   * required.
   *
   * @param canvas the canvas ({@code null} not permitted).
   * @param eOrig the event ({@code null} not permitted).
   */
  @Override
  public void handleMousePressed(ChartCanvas canvas, MouseEvent eOrig) {
    if (gestureHandlers == null || gestureHandlers.isEmpty() || !listensFor(Event.PRESSED))
      return;

    MouseEventWrapper e = new MouseEventWrapper(eOrig);
    ChartEntity entity = findChartEntity(e);
    ChartGesture.Entity gestureEntity = ChartGesture.getGestureEntity(entity);
    GestureButton button = GestureButton.getButton(e.getButton());
    // handle event
    lastDragEvent = new ChartGestureEvent(cw, e, entity,
        new ChartGesture(gestureEntity, Event.PRESSED, button));
    handleEvent(lastDragEvent);
  }

  /**
   * Handles a mouse dragged event. This implementation does nothing, override the method if
   * required.
   *
   * @param chartPanel the canvas ({@code null} not permitted).
   * @param eOrig the event ({@code null} not permitted).
   */
  @Override
  public void handleMouseDragged(ChartCanvas chartPanel, MouseEvent eOrig) {
    if (gestureHandlers == null || gestureHandlers.isEmpty() || !listensFor(Event.DRAGGED))
      return;

    if (lastDragEvent == null)
      return;

    MouseEventWrapper e = new MouseEventWrapper(eOrig);
    // keep the same chartEntity
    ChartEntity entity = lastDragEvent.getEntity();
    ChartGesture.Entity gestureEntity = lastDragEvent.getGesture().getEntity();
    GestureButton button = lastDragEvent.getGesture().getButton();

    // handle event
    lastDragEvent = new ChartGestureEvent(cw, e, entity,
        new ChartGesture(gestureEntity, Event.DRAGGED, button));
    handleEvent(lastDragEvent);
  }

  /**
   * Handles a scroll event. This implementation does nothing, override the method if required.
   *
   * @param chartPanel the canvas ({@code null} not permitted).
   * @param eOrig the event ({@code null} not permitted).
   */
  @Override
  public void handleScroll(ChartCanvas chartPanel, ScrollEvent eOrig) {
    if (gestureHandlers == null || gestureHandlers.isEmpty() || !listensFor(Event.MOUSE_WHEEL))
      return;

    MouseEventWrapper e = new MouseEventWrapper(eOrig);
    ChartEntity entity = findChartEntity(e);
    ChartGesture.Entity gestureEntity = ChartGesture.getGestureEntity(entity);
    GestureButton button = GestureButton.getButton(e.getButton());

    // handle event
    handleEvent(new ChartGestureEvent(cw, e, entity,
        new ChartGesture(gestureEntity, Event.MOUSE_WHEEL, button)));
  }
}
