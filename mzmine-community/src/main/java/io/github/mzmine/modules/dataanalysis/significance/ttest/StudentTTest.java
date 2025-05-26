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
import io.github.mzmine.modules.dataanalysis.significance.RowSignificanceTest;
import io.github.mzmine.modules.dataanalysis.significance.RowSignificanceTestResult;
import io.github.mzmine.modules.dataanalysis.utils.StatisticUtils;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.parameters.parametertypes.statistics.StorableTTestConfiguration;
import java.util.List;
import java.util.Objects;
import org.apache.commons.math3.stat.inference.TestUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @param <T> Type of the metadata column.
 */
public final class StudentTTest<T> implements RowSignificanceTest {

  private final TTestSamplingConfig samplingConfig;
  private final MetadataColumn<T> column;
  private final T groupA;
  private final T groupB;
  private final List<RawDataFile> groupedFilesA;
  private final List<RawDataFile> groupedFilesB;

  public StudentTTest(@NotNull TTestSamplingConfig samplingConfig, MetadataColumn<T> column,
      T groupA, T groupB) {
    this.samplingConfig = samplingConfig;
    this.column = column;
    this.groupA = groupA;
    this.groupB = groupB;

    groupedFilesA = MZmineCore.getProjectMetadata().getMatchingFiles(column, this.groupA);
    groupedFilesB = MZmineCore.getProjectMetadata().getMatchingFiles(column, this.groupB);
  }

  @Override
  public RowSignificanceTestResult test(FeatureListRow row, AbundanceMeasure abundanceMeasure) {
    final double[] groupAAbundance = StatisticUtils.extractAbundance(row, groupedFilesA,
        abundanceMeasure);
    final double[] groupBAbundance = StatisticUtils.extractAbundance(row, groupedFilesB,
        abundanceMeasure);

    if (!checkConditions(groupAAbundance, groupBAbundance)) {
      return null;
    }
    final double p = switch (samplingConfig) {
      case PAIRED -> TestUtils.pairedTTest(groupAAbundance, groupBAbundance);
      case UNPAIRED -> TestUtils.tTest(groupAAbundance, groupBAbundance);
    };
    return new TTestResult(row, column.getTitle(), p);
  }

  private boolean checkConditions(double[] abundancesA, double[] abundancesB) {
    switch (samplingConfig) {
      case PAIRED -> {
        // only perform paired test if the number of abundances is equal (pre/post treatment)
        if (abundancesA.length != abundancesB.length || abundancesA.length < 2) {
          return false;
        }
      }
      case UNPAIRED -> {
        if (abundancesA.length < 2 || abundancesB.length < 2) {
          return false;
        }
      }
    }
    return true;
  }

  public TTestSamplingConfig samplingConfig() {
    return samplingConfig;
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
    final MetadataTable metadata = MZmineCore.getProjectMetadata();
    return metadata.getMatchingFiles(column(), groupA());
  }

  public List<RawDataFile> getGroupBFiles() {
    final MetadataTable metadata = MZmineCore.getProjectMetadata();
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
    var that = (StudentTTest) obj;
    return Objects.equals(this.samplingConfig, that.samplingConfig) && Objects.equals(this.column,
        that.column) && Objects.equals(this.groupA, that.groupA) && Objects.equals(this.groupB,
        that.groupB);
  }

  @Override
  public int hashCode() {
    return Objects.hash(samplingConfig, column, groupA, groupB);
  }

  @Override
  public String toString() {
    return "StudentTTest{" + "samplingConfig=" + samplingConfig + ", column=" + column + ", groupA="
        + groupA + ", groupB=" + groupB + ", groupedFilesA=" + groupedFilesA + ", groupedFilesB="
        + groupedFilesB + '}';
  }

  public StorableTTestConfiguration toConfiguration() {
    return new StorableTTestConfiguration(samplingConfig(), column().getTitle(),
        groupA().toString(), groupB().toString());
  }
}
