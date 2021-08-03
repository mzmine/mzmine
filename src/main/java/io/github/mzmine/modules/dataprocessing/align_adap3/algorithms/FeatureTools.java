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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NavigableMap;
import org.apache.commons.lang3.ArrayUtils;



/**
 * <p>
 * FeatureTools class.
 * </p>
 *
 * @author owen myers Modified by Dharak Shah to include in MSDK
 */
public class FeatureTools {
  /**
   * <p>
   * fixRightBoundry.
   * </p>
   *
   * @param intensities an array of double.
   * @param peakRight a int.
   * @return a int.
   */
  public static int fixRightBoundry(double[] intensities, int peakRight) {
    boolean foundLocalMin = false;
    int curRight = peakRight;
    int bestRight = peakRight;


    while (!foundLocalMin) {
      int checkRightPoint = curRight + 1;

      int checkLeftPoint = curRight - 1;
      if (checkLeftPoint <= 0) {
        return peakRight;
      }


      if (checkRightPoint >= intensities.length) {
        bestRight = curRight;
        foundLocalMin = true;
      } else if (intensities[curRight] < 1.0) {
        bestRight = curRight;
        foundLocalMin = true;
      } else if ((intensities[checkRightPoint] >= intensities[curRight])
          && (intensities[checkLeftPoint] >= intensities[curRight])) {
        bestRight = curRight;
        foundLocalMin = true;
      } else if (intensities[checkRightPoint] <= intensities[curRight]) {
        curRight++;
      } else if (intensities[checkLeftPoint] < intensities[curRight]) {
        curRight--;
      } else {
        // throw new InvalidFeatureException("Problem fixing right boundry");
        return -1;
      }

    }
    return bestRight;
  }

  /**
   * <p>
   * fixLeftBoundry.
   * </p>
   *
   * @param intensities an array of double.
   * @param peakLeft a int.
   * @return a int.
   */
  public static int fixLeftBoundry(double[] intensities, int peakLeft) {
    boolean foundLocalMin = false;
    int curLeft = peakLeft;
    int bestLeft = peakLeft;

    while (!foundLocalMin) {
      int checkLeftPoint = curLeft - 1;

      int checkRightPoint = curLeft + 1;
      if (checkRightPoint >= intensities.length) {
        return peakLeft;
      }

      if (checkLeftPoint < 0) {
        bestLeft = curLeft;
        foundLocalMin = true;
      } else if (intensities[curLeft] < 1.0) {
        bestLeft = curLeft;
        foundLocalMin = true;
      }

      else if ((intensities[checkRightPoint] >= intensities[curLeft])
          && (intensities[checkLeftPoint] >= intensities[curLeft])) {
        bestLeft = curLeft;
        foundLocalMin = true;
      } else if (intensities[checkLeftPoint] <= intensities[curLeft]) {
        curLeft--;
      } else if (intensities[checkRightPoint] < intensities[curLeft]) {
        curLeft++;
      } else {
        // throw new InvalidFeatureException("Problem fixing left boundry");
        return -1;
      }

    }
    return bestLeft;

  }

  /**
   * <p>
   * isShared.
   * </p>
   *
   * @param rt an array of double.
   * @param intensities an array of double.
   * @param peakLeft a int.
   * @param peakRight a int.
   * @param edgeToHeightRatio a double.
   * @param deltaToHeightRatio a double.
   * @return a boolean.
   */
  public static boolean isShared(double[] rt, double[] intensities, int peakLeft, int peakRight,
      double edgeToHeightRatio, double deltaToHeightRatio) {
    // double BHR = edgeToHeightRatio; // Boundry Height Ratio.
    // double edgeHightDiffRatio=deltaToHeightRatio;


    double peakHeight = 0.0;

    for (int i = peakLeft; i < (peakRight + 1); i++) {
      double curInt = intensities[i];
      if (curInt > peakHeight) {
        peakHeight = curInt;
      }
    }
    assert (peakHeight > 0.0);

    double leftInt = intensities[peakLeft];
    double rightInt = intensities[peakRight];

    double leftToApexRatio = leftInt / peakHeight;
    double rightToApexRatio = rightInt / peakHeight;

    double boundToApexRatio = java.lang.Math.abs(leftInt - rightInt) / peakHeight;


    boolean goodPeak = false;
    if ((leftToApexRatio < edgeToHeightRatio) && (rightToApexRatio < edgeToHeightRatio)
        && (boundToApexRatio < deltaToHeightRatio)) {
      goodPeak = true;
    }
    boolean meargeLeft = false;
    if ((!goodPeak) && (leftToApexRatio >= rightToApexRatio)) {
      meargeLeft = true;
    }

    boolean meargeRight = false;
    if ((!goodPeak) && (leftToApexRatio < rightToApexRatio)) {
      meargeLeft = true;
    }

    if (meargeRight || meargeLeft) {
      return true;
    } else {
      return false;
    }

  }

  /**
   * Return true if 1) there are more then one local maximum 2) at least one of left-to-apex,
   * right-to-apex, or delta-to-apex ratios is higher then the corresponding threshold
   *
   * @param intensities list of peak intensities
   * @param edgeToHeightThreshold threshold for left-to-apex and right-to-apex ratios
   * @param deltaToHeightThreshold threshold for delta-to-apex ratio
   * @return true one of the conditions 1) or 2) is satisfied, false otherwise
   */
  public static boolean isShared(List<Double> intensities, double edgeToHeightThreshold,
      double deltaToHeightThreshold) {
    final int size = intensities.size();
    final double leftIntensity = intensities.get(0);
    final double rightIntensity = intensities.get(size - 1);

    double absoluteMaximum = Double.max(leftIntensity, rightIntensity);
    int localMaximaCount = 0;
    int index = 1;

    while (index < size - 1) {
      final double currentValue = intensities.get(index);

      if (currentValue > absoluteMaximum)
        absoluteMaximum = currentValue;

      int prevIndex = index - 1;
      int nextIndex = index + 1;

      while (nextIndex + 1 < size && currentValue == intensities.get(nextIndex))
        ++nextIndex;

      if (intensities.get(prevIndex) < currentValue && currentValue > intensities.get(nextIndex))
        ++localMaximaCount;

      index = nextIndex;
    }

    if (localMaximaCount > 1)
      return true;

    final double leftToApexRatio = leftIntensity / absoluteMaximum;
    final double rightToApexRatio = rightIntensity / absoluteMaximum;
    final double deltaToApexRatio =
        java.lang.Math.abs(leftIntensity - rightIntensity) / absoluteMaximum;

    return leftToApexRatio >= edgeToHeightThreshold || rightToApexRatio >= edgeToHeightThreshold
        || deltaToApexRatio >= deltaToHeightThreshold;
  }



  /**
   * <p>
   * sharpnessAngleAvgSlopes.This returns the angle calculated between the two "means" of the slopes
   * on each side of the peak
   * </p>
   *
   * @param rt an array of double.
   * @param intensities an array of double.
   * @param peakLeft a int.
   * @param peakRight a int.
   * @return a double.
   */
  public static double sharpnessAngleAvgSlopes(double[] rt, double[] intensities, int peakLeft,
      int peakRight) {

    int peakWidth = peakRight - peakLeft;
    int halfPeakWidthRoundUp = (int) java.lang.Math.round((double) peakWidth / 2.0);

    // first get the value of the peak
    int peakIndex = 0;
    double peakHeight = 0.0;

    for (int i = peakLeft; i < (peakRight + 1); i++) {
      double curInt = intensities[i];
      if (curInt > peakHeight) {
        peakHeight = curInt;
        peakIndex = i;
      }
    }
    assert (peakHeight > 0.0);

    // double peakRT = rt[peakIndex];
    double peakX = (double) peakIndex;
    // We are going to ceck out beyond the peak bounds by half the peak width just becuase it is
    // possible
    // for the bounds to end at the actual peak.

    // Go to the left.
    // int finalLeft = peakIndex - (peakWidth);
    int finalLeft = peakLeft - halfPeakWidthRoundUp;
    if (finalLeft < 0) {
      finalLeft = 0;
    }
    double sumOfLeftSlope = 0.0;
    int numLeft = 0;
    for (int i = peakIndex - 1; i >= finalLeft; i--) {

      // double curX = rt[i];
      double curX = (double) i;
      double curY = intensities[i];
      // double curSlope = (peakHeight-curY)/(peakRT-curX);
      double curSlope = (peakHeight - curY) / (peakX - curX);
      sumOfLeftSlope += curSlope;
      numLeft++;
    }
    double meanLeftSlope = sumOfLeftSlope / ((double) numLeft);
    double rad_left_theta = java.lang.Math.atan(1.0 / java.lang.Math.abs(meanLeftSlope));



    // Go to the right
    // int finalRight = peakIndex + (peakWidth);
    int finalRight = peakRight + halfPeakWidthRoundUp;
    if (finalRight >= intensities.length) {
      finalRight = intensities.length - 1;
    }
    double sumOfRightSlope = 0.0;
    int numRight = 0;
    for (int i = peakIndex + 1; i <= finalRight; i++) {

      // double curX = rt[i];
      double curX = (double) i;
      double curY = intensities[i];
      // double curSlope = (curY-peakHeight)/(curX-peakRT);
      double curSlope = (curY - peakHeight) / (curX - peakX);
      sumOfRightSlope += curSlope;
      numRight++;
    }
    double meanRightSlope = sumOfRightSlope / ((double) numRight);
    double rad_right_theta = java.lang.Math.atan(1.0 / java.lang.Math.abs(meanRightSlope));


    // if the right slope is positive then correct the angle
    if (meanRightSlope > 0) {
      rad_right_theta = java.lang.Math.PI - rad_right_theta;
    }
    // if the left slope is negative then correct the angle
    if (meanLeftSlope < 0) {
      rad_left_theta = java.lang.Math.PI - rad_left_theta;
    }

    double angle_between = rad_left_theta + rad_right_theta;

    return angle_between;

  }


  /**
   * <p>
   * sharpnessYang.
   * </p>
   *
   * @param chromatogram a {@link NavigableMap} object.
   * @return a double.
   */
  public static double sharpnessYang(NavigableMap<Double, Double> chromatogram) {
    final int size = chromatogram.size();

    double[] retTimes = ArrayUtils.toPrimitive(chromatogram.keySet().toArray(new Double[size]));

    double[] intensities = ArrayUtils.toPrimitive(chromatogram.values().toArray(new Double[size]));

    return sharpnessYang(retTimes, intensities, 0, size - 1);
  }

  /**
   * <p>
   * sharpnessYang.
   * </p>
   *
   * @param rt an array of double.
   * @param intensities an array of double.
   * @param peakLeft a int.
   * @param peakRight a int.
   * @return a double.
   */
  public static double sharpnessYang(double[] rt, double[] intensities, int peakLeft,
      int peakRight) {



    // first get the value of the peak
    int peakIndex = 0;
    double peakHeight = 0.0;

    for (int i = peakLeft; i < (peakRight + 1); i++) {
      double curInt = intensities[i];
      if (curInt > peakHeight) {
        peakHeight = curInt;
        peakIndex = i;
      }
    }
    assert (peakHeight > 0.0);

    double peakRT = rt[peakIndex];

    ///////////////////////////////////////////////////////////////////////////
    // find the line connecting the two boundries. Then find the height of line AT the peak
    /////////////////////////////////////////////////////////////////////////// position.
    ///////////////////////////////////////////////////////////////////////////
    double leftHeight = intensities[peakLeft];
    double rightHeight = intensities[peakRight];
    double leftRT = rt[peakLeft];
    double rightRT = rt[peakRight];

    double slope = (rightHeight - leftHeight) / (rightRT - leftRT);
    double intercept = leftHeight - slope * leftRT;

    double heightOnLineAtPeakLoc = slope * peakRT + intercept;

    ///////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    double p25Height = 0.25 * (peakHeight - heightOnLineAtPeakLoc);
    if (p25Height < 0.0) {
      return -1.0;
    }
    p25Height += heightOnLineAtPeakLoc;
    ///////////////////////////////////////////////////////////////////////////
    // find the slopes on the left side for points higher than p25Height
    ///////////////////////////////////////////////////////////////////////////
    List<Double> leftSlopes = new ArrayList<Double>();

    for (int i = peakLeft; i < peakIndex; i++) {
      if (intensities[i] < p25Height) {
        continue;
      }
      // double curSlope = (peakHeight-intensities[i])/(peakRT-rt[i]);
      double curSlope = (peakHeight - intensities[i]) / ((double) (peakIndex - i));
      leftSlopes.add(curSlope);
    }

    Collections.sort(leftSlopes);

    ///////////////////////////////////////////////////////////////////////////
    // find the slopes on the left side for points higher than p25Height
    ///////////////////////////////////////////////////////////////////////////
    List<Double> rightSlopes = new ArrayList<Double>();

    for (int i = peakIndex + 1; i <= peakRight; i++) {
      if (intensities[i] < p25Height) {
        continue;
      }
      // double curSlope = (intensities[i]-peakHeight)/(rt[i]-peakRT);
      double curSlope = (intensities[i] - peakHeight) / ((double) (i - peakIndex));

      rightSlopes.add(curSlope);
    }

    Collections.sort(rightSlopes);

    ///////////////////////////////////////////////////////////////////////////
    // find the median slopes on each side
    ///////////////////////////////////////////////////////////////////////////
    double medianLeft = 0;
    double medianRight = 0;

    if (!leftSlopes.isEmpty()) {
      if (leftSlopes.size() % 2 == 0) {
        double s1 = leftSlopes.get(leftSlopes.size() / 2);
        double s2 = leftSlopes.get(leftSlopes.size() / 2 - 1);
        medianLeft = (s1 + s2) / 2.0;
      } else {
        medianLeft = leftSlopes.get(leftSlopes.size() / 2);
      }
    }
    if (!rightSlopes.isEmpty()) {
      if (rightSlopes.size() % 2 == 0) {
        double s1 = rightSlopes.get(rightSlopes.size() / 2);
        double s2 = rightSlopes.get(rightSlopes.size() / 2 - 1);
        medianRight = (s1 + s2) / 2.0;
      } else {
        medianRight = rightSlopes.get(rightSlopes.size() / 2);
      }
    }

    if ((leftSlopes.isEmpty()) && (rightSlopes.isEmpty())) {
      return -1;
    } else if (rightSlopes.isEmpty()) {
      return medianLeft;
    } else if (leftSlopes.isEmpty()) {
      return medianRight;
    } else {
      // subtract the right side because we expect it to be negative;
      return (medianLeft - medianRight) / 2.0;
    }
  }

  //
  /**
   * <p>
   * sharpnessAngleAvgAngles. This returns the angle calculated between the two "means" of the
   * slopes on each side of the peak.
   * </p>
   *
   * @param rt an array of double.
   * @param intensities an array of double.
   * @param peakLeft a int.
   * @param peakRight a int.
   * @return a double.
   */
  public static double sharpnessAngleAvgAngles(double[] rt, double[] intensities, int peakLeft,
      int peakRight) {

    int peakWidth = peakRight - peakLeft;
    int halfPeakWidthRoundUp = (int) java.lang.Math.round((double) peakWidth / 2.0);

    // first get the value of the peak
    int peakIndex = 0;
    double peakHeight = 0.0;

    for (int i = peakLeft; i < (peakRight + 1); i++) {
      double curInt = intensities[i];
      if (curInt > peakHeight) {
        peakHeight = curInt;
        peakIndex = i;
      }
    }
    assert (peakHeight > 0.0);

    double weightStep = 1.0;
    double sumOfWeights = 0.0;

    // double peakRT = rt[peakIndex];
    double peakX = (double) peakIndex;
    // We are going to ceck out beyond the peak bounds by half the peak width just becuase it is
    // possible
    // for the bounds to end at the actual peak.

    // Go to the left.
    int finalLeft = peakIndex - halfPeakWidthRoundUp;
    // int finalLeft = peakLeft - (peakWidth);
    if (finalLeft < 0) {
      finalLeft = 0;
    }
    double sumOfLeftAngle = 0.0;
    int numLeft = 0;

    // skip poing imeadiatly adjacent to the peak because it will make the average angle of good
    // peaks smaller and the average angle of
    // bad peaks (ones where it might jus be noise) better.
    for (int i = peakIndex - 1; i >= finalLeft; i--) {

      // double curX = rt[i];
      double curX = (double) i;
      double curY = intensities[i];
      // double curSlope = (peakHeight-curY)/(peakRT-curX);
      double curSlope = (peakHeight - curY) / (peakX - curX);

      double cur_left_theta = java.lang.Math.atan(1.0 / java.lang.Math.abs(curSlope));

      if (curSlope < 0) {
        cur_left_theta = java.lang.Math.PI - cur_left_theta;
      }
      sumOfLeftAngle += (weightStep * (numLeft + 1)) * cur_left_theta;
      sumOfWeights += weightStep * (numLeft + 1);
      numLeft++;
    }
    // double meanLeftAngle = sumOfLeftAngle/((double) numLeft);
    double meanLeftAngle = sumOfLeftAngle / (sumOfWeights);



    sumOfWeights = 0.0;

    // Go to the right
    int finalRight = peakIndex + halfPeakWidthRoundUp;
    // int finalRight = peakRight + (peakWidth);
    if (finalRight >= intensities.length) {
      finalRight = intensities.length - 1;
    }
    double sumOfRightAngle = 0.0;
    int numRight = 0;
    for (int i = peakIndex + 1; i <= finalRight; i++) {

      // double curX = rt[i];
      double curX = (double) i;
      double curY = intensities[i];
      // double curSlope = (curY-peakHeight)/(curX-peakRT);
      double curSlope = (curY - peakHeight) / (curX - peakX);

      double cur_right_theta = java.lang.Math.atan(1.0 / java.lang.Math.abs(curSlope));
      if (curSlope > 0) {
        cur_right_theta = java.lang.Math.PI - cur_right_theta;
      }
      sumOfRightAngle += (weightStep * (numRight + 1)) * cur_right_theta;
      sumOfWeights += weightStep * (numRight + 1);
      numRight++;
    }
    // double meanRightAngle = sumOfRightAngle/((double) numRight);
    double meanRightAngle = sumOfRightAngle / (sumOfWeights);



    double angle_between = meanLeftAngle + meanRightAngle;

    return angle_between;

  }

  /**
   * <p>
   * findMeanOfSignal.
   * </p>
   *
   * @param intensities an array of double.
   * @param peakLeft a int.
   * @param peakRight a int.
   * @return a double.
   */
  public static double findMeanOfSignal(double[] intensities, int peakLeft, int peakRight) {
    double meanSignal = 0.0;
    int count = 0;
    for (int i = peakLeft; i <= peakRight; i++) {
      count += 1;
      meanSignal += intensities[i];
    }
    return meanSignal / ((double) count);

  }

  /**
   * <p>
   * findMinIntensityOfSignal.
   * </p>
   *
   * @param intensities an array of double.
   * @param peakLeft a int.
   * @param peakRight a int.
   * @return a double.
   */
  public static double findMinIntensityOfSignal(double[] intensities, int peakLeft, int peakRight) {

    double peakHeight = 0.0;

    for (int i = peakLeft; i < (peakRight + 1); i++) {
      double curInt = intensities[i];
      if (curInt > peakHeight) {
        peakHeight = curInt;

      }
    }

    double minSig = peakHeight;
    for (int i = peakLeft; i <= peakRight; i++) {
      double curInt = intensities[i];
      if (curInt < minSig) {
        minSig = curInt;

      }
    }
    return minSig;

  }

  /**
   * <p>
   * trapazoidAreaUnderCurve.
   * </p>
   *
   * @param intensities an array of double.
   * @param retentionTimes an array of double.
   * @param peakLeft a int.
   * @param peakRight a int.
   * @return a double.
   */
  public static double trapazoidAreaUnderCurve(double[] intensities, double[] retentionTimes,
      int peakLeft, int peakRight) {
    double area = 0.0;

    for (int i = peakLeft; i < peakRight; i++) {
      double w = retentionTimes[i + 1] - retentionTimes[i];
      double triangle = .5 * w * java.lang.Math.abs(intensities[i] - intensities[i + 1]);
      double h = java.lang.Math.min(intensities[i], intensities[i + 1]);
      area += triangle + w * h;
    }
    return area;
  }
}
