/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.visualization.feat_histogram;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.types.numbers.abstr.NumberType;
import io.github.mzmine.javafx.mvci.FxUpdateTask;
import io.github.mzmine.modules.visualization.scan_histogram.chart.HistogramData;
import io.github.mzmine.taskcontrol.progress.TotalFinishedItemsProgress;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Creates new datasets and updates the data model on FX thread, only if still the latest scheduled
 * task
 */
class FeatureHistogramPlotUpdateTask extends FxUpdateTask<FeatureHistogramPlotModel> {

  private final NumberType<?> selectedType;

  private final TotalFinishedItemsProgress progress = new TotalFinishedItemsProgress();
  private final List<FeatureList> flists;
  private HistogramData temporaryDatasets;


  FeatureHistogramPlotUpdateTask(FeatureHistogramPlotModel model) {
    super("feature_histogram_update", model);
    this.selectedType = model.getSelectedType();

    flists = model.getFeatureLists();
    progress.setTotal(flists.stream().mapToLong(FeatureList::getNumberOfRows).sum());
  }

  @Override
  public boolean checkPreConditions() {
    return !flists.isEmpty() && selectedType != null;
  }

  @Override
  protected void process() {
    if (!checkPreConditions()) {
      return;
    }
    DoubleArrayList data = new DoubleArrayList();
    for (var flist : flists) {
      if (isCanceled()) {
        return;
      }
      for (final FeatureListRow row : flist.getRows()) {
        row.streamFeatures().map(f -> f.get(selectedType)).filter(Objects::nonNull)
            .mapToDouble(Number::doubleValue).forEach(data::add);

        progress.getAndIncrement();
      }
    }

    temporaryDatasets = new HistogramData(data.toDoubleArray());
  }

  @Override
  protected void updateGuiModel() {
    if (temporaryDatasets == null || !isFinished()) {
      return;
    }
    model.setDataset(temporaryDatasets);
  }

  @Override
  public String getTaskDescription() {
    return "Updating feature histogram";
  }

  @Override
  public double getFinishedPercentage() {
    return progress.progress();
  }


}
