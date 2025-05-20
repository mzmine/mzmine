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

package io.github.mzmine.modules.dataanalysis.utils;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.modules.dataanalysis.significance.RowSignificanceTestResult;
import io.github.mzmine.modules.dataanalysis.utils.scaling.ScalingFunction;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import org.apache.commons.math.util.MathUtils;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

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

  /**
   * Calculates the log2 transformed fold change between two groups.
   */
  public static double calculateLog2FoldChange(List<RawDataFile> groupAFiles,
      List<RawDataFile> groupBFiles, AbundanceMeasure abundanceMeasure,
      RowSignificanceTestResult result) {
    final double[] ab1 = StatisticUtils.extractAbundance(result.row(), groupAFiles,
        abundanceMeasure);
    final double[] abB = StatisticUtils.extractAbundance(result.row(), groupBFiles,
        abundanceMeasure);
    return MathUtils.log(2,
        Arrays.stream(ab1).average().getAsDouble() / Arrays.stream(abB).average().getAsDouble());
  }

  /**
   * Performs mean centering on the data. Values may only be positive.
   */
  public static RealMatrix center(RealMatrix data, boolean inPlace) {

    RealMatrix result = inPlace ? data
        : new Array2DRowRealMatrix(data.getRowDimension(), data.getColumnDimension());

    for (int col = 0; col < data.getColumnDimension(); col++) {
      final RealVector columnVector = data.getColumnVector(col);
//      double sum = 0;
//      for (int row = 0; row < columnVector.getDimension(); row++) {
//        sum += columnVector.getEntry(row);
//      }
      final double sum = columnVector.getL1Norm();
      final double mean = sum / columnVector.getDimension();

      var resultVector = result.getColumnVector(col);
      resultVector = columnVector.mapSubtract(mean);
      result.setColumnVector(col, resultVector);
    }
    return result;
  }

  /**
   * Scales the values in every column. To be used before centering the matrix.
   */
  public static RealMatrix scale(RealMatrix data, ScalingFunction scaling, boolean inPlace) {
    final RealMatrix result = inPlace ? data
        : new Array2DRowRealMatrix(data.getRowDimension(), data.getColumnDimension());

    for (int colIndex = 0; colIndex < data.getColumnDimension(); colIndex++) {
      final RealVector columnVector = data.getColumnVector(colIndex);
      final RealVector resultVector = scaling.apply(columnVector);
      result.setColumnVector(colIndex, resultVector);
    }
    return result;
  }

  public static RealMatrix scaleAndCenter(RealMatrix data, ScalingFunction scaling,
      boolean inPlace) {
    return center(scale(data, scaling, inPlace), inPlace);
  }

  public static RealMatrix imputeMissingValues(RealMatrix data, boolean inPlace,
      Function<RealVector, Double> imputationFunction) {
    RealMatrix result = inPlace ? data : data.copy();

    for (int columnIndex = 0; columnIndex < result.getColumnDimension(); columnIndex++) {
      final RealVector columnVector = result.getColumnVector(columnIndex);
      final double imputedValue = imputationFunction.apply(columnVector);
      for (int i = 0; i < columnVector.getDimension(); i++) {
        final double entry = columnVector.getEntry(i);
        if (Double.isNaN(entry)) {
          columnVector.setEntry(i, imputedValue);
        }
      }
      result.setColumnVector(columnIndex, columnVector);
    }
    return result;
  }

  public static RealMatrix createDatasetFromRows(List<FeatureListRow> rows,
      List<RawDataFile> allFiles, AbundanceMeasure measure) {

    // colums = features, rows = raw files
    //        f1  f2  f3  f4
    // file1  5   0   2   3
    // file2  2   2   1   1
    // file3  3   4   4   5

    final RealMatrix data = new Array2DRowRealMatrix(allFiles.size(), rows.size());

    for (int fileIndex = 0; fileIndex < allFiles.size(); fileIndex++) {
      final RawDataFile file = allFiles.get(fileIndex);
      for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
        final FeatureListRow row = rows.get(rowIndex);
        final Feature feature = row.getFeature(file);

        final double abundance;
        if (feature != null) {
          abundance = measure.getOrNaN((ModularDataModel) feature);
        } else {
          abundance = Double.NaN;
        }
        data.setEntry(fileIndex, rowIndex, abundance);
      }
    }

    return data;
  }
}
