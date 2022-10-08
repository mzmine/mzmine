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

import org.jfree.chart.axis.ValueAxis;

import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.Entity;
import io.github.mzmine.gui.chartbasics.gestures.ChartGestureDragDiffHandler.Orientation;
import io.github.mzmine.gui.chartbasics.gui.wrapper.ChartViewWrapper;

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
   * Difference value of the last two events in data space
   * 
   * @return
   */
  public double getDiff() {
    return diff;
  }

  public Entity getEntity() {
    return firstEvent.getGesture().getEntity();
  }

  public ChartViewWrapper getChartWrapper() {
    return firstEvent.getChartWrapper();
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
