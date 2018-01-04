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

import org.jfree.chart.ChartPanel;
import org.jfree.chart.axis.ValueAxis;
import net.sf.mzmine.chartbasics.gestures.ChartGesture.Entity;
import net.sf.mzmine.chartbasics.gestures.ChartGestureDragDiffHandler.Orientation;

/**
 * This event gets consumed by a {@link ChartGestureDragDiffHandler}
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class ChartGestureDragDiffEvent {

  private ChartGestureEvent firstEvent, lastEvent, latestEvent;
  private Orientation orient;
  // drag start and difference between last and latestEvent
  private double start, diff;

  public ChartGestureDragDiffEvent(ChartGestureEvent firstEvent, ChartGestureEvent lastEvent,
      ChartGestureEvent latestEvent, double start, double diff, Orientation orient) {
    super();
    this.firstEvent = firstEvent;
    this.latestEvent = latestEvent;
    this.lastEvent = lastEvent;
    this.start = start;
    this.diff = diff;
    this.orient = orient;
  }

  public void setLatestEvent(ChartGestureEvent latestEvent) {
    lastEvent = this.latestEvent;
    this.latestEvent = latestEvent;
  }

  /**
   * First event (usually Event.PRESSED)
   * 
   * @return
   */
  public ChartGestureEvent getFirstEvent() {
    return firstEvent;
  }

  /**
   * Event previous to latest event
   * 
   * @return
   */
  public ChartGestureEvent getLastEvent() {
    return lastEvent;
  }

  public ChartGestureEvent getLatestEvent() {
    return latestEvent;
  }

  /**
   * Start value of first event in data space
   * 
   * @return
   */
  public double getStart() {
    return start;
  }

  /**
   * Difference value of the last two eventsin data space
   * 
   * @return
   */
  public double getDiff() {
    return diff;
  }

  public Entity getEntity() {
    return firstEvent.getGesture().getEntity();
  }

  public ChartPanel getChartPanel() {
    return firstEvent.getChartPanel();
  }

  /**
   * The ValueAxis of this event's entity or null if the entity is different to an AxisEntity or if
   * the axis is not a ValueAxis
   * 
   * @return
   */
  public ValueAxis getAxis() {
    return firstEvent.getAxis();
  }

  public Orientation getOrient() {
    return orient;
  }
}
