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

import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.event.ChartProgressEvent;
import org.jfree.chart.event.ChartProgressListener;

/**
 * The model for {@link EChartViewer}. This model automatically passes on the rendering info to the
 * chart and plot model so that it can be used to locate data points on screen and convert screen
 * coordinates to data space.
 */
public class FxEChartViewerModel implements ChartProgressListener {

  private final EChartViewer viewer;
  private final ObjectProperty<JFreeChart> chart = new SimpleObjectProperty<>();

  public FxEChartViewerModel(EChartViewer viewer) {
    this.viewer = viewer;
    this.chart.subscribe((oldValue, newValue) -> {
      if (oldValue != null) {
        oldValue.removeProgressListener(this);
      }
      if (newValue instanceof FxJFreeChart fxChart) {
        fxChart.addProgressListener(this);
      }
    });
  }


  public JFreeChart getChart() {
    return chart.get();
  }

  public ObjectProperty<JFreeChart> chartProperty() {
    return chart;
  }

  public void setChart(JFreeChart chart) {
    this.chart.set(chart);
  }

  @Override
  public void chartProgress(ChartProgressEvent event) {
    final JFreeChart chart = this.chart.get();
    if (event.getType() == ChartProgressEvent.DRAWING_FINISHED) {
      if (chart instanceof ChartRenderingInfoPropertyProvider fxChart) {
        fxChart.setRenderingInfo(viewer.getCanvas().getRenderingInfo());
      }
      if (chart != null && chart.getPlot() instanceof ChartRenderingInfoPropertyProvider fxChart) {
        fxChart.setRenderingInfo(viewer.getCanvas().getRenderingInfo());
      }
    }
  }
}
