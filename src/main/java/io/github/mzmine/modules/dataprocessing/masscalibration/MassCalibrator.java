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
import io.github.mzmine.modules.dataprocessing.masscalibration.standardslist.StandardsList;
import io.github.mzmine.modules.dataprocessing.masscalibration.standardslist.StandardsListItem;
import io.github.mzmine.modules.dataprocessing.masscalibration.errormodelling.FixedLengthRangeBiasEstimator;
import io.github.mzmine.modules.dataprocessing.masscalibration.errormodelling.RangeExtenderBiasEstimator;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Class for calibrating mass spectra
 */
public class MassCalibrator {

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
   * @param standardsList             list of standard calibrants used for bias estimation
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
   * Calibrates the mass list
   *
   * @param massList         the list of mz peaks to calibrate
   * @param retentionTimeSec the retention time of the scan that the mass list comes from
   * @return new mass calibrated list of mz peaks
   */
  public DataPoint[] calibrateMassList(DataPoint[] massList, double retentionTimeSec) {
//    all, zero, single, multiple = 0;
    massListsCount++;

    int oldall = all;
    int oldzero = zero;
    int oldsingle = single;
    int oldmultiple = multiple;

//    logger.info("Calibrating mass list at retention time of " + retentionTimeSec + " seconds");
    System.out.println("Calibrating mass list at retention time of " + retentionTimeSec + " seconds");

    ArrayList<Pair<Double, Double>> mzMatches = matchPeaksWithCalibrants(massList, retentionTimeSec);

    System.out.println(String.format("mzmatches length %d", mzMatches.size()));
    if(mzMatches.size() == 0){
//      logger.info("No matches, shifting masses by zero");
      System.out.println("No matches, shifting masses by zero");
      return massList.clone();
    }
    ArrayList<Double> ppmErrors = getPpmErrors(mzMatches);

    FixedLengthRangeBiasEstimator fixedRangeEstimator = new FixedLengthRangeBiasEstimator(ppmErrors, 2);
    double fixedRangeEstimate = fixedRangeEstimator.getBiasEstimate();

    RangeExtenderBiasEstimator rangeExtender = new RangeExtenderBiasEstimator(ppmErrors,
            fixedRangeEstimator.getMostErrorsStart(), fixedRangeEstimator.getMostErrorsEnd(), 5);
    double stretchedRangeEstimate = rangeExtender.getBiasEstimate();

//    logger.info(String.format("Found %d matches, bias estimate %f", ppmErrors.size(), stretchedRangeEstimate));
    System.out.println(String.format("Found %d matches, bias estimate %f", ppmErrors.size(), stretchedRangeEstimate));

    DataPoint[] calibratedMassList = new DataPoint[massList.length];
    for (int i = 0; i < massList.length; i++) {
      DataPoint oldDataPoint = massList[i];
      calibratedMassList[i] = new SimpleDataPoint(oldDataPoint.getMZ() + stretchedRangeEstimate,
              oldDataPoint.getIntensity());
    }

    System.out.printf("Mass list having %d peaks, %d zero matches, %d single matches, %d multiple matches %n",
            all - oldall, zero - oldzero, single - oldsingle, multiple - oldmultiple);
    System.out.printf("All mass list having %d peaks, %d zero matches, %d single matches, %d multiple matches %n",
            all, zero, single, multiple);

    return calibratedMassList;
  }

  /**
   * Returns a list of ppm errors given a list of measured and predicted mz values
   *
   * @param mzMatches
   * @return
   */
  protected ArrayList<Double> getPpmErrors(ArrayList<Pair<Double, Double>> mzMatches) {
//    ArrayList<Double> ppmErrors = new ArrayList<>(mzMatches.size());
    ArrayList<Double> ppmErrors = new ArrayList<>();
    for (int i = 0; i < mzMatches.size(); i++) {
//      System.out.println(ppmErrors.size() + " " + mzMatches.size());
//      ppmErrors.set(i, (mzMatches.get(i).getLeft() - mzMatches.get(i).getRight()) / mzMatches.get(i).getRight() * 1_000_000);
      ppmErrors.add(mzMatches.get(i).getLeft() - mzMatches.get(i).getRight() / mzMatches.get(i).getRight() * 1_000_000);
    }
    return ppmErrors;
  }

  /**
   * Match mz peaks with standard calibrants using provided tolerance values
   * when more than single calibrant is within the tolerance no match is made
   * as the peak might correspond to different ions, giving different mz error in later calibration stages
   *
   * @param massList
   * @param retentionTimeSec
   * @return a list of pairs, left is actual measured mz peak, right is predicted mz value of a matched calibrant
   */
  protected ArrayList<Pair<Double, Double>> matchPeaksWithCalibrants(DataPoint[] massList, double retentionTimeSec) {
//    logger.info(String.format("Matching peaks with calibrants, %d peaks %f rt seconds",
    System.out.println(String.format("Matching peaks with calibrants, %d peaks %f rt seconds",
            massList.length, retentionTimeSec));

    ArrayList<Pair<Double, Double>> matches = new ArrayList<>();

    Range<Double> rtSecRange = Range.closed(retentionTimeSec - retentionTimeSecTolerance,
            retentionTimeSec + retentionTimeSecTolerance);

    for (DataPoint dataPoint : massList) {
      double mz = dataPoint.getMZ();
      Range<Double> mzRange = Range.closed(mz - mzRatioTolerance, mz + mzRatioTolerance);

      List<StandardsListItem> dataPointMatches = standardsList.getInRanges(mzRange, rtSecRange);

      all++;

      if(dataPointMatches.size() > 1){
        System.out.println(String.format("Found %d matches for data point", dataPointMatches.size()));
        multiple++;
        continue;
      }

      if (dataPointMatches.size() != 1) {
          zero++;
//        logger.info(String.format("Found %d matches for data point", dataPointMatches.size()));
        continue;
      }

      single++;


      StandardsListItem matchedItem = dataPointMatches.get(0);
      double matchedMz = matchedItem.getMzRatio();

//      logger.info(String.format("Matched data point mz %f, rt %f with mz %f, rt %f",
      System.out.println(String.format("Matched data point mz %f, rt %f with mz %f, rt %f",
              mz, retentionTimeSec, matchedMz, matchedItem.getRetentionTimeSec()));

      matches.add(Pair.of(mz, matchedMz));
    }

//    logger.info(String.format("Found %d matches", matches.size()));
    System.out.println(String.format("Found %d matches", matches.size()));

    return matches;
  }
}
