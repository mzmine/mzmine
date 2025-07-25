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

package io.github.mzmine.datamodel.statistics;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.modules.visualization.projectmetadata.SampleTypeFilter;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalDouble;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.jetbrains.annotations.NotNull;

public class DataTableUtils {


  public static OptionalDouble getMinimum(double[] feature, boolean excludeZero) {
    double min = Double.MAX_VALUE;
    for (double value : feature) {
      if (Double.isNaN(value) || (excludeZero && Double.compare(0d, value) == 0)) {
        continue;
      }
      if (value < min) {
        min = value;
      }
    }
    if (Double.compare(Double.MAX_VALUE, min) == 0) {
      return OptionalDouble.empty();
    }
    return OptionalDouble.of(min);
  }

  public static OptionalDouble getMinimum(DataTable data, boolean excludeZero) {
    double min = Double.MAX_VALUE;
    for (double[] feature : data) {
      for (double value : feature) {
        if (Double.isNaN(value) || (excludeZero && Double.compare(0d, value) == 0)) {
          continue;
        }
        if (value < min) {
          min = value;
        }
      }
    }
    if (Double.compare(Double.MAX_VALUE, min) == 0) {
      return OptionalDouble.empty();
    }
    return OptionalDouble.of(min);
  }

  public static OptionalDouble getMaximum(DataTable data) {
    double max = Double.NEGATIVE_INFINITY;
    for (double[] feature : data) {
      for (double value : feature) {
        if (Double.isNaN(value)) {
          continue;
        }
        if (value > max) {
          max = value;
        }
      }
    }
    if (Double.compare(Double.NEGATIVE_INFINITY, max) == 0) {
      return OptionalDouble.empty();
    }
    return OptionalDouble.of(max);
  }


  /**
   * replace all NaN and optionally 0 values with an imputed value
   *
   * @param data         the data table
   * @param featureIndex the row index
   * @param imputedValue the replacement value
   * @param replaceZero  also replace 0 values
   */
  public static void replaceNaN(DataTable data, int featureIndex, double imputedValue,
      boolean replaceZero) {
    for (int sampleIndex = 0; sampleIndex < data.getNumberOfSamples(); sampleIndex++) {
      final double value = data.getValue(featureIndex, sampleIndex);
      if (Double.isNaN(value) || (replaceZero && Double.compare(value, 0d) == 0)) {
        data.setValue(featureIndex, sampleIndex, imputedValue);
      }
    }
  }

  /**
   * colums = features, rows = raw files
   * <pre>
   *        f1  f2  f3  f4
   * file1  5   0   2   3
   * file2  2   2   1   1
   * file3  3   4   4   5
   * </pre>
   *
   * @param dataTable table to convert
   * @return 2D matrix
   */
  public static RealMatrix createRealMatrix(FeaturesDataTable dataTable) {

    if (dataTable.getNumberOfFeatures() < dataTable.getNumberOfSamples()) {
      throw new IllegalStateException(
          "Cannot perform PCA on a dataset with less rows/features than samples.");
    }

    final RealMatrix data = new Array2DRowRealMatrix(dataTable.getNumberOfSamples(),
        dataTable.getNumberOfFeatures());

    for (int rowIndex = 0; rowIndex < dataTable.getNumberOfFeatures(); rowIndex++) {
      data.setColumn(rowIndex, dataTable.getFeatureData(rowIndex, true));
    }

    return data;
  }

  /**
   * Applies sorting to the rows in a data table
   *
   * @param data   the table to be sorted
   * @param sorter sorter for rows
   * @return a new sorted data table
   */
  public static FeaturesDataTable createSortedCopy(FeaturesDataTable data,
      Comparator<FeatureListRow> sorter) {
    final FeatureListRowAbundances[] sortedData = data.streamDataRows()
        .sorted((a, b) -> sorter.compare(a.row(), b.row()))
        .toArray(FeatureListRowAbundances[]::new);
    return data.copyWithNewRows(sortedData);
  }

  /**
   * @return a new table with only matching raw files or null if no samples
   */
  public static @NotNull FeaturesDataTable applySampleFilter(@NotNull FeaturesDataTable data,
      @NotNull SampleTypeFilter sampleFilter) {
    final List<RawDataFile> files = data.getRawDataFiles().stream().filter(sampleFilter::matches)
        .toList();

    if (files.isEmpty()) {
      return FeaturesDataTable.EMPTY;
    }

    // only for specific samples
    return data.subsetBySamples(files);
  }

  /**
   * @param table  the data table
   * @param row    a single row to extract data from
   * @param groups list of groups to subset the row data into groups
   * @return list of groups' data arrays for a single row.  Each data array represents a group of
   * abundances of the row in a data files group.
   */
  public static List<double[]> extractGroupsRowData(FeaturesDataTable table, FeatureListRow row,
      List<List<RawDataFile>> groups) {
    return groups.stream().map(group -> DataTableUtils.extractRowData(table, row, group)).toList();
  }

  /**
   * Extracts a double array for a subset of samples
   *
   * @param table  the actual data
   * @param row    the row
   * @param subset defines the target data
   * @return a value array for the subset
   */
  public static double[] extractRowData(FeaturesDataTable table, FeatureListRow row,
      List<RawDataFile> subset) {
    return extractRowData(table, table.getFeatureIndex(row), subset);
  }

  /**
   * Extracts a double array for a subset of samples
   *
   * @param dataTable    the actual data
   * @param featureIndex the row index
   * @param subset       defines the target data
   * @return a value array for the subset
   */
  public static double[] extractRowData(FeaturesDataTable dataTable, int featureIndex,
      List<RawDataFile> subset) {
    final double[] data = new double[subset.size()];
    for (int i = 0; i < subset.size(); i++) {
      final int sampleIndex = dataTable.getSampleIndex(subset.get(i));
      data[i] = dataTable.getValue(featureIndex, sampleIndex);
    }
    return data;
  }

  public static <T extends DataTable> void fillFeatureData(T data, int featureIndex, double value) {
    for (int sampleIndex = 0; sampleIndex < data.getNumberOfSamples(); sampleIndex++) {
      data.setValue(featureIndex, sampleIndex, value);
    }
  }

  public static <T extends DataTable> void fillSampleData(T data, int sampleIndex, double value) {
    for (int rowIndex = 0; rowIndex < data.getNumberOfSamples(); rowIndex++) {
      data.setValue(rowIndex, sampleIndex, value);
    }
  }

}
