/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.gui.chartbasics.gui.javafx;

import java.util.logging.Logger;
import javafx.animation.Animation.Status;
import javafx.animation.PauseTransition;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.util.Duration;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.event.ChartChangeEvent;
import org.jfree.chart.event.ChartChangeListener;
import org.jfree.chart.fx.ChartCanvas;
import org.jfree.chart.plot.Plot;

/// This adapter accumulates events and delays the chart draw until a later time. Maximum delay is a
/// single pause of 25 ms to keep the refresh rate responsive. So the [PauseTransition] is never
/// restarted if it is already running.
///
/// This class needs to be attached to a ChartViewer because there are some chart draw events that
/// directly call the draw event without checking the [Plot#isNotify()] or [JFreeChart#isNotify()] -
/// so the notify later mechanism does not always stop multi draw events.
///
/// Every [EChartViewer] can use this delayed draws and this is currently attached in its
/// constructor.
///
/// How it works:
///
///   - Chart, plot, axes, etc send [ChartChangeEvent] to [ChartChangeListener]
///   - notify later works often but not for all changes
///   - [ChartCanvas] registers a listener on JFreeChart and calls draw for every event
///   - [DelayedChartDrawAdapter] removes the ChartCanvas as a listener and attaches itself to
/// accumulate events
public class DelayedChartDrawAdapter implements ChartChangeListener {

  private static final Logger logger = Logger.getLogger(DelayedChartDrawAdapter.class.getName());

  private final ObjectProperty<JFreeChart> lastChart = new SimpleObjectProperty<>();
  private final PauseTransition delay = new PauseTransition(Duration.millis(25));
  @NotNull
  private final EChartViewer viewer;

  public DelayedChartDrawAdapter(EChartViewer viewer) {
    this.viewer = viewer;

    viewer.getModel().chartProperty().subscribe((chart) -> {
      detach(); // from old chart

      lastChart.set(chart);
      if (chart != null) {
        // remove auto draw event
        chart.removeChangeListener(viewer.getCanvas());
        chart.addChangeListener(this);
      }
    });
  }

  /**
   * Will attach a {@link DelayedChartDrawAdapter} that listens for changes to the internal chart
   * and also to chart changed events.
   */
  public static DelayedChartDrawAdapter attach(EChartViewer viewer) {
    return new DelayedChartDrawAdapter(viewer);
  }

  public void detach() {
    final JFreeChart chart = lastChart.get();
    if (chart == null) {
      return;
    }
    delay.stop();
    chart.removeChangeListener(this);
    chart.addChangeListener(viewer.getCanvas());
  }

  @Override
  public void chartChanged(ChartChangeEvent event) {
    delay.setOnFinished(_ -> viewer.getCanvas().chartChanged(event));
    // only restart the timer if it is not already running
    // this improves the zooming behavior on axes
    // on scroll or drag on the axis there are many event and at maximum we wait delay.duration once
    // otherwise the timer would always reset and the zooming would seem to lag
    if (delay.getStatus() != Status.RUNNING) {
      delay.playFromStart();
    }

    final JFreeChart chart = viewer.getChart();
    if (chart == null) {
      return;
    }

//    String id = JFreeChartUtils.createChartLogIdentifier(viewer, chart);
//    logger.fine("Delayed chart draw on " + id);
  }
}
