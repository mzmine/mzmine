/*
 * (C) Copyright 2015-2017 by MSDK Development Team
 *
 * This software is dual-licensed under either
 *
 * (a) the terms of the GNU Lesser General Public License version 2.1 as published by the Free
 * Software Foundation
 *
 * or (per the licensee's choosing)
 *
 * (b) the terms of the Eclipse Public License v1.0 as published by the Eclipse Foundation.
 */
package io.github.mzmine.modules.dataprocessing.featdet_adap3d;

/**
 * <p>
 * ADAP3DFeatureDetectionParameters class.
 * </p>
 *
 */
public class ADAP3DFeatureDetectionParameters {

  private double peakSimilarityThreshold = 0.5;
  private double biGaussianSimilarityThreshold = 0.5;
  private int largeScaleIn = 10;
  private double coefAreaRatioTolerance = 100;
  private double minPeakWidth = 0.0;
  private double maxPeakWidth = 10.0;

  /**
   * <p>
   * Setter for the field <code>peakSimilarityThreshold</code>.
   * </p>
   *
   * @param thresholdValue a double.
   */
  public void setPeakSimilarityThreshold(double thresholdValue) {
    peakSimilarityThreshold = thresholdValue;
  }

  /**
   * <p>
   * Getter for the field <code>peakSimilarityThreshold</code>.
   * </p>
   *
   * @return a double.
   */
  public double getPeakSimilarityThreshold() {
    return peakSimilarityThreshold;
  }

  /**
   * <p>
   * Setter for the field <code>biGaussianSimilarityThreshold</code>.
   * </p>
   *
   * @param thresholdValue a double.
   */
  public void setBiGaussianSimilarityThreshold(double thresholdValue) {
    biGaussianSimilarityThreshold = thresholdValue;
  }

  /**
   * <p>
   * Getter for the field <code>biGaussianSimilarityThreshold</code>.
   * </p>
   *
   * @return a double.
   */
  public double getBiGaussianSimilarityThreshold() {
    return biGaussianSimilarityThreshold;
  }

  /**
   * <p>
   * Setter for the field <code>largeScaleIn</code>.
   * </p>
   *
   * @param largeScale a int.
   */
  public void setLargeScaleIn(int largeScale) {
    largeScaleIn = largeScale;
  }

  /**
   * <p>
   * Getter for the field <code>largeScaleIn</code>.
   * </p>
   *
   * @return a int.
   */
  public int getLargeScaleIn() {
    return largeScaleIn;
  }

  /**
   * <p>
   * Setter for the field <code>coefAreaRatioTolerance</code>.
   * </p>
   *
   * @param coefOverAreaThreshold a double.
   */
  public void setCoefAreaRatioTolerance(double coefOverAreaThreshold) {
    coefAreaRatioTolerance = coefOverAreaThreshold;
  }

  /**
   * <p>
   * Getter for the field <code>coefAreaRatioTolerance</code>.
   * </p>
   *
   * @return a double.
   */
  public double getCoefAreaRatioTolerance() {
    return coefAreaRatioTolerance;
  }

  /**
   * <p>
   * Setter for the field <code>minPeakWidth</code>.
   * </p>
   *
   * @param peakWidth a double.
   */
  public void setMinPeakWidth(double peakWidth) {
    minPeakWidth = peakWidth;
  }

  /**
   * <p>
   * Getter for the field <code>minPeakWidth</code>.
   * </p>
   *
   * @return a double.
   */
  public double getMinPeakWidth() {
    return minPeakWidth;
  }

  /**
   * <p>
   * Setter for the field <code>maxPeakWidth</code>.
   * </p>
   *
   * @param peakWidth a double.
   */
  public void setMaxPeakWidth(double peakWidth) {
    maxPeakWidth = peakWidth;
  }

  /**
   * <p>
   * Getter for the field <code>maxPeakWidth</code>.
   * </p>
   *
   * @return a double.
   */
  public double getMaxPeakWidth() {
    return maxPeakWidth;
  }
}
