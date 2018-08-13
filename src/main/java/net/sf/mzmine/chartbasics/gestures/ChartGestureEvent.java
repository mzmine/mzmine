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
import java.awt.geom.Point2D;
import java.util.ArrayList;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.AxisEntity;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.plot.PlotOrientation;
import net.sf.mzmine.chartbasics.ChartLogics;
import net.sf.mzmine.chartbasics.gestures.ChartGesture.Entity;
import net.sf.mzmine.chartbasics.gestures.ChartGesture.Event;
import net.sf.mzmine.chartbasics.gestures.ChartGesture.Key;

/**
 * {@link ChartGesture}s are part of {@link ChartGestureEvent} which are generated and processed by
 * the {@link ChartGestureMouseAdapter}. Processing can be performed in multiple
 * {@link ChartGestureHandler}s. <br>
 * ChartGestures can be filtered by <br>
 * - MouseEvents<br>
 * - Mouse buttons <br>
 * - Keyboard keys (Ctrl, Alt, Shift) <br>
 * - ChartEntities (general like AXIS or specific like DOMAIN_AXIS) <br>
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class ChartGestureEvent {

  private ChartPanel cp;
  private MouseEvent mouseEvent;
  private ChartGesture gesture;
  private ChartEntity entity;

  public ChartGestureEvent(ChartPanel cp, MouseEvent mouseEvent, ChartEntity entity,
      ChartGesture gesture) {
    super();
    this.cp = cp;
    this.mouseEvent = mouseEvent;
    this.gesture = gesture;
    this.entity = entity;

    if (mouseEvent != null) {
      // extract keys and set to gesture
      ArrayList<Key> keys = new ArrayList<Key>();
      if (mouseEvent.isAltDown())
        keys.add(Key.ALT);
      if (mouseEvent.isControlDown())
        keys.add(Key.CTRL);
      if (mouseEvent.isShiftDown())
        keys.add(Key.SHIFT);
      gesture.setKey(Key.fromList(keys));
    }
  }

  public ChartGesture getGesture() {
    return gesture;
  }

  public ChartPanel getChartPanel() {
    return cp;
  }

  public MouseEvent getMouseEvent() {
    return mouseEvent;
  }

  public void setChartPanel(ChartPanel cp) {
    this.cp = cp;
  }

  public void setMouseEvent(MouseEvent mouseEvent) {
    this.mouseEvent = mouseEvent;
  }

  public void setGesture(ChartGesture gesture) {
    this.gesture = gesture;
  }

  public ChartEntity getEntity() {
    return entity;
  }

  public void setEntity(ChartEntity entity) {
    this.entity = entity;
  }

  @Override
  public String toString() {
    return gesture.toString() + " " + super.toString();
  }

  /**
   * True if first gesture event equals e
   * 
   * @param e
   * @return
   */
  public boolean checkEvent(Event e) {
    return getGesture().getEvent()[0].equals(e);
  }

  /**
   * The ValueAxis of this event's entity or null if the entity is different to an AxisEntity or if
   * the axis is not a ValueAxis
   * 
   * @return
   */
  public ValueAxis getAxis() {
    if (entity != null && entity instanceof AxisEntity
        && ((AxisEntity) entity).getAxis() instanceof ValueAxis)
      return (ValueAxis) ((AxisEntity) entity).getAxis();
    else
      return null;
  }

  /**
   * Transforms mouse coordinates to data space coordinates. Same as
   * {@link ChartLogics#mouseXYToPlotXY(ChartPanel, int, int)}
   * 
   * @param chartPanel
   * @param e
   * @return
   */
  public Point2D getCoordinates(ChartPanel chartPanel, int x, int y) {
    return ChartLogics.mouseXYToPlotXY(chartPanel, x, y);
  }

  /**
   * True if axis is vertical, false if horizontal, null if there was an error
   * 
   * @param axis
   * @return
   */
  public Boolean isVerticalAxis(ChartPanel cp, ValueAxis axis) {
    if (axis == null)
      return null;
    JFreeChart chart = cp.getChart();
    PlotOrientation orient = PlotOrientation.HORIZONTAL;
    if (chart.getXYPlot() != null)
      orient = chart.getXYPlot().getOrientation();
    else if (chart.getCategoryPlot() != null)
      orient = chart.getCategoryPlot().getOrientation();
    // error
    if (orient == null)
      return null;

    Entity entity = this.getGesture().getEntity();
    double start = 0;
    // horizontal
    if ((entity.equals(Entity.DOMAIN_AXIS) && orient.equals(PlotOrientation.VERTICAL))
        || (entity.equals(Entity.RANGE_AXIS) && orient.equals(PlotOrientation.HORIZONTAL))) {
      return false;
    }
    // vertical
    else if ((entity.equals(Entity.RANGE_AXIS) && orient.equals(PlotOrientation.VERTICAL))
        || (entity.equals(Entity.DOMAIN_AXIS) && orient.equals(PlotOrientation.HORIZONTAL))) {
      return true;
    }
    // error
    return null;
  }
}
