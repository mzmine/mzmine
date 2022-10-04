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

import org.jetbrains.annotations.Nullable;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.event.AxisChangeEvent;
import org.jfree.chart.event.AxisChangeListener;
import org.jfree.data.Range;

import io.github.mzmine.gui.chartbasics.gui.wrapper.ChartViewWrapper;

public abstract class AxisRangeChangedListener implements AxisChangeListener {

  // last lower / upper range
  private Range lastRange = null;
  private ChartViewWrapper chart;

  public AxisRangeChangedListener(@Nullable ChartViewWrapper cp) {
    chart = cp;
  }

  @Override
  public void axisChanged(AxisChangeEvent e) {
    ValueAxis a = (ValueAxis) e.getAxis();
    Range r = a.getRange();

    if (r != null && (lastRange == null || !r.equals(lastRange))) {
      // range has changed
      axisRangeChanged(chart, a, lastRange, r);
    }
    lastRange = r;
  }

  /**
   * only if axis range has changed
   * 
   * @param chart (null if no chart was defined when this listener was created)
   * @param axis
   * @param lastR
   * @param newR
   */
  public abstract void axisRangeChanged(@Nullable ChartViewWrapper chart, ValueAxis axis,
      Range lastR, Range newR);
}
