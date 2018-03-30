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

import java.util.LinkedList;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.data.Range;

/**
 * The ZoomHistory stores all zoom states which are active for at least 1 second. It allows to jump
 * to previous and next states. To obtain the ZoomHistory object from any ChartPanel use:
 * 
 * <pre>
 * {@code (ZoomHistory) chartPanel.getClientProperty(ZoomHistory.PROPERTY_NAME)}
 * </pre>
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class ZoomHistory extends AxesRangeChangedListener implements Runnable {

  public static final String PROPERTY_NAME = "ZOOM_HISTORY";

  // history for Range[domain-, range-axis]
  // newest first
  private LinkedList<Range[]> history;
  private int currentI = 0;
  private int maxSize = 0;

  // latest event
  private Range[] newRange;
  private Thread thread;
  private boolean isRunning = false;

  // last change
  private static final long MIN_TIME_DIFF = 1000;
  private long lastChangeTime = 0;

  /**
   * Creates a ZoomHistory for the given ChartPanel as a ClientProperty. The history collects all
   * zoom states that are active for at least 1 second. It allows to jump to previous and next zoom
   * states. To obtain the ZoomHistory object from any ChartPanel use:
   * 
   * <pre>
   * {@code (ZoomHistory) chartPanel.getClientProperty(ZoomHistory.PROPERTY_NAME)}
   * </pre>
   * 
   * @param cp
   * @param maxSize
   */
  public ZoomHistory(ChartPanel cp, int maxSize) {
    super(cp);
    cp.putClientProperty(PROPERTY_NAME, this);
    history = new LinkedList<Range[]>();
    // max
    this.maxSize = maxSize;
  }

  @Override
  public void axesRangeChanged(ChartPanel chart, ValueAxis axis, Range lastR, Range newR) {
    // ranges
    Range dom = chart.getChart().getXYPlot().getDomainAxis().getRange();
    Range ran = chart.getChart().getXYPlot().getRangeAxis().getRange();
    newRange = new Range[] {dom, ran};

    // set time
    lastChangeTime = System.nanoTime();

    if (!isRunning) {
      thread = new Thread(this);
      thread.start();
      isRunning = true;
    }
  }

  @Override
  public void run() {
    Thread thisThread = Thread.currentThread();
    while (thisThread == thread) {
      // greater than time limit?
      long ctime = System.nanoTime();
      if ((ctime - lastChangeTime) / 1000000 >= MIN_TIME_DIFF) {
        //
        handleLatestEvent();
        // end
        isRunning = false;
        break;
      }
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * jump in history or add new
   */
  private void handleLatestEvent() {
    // is already in linked list?
    boolean found = false;
    for (int i = 0; i < history.size() && !found; i++) {
      Range[] r = history.get(i);
      if (newRange[0].equals(r[0]) && newRange[1].equals(r[1])) {
        found = true;
        currentI = i;
      }
    }
    if (!found) {
      // remove all history objects 0 to currentI-1
      for (int i = 0; i < currentI; i++)
        history.removeFirst();
      history.addFirst(newRange);

      if (history.size() > maxSize)
        history.removeLast();
      currentI = 0;
    }
  }

  /**
   * Current zoom range
   * 
   * @return
   */
  public Range[] getCurrentRange() {
    if (history.isEmpty())
      return null;
    return history.get(currentI);
  }

  /**
   * Previous zoom range without changing the active state of the history
   * 
   * @return
   */
  public Range[] getPreviousRange() {
    if (history.isEmpty() || currentI + 1 >= getSize())
      return null;
    return history.get(currentI + 1);
  }

  /**
   * Next zoom range without changing the active state of the history
   * 
   * @return
   */
  public Range[] getNextRange() {
    if (history.isEmpty() || currentI - 1 < 0)
      return null;
    return history.get(currentI - 1);
  }

  /**
   * Jump to previous zoom range (change current state)
   * 
   * @return
   */
  public Range[] setPreviousPoint() {
    currentI++;
    if (currentI >= getSize())
      currentI = getSize() - 1;
    return getCurrentRange();
  }

  /**
   * Jump to next zoom range (change current state)
   * 
   * @return
   */
  public Range[] setNextPoint() {
    currentI--;
    if (currentI < 0)
      currentI = 0;
    return getCurrentRange();
  }

  /**
   * Might want to clear the history after completing the creation of a chart
   */
  public void clear() {
    stopRunningUpdates();
    history.clear();
  }

  /**
   * Stops the threads run method that updates the history on axes changed events
   */
  public void stopRunningUpdates() {
    thread = null;
    isRunning = false;
  }

  public LinkedList<Range[]> getHistory() {
    return history;
  }

  public int getSize() {
    return history.size();
  }

  public int getCurrentIndex() {
    return currentI;
  }
}
