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
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.annotations.MissingValueType;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ProviderAndRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.AnyXYProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYShapeRenderer;
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
import java.awt.Color;
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

  public void computeDataset(Runnable r) {
    final FeatureList flist = model.getSelectedFlist();
    if (flist == null) {
      return;
    }

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
    lastTask = task;
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
      List<ProviderAndRenderer> datasets = new ArrayList<>();
      colors.resetColorCounter(); // set color index to 0
      for (Entry<DataType<?>, List<RowSignificanceTestResult>> entry : dataTypeMap.entrySet()) {
        final DataType<?> type = entry.getKey();
        final List<RowSignificanceTestResult> testResults = entry.getValue();

        final List<RowSignificanceTestResult> significantRows = testResults.stream()
            .filter(result -> result.pValue() < 0.05).toList();
        final List<RowSignificanceTestResult> insignificantRows = testResults.stream()
            .filter(result -> result.pValue() >= 0.05).toList();

        final Color color = colors.getNextColorAWT();
        if (!significantRows.isEmpty()) {
          final double[] log2FoldChangeSignificant = calculateLog2FoldChange(significantRows,
              groupAFiles, groupBFiles);
          var provider = new AnyXYProvider(color,
              STR."\{type.equals(DataTypes.get(MissingValueType.class)) ? "not annotated"
                  : type.getHeaderString()} (p < 0.05)", significantRows.size(),
              i -> log2FoldChangeSignificant[i], i -> -Math.log10(significantRows.get(i).pValue()));
          datasets.add(new ProviderAndRenderer(provider, new ColoredXYShapeRenderer(false)));
        }
        if (!insignificantRows.isEmpty()) {
          final double[] log2FoldChangeInsignificant = calculateLog2FoldChange(insignificantRows,
              groupAFiles, groupBFiles);
          var provider = new AnyXYProvider(color,
              STR."\{type.equals(DataTypes.get(MissingValueType.class)) ? "not annotated"
                  : type.getHeaderString()} (p ≥ 0.05)", insignificantRows.size(),
              i -> log2FoldChangeInsignificant[i],
              i -> -Math.log10(insignificantRows.get(i).pValue()));
          datasets.add(new ProviderAndRenderer(provider, new ColoredXYShapeRenderer(true)));
        }
      }

      MZmineCore.runOnFxThreadAndWait(() -> {
        model.setDatasets(datasets);
      });
      r.run();
    });
  }

  private double[] calculateLog2FoldChange(List<RowSignificanceTestResult> testResults,
      List<RawDataFile> groupAFiles, List<RawDataFile> groupBFiles) {
    return testResults.stream().mapToDouble(result -> {
      final double[] ab1 = StatisticUtils.extractAbundance(result.row(), groupAFiles,
          model.getAbundanceMeasure());
      final double[] abB = StatisticUtils.extractAbundance(result.row(), groupBFiles,
          model.getAbundanceMeasure());
      return MathUtils.log(2,
          Arrays.stream(ab1).average().getAsDouble() / Arrays.stream(abB).average().getAsDouble());
    }).toArray();
  }

}
