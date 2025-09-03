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

import java.util.logging.Logger;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.Plot;

public class FxJFreeChartModel implements FxBaseChartModel, ChartRenderingInfoPropertyProvider {

  private static final Logger logger = Logger.getLogger(FxJFreeChartModel.class.getName());

  private final ObjectProperty<@Nullable JFreeChart> chart = new SimpleObjectProperty<>();
  private final ReadOnlyObjectWrapper<@Nullable Plot> plot = new ReadOnlyObjectWrapper<>();
  /**
   * Automatically set in {@link FxEChartViewerModel} and represents the rendering info of the
   * latest render event
   */
  private final ObjectProperty<@Nullable ChartRenderingInfo> renderingInfo = new SimpleObjectProperty<>();

  public FxJFreeChartModel(@Nullable JFreeChart chart) {
    this.chart.set(chart);
    this.plot.bind(this.chart.map(ch -> ch != null ? ch.getPlot() : null).orElse(null));

    initListeners();
  }

  private void initListeners() {
    applyNotifyLater(chart, _ -> updateAll());

    // send rendering info to plot
    plot.subscribe((oldPlot, newPlot) -> {
      if (oldPlot instanceof ChartRenderingInfoPropertyProvider provider) {
        provider.renderingInfoProperty().unbind();
      }
      if (newPlot instanceof ChartRenderingInfoPropertyProvider provider) {
        provider.renderingInfoProperty().bind(renderingInfo);
      }
    });
  }

  private void updateAll() {
  }

  @Override
  public @Nullable JFreeChart getChart() {
    return chart.get();
  }

  public ObjectProperty<@Nullable JFreeChart> chartProperty() {
    return chart;
  }

  @Override
  public @Nullable Plot getPlot() {
    return plot.get();
  }

  public ReadOnlyObjectProperty<Plot> plotProperty() {
    return plot.getReadOnlyProperty();
  }

  @Override
  public @NotNull ObjectProperty<@Nullable ChartRenderingInfo> renderingInfoProperty() {
    return renderingInfo;
  }

}
