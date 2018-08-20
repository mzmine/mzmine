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

import java.awt.event.MouseEvent;
import java.util.List;
import java.util.stream.Stream;
import org.jfree.chart.entity.AxisEntity;
import org.jfree.chart.entity.CategoryItemEntity;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.JFreeChartEntity;
import org.jfree.chart.entity.LegendItemEntity;
import org.jfree.chart.entity.PlotEntity;
import org.jfree.chart.entity.TitleEntity;
import org.jfree.chart.entity.XYAnnotationEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.TextTitle;
import net.sf.mzmine.chartbasics.javafx.mouse.MouseEventWrapper;

/**
 * {@link ChartGesture}s are part of {@link ChartGestureEvent} which are generated and processed by
 * the {@link ChartGestureMouseAdapter}. Processing can be performed in multiple
 * {@link ChartGestureHandler}s. <br>
 * ChartGestures can be filtered by <br>
 * - MouseEvents (also mouse wheel)<br>
 * - Mouse buttons <br>
 * - Keyboard keys (Ctrl, Alt, Shift) <br>
 * - ChartEntities (general like AXIS or specific like DOMAIN_AXIS) <br>
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class ChartGesture {
  // ########################################################################
  // statics
  public static final ChartGesture ALL = new ChartGesture(Entity.ALL, Event.ALL, Button.ALL);
  //
  private Entity entity;
  private Event[] event;
  private Key key;
  private Button button = Button.BUTTON1;

  /**
   * Target ChartEntity and mouse Event Entity.All and Event.ALL do not filter BUTTON.BUTTON1 is
   * used
   * 
   * @param entity
   * @param event
   */
  public ChartGesture(Entity entity, Event event) {
    this(entity, event, Button.BUTTON1);
  }

  /**
   * Target ChartEntity and mouse Event Entity.All and Event.ALL do not filter BUTTON.BUTTON1 is
   * used
   * 
   * @param entity
   * @param event
   * @param key
   */
  public ChartGesture(Entity entity, Event event, Key key) {
    this(entity, event, Button.BUTTON1, key);
  }

  /**
   * Target ChartEntity and mouse Event Entity.All and Event.ALL do not filter BUTTON.BUTTON1 is
   * used
   * 
   * @param entity
   * @param event
   */
  public ChartGesture(Entity entity, Event[] event) {
    this(entity, event, Button.BUTTON1);
  }

  /**
   * Target ChartEntity and mouse Event Entity.All and Event.ALL do not filter
   * 
   * @param entity
   * @param event
   * @param button MouseEvent.BUTTON...
   */
  public ChartGesture(Entity entity, Event event, Button button) {
    this(entity, new Event[] {event}, button);
  }

  /**
   * Target ChartEntity and mouse Event Entity.All and Event.ALL do not filter
   * 
   * @param entity
   * @param event
   * @param button MouseEvent.BUTTON...
   */
  public ChartGesture(Entity entity, Event event, Button button, Key key) {
    this(entity, new Event[] {event}, button, key);
  }

  /**
   * Target ChartEntity and mouse Event Entity.All and Event.ALL do not filter
   * 
   * @param entity
   * @param event
   * @param button MouseEvent.BUTTON...
   */
  public ChartGesture(Entity entity, Event[] event, Button button) {
    this(entity, event, button, null);
  }

  /**
   * Target ChartEntity and mouse Event Entity.All and Event.ALL do not filter
   * 
   * @param entity
   * @param event
   * @param button MouseEvent.BUTTON...
   */
  public ChartGesture(Entity entity, Event[] event, Button button, Key key) {
    this.entity = entity;
    this.event = event;
    this.button = button;
    this.key = key;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof ChartGesture))
      return false;
    else {
      ChartGesture g = (ChartGesture) obj;
      return this.getEntity().equals(g.getEntity()) && this.getEvent().equals(g.getEvent())
          && this.getButton().equals(g.getButton()) && this.getKey().equals(g.getKey());
    }
  }

  /**
   * True if the argument fits to this gesture. The entity and event have to fit
   * 
   * @param g
   */
  public boolean filter(ChartGesture g) {
    return this.getEntity().filter(g.getEntity())
        && (this.getButton() == null || this.getButton().filter(g.getButton())) &&
        // any element fits to any element?
        Stream.of(this.getEvent())
            .anyMatch(e1 -> Stream.of(g.getEvent()).anyMatch(e2 -> e1.filter(e2)))
        &&
        // key match?
        (this.getKey() == null || this.getKey().filter(g.getKey()));
  }


  public Key getKey() {
    return key;
  }

  public void setKey(Key key) {
    this.key = key;
  }

  public Entity getEntity() {
    return entity;
  }

  public Event[] getEvent() {
    return event;
  }

  public void setEntity(Entity entity) {
    this.entity = entity;
  }

  public void setEvent(Event[] event) {
    this.event = event;
  }

  /**
   * The gesture entity type
   * 
   * @param entity
   * @return
   */
  public static Entity getGestureEntity(ChartEntity entity) {
    return Entity.getGestureEntity(entity);
  }

  /**
   * returns the BUTTON for a MouseEvent.BUTTON
   * 
   * @param mouseeventbutton
   * @return
   */
  public static Button getButton(int mouseeventbutton) {
    return Button.getButton(mouseeventbutton);
  }

  public Button getButton() {
    return button;
  }

  public void setButton(Button button) {
    this.button = button;
  }

  @Override
  public String toString() {
    return (button == null ? "" : (button.toString() + " "))
        + (key == null ? "" : key.toString() + " ") + event[0].toString() + " " + entity.toString();
  }

  // ########################################################################
  // Filter section
  // Event, Entity, Button, Key
  /**
   * The mouse event (filter)
   */
  public enum Event {
    ALL, CLICK, DOUBLE_CLICK, PRESSED, RELEASED, MOVED, DRAGGED, ENTERED, EXITED, MOUSE_WHEEL;

    /**
     * True if e fits into this event
     * 
     * @param e
     * @return
     */
    public boolean filter(Event e) {
      if (this.equals(ALL))
        return true;
      else
        return this.equals(e);
    }

    @Override
    public String toString() {
      return super.toString().replaceAll("_", " ");
    }
  }
  /**
   * The chart entity (filter)
   */
  public enum Entity {
    ALL, NONE, GENERAL, AXIS, DOMAIN_AXIS, RANGE_AXIS, PLOT, LEGEND_ITEM, XY_ITEM, XY_ANNOTATION, CATEGORY_ITEM, TITLE, TEXT_TITLE, NON_TEXT_TITLE, JFREECHART;

    /**
     * True if e fits into this entity
     * 
     * @param e
     * @return
     */
    public boolean filter(Entity e) {
      if (this.equals(ALL))
        return true;
      if (this.equals(AXIS))
        return e.toString().endsWith("AXIS");
      if (this.equals(TITLE))
        return e.toString().endsWith("TITLE");
      else
        return this.equals(e);
    }

    /**
     * The gesture entity type
     * 
     * @param entity
     * @return
     */
    public static Entity getGestureEntity(ChartEntity entity) {
      if (entity == null)
        return NONE;
      if (entity instanceof PlotEntity)
        return PLOT;
      if (entity instanceof AxisEntity) {
        AxisEntity e = (AxisEntity) entity;
        if (e.getAxis().getPlot() instanceof XYPlot) {
          XYPlot plot = ((XYPlot) e.getAxis().getPlot());
          for (int i = 0; i < plot.getDomainAxisCount(); i++)
            if (plot.getDomainAxis(i).equals(e.getAxis()))
              return DOMAIN_AXIS;
          for (int i = 0; i < plot.getRangeAxisCount(); i++)
            if (plot.getRangeAxis(i).equals(e.getAxis()))
              return RANGE_AXIS;
        }
        // else return basic axis
        return AXIS;
      }
      if (entity instanceof LegendItemEntity)
        return LEGEND_ITEM;
      if (entity instanceof XYItemEntity)
        return XY_ITEM;
      if (entity instanceof XYAnnotationEntity)
        return XY_ANNOTATION;
      if (entity instanceof TitleEntity) {
        if (((TitleEntity) entity).getTitle() instanceof TextTitle)
          return TEXT_TITLE;
        else
          return NON_TEXT_TITLE;
      }
      if (entity instanceof JFreeChartEntity)
        return JFREECHART;
      if (entity instanceof CategoryItemEntity)
        return CATEGORY_ITEM;
      return GENERAL;
    }

    @Override
    public String toString() {
      return super.toString().replaceAll("_", " ");
    }
  }

  public enum Button {
    ALL, BUTTON1, BUTTON2, BUTTON3;

    /**
     * True if e fits into this button
     * 
     * @param e
     * @return
     */
    public boolean filter(Button e) {
      if (this.equals(ALL))
        return true;
      else
        return this.equals(e);
    }

    /**
     * returns the BUTTON for a MouseEvent.BUTTON
     * 
     * @param mouseeventbutton
     * @return
     */
    public static Button getButton(int mouseeventbutton) {
      switch (mouseeventbutton) {
        case MouseEvent.BUTTON1:
          return BUTTON1;
        case MouseEvent.BUTTON2:
          return BUTTON2;
        case MouseEvent.BUTTON3:
          return BUTTON3;
      }
      return null;
    }
  }

  public enum Key {
    ALL, NONE, ALT, SHIFT, CTRL, CTRL_SHIFT, ALT_SHIFT, CTRL_ALT, CTRL_ALT_SHIFT;

    /**
     * True if e fits into this button
     * 
     * @param e
     * @return
     */
    public boolean filter(Key e) {
      if (this.equals(ALL))
        return true;
      else if (this.equals(NONE))
        return e == null || e.equals(NONE);
      else
        return this.equals(e);
    }

    /**
     * True if e fits into this button
     * 
     * @param e
     * @return
     */
    public boolean filter(MouseEventWrapper e) {
      switch (this) {
        case ALL:
          return true;
        case NONE:
          return !(e.isAltDown() || e.isControlDown() || e.isShiftDown());
        case ALT:
          return e.isAltDown() && !e.isControlDown() && !e.isShiftDown();
        case CTRL:
          return !e.isAltDown() && e.isControlDown() && !e.isShiftDown();
        case SHIFT:
          return !e.isAltDown() && !e.isControlDown() && e.isShiftDown();
        case CTRL_SHIFT:
          return !e.isAltDown() && e.isControlDown() && e.isShiftDown();
        case CTRL_ALT:
          return e.isAltDown() && e.isControlDown() && !e.isShiftDown();
        case CTRL_ALT_SHIFT:
          return e.isAltDown() && e.isControlDown() && e.isShiftDown();
        case ALT_SHIFT:
          return e.isAltDown() && !e.isControlDown() && e.isShiftDown();
      }
      return false;
    }

    /**
     * Converts a list to a Key
     * 
     * @param keys
     * @return
     */
    public static Key fromList(List<Key> keys) {
      if (keys == null || keys.size() == 0)
        return NONE;
      if (keys.contains(ALL))
        return ALL;
      if (keys.contains(CTRL)) {
        if (keys.size() == 1)
          return CTRL;
        else if (keys.contains(SHIFT)) {
          if (keys.contains(ALT))
            return CTRL_ALT_SHIFT;
          else
            return CTRL_SHIFT;
        } else if (keys.contains(ALT)) {
          return CTRL_ALT;
        }
      } else if (keys.contains(SHIFT)) {
        if (keys.size() == 1)
          return SHIFT;
        else if (keys.contains(ALT))
          return ALT_SHIFT;
      } else if (keys.contains(ALT)) {
        if (keys.size() == 1)
          return ALT;
      }
      // else
      return NONE;
    }

    @Override
    public String toString() {
      return super.toString().replace("_", " ");
    }
  }
}
