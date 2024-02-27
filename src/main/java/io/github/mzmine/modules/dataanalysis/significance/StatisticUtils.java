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

package io.github.mzmine.modules.dataanalysis.significance;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.apache.commons.math.util.MathUtils;

public class StatisticUtils {

  public static double[] extractAbundance(FeatureListRow row, List<RawDataFile> group,
      AbundanceMeasure measure) {
    return group.stream().map(file -> measure.get((ModularFeature) row.getFeature(file)))
        .filter(Objects::nonNull).mapToDouble(Float::doubleValue).toArray();
  }

  public static double[] calculateLog2FoldChange(List<RowSignificanceTestResult> testResults,
      List<RawDataFile> groupAFiles, List<RawDataFile> groupBFiles,
      AbundanceMeasure abundanceMeasure) {
    return testResults.stream().mapToDouble(result -> {
      return calculateLog2FoldChange(groupAFiles, groupBFiles, abundanceMeasure, result);
    }).toArray();
  }

  public static double calculateLog2FoldChange(List<RawDataFile> groupAFiles, List<RawDataFile> groupBFiles,
      AbundanceMeasure abundanceMeasure, RowSignificanceTestResult result) {
    final double[] ab1 = StatisticUtils.extractAbundance(result.row(), groupAFiles,
        abundanceMeasure);
    final double[] abB = StatisticUtils.extractAbundance(result.row(), groupBFiles,
        abundanceMeasure);
    return MathUtils.log(2,
        Arrays.stream(ab1).average().getAsDouble() / Arrays.stream(abB).average().getAsDouble());
  }
}
