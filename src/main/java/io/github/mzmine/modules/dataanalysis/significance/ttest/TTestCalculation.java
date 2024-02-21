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

package io.github.mzmine.modules.dataanalysis.significance.ttest;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataanalysis.significance.InvalidTestModelException;
import io.github.mzmine.modules.dataanalysis.significance.StatisticUtils;
import io.github.mzmine.modules.dataanalysis.significance.TTestResult;
import io.github.mzmine.modules.visualization.projectmetadata.MetadataColumnDoesNotExistException;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.taskcontrol.operations.AbstractTaskSubProcessor;
import io.github.mzmine.taskcontrol.operations.TaskSubSupplier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import org.apache.commons.math3.stat.inference.TTest;
import org.apache.commons.math3.stat.inference.TestUtils;
import org.jetbrains.annotations.NotNull;

public class TTestCalculation<T> extends AbstractTaskSubProcessor implements
    TaskSubSupplier<List<TTestResult>> {

  private static final Logger logger = Logger.getLogger(TTestCalculation.class.getName());

  private final List<FeatureListRow> rows;
  private final TTestSamplingConfig samplingConfig;
  private final AbundanceMeasure abundanceMeasure;
  private final String groupingColumnName;
  private final MetadataColumn<T> groupingColumn;
  private final T[] groupingValues;
  private final List<TTestResult> results = new ArrayList<>();
  private AtomicLong processedRows = new AtomicLong(0);

  public TTestCalculation(@NotNull List<FeatureListRow> rows, TTestSamplingConfig testConfig,
      AbundanceMeasure abundanceMeasure, @NotNull String groupingColumnName) {
    this.rows = rows;
    this.samplingConfig = testConfig;
    this.abundanceMeasure = abundanceMeasure;
    this.groupingColumnName = groupingColumnName;
    groupingColumn = null;
    groupingValues = null;
  }

  public TTestCalculation(@NotNull List<FeatureListRow> rows, TTestSamplingConfig testConfig,
      AbundanceMeasure abundanceMeasure, @NotNull MetadataColumn<T> groupingColumn,
      @NotNull T[] groupingValues) {
    this.rows = rows;
    this.samplingConfig = testConfig;
    this.abundanceMeasure = abundanceMeasure;
    this.groupingColumnName = groupingColumn.getTitle();
    this.groupingColumn = groupingColumn;
    if (groupingValues.length != 2) {
      throw new InvalidTestModelException(
          STR."t-Test is only applicable to two values. \{groupingValues.length} selected.");
    }
    this.groupingValues = groupingValues;
  }

  @Override
  public @NotNull String getTaskDescription() {
    return STR."Performing t-Test for group \{groupingColumnName}";
  }

  @Override
  public double getFinishedPercentage() {
    return rows.isEmpty() ? 0 : (double) processedRows.get() / rows.size();
  }

  @Override
  public void process() {
    final List<List<RawDataFile>> groupedFiles = groupFiles();
    assert groupedFiles.size() == 2;

    TTest test = new TTest();
    final List<TTestResult> results = rows.stream().map(row -> {

      final double[] group1Abundance = StatisticUtils.extractAbundance(row, groupedFiles.get(0),
          abundanceMeasure);
      final double[] group2Abundance = StatisticUtils.extractAbundance(row, groupedFiles.get(1),
          abundanceMeasure);

      if (!checkConditions(group1Abundance, group2Abundance)) {
        return null;
      }
      final double p = switch (samplingConfig) {
        case PAIRED -> TestUtils.pairedTTest(group1Abundance, group2Abundance);
        case UNPAIRED -> TestUtils.tTest(group1Abundance, group2Abundance);
      };
      processedRows.getAndIncrement();
      return new TTestResult(row, groupingColumnName, p);
    }).filter(Objects::nonNull).toList();
  }

  private boolean checkConditions(double[] group1Abundance, double[] group2Abundance) {
    switch (samplingConfig) {
      case PAIRED -> {
        // only perform paired test if the number of abundances is equal (pre/post treatment)
        if (group1Abundance.length != group2Abundance.length || group1Abundance.length < 2) {
          return false;
        }
      }
      case UNPAIRED -> {
        if (group1Abundance.length < 2 || group2Abundance.length < 2) {
          return false;
        }
      }
    }
    return true;
  }

  private @NotNull List<List<RawDataFile>> groupFiles() {
    if (groupingValues == null) {
      return extractGroupsFromColumn();
    } else {
      final MetadataTable metadata = MZmineCore.getProjectMetadata();
      final Map<T, List<RawDataFile>> valueFileMap = metadata.groupFilesByColumnValue(
          groupingColumn, groupingValues);
      return valueFileMap.values().stream().toList();
    }
  }

  @NotNull
  private List<List<RawDataFile>> extractGroupsFromColumn() {
    final MetadataTable metadata = MZmineCore.getProjectMetadata();
    final MetadataColumn<?> column = metadata.getColumnByName(groupingColumnName);
    if (column == null) {
      throw new MetadataColumnDoesNotExistException(groupingColumnName);
    }

    final Map<?, List<RawDataFile>> listMap = metadata.groupFilesByColumn(column);
    final List<List<RawDataFile>> groupedFiles = listMap.values().stream().toList();
    if (groupedFiles.size() != 2) {
      throw new InvalidTestModelException(
          STR."Cannot perform a t-Test for \{groupedFiles.size()} groups. Only two groups are applicable. Did you select the correct column (\{groupingColumnName})?");
    }
    return groupedFiles;
  }

  @Override
  public List<TTestResult> get() {
    return results;
  }
}
