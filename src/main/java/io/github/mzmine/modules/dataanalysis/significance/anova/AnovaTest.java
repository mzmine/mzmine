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

package io.github.mzmine.modules.dataanalysis.significance.anova;

import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataanalysis.significance.RowSignificanceTest;
import io.github.mzmine.modules.dataanalysis.significance.StatisticUtils;
import io.github.mzmine.modules.visualization.projectmetadata.MetadataColumnDoesNotExistException;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import org.apache.commons.math3.distribution.FDistribution;
import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AnovaTest implements RowSignificanceTest {

  private static final Logger logger = Logger.getLogger(AnovaTest.class.getName());
  private final String groupingColumnName;
  private final AtomicDouble finishedPercentage = new AtomicDouble(0d);
  private final List<List<RawDataFile>> groupedFiles;
  private List<AnovaResult> result = List.of();

  public AnovaTest(String groupingColumnName) throws MetadataColumnDoesNotExistException {
    this.groupingColumnName = groupingColumnName;

    final MetadataTable metadata = MZmineCore.getProjectMetadata();
    final MetadataColumn<?> groupingColumn = metadata.getColumnByName(groupingColumnName);

    if (groupingColumn == null) {
      throw new MetadataColumnDoesNotExistException(groupingColumnName);
    }

    final Map<?, List<RawDataFile>> fileGrouping = metadata.groupFilesByColumn(groupingColumn);
    groupedFiles = fileGrouping.values().stream().toList();
  }

  @Nullable
  private Double oneWayAnova(@NotNull double[][] intensityGroups) {

    int numGroups = intensityGroups.length;
    long numIntensities = Arrays.stream(intensityGroups).flatMapToDouble(Arrays::stream).count();

    double[] groupMeans = Arrays.stream(intensityGroups)
        .mapToDouble(intensities -> Arrays.stream(intensities).average().orElse(0.0)).toArray();

    double overallMean = Arrays.stream(intensityGroups).flatMapToDouble(Arrays::stream).average()
        .orElse(0.0);

    double sumOfSquaresOfError = IntStream.range(0, intensityGroups.length).mapToDouble(
            i -> Arrays.stream(intensityGroups[i]).map(x -> x - groupMeans[i]).map(x -> x * x).sum())
        .sum();

    double sumOfSquaresOfTreatment =
        (numGroups - 1) * Arrays.stream(groupMeans).map(x -> x - overallMean).map(x -> x * x).sum();

    long degreesOfFreedomOfTreatment = numGroups - 1;
    long degreesOfFreedomOfError = numIntensities - numGroups;

    if (degreesOfFreedomOfTreatment <= 0 || degreesOfFreedomOfError <= 0) {
      return null;
    }

    double meanSquareOfTreatment = sumOfSquaresOfTreatment / degreesOfFreedomOfTreatment;
    double meanSquareOfError = sumOfSquaresOfError / degreesOfFreedomOfError;

    if (meanSquareOfError == 0.0) {
      return null;
    }

    double anovaStatistics = meanSquareOfTreatment / meanSquareOfError;

    try {
      FDistribution distribution = new FDistribution(degreesOfFreedomOfTreatment,
          degreesOfFreedomOfError);
      return 1.0 - distribution.cumulativeProbability(anovaStatistics);
    } catch (MathIllegalArgumentException ex) {
      logger.warning("Error during F-distribution calculation: " + ex.getMessage());
      return null;
    }
  }

  /**
   * @param groupAbundances one array of abundances for each group or an 2D
   *                        array[group][abundances]
   */
  private boolean checkConditions(double[]... groupAbundances) {
    return groupAbundances.length > 2; // anova usually used for more than two groups
  }

  @Override
  public AnovaResult test(FeatureListRow row, AbundanceMeasure abundanceMeasure) {
    final double[][] intensityGroups = groupedFiles.stream()
        .map(group -> StatisticUtils.extractAbundance(row, group, abundanceMeasure))
        .toArray(double[][]::new);

    if (checkConditions(intensityGroups)) {
      final Double pValue = oneWayAnova(intensityGroups);
      if (pValue != null) {
        return new AnovaResult(row, groupingColumnName, pValue);
      }
    }
    return null;
  }
}
