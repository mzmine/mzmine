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

import java.lang.Math;
import java.util.ArrayList;
import java.util.List;

import io.github.mzmine.modules.dataprocessing.align_adap3.algorithms.SliceSparseMatrix.Triplet;

/**
 * <p>
 * Peak3DTest is used for determining true or false peak by comparing adjacent m/z-slices in profile
 * data.
 * </p>
 */
public class Peak3DTest {

  /**
   * <p>
   * Result class is used for returning lower and upper mz bound,boolean good peak value and list of
   * similarity value. Object of this class will return lowest mz boundary and highest mz boundary
   * of adjacent similar peaks for given mz value. Here lowerMzBound and upperMzBound are integer
   * because for sparse matrix we've rounded the mz value by multiplying 10000. It will also return
   * if the peak is good or not for given mz value.
   * </p>
   */
  public static class Result {
    List<Double> similarityValues;
    boolean goodPeak;
    int lowerMzBound;
    int upperMzBound;
  }

  enum Direction {
    UP, DOWN
  }

  /** Constant <code>EPSILON=1E-8</code> */
  public static final double EPSILON = 1E-8;

  /* Instance of SliceSparseMatrix containing the profile data */
  private final SliceSparseMatrix objsliceSparseMatrix;

  /* Full-Width Half-Max of m/z-profiles */
  private final int roundedFWHM;

  /**
   * <p>Constructor for Peak3DTest.</p>
   *
   * @param objsliceSparseMatrix a {@link SliceSparseMatrix} object.
   * @param fwhm a int.
   */
  public Peak3DTest(SliceSparseMatrix objsliceSparseMatrix, int fwhm) {
    this.objsliceSparseMatrix = objsliceSparseMatrix;
    this.roundedFWHM = fwhm;
  }

  /**
   * <p>Constructor for Peak3DTest.</p>
   */
  public Peak3DTest() {
    super();
    this.objsliceSparseMatrix = null;
    this.roundedFWHM = 0;
  }

  /**
   * <p>
   * execute method is used for testing a peak with given m/z-value (variable mz) and left and right
   * bounds (variables leftBound and rightBound).
   *
   * Peak is tested by comparing similarity between adjacent m/z slices.
   *
   * Let mzValues be a sorted list of all m/z values in the profile data. Let index be an integer
   * such that
   *
   * mzValues[index] == mz
   *
   * We find similarities between the EIC corresponding to m/z value mzValues[index] and adjacent
   * EICs corresponding to
   *
   * ..., mzValues[index-2], mzValues[index-1], mzValues[index+1], mzValues[index+2], ...
   *
   * as long as those similarities are higher than the similarity threshold. First, we check each
   * m/z-value higher than mzValues[index], stop when the current similarity becomes lower than the
   * similarity threshold, and save the last m/z-value (variable upperMZbound). Next, we check each
   * m/z-value lower than mzValues[index], stop when the current similarity becomes lower than the
   * similarity threshold, and save the last m/z-value (variable lowerMZbound)
   *
   * Peak is considered to be good if the differences upperMZbound - mzValues[index],
   * mzValues[index] - lowerMZbound, upperMZbound - lowerMZbound exceed certain thresholds, which
   * depend on FWHM-value.
   *
   * @param roundedMz a {@link Double} object. It's rounded m/z value. Original m/z value
   *        multiplied by 10000.
   * @param leftBound a {@link Integer} object. This is lowest scan number from which peak
   *        determining starts.
   * @param rightBound a {@link Integer} object. This is highest scan number on which peak
   *        determining ends.
   * @return a {@link Result} object. Result object contains similarity values, lower and upper mz
   *         boundaries for adjacent similar peaks.
   *         </p>
   * @param peakSimilarityThreshold a double.
   */
  public Result execute(int roundedMz, int leftBound, int rightBound,
      double peakSimilarityThreshold) {

    // Here I'm rounding Full width half max(fwhm) and mz value by factor of roundMZfactor.
    // For instance, roundedMz = (int) mz * 10000
    // int roundedFWHM = objsliceSparseMatrix.roundMZ(fwhm);

    // slice is used to store horizontal row from sparse matrix for given mz, left boundary and
    // right boundary.
    // left boundary and right boundary are used in form of scan numbers.
    List<Triplet> slice =
        (List<Triplet>) objsliceSparseMatrix.getHorizontalSlice(roundedMz, leftBound, rightBound);

    // referenceEIC is used for storing normalized intensities for m/z-value equal to mz.
    double[] referenceEIC = new double[rightBound - leftBound + 1];
    // normalize method is used for normalizing intensities for given mz value. It updates
    // referenceEIC.
    CurveTool.normalize(slice, leftBound, rightBound, roundedMz, referenceEIC);


    // We save all similarity values to this list
    List<Double> similarityValues = new ArrayList<Double>();

    // mzIndex is index of given mz from the sorted list of all mz values from raw file.
    int mzIndex = objsliceSparseMatrix.mzValues.indexOf(roundedMz);

    // Here we're getting highest mz value for which the EIC is similar to given mz value.
    int upperMzBound = findMZbound(leftBound, rightBound, roundedMz, roundedFWHM, mzIndex,
        referenceEIC, similarityValues, peakSimilarityThreshold, Direction.UP);

    // Here we're getting lowest mz value for which the EIC is similar to given mz value.
    int lowerMzBound = findMZbound(leftBound, rightBound, roundedMz, roundedFWHM, mzIndex,
        referenceEIC, similarityValues, peakSimilarityThreshold, Direction.DOWN);

    // Assigning values to object.
    Result objResult = new Result();
    objResult.similarityValues = similarityValues;
    objResult.lowerMzBound = lowerMzBound;
    objResult.upperMzBound = upperMzBound;

    int lowerBoundaryDiff = roundedMz - lowerMzBound;
    int upperBoundaryDiff = upperMzBound - roundedMz;

    // This is the condition for determing whether the peak is good or not.
    if ((upperBoundaryDiff >= roundedFWHM / 2) && (lowerBoundaryDiff >= roundedFWHM / 2)
        && (upperBoundaryDiff + lowerBoundaryDiff >= roundedFWHM)) {
      objResult.goodPeak = true;
    } else {
      objResult.goodPeak = false;
    }

    return objResult;

  }

  /**
   * <p>
   * findMZbound method is used to compare adjacent EICs to the reference EIC and return the last
   * m/z value such that its corresponding EIC is similar to the reference EIC.
   *
   * The similarity is calculated between two EICs in three steps: 1. Normalize EIC 2. Find area of
   * the difference of the normalized EICs (variable diffArea) 3. Calculate similarity by the
   * formula
   *
   * similarity = height * (exp(-diffArea / factor) - shift)
   *
   * @param roundedMz a {@link Integer} object. This is m/z value which is multiplied by
   *        10000 because of it's use in sparse matrix.
   * @param leftBound a {@link Integer} object. This is lowest scan number from which peak
   *        determining starts.
   * @param rightBound a {@link Integer} object. This is highest scan number on which peak
   *        determining ends.
   * @param roundedFWHM a {@link Double} object. fwhm is also multiplied by 10000 as m/z
   *        is multiplied by same.
   * @param mzIndex a {@link Integer} object. This is the index of given m/z value in
   *        sorted list of all m/z values.
   * @param referenceEIC a {@link Double} array. This array contains normalize intensities
   *        for given m/z value.(Intensities/area)
   * @param similarityValues a {@link Double} empty list. This empty list stores
   *        similarity values.
   * @param direction a {@link Enum} object. This enum provides direction whether to call function
   *        for m/z values greater than given m/z or less than given m/z.
   * 
   * @return curMZ a {@link Double} object. This is m/z value greater or less than given
   *         m/z value which is used for finding similar peaks.
   *         </p>
   */
  private int findMZbound(int leftBound, int rightBound, int roundedMz, double roundedFWHM,
      int mzIndex, double[] referenceEIC, List<Double> similarityValues,
      double peakSimilarityThreshold, Direction direction) {


    final int multiplier = direction == Direction.UP ? 1 : -1;
    final int arrayCount = rightBound - leftBound + 1;

    Integer curMZ = null;
    Integer lastGoodMZ = null;


    int curMzIndex = 0;

    double curSimilarity = 1.0;
    int curInc = 0;

    while (curSimilarity > peakSimilarityThreshold) {

      curInc += 1;

      // This condition is used to determine whether we're finding similar peaks for mz values lower
      // or upper.
      // than given mz value.curMzIndex maintains index of cur mz in sorted mz value list.
      curMzIndex = mzIndex + curInc * multiplier;

      // This condition checks whether we've mz values above or below given mz value.

      curMZ = objsliceSparseMatrix.mzValues.get(curMzIndex);
//      mzBound = objsliceSparseMatrix.mzValues.get(curMzIndex - multiplier);

      if (curMZ == null || Math.abs(curMZ - roundedMz) >= 2 * roundedFWHM)
        break;

      // //for getting slice of sparse matrix we need to provide original mz values which are there
      // in raw file.
      // double originalCurMZ = (double) curMZ / objsliceSparseMatrix.roundMzFactor;
      // curEIC will store normalized intensities for adjacent mz values.
      double[] curEIC = new double[arrayCount];

      // Here current horizontal slice from sparse matrix is stored adjacent mz value.
      List<Triplet> curSlice =
          (List<Triplet>) objsliceSparseMatrix.getHorizontalSlice(curMZ, leftBound, rightBound);
      double area = CurveTool.normalize(curSlice, leftBound, rightBound, curMZ, curEIC);

      // if area is too small continue.
      if (area < EPSILON)
        continue;

      // Here similarity value is calculated.
      curSimilarity = CurveTool.similarityValue(referenceEIC, curEIC, leftBound, rightBound);

      if (curSimilarity > peakSimilarityThreshold) {
        similarityValues.add(curSimilarity);
        lastGoodMZ = curMZ;
      }
    }

    lastGoodMZ = lastGoodMZ == null ? roundedMz : lastGoodMZ;
    return lastGoodMZ;
  }
}
