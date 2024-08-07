/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.parameters.dialogs.previewpane;

import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.DatasetAndRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.javafx.mvci.FxUpdateTask;
import io.github.mzmine.javafx.mvci.LatestTaskScheduler;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.parameters.ParameterSet;
import java.util.List;
import javafx.scene.layout.BorderPane;

public abstract class AbstractPreviewPane<T> extends BorderPane implements PreviewPane<T> {

  protected final NumberFormats formats = ConfigService.getGuiFormats();
  protected final SimpleXYChart<? extends PlotXYDataProvider> chart;
  protected final ParameterSet parameters;
  protected final LatestTaskScheduler scheduler = new LatestTaskScheduler();

  public AbstractPreviewPane(final ParameterSet parameters) {
    this.parameters = parameters;
    chart = createChart();
    setCenter(chart);
  }

  /**
   * Called by the setup dialog if the parameters or the selected feature change.
   */
  @Override
  public void updatePreview() {
    var task = new FxUpdateTask<>("updating %s preview".formatted(
        parameters.getModuleNameAttribute() != null ? parameters.getModuleNameAttribute()
            : "parameter"), new Object()) {
      private List<DatasetAndRenderer> datasetAndRenderers;

      @Override
      public String getTaskDescription() {
        return "Updating preview";
      }

      @Override
      public double getFinishedPercentage() {
        return 0;
      }

      @Override
      protected void process() {
        datasetAndRenderers = calculateNewDatasets(getValueForPreview());
      }

      @Override
      protected void updateGuiModel() {
        updateChart(datasetAndRenderers, chart);
      }
    };

    scheduler.onTaskThreadDelayed(task, LatestTaskScheduler.DEFAULT_DELAY);
  }
}
