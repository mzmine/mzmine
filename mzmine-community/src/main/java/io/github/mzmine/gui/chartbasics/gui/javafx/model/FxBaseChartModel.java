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

package io.github.mzmine.gui.chartbasics.gui.javafx.model;

import java.util.function.Consumer;
import javafx.beans.value.ObservableValue;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.Plot;

public interface FxBaseChartModel extends ChartRenderingInfoPropertyProvider {

  /**
   * Listen to property value change and apply logic with notify changes false
   *
   * @param newValueConsumer the logic that updates the chart
   */
  default <T> void applyNotifyLater(ObservableValue<T> property, Consumer<T> newValueConsumer) {
    property.subscribe((_, nv) -> {
      final Plot plot = getPlot();
      if (plot == null) {
        return;
      }
      applyWithNotifyChanges(false, () -> newValueConsumer.accept(nv));
    });
  }

  /**
   * Will set the chart.notify to tempState, perform logic that changes the chart, and reset to the
   * old notify state. If the old notify was true, a chart change event is fired. The old notify
   * will be false if this call is one of many boxed calls within methods.
   *
   * @param tempState usually false to avoid updating of a chart at every change event
   * @param logic     the logic that updates the chart
   */
  default void applyWithNotifyChanges(boolean tempState, Runnable logic) {
    final Plot plot = getPlot();
    if (plot == null) {
      return;
    }
    // use chart notify to stop upper level updates from happening
    final JFreeChart chart = getChart();
    applyWithNotifyChanges(tempState, chart == null ? plot.isNotify() : chart.isNotify(), logic);
  }

  /**
   * Will set the chart.notify to tempState, perform logic that changes the chart, and reset to the
   * old notify state. If the old notify was true, a chart change event is fired. The old notify
   * will be false if this call is one of many boxed calls within methods.
   *
   * @param tempState     usually false to avoid updating of a chart at every change event
   * @param logic         the logic that updates the chart
   * @param afterRunState the new state after running logic. If true, the chart is updated.
   */
  default void applyWithNotifyChanges(boolean tempState, boolean afterRunState, Runnable logic) {
    final Plot plot = getPlot();
    if (plot == null) {
      return;
    }
    final JFreeChart chart = getChart();
    if (chart != null) {
      chart.setNotify(tempState);
    } else {
      plot.setNotify(tempState);
    }
    try {
      // perform changes that t
      logic.run();
    } finally {
      // reset to old state and run changes if true
      // setting to true will automatically trigger a draw event
      if (chart != null) {
        chart.setNotify(afterRunState);
      } else {
        plot.setNotify(afterRunState);
      }
    }
  }


  @Nullable Plot getPlot();

  @Nullable JFreeChart getChart();
}
