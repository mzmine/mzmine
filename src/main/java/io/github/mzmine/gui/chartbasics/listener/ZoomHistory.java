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

import java.util.LinkedList;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.fx.ChartViewer;
import org.jfree.data.Range;

import io.github.mzmine.gui.chartbasics.gui.wrapper.ChartViewWrapper;

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
    this(new ChartViewWrapper(cp), maxSize);
  }

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
  public ZoomHistory(ChartViewer cp, int maxSize) {
    this(new ChartViewWrapper(cp), maxSize);
  }

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
  public ZoomHistory(ChartViewWrapper cp, int maxSize) {
    super(cp);
    cp.setZoomHistory(this);
    history = new LinkedList<Range[]>();
    // max
    this.maxSize = maxSize;
  }

  @Override
  public void axesRangeChanged(ChartViewWrapper chart, ValueAxis axis, Range lastR, Range newR) {
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
