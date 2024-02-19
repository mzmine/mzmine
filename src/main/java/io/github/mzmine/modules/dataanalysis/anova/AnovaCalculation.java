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

package io.github.mzmine.modules.dataanalysis.anova;

import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.taskcontrol.operations.AbstractTaskSubProcessor;
import io.github.mzmine.taskcontrol.operations.TaskSubSupplier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import org.apache.commons.math3.distribution.FDistribution;
import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AnovaCalculation extends AbstractTaskSubProcessor implements
    TaskSubSupplier<List<AnovaResult>> {

  private final List<FeatureListRow> rows;
  private final String groupingColumnName;

  private final List<AnovaResult> result = new ArrayList<>();

  private final AtomicDouble finishedPercentage = new AtomicDouble(0d);

  public AnovaCalculation(List<FeatureListRow> rows, String groupingColumnName) {
    this.rows = rows;
    this.groupingColumnName = groupingColumnName;
  }

  private static final Logger logger = Logger.getLogger(AnovaCalculation.class.getName());

  private void calculateSignificance() throws IllegalStateException {

    if (rows.isEmpty()) {
      return;
    }

    final MetadataTable metadata = MZmineCore.getProjectMetadata();
    final MetadataColumn<?> groupingColumn = metadata.getColumnByName(groupingColumnName);
    final Map<?, List<RawDataFile>> fileGrouping = metadata.groupFilesByColumn(groupingColumn);
    final List<List<RawDataFile>> groupedFiles = fileGrouping.values().stream().toList();

    final double finishedStep = 1.0 / rows.size();

    rows.forEach(row -> {

      if (isCanceled()) {
        return;
      }

      finishedPercentage.getAndAdd(finishedStep);

      double[][] intensityGroups = new double[groupedFiles.size()][];
      for (int i = 0; i < groupedFiles.size(); ++i) {
        List<RawDataFile> groupFiles = groupedFiles.get(i);
        intensityGroups[i] = row.getFeatures().stream()
            .filter(feature -> groupFiles.contains(feature.getRawDataFile()))
            .mapToDouble(Feature::getHeight).toArray();
      }

      final Double pValue = oneWayAnova(intensityGroups);
      if (pValue != null) {
        result.add(new AnovaResult(row, groupingColumnName, pValue));
      }
    });
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

    Double pValue = null;
    try {
      FDistribution distribution = new FDistribution(degreesOfFreedomOfTreatment,
          degreesOfFreedomOfError);
      pValue = 1.0 - distribution.cumulativeProbability(anovaStatistics);
    } catch (MathIllegalArgumentException ex) {
      logger.warning("Error during F-distribution calculation: " + ex.getMessage());
    }

    return pValue;
  }

  @Override
  public @NotNull String getTaskDescription() {
    return STR."Calculating ANOVA values for \{rows != null ? rows.size() : " rows"}";
  }

  @Override
  public double getFinishedPercentage() {
    return finishedPercentage.get();
  }

  @Override
  public List<AnovaResult> get() {
    return result;
  }

  @Override
  public void process() {
    try {
      calculateSignificance();
    } catch (Exception e) {
      logger.log(Level.WARNING, "Error while calculating ANOVA results.", e);
    }
  }
}
