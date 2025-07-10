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

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.modules.dataanalysis.significance.ttest.TTestResult;
import io.github.mzmine.modules.dataanalysis.utils.StatisticUtils;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.parameters.parametertypes.statistics.StorableTTestConfiguration;
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
  private final List<RawDataFile> groupedFilesA;
  private final List<RawDataFile> groupedFilesB;

  public UnivariateRowSignificanceTest(@NotNull SignificanceTests test, MetadataColumn<T> column,
      T groupA, T groupB) {
    this.test = test;
    this.column = column;
    this.groupA = groupA;
    this.groupB = groupB;

    groupedFilesA = ProjectService.getMetadata().getMatchingFiles(column, this.groupA);
    groupedFilesB = ProjectService.getMetadata().getMatchingFiles(column, this.groupB);
  }

  @Override
  public RowSignificanceTestResult test(FeatureListRow row, AbundanceMeasure abundanceMeasure) {
    final double[] groupAAbundance = StatisticUtils.extractAbundance(row, groupedFilesA,
        abundanceMeasure);
    final double[] groupBAbundance = StatisticUtils.extractAbundance(row, groupedFilesB,
        abundanceMeasure);

    try {
      final double p = test.checkAndTest(List.of(groupAAbundance, groupBAbundance));
      return new TTestResult(row, column.getTitle(), p);
    } catch (Exception e) {
      // expected that test may fail if number of samples is too low
      // TODO make sure this does not happen after imputing missing values and providing a p value for every input
      return null;
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

  @Override
  public int hashCode() {
    return Objects.hash(test, column, groupA, groupB);
  }

  @Override
  public String toString() {
    return "StudentTTest{" + "samplingConfig=" + test + ", column=" + column + ", groupA=" + groupA
        + ", groupB=" + groupB + ", groupedFilesA=" + groupedFilesA + ", groupedFilesB="
        + groupedFilesB + '}';
  }

  public StorableTTestConfiguration toConfiguration() {
    return new StorableTTestConfiguration(test, column().getTitle(), groupA().toString(),
        groupB().toString());
  }
}
