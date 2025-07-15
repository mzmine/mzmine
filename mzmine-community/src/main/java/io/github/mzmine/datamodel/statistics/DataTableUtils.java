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
import java.util.Map;
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
   * Extracts a double array for a subset of samples
   *
   * @param dataTable        the actual data
   * @param dataFileIndexMap a map derived from {@link FeaturesDataTable#getDataFileIndexMap()} to
   *                         quickly lookup data file index
   * @param featureIndex     the row index
   * @param subset           defines the target data
   * @return a value array for the subset
   */
  public static double[] extractRowData(FeaturesDataTable dataTable,
      Map<RawDataFile, Integer> dataFileIndexMap, int featureIndex, List<RawDataFile> subset) {
    final double[] data = new double[subset.size()];
    for (int i = 0; i < subset.size(); i++) {
      final int sampleIndex = dataFileIndexMap.get(subset.get(i));
      data[i] = dataTable.getValue(featureIndex, sampleIndex);
    }
    return data;
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
}
