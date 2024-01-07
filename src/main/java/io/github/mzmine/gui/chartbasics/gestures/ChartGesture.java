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

import io.github.mzmine.gui.chartbasics.gui.swing.ChartGestureMouseAdapter;
import io.github.mzmine.gui.chartbasics.gui.wrapper.MouseEventWrapper;
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

/**
 * {@link ChartGesture}s are part of {@link ChartGestureEvent} which are generated and processed by
 * the {@link ChartGestureMouseAdapter}. Processing can be performed in multiple {@link
 * ChartGestureHandler}s. <br> ChartGestures can be filtered by <br> - MouseEvents (also mouse
 * wheel)<br> - Mouse buttons <br> - Keyboard keys (Ctrl, Alt, Shift) <br> - ChartEntities (general
 * like AXIS or specific like DOMAIN_AXIS) <br>
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class ChartGesture {

  // ########################################################################
  // statics
  public static final ChartGesture ALL = new ChartGesture(Entity.ALL, Event.ALL, GestureButton.ALL);
  //
  private Entity entity;
  private Event[] event;
  private Key key;
  private GestureButton button = GestureButton.BUTTON1;

  /**
   * Target ChartEntity and mouse Event Entity.All and Event.ALL do not filter BUTTON.BUTTON1 is
   * used
   *
   * @param entity
   * @param event
   */
  public ChartGesture(Entity entity, Event event) {
    this(entity, event, GestureButton.BUTTON1);
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
    this(entity, event, GestureButton.BUTTON1, key);
  }

  /**
   * Target ChartEntity and mouse Event Entity.All and Event.ALL do not filter BUTTON.BUTTON1 is
   * used
   *
   * @param entity
   * @param event
   */
  public ChartGesture(Entity entity, Event[] event) {
    this(entity, event, GestureButton.BUTTON1);
  }

  /**
   * Target ChartEntity and mouse Event Entity.All and Event.ALL do not filter
   *
   * @param entity
   * @param event
   * @param button MouseEvent.BUTTON...
   */
  public ChartGesture(Entity entity, Event event, GestureButton button) {
    this(entity, new Event[]{event}, button);
  }

  /**
   * Target ChartEntity and mouse Event Entity.All and Event.ALL do not filter
   *
   * @param entity
   * @param event
   * @param button MouseEvent.BUTTON...
   */
  public ChartGesture(Entity entity, Event event, GestureButton button, Key key) {
    this(entity, new Event[]{event}, button, key);
  }

  /**
   * Target ChartEntity and mouse Event Entity.All and Event.ALL do not filter
   *
   * @param entity
   * @param event
   * @param button MouseEvent.BUTTON...
   */
  public ChartGesture(Entity entity, Event[] event, GestureButton button) {
    this(entity, event, button, null);
  }

  /**
   * Target ChartEntity and mouse Event Entity.All and Event.ALL do not filter
   *
   * @param entity
   * @param event
   * @param button MouseEvent.BUTTON...
   */
  public ChartGesture(Entity entity, Event[] event, GestureButton button, Key key) {
    this.entity = entity;
    this.event = event;
    this.button = button;
    this.key = key;
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
  public static GestureButton getButton(int mouseeventbutton) {
    return GestureButton.getButton(mouseeventbutton);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof ChartGesture)) {
      return false;
    } else {
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
    return this.getEntity().filter(g.getEntity()) && (this.getButton() == null || this.getButton()
        .filter(g.getButton())) &&
        // any element fits to any element?
        Stream.of(this.getEvent())
            .anyMatch(e1 -> Stream.of(g.getEvent()).anyMatch(e2 -> e1.filter(e2))) &&
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

  public void setEntity(Entity entity) {
    this.entity = entity;
  }

  public Event[] getEvent() {
    return event;
  }

  public void setEvent(Event[] event) {
    this.event = event;
  }

  public GestureButton getButton() {
    return button;
  }

  public void setButton(GestureButton button) {
    this.button = button;
  }

  @Override
  public String toString() {
    return (button == null ? "" : (button.toString() + " ")) + (key == null ? ""
        : key.toString() + " ") + event[0].toString() + " " + entity.toString();
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
      if (this.equals(ALL)) {
        return true;
      } else {
        return this.equals(e);
      }
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
    ALL,
    /**
     * Plot, XY items, XY annotations, category items
     */
    ALL_PLOT_AND_DATA, NONE, GENERAL, AXIS, DOMAIN_AXIS, RANGE_AXIS, PLOT, LEGEND_ITEM, XY_ITEM, XY_ANNOTATION, CATEGORY_ITEM, TITLE, TEXT_TITLE, NON_TEXT_TITLE, JFREECHART;

    /**
     * The gesture entity type
     *
     * @param entity
     * @return
     */
    public static Entity getGestureEntity(ChartEntity entity) {
      if (entity == null) {
        return NONE;
      }
      if (entity instanceof PlotEntity) {
        return PLOT;
      }
      if (entity instanceof AxisEntity) {
        AxisEntity e = (AxisEntity) entity;
        if (e.getAxis().getPlot() instanceof XYPlot) {
          XYPlot plot = ((XYPlot) e.getAxis().getPlot());
          for (int i = 0; i < plot.getDomainAxisCount(); i++) {
            if (plot.getDomainAxis(i).equals(e.getAxis())) {
              return DOMAIN_AXIS;
            }
          }
          for (int i = 0; i < plot.getRangeAxisCount(); i++) {
            if (plot.getRangeAxis(i).equals(e.getAxis())) {
              return RANGE_AXIS;
            }
          }
        }
        // else return basic axis
        return AXIS;
      }
      if (entity instanceof LegendItemEntity) {
        return LEGEND_ITEM;
      }
      if (entity instanceof XYItemEntity) {
        return XY_ITEM;
      }
      if (entity instanceof XYAnnotationEntity) {
        return XY_ANNOTATION;
      }
      if (entity instanceof TitleEntity) {
        if (((TitleEntity) entity).getTitle() instanceof TextTitle) {
          return TEXT_TITLE;
        } else {
          return NON_TEXT_TITLE;
        }
      }
      if (entity instanceof JFreeChartEntity) {
        return JFREECHART;
      }
      if (entity instanceof CategoryItemEntity) {
        return CATEGORY_ITEM;
      }
      return GENERAL;
    }

    /**
     * True if e fits into this entity
     *
     * @param e
     * @return
     */
    public boolean filter(Entity e) {
      if (this.equals(ALL)) {
        return true;
      }
      if (this.equals(AXIS)) {
        return e.toString().endsWith("AXIS");
      }
      if (this.equals(TITLE)) {
        return e.toString().endsWith("TITLE");
      }
      if (this.equals(ALL_PLOT_AND_DATA)) {
        return PLOT.equals(e) || XY_ANNOTATION.equals(e) || XY_ITEM.equals(e)
            || CATEGORY_ITEM.equals(e);
      } else {
        return this.equals(e);
      }
    }

    @Override
    public String toString() {
      return super.toString().replaceAll("_", " ");
    }
  }

  public enum GestureButton {
    ALL, BUTTON1, BUTTON2, BUTTON3;

    /**
     * returns the BUTTON for a MouseEvent.BUTTON
     *
     * @param mouseeventbutton
     * @return
     */
    public static GestureButton getButton(int mouseeventbutton) {
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

    /**
     * True if e fits into this button
     *
     * @param e
     * @return
     */
    public boolean filter(GestureButton e) {
      if (this.equals(ALL)) {
        return true;
      } else {
        return this.equals(e);
      }
    }
  }

  public enum Key {
    ALL, NONE, ALT, SHIFT, CTRL, CTRL_SHIFT, ALT_SHIFT, CTRL_ALT, CTRL_ALT_SHIFT;

    /**
     * Converts a list to a Key
     *
     * @param keys
     * @return
     */
    public static Key fromList(List<Key> keys) {
      if (keys == null || keys.size() == 0) {
        return NONE;
      }
      if (keys.contains(ALL)) {
        return ALL;
      }
      if (keys.contains(CTRL)) {
        if (keys.size() == 1) {
          return CTRL;
        } else if (keys.contains(SHIFT)) {
          if (keys.contains(ALT)) {
            return CTRL_ALT_SHIFT;
          } else {
            return CTRL_SHIFT;
          }
        } else if (keys.contains(ALT)) {
          return CTRL_ALT;
        }
      } else if (keys.contains(SHIFT)) {
        if (keys.size() == 1) {
          return SHIFT;
        } else if (keys.contains(ALT)) {
          return ALT_SHIFT;
        }
      } else if (keys.contains(ALT)) {
        if (keys.size() == 1) {
          return ALT;
        }
      }
      // else
      return NONE;
    }

    /**
     * True if e fits into this button
     *
     * @param e
     * @return
     */
    public boolean filter(Key e) {
      if (this.equals(ALL)) {
        return true;
      } else if (this.equals(NONE)) {
        return e == null || e.equals(NONE);
      } else {
        return this.equals(e);
      }
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

    @Override
    public String toString() {
      return super.toString().replace("_", " ");
    }
  }
}
