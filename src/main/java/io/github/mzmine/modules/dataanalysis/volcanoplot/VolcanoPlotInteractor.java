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

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.annotations.CompoundDatabaseMatchesType;
import io.github.mzmine.datamodel.features.types.annotations.LipidMatchListType;
import io.github.mzmine.datamodel.features.types.annotations.ManualAnnotationType;
import io.github.mzmine.datamodel.features.types.annotations.MissingValueType;
import io.github.mzmine.datamodel.features.types.annotations.SpectralLibraryMatchesType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaListType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataanalysis.significance.AnovaResult;
import io.github.mzmine.modules.dataanalysis.significance.anova.AnovaCalculation;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.taskcontrol.SimpleCalculation;
import io.github.mzmine.util.DataTypeUtils;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.math3.stat.inference.TTest;

public class VolcanoPlotInteractor {

  private static final DataType[] types = DataTypes.getAll(ManualAnnotationType.class,
      SpectralLibraryMatchesType.class, LipidMatchListType.class, CompoundDatabaseMatchesType.class,
      FormulaListType.class).toArray(DataType[]::new);

  private final VolcanoPlotModel model;

  public VolcanoPlotInteractor(VolcanoPlotModel model) {
    this.model = model;
  }

  public void computeDataset() {
    final MetadataColumn<?> column = model.getSelectedMetadataColumn();
    final FeatureList flist = model.getSelectedFlist();

    final AnovaCalculation anova = new AnovaCalculation(flist.getRows(), column.getTitle());
    final SimpleCalculation<AnovaCalculation> calc = SimpleCalculation.of(anova);

    final List<AnovaResult> anovaResults = calc.getProcessor().get();
    MZmineCore.getTaskController().addTask(calc);

    final Map<DataType, List<AnovaResult>> anovaByAnnotation = anovaResults.stream()
        .collect(Collectors.groupingBy(result -> {
          var best = DataTypeUtils.getBestTypeWithValue(result.row(), types);
          return best == null ? DataTypes.get(MissingValueType.class) : best;
        }));

    final TTest test = new TTest();
//    test.
  }

}
