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

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureAnnotationPriority;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.AnyXYProvider;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataanalysis.significance.RowSignificanceTest;
import io.github.mzmine.modules.dataanalysis.significance.RowSignificanceTestProcessor;
import io.github.mzmine.modules.dataanalysis.significance.RowSignificanceTestResult;
import io.github.mzmine.modules.dataanalysis.significance.StatisticUtils;
import io.github.mzmine.modules.dataanalysis.significance.ttest.Student_tTest;
import io.github.mzmine.taskcontrol.SimpleCalculation;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.DataTypeUtils;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.math.util.MathUtils;

public class VolcanoPlotInteractor {

  private static final DataType<?>[] annotationTypeHierarchy = FeatureAnnotationPriority.getDataTypesInOrder();

  private final VolcanoPlotModel model;

  private Task lastTask = null;

  public VolcanoPlotInteractor(VolcanoPlotModel model) {
    this.model = model;
  }

  public void computeDataset() {
    final FeatureList flist = model.getSelectedFlist();

    final RowSignificanceTest test = model.getTest();
    if (test == null) {
      return;
    }

    if (lastTask != null) {
      lastTask.cancel();
    }

    RowSignificanceTestProcessor<?> processor = new RowSignificanceTestProcessor<>(flist.getRows(),
        model.getAbundanceMeasure(), test);
    SimpleCalculation<RowSignificanceTestProcessor<?>> task = new SimpleCalculation<>(processor);

    MZmineCore.getTaskController().addTask(task);

    task.setOnFinished(() -> {
      lastTask = null;
      final List<RowSignificanceTestResult> rowSignificanceTestResults = task.getProcessor().get();
      final Map<DataType<?>, List<RowSignificanceTestResult>> dataTypeMap = DataTypeUtils.groupByBestDataType(
          rowSignificanceTestResults, RowSignificanceTestResult::row, true,
          annotationTypeHierarchy);

      if (!(test instanceof Student_tTest<?> ttest)) {
        return;
      }

      final List<RawDataFile> groupAFiles = ttest.getGroupAFiles();
      final List<RawDataFile> groupBFiles = ttest.getGroupBFiles();

      final SimpleColorPalette colors = MZmineCore.getConfiguration().getDefaultColorPalette();
      int colorIndex = 0;
      List<PlotXYDataProvider> datasets = new ArrayList<>();
      for (Entry<DataType<?>, List<RowSignificanceTestResult>> entry : dataTypeMap.entrySet()) {
        final DataType<?> type = entry.getKey();
        final List<RowSignificanceTestResult> testResults = entry.getValue();

        final double[] foldChange = testResults.stream().mapToDouble(result -> MathUtils.log(2,
            Arrays.stream(StatisticUtils.extractAbundance(result.row(), groupAFiles,
                model.getAbundanceMeasure())).average().getAsDouble() / Arrays.stream(
                StatisticUtils.extractAbundance(result.row(), groupBFiles,
                    model.getAbundanceMeasure())).average().getAsDouble())).toArray();

        var provider = new AnyXYProvider(colors.getAWT(colorIndex), type.getHeaderString(),
            testResults.size(), i -> foldChange[i], i -> -Math.log10(testResults.get(i).pValue()));
        datasets.add(provider);
      }

      model.setDatasets(datasets);
    });


  }

}
