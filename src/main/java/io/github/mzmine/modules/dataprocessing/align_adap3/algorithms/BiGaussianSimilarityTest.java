/*
 * Copyright 2006-2021 The MZmine Development Team
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
 */
package io.github.mzmine.modules.dataprocessing.align_adap3.algorithms;

import java.util.List;
import java.util.stream.IntStream;

import io.github.mzmine.modules.dataprocessing.align_adap3.algorithms.Peak3DTest.Result;
import io.github.mzmine.modules.dataprocessing.align_adap3.algorithms.SliceSparseMatrix.Triplet;

/**
 * <p>
 * BiGaussianSimilarityTest class is used for determining true or false peak by comparing BiGaussian
 * values with intensity values of given m/z and left and right bounds (variables leftBound and
 * rightBound).
 * </p>
 */
public class BiGaussianSimilarityTest {

  /**
   * <p>
   * execute method is used for testing a peak with given m/z-value (variable mz) and left and right
   * bounds (variables leftBound and rightBound). Peak is tested by comparing BiGaussian values with
   * intensity values from slice of sparse matrix.
   * </p>
   *
   * @param slice a {@link List} object. This is
   *        horizontal slice from sparse matrix of given m/z value.
   * @param leftBound a {@link Integer} object. This is lowest scan number from which peak
   *        determining starts.
   * @param rightBound a {@link Integer} object. This is highest scan number on which peak
   *        determining ends.
   * @param roundedMZ a {@link Double} object. It's rounded m/z value. Original m/z value
   *        multiplied by 10000.
   * @return a {@link Result} object. Result object contains similarity values, lower and upper mz
   *         boundaries for adjacent similar peaks.
   *         </p>
   * @param biGaussianSimilarityThreshold a double.
   */
  public boolean execute(List<Triplet> slice, int leftBound, int rightBound,
      int roundedMZ, double biGaussianSimilarityThreshold) {

    double[] referenceEIC = new double[rightBound - leftBound + 1];
    io.github.mzmine.modules.dataprocessing.align_adap3.algorithms.CurveTool.normalize(slice, leftBound, rightBound, roundedMZ, referenceEIC);

    try {
      io.github.mzmine.modules.dataprocessing.align_adap3.algorithms.BiGaussian objBiGaussian = new BiGaussian(slice, roundedMZ, leftBound, rightBound);
      double[] bigaussianValues = IntStream.range(0, referenceEIC.length)
          .mapToDouble(i -> objBiGaussian.getValue(leftBound + i)).toArray();

      double[] normBigaussianValues = new double[rightBound - leftBound + 1];
      io.github.mzmine.modules.dataprocessing.align_adap3.algorithms.CurveTool.normalize(bigaussianValues, normBigaussianValues);

      double similarityValue =
          CurveTool.similarityValue(referenceEIC, normBigaussianValues, leftBound, rightBound);

      return similarityValue > biGaussianSimilarityThreshold;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }
}
