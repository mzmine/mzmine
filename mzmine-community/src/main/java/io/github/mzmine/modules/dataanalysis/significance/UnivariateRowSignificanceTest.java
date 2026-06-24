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

package io.github.mzmine.modules.dataanalysis.significance;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.statistics.FeaturesDataTable;
import io.github.mzmine.modules.dataanalysis.significance.ttest.UnivariateRowSignificanceTestResult;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.parameters.parametertypes.statistics.UnivariateRowSignificanceTestConfig;
import io.github.mzmine.project.ProjectService;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/**
 * @param <T> Type of the metadata column.
 */
public final class UnivariateRowSignificanceTest<T> implements RowSignificanceTest {

  private final SignificanceTests test;
  private final MetadataColumn<T> column;
  private final T groupA;
  private final T groupB;
  private final FeaturesDataTable groupAData;
  private final FeaturesDataTable groupBData;

  public UnivariateRowSignificanceTest(@NotNull FeaturesDataTable dataTable,
      @NotNull SignificanceTests test, MetadataColumn<T> column, T groupA, T groupB) {
    List<RawDataFile> groupedFilesA = ProjectService.getMetadata().getMatchingFiles(column, groupA);
    List<RawDataFile> groupedFilesB = ProjectService.getMetadata().getMatchingFiles(column, groupB);

    if (groupedFilesA.size() < 2 || groupedFilesB.size() < 2) {
      throw new IllegalArgumentException("""
          Not enough matching files for group, at least two samples required but column %s had %s n=%d and %s n=%d samples.""".formatted(
          column.getTitle(), groupA, groupedFilesA.size(), groupB, groupedFilesB.size()));
    }

    // split table into the two groups
    var groupAData = dataTable.subsetBySamples(groupedFilesA);
    var groupBData = dataTable.subsetBySamples(groupedFilesB);
    this(groupAData, groupBData, test, column, groupA, groupB);
  }

  public FeaturesDataTable getGroupAData() {
    return groupAData;
  }

  public FeaturesDataTable getGroupBData() {
    return groupBData;
  }

  /**
   * Already provides the two separate grouped {@link FeaturesDataTable}
   *
   * @param groupAData data for group A
   * @param groupBData data for groupB
   */
  public UnivariateRowSignificanceTest(@NotNull FeaturesDataTable groupAData,
      @NotNull FeaturesDataTable groupBData, @NotNull SignificanceTests test,
      MetadataColumn<T> column, T groupA, T groupB) {
    this.test = test;
    this.column = column;
    this.groupA = groupA;
    this.groupB = groupB;
    this.groupAData = groupAData;
    this.groupBData = groupBData;

    if (groupAData.getNumberOfSamples() < 2 || groupBData.getNumberOfSamples() < 2) {
      throw new IllegalArgumentException("""
          Not enough matching files for group, at least two samples required but column %s had %s n=%d and %s n=%d samples.""".formatted(
          column.getTitle(), groupA, groupAData.getNumberOfSamples(), groupB,
          groupBData.getNumberOfSamples()));
    }

  }

  @Override
  public RowSignificanceTestResult test(FeatureListRow row) {
    final int rowIndex = groupAData.getFeatureIndex(row);
    final double[] groupAAbundance = groupAData.getFeatureData(rowIndex, false);
    final double[] groupBAbundance = groupBData.getFeatureData(rowIndex, false);

    try {
      final double p = test.checkAndTest(List.of(groupAAbundance, groupBAbundance));
      return new UnivariateRowSignificanceTestResult(row, column.getTitle(), p);
    } catch (Exception e) {
      // this should not happen after imputing missing values and providing a p value for every input
      throw new IllegalStateException(
          "Reached ANOVA error which should not happen after missing value imputation");
    }
  }

  public SignificanceTest getTest() {
    return test;
  }

  public MetadataColumn<T> column() {
    return column;
  }

  public T groupA() {
    return groupA;
  }

  public T groupB() {
    return groupB;
  }

  public List<RawDataFile> getGroupAFiles() {
    final MetadataTable metadata = ProjectService.getMetadata();
    return metadata.getMatchingFiles(column(), groupA());
  }

  public List<RawDataFile> getGroupBFiles() {
    final MetadataTable metadata = ProjectService.getMetadata();
    return metadata.getMatchingFiles(column(), groupB());
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    var that = (UnivariateRowSignificanceTest<?>) obj;
    return Objects.equals(this.test, that.test) && Objects.equals(this.column, that.column)
        && Objects.equals(this.groupA, that.groupA) && Objects.equals(this.groupB, that.groupB);
  }

  public UnivariateRowSignificanceTestConfig toConfiguration() {
    return new UnivariateRowSignificanceTestConfig(test, column().getTitle(), groupA().toString(),
        groupB().toString());
  }
}
