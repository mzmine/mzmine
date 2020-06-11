/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.masscalibration;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.modules.dataprocessing.masscalibration.errormodeling.*;
import io.github.mzmine.modules.dataprocessing.masscalibration.standardslist.StandardsList;
import io.github.mzmine.modules.dataprocessing.masscalibration.standardslist.StandardsListItem;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Class for calibrating mass spectra
 */
public class MassCalibrator {

  protected static final ErrorType massError = new PpmError();

  protected final double retentionTimeSecTolerance;
  protected final double mzRatioTolerance;
  protected final double errorDistributionDistance;
  protected final StandardsList standardsList;

  protected final Logger logger;

  protected int all, zero, single, multiple = 0;
  protected int massListsCount = 0;

  /**
   * Create new mass calibrator
   *
   * @param retentionTimeSecTolerance max difference in RT between standard calibrants and actual mz peaks
   * @param mzRatioTolerance          max difference in mz ratio between standard calibrants and actual mz peaks
   * @param errorDistributionDistance clustering distance parameter for extracting high density range of errors
   *                                  that are meant to approximate the set of substantial errors to the bias estimate
   * @param standardsList             list of standard calibrants used for m/z peaks matching and bias estimation
   */
  public MassCalibrator(double retentionTimeSecTolerance, double mzRatioTolerance,
                        double errorDistributionDistance, StandardsList standardsList) {
    this.retentionTimeSecTolerance = retentionTimeSecTolerance;
    this.mzRatioTolerance = mzRatioTolerance;
    this.errorDistributionDistance = errorDistributionDistance;
    this.standardsList = standardsList;

    this.logger = Logger.getLogger(this.getClass().getName());
  }

  /**
   * Find a list of errors from a mass list at certain retention time
   * all the m/z peaks are matched against the list of standard calibrants used
   * and when a match is made, the error is calculated and added to the list
   * currently, ppm errors are used by default, as per massError instantiation above
   *
   * @param massList
   * @param retentionTimeSec
   * @return
   */
  public ArrayList<Double> findMassListErrors(DataPoint[] massList, double retentionTimeSec) {
    List<MassPeakMatch> matches = matchPeaksWithCalibrants(massList, retentionTimeSec);
    ArrayList<Double> errors = getErrors(matches);
    return errors;
  }

  /**
   * Estimate measurement bias from errors
   *
   * @param errors list of errors
   * @return measurement bias estimate
   */
  public double estimateBiasFromErrors(List<Double> errors) {
    DistributionRange range = DistributionExtractor.fixedLengthRange(errors, 2);
    DistributionRange stretchedRange = DistributionExtractor.fixedToleranceExtensionRange(range,
            errorDistributionDistance);
    double stretchedRangeBiasEstimate = BiasEstimator.arithmeticMean(stretchedRange.getExtractedItems());
    return stretchedRangeBiasEstimate;
  }

  /**
   * Calibrates the mass list
   * shifts all m/z peaks against a bias estimate
   * bias estimate is currently given by an estimate of an overall ppm error of mass measurement
   * should be obtained by other methods in this class
   *
   * @param massList     the list of mz peaks to calibrate
   * @param biasEstimate bias estimate against which the mass list should be calibrated
   * @return new mass calibrated list of mz peaks
   */
  public DataPoint[] calibrateMassList(DataPoint[] massList, double biasEstimate) {
    massListsCount++;

    DataPoint[] calibratedMassList = new DataPoint[massList.length];
    for (int i = 0; i < massList.length; i++) {
      DataPoint oldDataPoint = massList[i];
      double oldMz = oldDataPoint.getMZ();
      double calibratedMz = massError.calibrateAgainstError(oldMz, biasEstimate);
      calibratedMassList[i] = new SimpleDataPoint(calibratedMz, oldDataPoint.getIntensity());
    }

    return calibratedMassList;
  }

  /**
   * Returns a list of errors of mass measurement given a list of mass peak matches
   *
   * @param mzMatches
   * @return
   */
  protected ArrayList<Double> getErrors(List<MassPeakMatch> mzMatches) {
    ArrayList<Double> errors = new ArrayList<>();
    for (MassPeakMatch match : mzMatches) {
      errors.add(massError.calculateError(match.getMeasuredMzRatio(), match.getMatchedMzRatio()));
    }
    return errors;
  }

  /**
   * Match mz peaks with standard calibrants using provided tolerance values
   * when more than single calibrant is within the tolerance no match is made
   * as the peak might correspond to different ions, giving different mz error in later calibration stages
   *
   * @param massList
   * @param retentionTimeSec
   * @return list of mass peak matches
   */
  protected ArrayList<MassPeakMatch> matchPeaksWithCalibrants(DataPoint[] massList, double retentionTimeSec) {
    ArrayList<MassPeakMatch> matches = new ArrayList<>();

    Range<Double> rtSecRange = Range.closed(retentionTimeSec - retentionTimeSecTolerance,
            retentionTimeSec + retentionTimeSecTolerance);

    for (DataPoint dataPoint : massList) {
      double mz = dataPoint.getMZ();
      Range<Double> mzRange = Range.closed(mz - mzRatioTolerance, mz + mzRatioTolerance);

      List<StandardsListItem> dataPointMatches = standardsList.getInRanges(mzRange, rtSecRange);

      all++;

      if (dataPointMatches.size() > 1) {
        multiple++;
        continue;
      }

      if (dataPointMatches.size() != 1) {
        zero++;
        continue;
      }

      single++;

      StandardsListItem matchedItem = dataPointMatches.get(0);
      double matchedMz = matchedItem.getMzRatio();
      double matchedRetentionTime = matchedItem.getRetentionTimeSec();

      matches.add(new MassPeakMatch(mz, retentionTimeSec, matchedMz, matchedRetentionTime));
    }

    return matches;
  }
}
