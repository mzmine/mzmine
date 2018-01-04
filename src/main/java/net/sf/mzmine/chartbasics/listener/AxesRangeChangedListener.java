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

package net.sf.mzmine.chartbasics.listener;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.event.AxisChangeEvent;
import org.jfree.chart.event.AxisChangeListener;
import org.jfree.data.Range;

/**
 * Listener for multiple axes
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public abstract class AxesRangeChangedListener implements AxisChangeListener {

  // last lower / upper range
  private ValueAxis[] axis;
  private Range[] lastRange = null;
  private ChartPanel chart;

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
  public AxesRangeChangedListener(ChartPanel cp) {
    this(2);
    chart = cp;
    if (chart != null) {
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
  public abstract void axesRangeChanged(ChartPanel chart, ValueAxis axis, Range lastR, Range newR);
}
