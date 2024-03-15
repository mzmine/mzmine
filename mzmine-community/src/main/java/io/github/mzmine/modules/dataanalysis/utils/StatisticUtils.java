/*
 * Copyright 2006-2022 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *
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

  public static final Function<RealVector, Double> oneFifthOfMinimumImputer = realVector -> {
    final double minValue = realVector.getMinValue();
    return minValue * 1 / 5;
  };

  public static final Function<RealVector, Double> zeroImputer = _ -> 0d;

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
  public static RealMatrix performMeanCenter(RealMatrix data, boolean inPlace) {

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
   * Scales the values in every column to be between 0-1. To be used before centering the matrix.
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
    return performMeanCenter(scale(data, scaling, inPlace), inPlace);
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
