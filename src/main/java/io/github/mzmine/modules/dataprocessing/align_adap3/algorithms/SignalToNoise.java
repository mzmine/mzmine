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

/*
 * To change this license header, choose License Headers in Project Properties. To change this
 * template file, choose Tools | Templates and open the template in the editor.
 */
package io.github.mzmine.modules.dataprocessing.align_adap3.algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/**
 * <p>
 * SignalToNoise class.
 * </p>
 *
 * @author owenmyers Modified by Dharak Shah to include in MSDK
 */
public class SignalToNoise {
  /**
   * <p>
   * findSNUsingWaveletCoefficents.
   * </p>
   *
   * @param allCoefficients an array of double.
   * @param bestCoeff a double.
   * @param peakLeft a int.
   * @param peakRight a int.
   * @param windowSizeMult a double.
   * @param absWaveCoeffs a boolean.
   * @return a double.
   */
  public static double findSNUsingWaveletCoefficents(double[][] allCoefficients, double bestCoeff,
      int peakLeft, int peakRight, double windowSizeMult, boolean absWaveCoeffs) {
    int peakWidth = peakRight - peakLeft;

    int leftRightWindowSize = (int) java.lang.Math.round(windowSizeMult * peakWidth);
    double toReturnSN = 0.0;
    int smallestScaleIndex = 0;
    double[] smallestScaleArr = allCoefficients[smallestScaleIndex];
    // writeArray(smallestScaleArr);
    if (absWaveCoeffs) {
      for (int i = 0; i < smallestScaleArr.length; i++) {
        smallestScaleArr[i] = java.lang.Math.abs(smallestScaleArr[i]);
      }
    }
    // Get the coefficients in some window. Use peakwidth to determine window
    List<Double> coefsUsedForSN = new ArrayList<Double>();
    int index = peakRight + 1;
    ///////////////////////////////////////////////////////////////////
    /////////////////// Stuff around the peak/////////////////////////
    ///////////////////////////////////////////////////////////////////
    while ((index < smallestScaleArr.length) && ((index - peakRight) < (leftRightWindowSize))) {
      coefsUsedForSN.add(smallestScaleArr[index]);
      index++;
    }
    index = peakLeft - 1;
    while ((index > 0) && ((peakLeft - index) < (leftRightWindowSize))) {
      coefsUsedForSN.add(smallestScaleArr[index]);
      index--;
    }
    ///////////////////////////////////////////////////////////////////
    //////////////////// Include the peak /////////////////////////////
    ///////////////////////////////////////////////////////////////////
    index = peakLeft;
    while (index <= peakRight) {
      coefsUsedForSN.add(smallestScaleArr[index]);
      index++;
    }

    double quantile = getQuantile(coefsUsedForSN, 0.95);
    // Collections.sort(coefsUsedForSN);
    // double multiplier =((double)coefsUsedForSN.size())/100.0;
    // // 5% from the spmalles will give you the 95th quartile
    // int indexLocationOf95th = (int) java.lang.Math.round(5*multiplier);

    toReturnSN = bestCoeff / quantile;


    // double [] target = new double[coefsUsedForSN.size()];
    // for (int i=0; i<target.length;i++){
    // target[i] = coefsUsedForSN.get(i);
    // }
    // writeArray(target);
    // Arrays.sort(smallestScaleArr);



    return toReturnSN;
  }

  /**
   * Estimate quantile of the list of values
   *
   * Estimation is equivalent to the default quantile estimate in R: if x[j] and x[j+1] are two
   * values adjacent to the quantile, than Q[p] = (gamma - 1) * x[j] + gamma * x[j+1] where gamma =
   * (size - 1) * p + 1 - j and j starts from 1
   *
   * @param values list of doubles
   * @param probability value between 0.0 and 1.0
   * @return quantile
   */
  public static double getQuantile(List<Double> values, double probability) {
    int size = values.size();

    if (size <= 1 || probability <= 0.0 || probability >= 1.0)
      throw new IllegalArgumentException("Cannot calculate quantile");

    Collections.sort(values);

    int index1 = (int) (size * probability);
    int index2 = index1 + 1;

    double gamma = (size - 1) * probability + 1 - index1;

    return (1 - gamma) * values.get(index1 - 1) + gamma * values.get(index2 - 1);
  }

  /**
   * <p>
   * filterBySNRandWindowSelect.
   * </p>
   *
   * @param intensities an array of double.
   * @param peakLeft a int.
   * @param peakRight a int.
   * @return a double.
   */
  public static double filterBySNRandWindowSelect(double[] intensities, int peakLeft,
      int peakRight) {

    DescriptiveStatistics stdDevStats = new DescriptiveStatistics();

    DescriptiveStatistics toFindMinWithPeakNoise = new DescriptiveStatistics();
    DescriptiveStatistics toFindLocalMean = new DescriptiveStatistics();

    // first get the value of the peak
    for (int i = peakLeft; i < (peakRight + 1); i++) {
      stdDevStats.addValue(intensities[i]);
    }
    double peakHeight = stdDevStats.getMax();
    stdDevStats.clear();

    int peakWidth = peakRight - peakLeft;

    // Initial size of expanding window will be twice the peak width on either side of the peak
    // (really half the total window size.
    int sampleWindowSize = peakWidth;


    int rightBound = peakRight + 10 * sampleWindowSize;
    int leftBound = peakLeft - 10 * sampleWindowSize;

    // Make sure the these values don't go out of bounds
    if (rightBound >= intensities.length) {
      rightBound = intensities.length - 1;
    }
    if (leftBound < 0) {
      leftBound = 0;
    }

    // Make a new arr containing only the value in the window
    List<Double> forNoiseCalc = new ArrayList<Double>();
    // first find the standard deviation from the windows EXCLUDING the peak
    for (int j = peakRight + 1; j < (rightBound + 1); j++) {
      forNoiseCalc.add(intensities[j]);
    }
    for (int j = leftBound; j < (peakLeft); j++) {
      forNoiseCalc.add(intensities[j]);
    }


    toFindMinWithPeakNoise.clear();
    DescriptiveStatistics toCollectSampleWindows = new DescriptiveStatistics();
    // See what you get when yu put all the windows together before final std calc.
    DescriptiveStatistics allWindowSTD = new DescriptiveStatistics();

    for (int i = 0; i < 1000; i++) {
      toCollectSampleWindows.clear();
      toCollectSampleWindows.clear();
      for (int j = 0; j < 1; j++) {
        int min = 0;
        int max = forNoiseCalc.size() / 2 - sampleWindowSize;
        int windowStart = ThreadLocalRandom.current().nextInt(min, max);
        for (int k = 0; k < sampleWindowSize; k++) {
          toCollectSampleWindows.addValue(forNoiseCalc.get(windowStart + k));
          allWindowSTD.addValue(forNoiseCalc.get(windowStart + k));
        }
      }
      for (int j = 0; j < 1; j++) {
        int min = forNoiseCalc.size() / 2;
        int max = forNoiseCalc.size() - sampleWindowSize;
        int windowStart = ThreadLocalRandom.current().nextInt(min, max);
        for (int k = 0; k < sampleWindowSize; k++) {
          toCollectSampleWindows.addValue(forNoiseCalc.get(windowStart + k));
          allWindowSTD.addValue(forNoiseCalc.get(windowStart + k));
        }
      }
      stdDevStats.addValue(toCollectSampleWindows.getStandardDeviation());
      double curNoPeakMean = toCollectSampleWindows.getMean();
      toFindLocalMean.addValue(curNoPeakMean);

      // Now add in the peak becausase the difference in std with the peak will tell us something
      // about it height.
      for (int j = peakLeft; j < (peakRight + 1); j++) {
        toCollectSampleWindows.addValue(intensities[j]);
      }
      double curWithPeakNoise = toCollectSampleWindows.getStandardDeviation();
      toFindMinWithPeakNoise.addValue(curWithPeakNoise);

    }
    // double bestNoise = stdDevStats.getMin();
    // I think we should use the mean instead of the min. This assumes that the majority of the
    // stuff within our max window
    // is noise.
    // double bestNoise = stdDevStats.getMean();
    // Lets try looking at the std from collecting all of the data first
    double bestNoise = allWindowSTD.getMean();



    // before calculating the signal to noise ratio we need to "normalize" the height.
    // What can happen is if a bad peak is found on a plateau the local stadard deviation will be
    // small compared to the absolute height (or mean of peak) of peak.
    //
    // Find the minimum intesity point starting at the boundry and going a boundry width out.
    // Then subtract this from the peak value (height or mean) before comparing to the standard
    // deviation.

    stdDevStats.clear();
    rightBound = peakRight + peakWidth;
    leftBound = peakLeft - peakWidth;

    // Make sure the these values don't go out of bounds
    if (rightBound >= intensities.length) {
      rightBound = intensities.length - 1;
    }
    if (leftBound < 0) {
      leftBound = 0;
    }
    // out to right
    for (int i = peakRight; i <= rightBound; i++) {
      stdDevStats.addValue(intensities[i]);
    }
    double smallIntensity1 = stdDevStats.getMin();
    stdDevStats.clear();
    for (int i = leftBound; i <= peakLeft; i++) {
      stdDevStats.addValue(intensities[i]);
    }
    double smallIntensity2 = stdDevStats.getMin();
    double smallIntensityAvg = (smallIntensity1 + smallIntensity2) / 2.0;


    double SNRatio = (peakHeight - smallIntensityAvg) / bestNoise;
    // double SNRatio = (meanOfSignal-smallIntensityAvg)/bestNoise;


    // if (bestWithPeakNoise/bestNoise <= 1.2){
    // return -1;
    // }

    return SNRatio;

  }

  /**
   * <p>
   * filterBySNWindowSweep.
   * </p>
   *
   * @param intensities an array of double.
   * @param peakLeft a int.
   * @param peakRight a int.
   * @return a double.
   */
  public static double filterBySNWindowSweep(double[] intensities, int peakLeft, int peakRight) {
    DescriptiveStatistics stdDevStats = new DescriptiveStatistics();
    DescriptiveStatistics toFindMinNoPeakNoise = new DescriptiveStatistics();
    DescriptiveStatistics toFindMinWithPeakNoise = new DescriptiveStatistics();
    DescriptiveStatistics toFindLocalMean = new DescriptiveStatistics();

    // first get the value of the peak
    for (int i = peakLeft; i < (peakRight + 1); i++) {
      stdDevStats.addValue(intensities[i]);
    }

    double peakHeight = stdDevStats.getMax();
    stdDevStats.clear();

    int peakWidth = peakRight - peakLeft;

    // Initial size of expanding window will be twice the peak width on either side of the peak
    // (really half the total window size.
    int initialWindowSize = 3 * peakWidth;
    // final size of expanding window.
    int finalWindowSize = 9 * peakWidth;


    toFindMinNoPeakNoise.clear();
    toFindMinWithPeakNoise.clear();

    // loop over different window sizes
    for (int i = 0; i < (finalWindowSize + 1); i++) {
      stdDevStats.clear();
      int curRight = peakRight + initialWindowSize + i;
      int curLeft = peakLeft - initialWindowSize - i;

      // Make sure the these values don't go out of bounds
      if (curRight >= intensities.length) {
        curRight = intensities.length - 1;
      }
      if (curLeft < 0) {
        curLeft = 0;
      }

      // first find the standard deviation from the windows EXCLUDING the peak
      for (int j = peakRight + 1; j < (curRight + 1); j++) {
        stdDevStats.addValue(intensities[j]);
      }
      for (int j = curLeft; j < (peakLeft); j++) {
        stdDevStats.addValue(intensities[j]);
      }
      double curNoPeakNoise = stdDevStats.getStandardDeviation();
      toFindMinNoPeakNoise.addValue(curNoPeakNoise);
      double curNoPeakMean = stdDevStats.getMean();
      toFindLocalMean.addValue(curNoPeakMean);

      // Now add in the peak becausase the difference in std with the peak will tell us something
      // about it height.
      for (int j = peakLeft; j < (peakRight + 1); j++) {
        stdDevStats.addValue(intensities[j]);
      }
      double curWithPeakNoise = stdDevStats.getStandardDeviation();
      toFindMinWithPeakNoise.addValue(curWithPeakNoise);
    }

    // for a good looking peak we think the difference between the noise with and without the peak
    // included should
    // be about a factor of 2. Specificaly with the peak the noise should be 2x higher than without
    // if it is real.
    double bestNoPeakNoise = toFindMinNoPeakNoise.getMin();



    // before calculating the signal to noise ratio we need to "normalize" the height.
    // What can happen is if a bad peak is found on a plateau the local stadard deviation will be
    // small compared to the absolute height (or mean of peak) of peak.
    //
    // Find the minimum intesity point starting at the boundry and going a boundry width out.
    // Then subtract this from the peak value (height or mean) before comparing to the standard
    // deviation.

    stdDevStats.clear();
    int rightBound = peakRight + peakWidth;
    int leftBound = peakLeft - peakWidth;

    // Make sure the these values don't go out of bounds
    if (rightBound >= intensities.length) {
      rightBound = intensities.length - 1;
    }
    if (leftBound < 0) {
      leftBound = 0;
    }
    // out to right
    for (int i = peakRight; i <= rightBound; i++) {
      stdDevStats.addValue(intensities[i]);
    }
    double smallIntensity1 = stdDevStats.getMin();
    stdDevStats.clear();
    for (int i = leftBound; i <= peakLeft; i++) {
      stdDevStats.addValue(intensities[i]);
    }
    double smallIntensity2 = stdDevStats.getMin();
    double smallIntensityAvg = (smallIntensity1 + smallIntensity2) / 2.0;


    double SNRatio = (peakHeight - smallIntensityAvg) / bestNoPeakNoise;
    // double SNRatio = (meanOfSignal-smallIntensityAvg)/bestNoPeakNoise;
    return SNRatio;

  }

  // This sweeps the window size ffrom the points furthest from the peak out and then from the
  // points closest to the peak
  // out twords the furthest.
  /**
   * <p>
   * filterBySNWindowInOutSweep.
   * </p>
   *
   * @param intensities an array of double.
   * @param peakLeft a int.
   * @param peakRight a int.
   * @return a double.
   */
  public static double filterBySNWindowInOutSweep(double[] intensities, int peakLeft,
      int peakRight) {
    DescriptiveStatistics stdDevStats = new DescriptiveStatistics();
    DescriptiveStatistics toFindMinNoPeakNoise = new DescriptiveStatistics();
    DescriptiveStatistics toFindMinWithPeakNoise = new DescriptiveStatistics();
    DescriptiveStatistics toFindLocalMean = new DescriptiveStatistics();

    // first get the value of the peak
    for (int i = peakLeft; i < (peakRight + 1); i++) {
      stdDevStats.addValue(intensities[i]);
    }

    double peakHeight = stdDevStats.getMax();
    stdDevStats.clear();

    int peakWidth = peakRight - peakLeft;

    // Initial size of expanding window will be twice the peak width on either side of the peak
    // (really half the total window size.
    int initialWindowSize = 2 * peakWidth;
    // final size of expanding window.
    int finalWindowSize = 8 * peakWidth;


    toFindMinNoPeakNoise.clear();
    toFindMinWithPeakNoise.clear();

    // loop over different window sizes
    for (int i = 0; i < (finalWindowSize + 1); i++) {
      stdDevStats.clear();
      int curRight = peakRight + initialWindowSize + i + 1;
      int curLeft = peakLeft - initialWindowSize - i - 1;

      int this_time_num_added = 0;

      // Make sure the these values don't go out of bounds
      if (curRight >= intensities.length) {
        curRight = intensities.length - 1;
      }
      if (curLeft < 0) {
        curLeft = 0;
      }

      if (!(java.lang.Math.abs(curLeft - peakLeft) < initialWindowSize)) {
        for (int j = curLeft; j < peakLeft; j++) {
          stdDevStats.addValue(intensities[j]);
          this_time_num_added += 1;
        }
      }
      if (!(java.lang.Math.abs(curRight - peakRight) < initialWindowSize)) {
        for (int j = curRight; j > peakRight; j--) {
          stdDevStats.addValue(intensities[j]);
          this_time_num_added += 1;
        }
      }
      if (this_time_num_added == 0) {
        continue;
      }


      // first find the standard deviation from the windows EXCLUDING the peak
      double curNoPeakNoise = stdDevStats.getStandardDeviation();
      toFindMinNoPeakNoise.addValue(curNoPeakNoise);
      double curNoPeakMean = stdDevStats.getMean();
      toFindLocalMean.addValue(curNoPeakMean);

      // Now add in the peak becausase the difference in std with the peak will tell us something
      // about it height.
      for (int j = peakLeft; j < (peakRight + 1); j++) {
        stdDevStats.addValue(intensities[j]);
      }
      double curWithPeakNoise = stdDevStats.getStandardDeviation();
      toFindMinWithPeakNoise.addValue(curWithPeakNoise);
    }
    // Now sweep the points closest to the peak out
    int anchorRight = peakRight + initialWindowSize + finalWindowSize;
    int anchorLeft = peakRight - initialWindowSize - finalWindowSize;
    // Make sure the these values don't go out of bounds
    if (anchorRight >= intensities.length) {
      anchorRight = intensities.length - 1;
    }
    if (anchorLeft < 0) {
      anchorLeft = 0;
    }

    for (int i = 0; i < (finalWindowSize - initialWindowSize); i++) {

      stdDevStats.clear();
      int curRight = peakRight + 1 + i;
      int curLeft = peakLeft - 1 - i;

      int this_time_num_added = 0;

      // Make sure the these values don't go out of bounds
      if (curRight >= intensities.length) {
        curRight = intensities.length - 1;
      }
      if (curLeft < 0) {
        curLeft = 0;
      }

      if (!(java.lang.Math.abs(curLeft - anchorLeft) < initialWindowSize)) {
        for (int j = curLeft; j > anchorLeft; j--) {
          stdDevStats.addValue(intensities[j]);
          this_time_num_added += 1;
        }
      }
      if (!(java.lang.Math.abs(curRight - anchorRight) < initialWindowSize)) {
        for (int j = curRight; j < anchorRight; j++) {
          stdDevStats.addValue(intensities[j]);
          this_time_num_added += 1;
        }
      }
      if (this_time_num_added == 0) {
        continue;
      }

      double curNoPeakNoise = stdDevStats.getStandardDeviation();
      toFindMinNoPeakNoise.addValue(curNoPeakNoise);
    }

    // for a good looking peak we think the difference between the noise with and without the peak
    // included should
    // be about a factor of 2. Specificaly with the peak the noise should be 2x higher than without
    // if it is real.
    double bestNoPeakNoise = toFindMinNoPeakNoise.getMin();



    // if (meanOfSignal>(1.5*smallestLocalMean)){
    //
    // }
    // if (bestWithPeakNoise/bestNoPeakNoise <= 1.5){
    // return false;
    // }


    // before calculating the signal to noise ratio we need to "normalize" the height.
    // What can happen is if a bad peak is found on a plateau the local stadard deviation will be
    // small compared to the absolute height (or mean of peak) of peak.
    //
    // Find the minimum intesity point starting at the boundry and going a boundry width out.
    // Then subtract this from the peak value (height or mean) before comparing to the standard
    // deviation.

    stdDevStats.clear();
    int rightBound = peakRight + peakWidth;
    int leftBound = peakLeft - peakWidth;

    // Make sure the these values don't go out of bounds
    if (rightBound >= intensities.length) {
      rightBound = intensities.length - 1;
    }
    if (leftBound < 0) {
      leftBound = 0;
    }
    // out to right
    for (int i = peakRight; i <= rightBound; i++) {
      stdDevStats.addValue(intensities[i]);
    }
    double smallIntensity1 = stdDevStats.getMin();
    stdDevStats.clear();
    for (int i = leftBound; i <= peakLeft; i++) {
      stdDevStats.addValue(intensities[i]);
    }
    double smallIntensity2 = stdDevStats.getMin();
    double smallIntensityAvg = (smallIntensity1 + smallIntensity2) / 2.0;


    double SNRatio = (peakHeight - smallIntensityAvg) / bestNoPeakNoise;
    // double SNRatio = (meanOfSignal-smallIntensityAvg)/bestNoPeakNoise;

    return SNRatio;

  }

  // Uses two windows on eaiter side of the peak that are a constand width. These windows then both
  // slide out to a set distance ithout changing side. The smallest standard deviation is taken to
  // be
  // the noise.
  /**
   * <p>
   * filterBySNStaticWindowSweep.
   * </p>
   *
   * @param intensities an array of double.
   * @param peakLeft a int.
   * @param peakRight a int.
   * @return a double.
   */
  public static double filterBySNStaticWindowSweep(double[] intensities, int peakLeft,
      int peakRight) {
    DescriptiveStatistics stdDevStats = new DescriptiveStatistics();
    DescriptiveStatistics toFindMinNoPeakNoise = new DescriptiveStatistics();

    // first get the value of the peak
    for (int i = peakLeft; i < (peakRight + 1); i++) {
      stdDevStats.addValue(intensities[i]);
    }

    double peakHeight = stdDevStats.getMax();
    stdDevStats.clear();

    int peakWidth = peakRight - peakLeft;

    // Initial size of expanding window will be twice the peak width on either side of the peak
    // (really half the total window size.
    int windowSize = 2 * peakWidth;
    // final size of expanding window.
    int furthestPoint = 8 * peakWidth;

    int finalCurLeft = 0;
    int finalCurRight = 0;
    boolean stop_sliding_right = false;
    boolean stop_sliding_left = false;
    for (int i = 0; i < (furthestPoint - windowSize); i++) {
      stdDevStats.clear();

      int curRight = peakRight + i;
      int curRightRight = curRight + windowSize;

      if ((curRightRight >= intensities.length) && (!stop_sliding_right)) {
        stop_sliding_right = true;
        finalCurRight = curRight - 1;
      }
      if (stop_sliding_right) {
        curRight = finalCurRight;
        // curRightRight = curRightRight - windowSize;
        curRightRight = intensities.length - 1;

      }

      int curLeft = peakLeft - i;
      int curLeftLeft = curLeft - windowSize;

      if ((curLeftLeft < 0) && (!stop_sliding_left)) {
        stop_sliding_left = true;
        // revert to last value
        finalCurLeft = curLeft + 1;
      }
      if (stop_sliding_left) {
        curLeft = finalCurLeft;
        // curLeftLeft = curLeftLeft + windowSize;
        curLeftLeft = 0;

      }

      for (int j = curRight; j <= curRightRight; j++) {
        stdDevStats.addValue(intensities[j]);
      }
      for (int j = curLeft; j >= curLeftLeft; j--) {
        stdDevStats.addValue(intensities[j]);
      }
      double curNoPeakNoise = stdDevStats.getStandardDeviation();


      toFindMinNoPeakNoise.addValue(curNoPeakNoise);
    }
    double bestNoPeakNoise = toFindMinNoPeakNoise.getMin();
    // before calculating the signal to noise ratio we need to "normalize" the height.
    // What can happen is if a bad peak is found on a plateau the local stadard deviation will be
    // small compared to the absolute height (or mean of peak) of peak.
    //
    // Find the minimum intesity point starting at the boundry and going a boundry width out.
    // Then subtract this from the peak value (height or mean) before comparing to the standard
    // deviation.

    stdDevStats.clear();
    int rightBound = peakRight + peakWidth;
    int leftBound = peakLeft - peakWidth;

    // Make sure the these values don't go out of bounds
    if (rightBound >= intensities.length) {
      rightBound = intensities.length - 1;
    }
    if (leftBound < 0) {
      leftBound = 0;
    }
    // out to right
    for (int i = peakRight; i <= rightBound; i++) {
      stdDevStats.addValue(intensities[i]);
    }
    double smallIntensity1 = stdDevStats.getMin();
    stdDevStats.clear();
    for (int i = leftBound; i <= peakLeft; i++) {
      stdDevStats.addValue(intensities[i]);
    }
    double smallIntensity2 = stdDevStats.getMin();
    double smallIntensityAvg = (smallIntensity1 + smallIntensity2) / 2.0;


    double SNRatio = (peakHeight - smallIntensityAvg) / bestNoPeakNoise;
    // double SNRatio = (meanOfSignal-smallIntensityAvg)/bestNoPeakNoise;

    return SNRatio;
  }
}
