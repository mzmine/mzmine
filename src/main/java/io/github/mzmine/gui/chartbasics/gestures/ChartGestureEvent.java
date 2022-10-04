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

import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.AxisEntity;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.fx.ChartViewer;
import org.jfree.chart.plot.PlotOrientation;

import io.github.mzmine.gui.chartbasics.ChartLogics;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.Entity;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.Event;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.Key;
import io.github.mzmine.gui.chartbasics.gui.swing.ChartGestureMouseAdapter;
import io.github.mzmine.gui.chartbasics.gui.wrapper.ChartViewWrapper;
import io.github.mzmine.gui.chartbasics.gui.wrapper.MouseEventWrapper;
import javafx.scene.input.ScrollEvent;

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
public class ChartGestureEvent { // ChartPanel or ChartCanvas
  // ChartPanel or ChartCanvas
  private ChartViewWrapper cp;

  //
  private MouseEventWrapper mouseEvent;
  private ChartGesture gesture;
  private ChartEntity entity;

  public ChartGestureEvent(ChartPanel cp, MouseEvent mEvent, ChartEntity entity,
      ChartGesture gesture) {
    this(new ChartViewWrapper(cp), new MouseEventWrapper(mEvent), entity, gesture);
  }

  public ChartGestureEvent(ChartViewer cp, javafx.scene.input.MouseEvent mEvent, ChartEntity entity,
      ChartGesture gesture) {
    this(new ChartViewWrapper(cp), new MouseEventWrapper(mEvent), entity, gesture);
  }

  public ChartGestureEvent(ChartViewer cp, ScrollEvent mEvent, ChartEntity entity,
      ChartGesture gesture) {
    this(new ChartViewWrapper(cp), new MouseEventWrapper(mEvent), entity, gesture);
  }

  public ChartGestureEvent(ChartViewWrapper cp, MouseEventWrapper mEvent, ChartEntity entity,
      ChartGesture gesture) {
    super();
    this.cp = cp;
    this.mouseEvent = mEvent;
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

  /**
   * ChartPanel or ChartCanvas
   * 
   * @return
   */
  public ChartViewWrapper getChartWrapper() {
    return cp;
  }

  public JFreeChart getChart() {
    return cp.getChart();
  }

  public boolean isChartPanel() {
    return cp.isSwing();
  }

  public boolean isChartCanvas() {
    return cp.isSwing();
  }

  public MouseEventWrapper getMouseEvent() {
    return mouseEvent;
  }

  public void setGesture(ChartGesture gesture) {
    this.gesture = gesture;
  }

  public ChartEntity getEntity() {
    return entity;
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
   * @param e
   * @return
   */
  public Point2D getCoordinates(double x, double y) {
    return cp.mouseXYToPlotXY(x, y);
  }

  /**
   * Transforms mouse coordinates to data space coordinates. Same as
   * {@link ChartLogics#mouseXYToPlotXY(ChartPanel, int, int)}
   * 
   * @return
   */
  public Point2D getCoordinates(int x, int y) {
    return cp.mouseXYToPlotXY(x, y);
  }

  /**
   * Transforms mouse coordinates to data space coordinates. Same as
   * {@link ChartLogics#mouseXYToPlotXY(ChartPanel, int, int)}
   * 
   * @return
   */
  public Point2D getCoordinates() {
    return cp.mouseXYToPlotXY(mouseEvent.getX(), mouseEvent.getY());
  }

  /**
   * Mouse event point
   * 
   * @return
   */
  public Point2D getPoint() {
    return mouseEvent.getPoint();
  }

  /**
   * Returns the index of the subplot that contains the specified (x, y) point (the "source" point).
   * The source point will usually come from a mouse click on a {@link org.jfree.chart.ChartPanel},
   * and this method is then used to determine the subplot that contains the source point.
   *
   * @param source the source point (in Java2D space, {@code null} not permitted).
   *
   * @return The subplot index (or -1 if no subplot contains {@code source}).
   */
  public int getSubplotIndex() {
    return cp.getRenderingInfo().getPlotInfo().getSubplotIndex(getPoint());
  }

  /**
   * True if axis is vertical, false if horizontal, null if there was an error
   * 
   * @param axis
   * @return
   */
  public Boolean isVerticalAxis(ValueAxis axis) {
    if (axis == null)
      return null;
    JFreeChart chart = getChart();
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
