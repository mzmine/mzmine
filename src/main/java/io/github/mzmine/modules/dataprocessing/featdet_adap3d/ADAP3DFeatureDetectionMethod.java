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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.msdk.MSDKMethod;
import io.github.msdk.datamodel.Feature;
import io.github.msdk.datamodel.MsScan;
import io.github.msdk.datamodel.RawDataFile;
import io.github.msdk.datamodel.SimpleChromatogram;
import io.github.msdk.datamodel.SimpleFeature;
import io.github.mzmine.modules.dataprocessing.align_adap3.algorithms.ADAP3DPeakDetectionAlgorithm;
import io.github.mzmine.modules.dataprocessing.align_adap3.algorithms.CurveTool;
import io.github.mzmine.modules.dataprocessing.align_adap3.algorithms.SliceSparseMatrix;

/**
 * <p>
 * This class is used to run the whole ADAP3D algorithm and get peaks.
 * </p>
 */
public class ADAP3DFeatureDetectionMethod implements MSDKMethod<List<Feature>> {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final @Nonnull RawDataFile rawFile;
  private final @Nullable Predicate<MsScan> msScanPredicate;
  private final @Nonnull ADAP3DFeatureDetectionParameters parameters;

  private SliceSparseMatrix objSliceSparseMatrix;

  /**
   *  percent of average peak width (found from initial high intensity peaks)
   *  used for determining allowance of peak widths lower bound.
   */
  private static final double LOW_BOUND_PEAK_WIDTH_PERCENT = 0.75;

  private final List<Feature> finalFeatureList;

  private ADAP3DPeakDetectionAlgorithm objPeakDetection;

  private boolean canceled = false;


  /**
   * <p>
   * Constructor
   * </p>
   *
   * @param rawFile {@link RawDataFile} object.
   */
  public ADAP3DFeatureDetectionMethod(@Nonnull RawDataFile rawFile) {
    this(rawFile, s -> true, new ADAP3DFeatureDetectionParameters());
  }
  
  /**
   * <p>
   * Constructor
   * </p>
   *
   * @param rawFile {@link RawDataFile} object.
   * @param msScanPredicate a {@link Predicate} object. Only MsScan which pass
   *        this predicate will be processed.
   */
  public ADAP3DFeatureDetectionMethod(@Nonnull RawDataFile rawFile,
                                      @Nullable Predicate<MsScan> msScanPredicate) {
    this(rawFile, msScanPredicate, new ADAP3DFeatureDetectionParameters());
  }
  
  /**
   * <p>
   * Constructor
   * </p>
   *
   * @param rawFile {@link RawDataFile} object.
   */
  public ADAP3DFeatureDetectionMethod(@Nonnull RawDataFile rawFile,
                                      @Nonnull ADAP3DFeatureDetectionParameters params) {
    this(rawFile, s -> true, params);
  }

  /**
   * <p>
   * Constructor
   * </p>
   *
   * @param rawFile {@link RawDataFile} object.
   * @param msScanPredicate a {@link Predicate} object. Only MsScan which pass
   *        this predicate will be processed.
   */
  public ADAP3DFeatureDetectionMethod(@Nonnull RawDataFile rawFile,
                                      @Nullable Predicate<MsScan> msScanPredicate,
                                      @Nonnull ADAP3DFeatureDetectionParameters parameters) {
    this.rawFile = rawFile;
    this.msScanPredicate = msScanPredicate;
    this.parameters = parameters;
    this.finalFeatureList = new ArrayList<>();
  }

  /**
   * <p>
   * This method performs 3 steps:<br>
   * Step 1. Run ADAP3DPeakDetectionAlgorithm.execute with the default parameters and detect 20
   * highest peaks. <br>
   * Step 2. Estimate new parameters for ADAP3DPeakDetectionAlgorithm from those 20 peaks. <br>
   * Step 3. Run ADAP3DPeakDetectionAlgorithm.execute with the new parameters and detect all other
   * peaks.
   * </p>
   *
   * @return newFeatureList a list of {@link Feature}
   */
  public List<Feature> execute() {

    logger.info("Starting ADAP3D feature detection on file " + rawFile.getName());
    
    // Create SliceSparseMatrix
    logger.debug("Loading the raw data into SliceSparceMatrix");
    this.objSliceSparseMatrix = new SliceSparseMatrix(rawFile, msScanPredicate);
    
    // Here fwhm across all the scans of raw data file is determined.
    logger.debug("Estimating FWHM values across all scans");
    CurveTool objCurveTool = new CurveTool(objSliceSparseMatrix);
    double fwhm = objCurveTool.estimateFwhmMs();
    int roundedFWHM = objSliceSparseMatrix.roundMZ(fwhm);

//    ADAP3DFeatureDetectionParameters objParameters = new ADAP3DFeatureDetectionParameters();

    // Here first 20 peaks are determined to estimate the parameters to determine remaining peaks.
    logger.debug("Detecting 20 highest peaks to determine optimal parameters");
    objPeakDetection = new ADAP3DPeakDetectionAlgorithm(objSliceSparseMatrix);
    List<ADAP3DPeakDetectionAlgorithm.GoodPeakInfo> goodPeakList =
        objPeakDetection.execute(20, parameters, roundedFWHM);

    // If the algorithm's execution is stopped, execute method of PeakDetection class will return
    // zero peaks.
    if (canceled)
      return finalFeatureList;

    // Here we're making features for first 20 peaks and add it into the list of feature.
    logger.debug("Converting 20 highest peaks to MSDK features");

    convertPeaksToFeatures(goodPeakList, finalFeatureList);

    // Estimation of parameters.
    logger.debug("Estimating optimal parameters");
    double[] peakWidth = new double[goodPeakList.size()];
    double avgCoefOverArea = 0.0;
    double avgPeakWidth = 0.0;

    // Average peak width has been determined in terms of retention time.
    for (int i = 0; i < goodPeakList.size(); i++) {
      peakWidth[i] = objSliceSparseMatrix.getRetentionTime(goodPeakList.get(i).upperScanBound) / 60
          - objSliceSparseMatrix.getRetentionTime(goodPeakList.get(i).lowerScanBound) / 60;
      avgPeakWidth += peakWidth[i];
      avgCoefOverArea += goodPeakList.get(i).objResult.coefOverArea;
    }

    avgPeakWidth = avgPeakWidth / goodPeakList.size();
    avgCoefOverArea = avgCoefOverArea / goodPeakList.size();

    int highestWaveletScale = (int) (avgPeakWidth * 60 / 2);
    double coefOverAreaThreshold = avgCoefOverArea / 1.5;


    List<Double> peakWidthList = Arrays.asList(ArrayUtils.toObject(peakWidth));
    double peakDurationLowerBound = avgPeakWidth - LOW_BOUND_PEAK_WIDTH_PERCENT * avgPeakWidth;

    double peakDurationUpperBound =
        Collections.max(peakWidthList) + LOW_BOUND_PEAK_WIDTH_PERCENT * avgPeakWidth;

    // set the estimated parameters.
    parameters.setLargeScaleIn(highestWaveletScale);
    parameters.setMinPeakWidth(peakDurationLowerBound);
    parameters.setMaxPeakWidth(peakDurationUpperBound);
    parameters.setCoefAreaRatioTolerance(coefOverAreaThreshold);

    // run the algorithm with new parameters to determine the remaining peaks.
    logger.debug("Running ADAP3D using optimized parameters");
    List<ADAP3DPeakDetectionAlgorithm.GoodPeakInfo> newGoodPeakList =
        objPeakDetection.execute(parameters, roundedFWHM);

    // If the algorithm's execution is stopped, execute method of PeakDtection class will return
    // null. Hence execute method of this class will also return null.
    if (canceled)
      return null;

    // Here we're making features for remaining peaks and add it into the list of feature.
    logger.debug("Converting all peaks to MSDK features");
    convertPeaksToFeatures(newGoodPeakList, finalFeatureList);

    logger.info("Finished ADAP3D feature detection on file " + rawFile.getName());
    
    return finalFeatureList;
  }



  /**
   * <p>
   * This method takes list of GoodPeakInfo and returns list of type SimpleFeature. This method also
   * builds Chromatogram for each good peak.
   * </p>
   */
  private void convertPeaksToFeatures(List<ADAP3DPeakDetectionAlgorithm.GoodPeakInfo> goodPeakList,
      List<Feature> featureList) {

    int lowerScanBound;
    int upperScanBound;

    for (ADAP3DPeakDetectionAlgorithm.GoodPeakInfo goodPeakInfo : goodPeakList) {

      lowerScanBound = goodPeakInfo.lowerScanBound;
      upperScanBound = goodPeakInfo.upperScanBound;
      double mz = goodPeakInfo.mz;
      float[] rtArray = objSliceSparseMatrix.getRetentionTimeArray(lowerScanBound, upperScanBound);
      float[] intensityArray = objSliceSparseMatrix.getIntensities(goodPeakInfo);
      double[] mzArray = new double[upperScanBound - lowerScanBound + 1];

      for (int j = 0; j < upperScanBound - lowerScanBound + 1; j++) {
        mzArray[j] = mz;
      }

      SimpleChromatogram chromatogram = new SimpleChromatogram();
      chromatogram.setDataPoints(rtArray, mzArray, intensityArray,
          upperScanBound - lowerScanBound + 1);

      SimpleFeature feature = new SimpleFeature();
      feature.setArea(CurveTool.normalize(intensityArray));
      feature.setHeight(goodPeakInfo.maxHeight);

      int maxHeightScanNumber = goodPeakInfo.maxHeightScanNumber;
      float retentionTime = (float) objSliceSparseMatrix.getRetentionTime(maxHeightScanNumber);

      feature.setRetentionTime(retentionTime);
      feature.setMz(mz);
      feature.setChromatogram(chromatogram);
      featureList.add(feature);
    }
  }

  /** {@inheritDoc} */
  @Override
  public Float getFinishedPercentage() {
    if (objPeakDetection != null)
      return objPeakDetection.getFinishedPercent();
    else
      return (float) 0;
  }

  /** {@inheritDoc} */
  @Override
  public List<Feature> getResult() {
    return finalFeatureList;
  }

  /** {@inheritDoc} */
  @Override
  public void cancel() {
    logger.info("Cancelling ADAP3D feature detection on file " + rawFile.getName());
    canceled = true;
    if (objPeakDetection != null) {
      objPeakDetection.cancel();
    }
  }
}
