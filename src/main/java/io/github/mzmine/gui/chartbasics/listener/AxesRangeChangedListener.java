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

package io.github.mzmine.gui.chartbasics.listener;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.event.AxisChangeEvent;
import org.jfree.chart.event.AxisChangeListener;
import org.jfree.data.Range;

import io.github.mzmine.gui.chartbasics.gui.wrapper.ChartViewWrapper;

/**
 * Listener for multiple axes
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public abstract class AxesRangeChangedListener implements AxisChangeListener {

  // last lower / upper range
  private ValueAxis[] axis;
  private Range[] lastRange = null;
  private ChartViewWrapper chart;

  /**
   * Creates no listeners. Needs to by notified by external AxisRangeChangedListeners
   */
  public AxesRangeChangedListener(int axiscount) {
    axis = new ValueAxis[axiscount];
    lastRange = new Range[axiscount];
  }

  /**
   * Creates two axisrangechangedlistener for the Domain and Range axis
   * 
   * @param cp
   */
  public AxesRangeChangedListener(ChartViewWrapper cp) {
    this(2);
    chart = cp;
    if (chart != null && chart.getChart() != null) {
      chart.getChart().getXYPlot().getDomainAxis().addChangeListener(this);
      chart.getChart().getXYPlot().getRangeAxis().addChangeListener(this);
    }
  }

  @Override
  public void axisChanged(AxisChangeEvent e) {
    ValueAxis a = (ValueAxis) e.getAxis();
    Range r = a.getRange();

    boolean found = false;
    int i = 0;
    for (i = 0; i < axis.length && !found; i++) {
      // get index of axis
      if (axis[i] == null)
        break;
      if (a.equals(axis[i])) {
        found = true;
        break;
      }
    }
    if (i >= axis.length)
      i = axis.length - 1;
    // insert if not found
    if (!found) {
      axis[i] = a;
    }

    if (r != null && (lastRange[i] == null || !r.equals(lastRange[i]))) {
      // range has changed
      axesRangeChanged(chart, a, lastRange[i], r);
    }
    lastRange[i] = r;
  }

  /**
   * only if axis range has changed
   * 
   * @param axis
   * @param lastR
   * @param newR
   */
  public abstract void axesRangeChanged(ChartViewWrapper chart, ValueAxis axis, Range lastR,
      Range newR);
}
