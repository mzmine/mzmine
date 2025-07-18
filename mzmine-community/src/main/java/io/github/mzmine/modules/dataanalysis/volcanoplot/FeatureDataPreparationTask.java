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

package io.github.mzmine.modules.dataanalysis.volcanoplot;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.statistics.FeaturesDataTable;
import io.github.mzmine.javafx.mvci.FxUpdateTask;
import io.github.mzmine.modules.dataanalysis.utils.StatisticUtils;
import io.github.mzmine.parameters.parametertypes.statistics.AbundanceDataTablePreparationConfig;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;

/**
 * Simple task that extracts and prepares feature data and sets it to a property
 */
public class FeatureDataPreparationTask extends FxUpdateTask<ObjectProperty<FeaturesDataTable>> {

  private final ObservableList<FeatureListRow> rows;
  private final List<RawDataFile> selectedFiles;
  private final AbundanceDataTablePreparationConfig config;
  private FeaturesDataTable dataTable;

  public FeatureDataPreparationTask(ObjectProperty<FeaturesDataTable> model,
      FeatureList featureList, AbundanceDataTablePreparationConfig config) {
    this(model, featureList.getRows(), featureList.getRawDataFiles(), config);
  }

  public FeatureDataPreparationTask(ObjectProperty<FeaturesDataTable> model,
      ObservableList<FeatureListRow> rows, List<RawDataFile> selectedFiles,
      AbundanceDataTablePreparationConfig config) {
    super("Prepare feature data table", model);
    this.rows = rows;
    this.selectedFiles = selectedFiles;
    this.config = config;
  }

  @Override
  protected void process() {
    dataTable = StatisticUtils.extractAbundancesPrepareData(rows, selectedFiles, config);
  }

  @Override
  protected void updateGuiModel() {
    model.set(dataTable);
  }

  @Override
  public String getTaskDescription() {
    return "Preparing feature data table";
  }

  @Override
  public double getFinishedPercentage() {
    return 0;
  }
}
