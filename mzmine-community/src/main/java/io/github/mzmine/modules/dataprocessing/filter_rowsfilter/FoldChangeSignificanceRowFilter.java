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

package io.github.mzmine.modules.dataprocessing.filter_rowsfilter;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.modules.dataanalysis.significance.SignificanceTests;
import io.github.mzmine.modules.dataanalysis.significance.UnivariateRowSignificanceTest;
import io.github.mzmine.modules.dataanalysis.utils.StatisticUtils;
import io.github.mzmine.parameters.parametertypes.metadata.Metadata2GroupsSelection;
import io.github.mzmine.parameters.parametertypes.statistics.StorableTTestConfiguration;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/**
 * Filter rows based on fold-change, uses log2FC for equal scaling of up and down regulation as 1
 * and -1 represent 2x up and 2x down regulation. Filter rows based on t-test statistics if greater
 * than p-value.
 */
public final class FoldChangeSignificanceRowFilter {

  private final @NotNull Metadata2GroupsSelection grouping;
  private final double maxValueP;
  private final double minLog2FoldChange;
  private final FoldChangeFilterSides foldChangeFilterSides;
  private final @NotNull AbundanceMeasure abundanceMeasure;
  private final SignificanceTests test;
  private final UnivariateRowSignificanceTest<Object> rowTest;
  private final boolean applySignificancePFilter;
  private final boolean applyFoldChangeFilter;
  private final List<RawDataFile> groupA;
  private final List<RawDataFile> groupB;

  /**
   * @param rawFiles              all raw data files in a feature list to use as groups. grouping
   *                              defines the groups
   * @param grouping              column and two groups
   * @param maxValueP             maxmimum p value. use <=0 to deactivate
   * @param minLog2FoldChange     use 0 to deactivate, either signed or absolute value
   * @param foldChangeFilterSides FC may be applied to Math.abs(value) to filter both up and down
   *                              regulation or as a signed filter to only retain one side.
   */
  public FoldChangeSignificanceRowFilter(List<RawDataFile> rawFiles,
      @NotNull Metadata2GroupsSelection grouping, double maxValueP, double minLog2FoldChange,
      FoldChangeFilterSides foldChangeFilterSides, @NotNull AbundanceMeasure abundanceMeasure,
      SignificanceTests test) {
    this.grouping = grouping;
    this.groupA = grouping.getMatchingFilesA(rawFiles);
    this.groupB = grouping.getMatchingFilesB(rawFiles);

    // need at least 2 values per group
    if (groupA.size() < 2 || groupB.size() < 2) {
      throw new IllegalArgumentException(
          "Groups require at least 2 samples for univariate filters by fold-change or t-test. Grouping: %s and groupA n=%d, groupB n=%d".formatted(
              grouping, groupA.size(), groupB.size()));
    }

    this.maxValueP = maxValueP;
    this.minLog2FoldChange = minLog2FoldChange;
    this.foldChangeFilterSides = foldChangeFilterSides;
    this.abundanceMeasure = abundanceMeasure;
    this.test = test;
    rowTest = new StorableTTestConfiguration(test, grouping().columnName(), grouping.groupA(),
        grouping.groupB()).toValidConfig();
    if (rowTest == null) {
      throw new IllegalArgumentException(
          "Invalid group selection for univariate test: " + grouping);
    }
    this.applyFoldChangeFilter = Double.compare(minLog2FoldChange, 0d) != 0;
    this.applySignificancePFilter = Double.compare(maxValueP, 0d) != 0;
  }

  public boolean applyFoldChangeFilter() {
    return applyFoldChangeFilter;
  }

  public boolean applySignificancePFilter() {
    return applySignificancePFilter;
  }


  public boolean matches(FeatureListRow row) {
    // TODO check that 0-values are handled so that every sample in each group has a value
    final double[] aValues = StatisticUtils.extractAbundance(row, groupA, abundanceMeasure);
    final double[] bValues = StatisticUtils.extractAbundance(row, groupB, abundanceMeasure);

    if (applySignificancePFilter) {
      final double p = rowTest.getTest().test(List.of(aValues, bValues));
      if (p > maxValueP) {
        return false;
      }
    }

    if (applyFoldChangeFilter) {
      final double log2FC = StatisticUtils.calculateLog2FoldChange(aValues, bValues);
      if (foldChangeFilterSides == FoldChangeFilterSides.ABS_TWO_SIDED
          && Math.abs(log2FC) < minLog2FoldChange) {
        return false;
      } else {
        // negative side then positive side
        if ((minLog2FoldChange < 0 && log2FC > minLog2FoldChange) || //
            (minLog2FoldChange > 0 && log2FC < minLog2FoldChange)) {
          return false;
        }
      }
    }

    return true;
  }

  public @NotNull Metadata2GroupsSelection grouping() {
    return grouping;
  }

  public double maxValueP() {
    return maxValueP;
  }

  public double minLog2FoldChange() {
    return minLog2FoldChange;
  }

  public FoldChangeFilterSides useAbsoluteFoldChange() {
    return foldChangeFilterSides;
  }

  public @NotNull AbundanceMeasure abundanceMeasure() {
    return abundanceMeasure;
  }

  public SignificanceTests test() {
    return test;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    var that = (FoldChangeSignificanceRowFilter) obj;
    return Objects.equals(this.grouping, that.grouping)
        && Double.doubleToLongBits(this.maxValueP) == Double.doubleToLongBits(that.maxValueP)
        && Double.doubleToLongBits(this.minLog2FoldChange) == Double.doubleToLongBits(
        that.minLog2FoldChange) && Objects.equals(this.foldChangeFilterSides,
        that.foldChangeFilterSides) && Objects.equals(this.abundanceMeasure, that.abundanceMeasure)
        && Objects.equals(this.test, that.test);
  }

  @Override
  public int hashCode() {
    return Objects.hash(grouping, maxValueP, minLog2FoldChange, foldChangeFilterSides,
        abundanceMeasure, test);
  }

  @Override
  public String toString() {
    return "FoldChangeSignificanceRowFilter[" + "grouping=" + grouping + ", " + "maxValueP="
        + maxValueP + ", " + "minFoldChangeFactor=" + minLog2FoldChange + ", "
        + "useAbsoluteFoldChange=" + foldChangeFilterSides + ", " + "abundanceMeasure="
        + abundanceMeasure + ", " + "test=" + test + ']';
  }

}
