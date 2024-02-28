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

package io.github.mzmine.modules.dataanalysis.volcanoplot;

import io.github.mzmine.datamodel.features.FeatureAnnotationPriority;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.annotations.MissingValueType;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.DatasetAndRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.RunOption;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYShapeRenderer;
import io.github.mzmine.gui.framework.fx.mvci.FxInteractor;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataanalysis.significance.RowSignificanceTest;
import io.github.mzmine.modules.dataanalysis.significance.RowSignificanceTestProcessor;
import io.github.mzmine.modules.dataanalysis.significance.RowSignificanceTestResult;
import io.github.mzmine.modules.dataanalysis.significance.ttest.StudentTTest;
import io.github.mzmine.util.DataTypeUtils;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class VolcanoPlotInteractor extends FxInteractor<VolcanoPlotModel> {

  private List<DatasetAndRenderer> temporaryDatasets;

  public VolcanoPlotInteractor(VolcanoPlotModel model) {
    super(model);
  }

  @Override
  public void updateModel() {
    model.setDatasets(temporaryDatasets);
    temporaryDatasets = null;
  }

  public void computeDataset() {

    final FeatureList flist = model.getSelectedFlist();
    if (flist == null) {
      return;
    }

    final RowSignificanceTest test = model.getTest();
    if (test == null) {
      return;
    }

    RowSignificanceTestProcessor<?> processor = new RowSignificanceTestProcessor<>(flist.getRows(),
        model.getAbundanceMeasure(), test);
    processor.process();
    prepareTestResultsForPresentation(processor, test);
  }

  private void prepareTestResultsForPresentation(RowSignificanceTestProcessor<?> task,
      RowSignificanceTest test) {
    final List<RowSignificanceTestResult> rowSignificanceTestResults = task.get();
    final Map<DataType<?>, List<RowSignificanceTestResult>> dataTypeMap = DataTypeUtils.groupByBestDataType(
        rowSignificanceTestResults, RowSignificanceTestResult::row, true,
        FeatureAnnotationPriority.getDataTypesInOrder());

    if (!(test instanceof StudentTTest<?> ttest)) {
      return;
    }

    final SimpleColorPalette colors = MZmineCore.getConfiguration().getDefaultColorPalette();
    temporaryDatasets = new ArrayList<>();
    colors.resetColorCounter(); // set color index to 0
    for (Entry<DataType<?>, List<RowSignificanceTestResult>> entry : dataTypeMap.entrySet()) {

      final DataType<?> type = entry.getKey();
      final List<RowSignificanceTestResult> testResults = entry.getValue();

      final double pValue = model.getpValue();
      final List<RowSignificanceTestResult> significantRows = testResults.stream()
          .filter(result -> result.pValue() < pValue).toList();
      final List<RowSignificanceTestResult> insignificantRows = testResults.stream()
          .filter(result -> result.pValue() >= pValue).toList();

      final Color color = colors.getNextColorAWT();
      if (!significantRows.isEmpty()) {
        var provider = new VolcanoDatasetProvider(ttest, significantRows, color,
            STR."\{type.equals(DataTypes.get(MissingValueType.class)) ? "not annotated"
                : type.getHeaderString()} (p < \{pValue})", model.getAbundanceMeasure());
        temporaryDatasets.add(
            new DatasetAndRenderer(new ColoredXYDataset(provider, RunOption.THIS_THREAD),
                new ColoredXYShapeRenderer(false)));
      }
      if (!insignificantRows.isEmpty()) {
        var provider = new VolcanoDatasetProvider(ttest, insignificantRows, color,
            STR."\{type.equals(DataTypes.get(MissingValueType.class)) ? "not annotated"
                : type.getHeaderString()} (p ≥ \{pValue})", model.getAbundanceMeasure());
        temporaryDatasets.add(
            new DatasetAndRenderer(new ColoredXYDataset(provider, RunOption.THIS_THREAD),
                new ColoredXYShapeRenderer(true)));
      }
    }
  }
}
