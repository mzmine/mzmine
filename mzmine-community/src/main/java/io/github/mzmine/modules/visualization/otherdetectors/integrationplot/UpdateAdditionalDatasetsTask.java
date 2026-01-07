/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.visualization.otherdetectors.integrationplot;

import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.RunOption;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.IntensityTimeSeriesToXYProvider;
import io.github.mzmine.javafx.mvci.FxUpdateTask;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class UpdateAdditionalDatasetsTask extends FxUpdateTask<IntegrationPlotModel> {

  private final List<ColoredXYDataset> newDatasets = new ArrayList<>();
  private long total = 0;
  private long processed = 0;

  protected UpdateAdditionalDatasetsTask(@NotNull IntegrationPlotModel model) {
    super("Updating additional datasets.", model);
  }

  @Override
  protected void process() {
    final List<IntensityTimeSeriesToXYProvider> newSeries = model.getAdditionalDataProviders();
    total = newSeries.size();

    for (int i = 0; i < newSeries.size(); i++) {
      PlotXYDataProvider provider = newSeries.get(i);
      ColoredXYDataset ds = new ColoredXYDataset(provider, RunOption.THIS_THREAD);
      newDatasets.add(ds);
      processed++;
    }
  }

  @Override
  protected void updateGuiModel() {
    model.setAdditionalTimeSeriesDatasets(newDatasets);
  }

  @Override
  public String getTaskDescription() {
    return "Updating additional datasets.";
  }

  @Override
  public double getFinishedPercentage() {
    return total != 0 ? (double) processed / total : 0;
  }

  @Override
  public boolean checkPreConditions() {
    // if both are empty, we don't need to do anything -> return false in that case
    return !(model.additionalDataProvidersProperty().isEmpty()
        && model.additionalTimeSeriesDatasetsProperty().isEmpty());
  }
}
