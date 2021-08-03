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
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.mzmine.modules.dataprocessing.featdet_adap3d.ADAP3DFeatureDetectionParameters;
import io.github.mzmine.modules.dataprocessing.align_adap3.algorithms.SliceSparseMatrix.Triplet;
import io.github.mzmine.modules.dataprocessing.featdet_adap3d.datamodel.Result;

/**
 * <p>ADAP3DPeakDetectionAlgorithm class.</p>
 *
 */
public class ADAP3DPeakDetectionAlgorithm {

  /**
   * <p>
   * GoodPeakInfo class is used to save information of good peaks.
   * </p>
   */
  public static class GoodPeakInfo {
    public double mz;
    public int upperScanBound;
    public int lowerScanBound;
    public float maxHeight;
    public int maxHeightScanNumber;
    public Result objResult;
  }

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  
  private final io.github.mzmine.modules.dataprocessing.align_adap3.algorithms.SliceSparseMatrix objSliceSparseMatrix;

  /**
   * Flag for stopping the adap3d algorithm execution.
   */
  private boolean canceled = false;

  private float progressPercent;

  /**
   * <p>
   * Constructor
   * </p>
   *
   * @param objSliceSparseMatrix is sparse matrix created from raw data file.
   */
  public ADAP3DPeakDetectionAlgorithm(SliceSparseMatrix objSliceSparseMatrix) {
    this.objSliceSparseMatrix = objSliceSparseMatrix;
  }

  /**
   * <p>
   * This method determines the number of good peaks provided by the user.
   * </p>
   *
   * @param numOfPeaks a {@link Integer} object. This is the maximum number of good peaks
   *        method will determine.
   * @param roundedFWHM is full width half max of whole raw data file.
   * @return peakList a list of {@link GoodPeakInfo} object type. This contains information of good
   *         peaks.
   * @param objParameters a {@link ADAP3DFeatureDetectionParameters} object.
   */
  public List<GoodPeakInfo> execute(int numOfPeaks, ADAP3DFeatureDetectionParameters objParameters, int roundedFWHM) {
    
    logger.debug("Starting ADAP3D algorithm for " + numOfPeaks + " peaks");
    
    int maxCount = 0;
    Triplet maxIntensityTriplet = objSliceSparseMatrix.findNextMaxIntensity();
    List<GoodPeakInfo> peakList = new ArrayList<GoodPeakInfo>();


    while (maxCount < numOfPeaks) {
      if (maxCount == 20)
        break;
      GoodPeakInfo goodPeak = iteration(maxIntensityTriplet, roundedFWHM, objParameters);
      if (goodPeak != null) {
        maxCount++;
        peakList.add(goodPeak);
      }


      if (canceled)
        return peakList;

      progressPercent = objSliceSparseMatrix.getFinishedPercent(maxIntensityTriplet);
      maxIntensityTriplet = objSliceSparseMatrix.findNextMaxIntensity();

    }
    
    logger.debug("Finished ADAP3D algorithm for " + numOfPeaks + " peaks");

    return peakList;
  }


  /**
   * <p>
   * This method executes the iteration method to find good peaks.
   * </p>
   *
   * @param roundedFWHM is full width half max of whole raw data file.
   * @return peakList a list of {@link GoodPeakInfo} object type. This contains information of good
   *         peaks.
   * @param objParameters a {@link ADAP3DFeatureDetectionParameters} object.
   */
  public List<GoodPeakInfo> execute(ADAP3DFeatureDetectionParameters objParameters, int roundedFWHM) {

    logger.debug("Starting ADAP3D algorithm for all good peaks");

    Triplet maxIntensityTriplet = objSliceSparseMatrix.findNextMaxIntensity();
    List<GoodPeakInfo> peakList = new ArrayList<GoodPeakInfo>();
    

    while (maxIntensityTriplet != null) {
      
      if (canceled)
        return null;

      //logger.debug("Running iteration on triplet " + maxIntensityTriplet);
      GoodPeakInfo goodPeak = iteration(maxIntensityTriplet, roundedFWHM, objParameters);
      if (goodPeak != null)
        peakList.add(goodPeak);


      maxIntensityTriplet = objSliceSparseMatrix.findNextMaxIntensity();
    }

    logger.debug("Finished ADAP3D algorithm for all good peaks");

    return peakList;
  }

  /**
   * <p>
   * This method finds if there's a good peak or not.
   * </p>
   * 
   * @param triplet a
   *        {@link Triplet}
   *        object. This is the element of sparse matrix.
   * @param fwhm a {@link Double} object. This is estimated full width half max.
   * 
   * @return objPeakInfo a {@link GoodPeakInfo} object. This contains information of good peak. If
   *         there's no good peak it'll return null.
   */
  private GoodPeakInfo iteration(Triplet triplet, int fwhm, ADAP3DFeatureDetectionParameters objParameters) {

    GoodPeakInfo objPeakInfo = null;
    int lowerScanBound = triplet.scanListIndex - objParameters.getLargeScaleIn() < 0 ? 0
        : triplet.scanListIndex - objParameters.getLargeScaleIn();
    int upperScanBound = triplet.scanListIndex + objParameters.getLargeScaleIn() >= objSliceSparseMatrix
        .getSizeOfRawDataFile() ? objSliceSparseMatrix.getSizeOfRawDataFile() - 1
            : triplet.scanListIndex + objParameters.getLargeScaleIn();

    // Here we're getting horizontal slice.
    List<Triplet> slice =
        objSliceSparseMatrix.getHorizontalSlice(triplet.mz, lowerScanBound, upperScanBound);

    // Below CWT is called to get bounds of peak.
    ContinuousWaveletTransform continuousWavelet =
        new ContinuousWaveletTransform(1, objParameters.getLargeScaleIn(), 1);
    List<ContinuousWaveletTransform.DataPoint> listOfDataPoint =
        new ArrayList<ContinuousWaveletTransform.DataPoint>();
    listOfDataPoint = objSliceSparseMatrix.getCWTDataPoint(slice);

    continuousWavelet.setX(listOfDataPoint);
    continuousWavelet.setSignal(listOfDataPoint);
    continuousWavelet.setPeakWidth(objParameters.getMinPeakWidth(),
        objParameters.getMaxPeakWidth());
    continuousWavelet.setcoefAreaRatioTolerance(objParameters.getCoefAreaRatioTolerance());

    // Peaks are detected from CWT.
    List<Result> peakList = continuousWavelet.findPeaks();

    // If there's no peak detected.
    if (peakList.isEmpty()) {
      removeDataPoints(triplet.mz - fwhm, triplet.mz + fwhm, lowerScanBound, upperScanBound);
    }

    else {
      io.github.mzmine.modules.dataprocessing.align_adap3.algorithms.Peak3DTest objPeak3DTest = new io.github.mzmine.modules.dataprocessing.align_adap3.algorithms.Peak3DTest(objSliceSparseMatrix, fwhm);
      BiGaussianSimilarityTest objBiGaussianTest = new BiGaussianSimilarityTest();

      boolean remove = true;

      for (int i = 0; i < peakList.size(); i++) {



        List<Triplet> curSlice = objSliceSparseMatrix.getHorizontalSlice(triplet.mz,
            peakList.get(i).curLeftBound + lowerScanBound,
            peakList.get(i).curRightBound + lowerScanBound);

        double sliceMaxIntensity = curSlice.stream().map(x -> x != null ? x.intensity : 0.0)
            .max(Double::compareTo).orElse(0.0);
        int scanNumber = curSlice.stream()
            .map(x -> x != null && x.intensity == sliceMaxIntensity ? x.scanListIndex : 0)
            .max(Integer::compareTo).orElse(0);

        // If there's no peak at apex.
        if (scanNumber != triplet.scanListIndex) {
          if (remove) {
            removeDataPoints(triplet.mz - fwhm, triplet.mz + fwhm, lowerScanBound, upperScanBound);
            remove = false;
          }
          restoreDataPoints(triplet.mz - fwhm, triplet.mz + fwhm,
              peakList.get(i).curLeftBound + lowerScanBound,
              peakList.get(i).curRightBound + lowerScanBound);
        }

        // If there's peak at apex.
        else {
          Peak3DTest.Result peak =
              objPeak3DTest.execute(triplet.mz, peakList.get(i).curLeftBound + lowerScanBound,
                  peakList.get(i).curRightBound + lowerScanBound,
                  objParameters.getPeakSimilarityThreshold());
          boolean goodPeak =
              objBiGaussianTest.execute(curSlice, peakList.get(i).curLeftBound + lowerScanBound,
                  peakList.get(i).curRightBound + lowerScanBound, triplet.mz,
                  objParameters.getBiGaussianSimilarityThreshold());

          // If there's good peak
          if (peak.goodPeak && goodPeak) {
            removeDataPoints(peak.lowerMzBound, peak.upperMzBound,
                peakList.get(i).curLeftBound + lowerScanBound,
                peakList.get(i).curRightBound + lowerScanBound);
            objPeakInfo = new GoodPeakInfo();
            objPeakInfo.mz = (double) triplet.mz / 10000;
            objPeakInfo.lowerScanBound = peakList.get(i).curLeftBound + lowerScanBound;
            objPeakInfo.upperScanBound = peakList.get(i).curRightBound + lowerScanBound;
            objPeakInfo.maxHeight = triplet.intensity;
            objPeakInfo.maxHeightScanNumber = triplet.scanListIndex;
            objPeakInfo.objResult = peakList.get(i);
          } else {
            removeDataPoints(peak.lowerMzBound, peak.upperMzBound,
                peakList.get(i).curLeftBound + lowerScanBound,
                peakList.get(i).curRightBound + lowerScanBound);
          }
        }

      }
    }
    return objPeakInfo;
  }

  /**
   * <p>
   * This method removes data point in loop by calling removeDataPoints method from
   * SliceSparseMatrix.
   * </p>
   * 
   * @param lowerMZ a {@link Integer} object. This is the lower m/z boundary from which
   *        data point removal starts.
   * @param upperMZ a {@link Integer} object. This is the lower m/z boundary at which data
   *        point removal ends.
   * @param lowerScanBound a {@link Integer} object. This is the lower scan boundary from
   *        which data point removal starts.
   * @param upperScanBound a {@link Integer} object. This is the upper scan boundary at
   *        which data point removal ends.
   * 
   */
  private void removeDataPoints(int lowerMZ, int upperMZ, int lowerScanBound, int upperScanBound) {
    for (int i = lowerMZ; i <= upperMZ; i++) {
      objSliceSparseMatrix.removeDataPoints(i, lowerScanBound, upperScanBound);
    }
  }

  /**
   * <p>
   * This method restores data point in loop by calling restoreDataPoints method from
   * SliceSparseMatrix.
   * </p>
   * 
   * @param lowerMZ a {@link Integer} object. This is the lower m/z boundary from which
   *        data point restoration starts.
   * @param upperMZ a {@link Integer} object. This is the lower m/z boundary at which data
   *        point restoration ends.
   * @param lowerScanBound a {@link Integer} object. This is the lower scan boundary from
   *        which data point restoration starts.
   * @param upperScanBound a {@link Integer} object. This is the upper scan boundary at
   *        which data point restoration ends.
   */
  private void restoreDataPoints(int lowerMZ, int upperMZ, int lowerScanBound, int upperScanBound) {
    for (int i = lowerMZ; i <= upperMZ; i++) {
      objSliceSparseMatrix.restoreDataPoints(i, lowerScanBound, upperScanBound);
    }
  }

  /**
   * <p>
   * This method is used to stop adap3d algorithm.
   * </p>
   */
  public void cancel() {
    this.canceled = true;
  }

  /**
   * <p>
   * This method tracks progress of algorithm
   * </p>
   *
   * @return a float.
   */
  public float getFinishedPercent() {
    return progressPercent;
  }
}
