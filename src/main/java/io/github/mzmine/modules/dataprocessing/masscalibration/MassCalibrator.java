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
import io.github.mzmine.testing.FixedLengthRangeBiasEstimator;
import io.github.mzmine.testing.RangeExtenderBiasEstimator;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * Class for calibrating mass spectra
 */
public class MassCalibrator {

  protected final double retentionTimeSecTolerance;
  protected final double mzRatioTolerance;
  protected final double errorDistributionDistance;
  protected final StandardsList standardsList;

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
  }

  /**
   * Calibrates the mass list
   *
   * @param massList         the list of mz peaks to calibrate
   * @param retentionTimeSec the retention time of the scan that the mass list comes from
   * @return new mass calibrated list of mz peaks
   */
  public DataPoint[] calibrateMassList(DataPoint[] massList, double retentionTimeSec) {
    ArrayList<Pair<Double, Double>> mzMatches = matchPeaksWithCalibrants(massList, retentionTimeSec);
    ArrayList<Double> ppmErrors = getPpmErrors(mzMatches);

    FixedLengthRangeBiasEstimator fixedRangeEstimator = new FixedLengthRangeBiasEstimator(ppmErrors, 2);
    double fixedRangeEstimate = fixedRangeEstimator.getBiasEstimate();

    RangeExtenderBiasEstimator rangeExtender = new RangeExtenderBiasEstimator(ppmErrors,
            fixedRangeEstimator.getMostErrorsStart(), fixedRangeEstimator.getMostErrorsEnd(), 5);
    double stretchedRangeEstimate = rangeExtender.getBiasEstimate();

//    DataPoint[] calibratedMassList = massList.clone();
    DataPoint[] calibratedMassList = new DataPoint[massList.length];
    for (int i = 0; i < massList.length; i++) {
      DataPoint oldDataPoint = massList[i];
      calibratedMassList[i] = new SimpleDataPoint(oldDataPoint.getMZ(), oldDataPoint.getIntensity());
    }

    return calibratedMassList;
  }

  /**
   * Returns a list of ppm errors given a list of measured and predicted mz values
   *
   * @param mzMatches
   * @return
   */
  protected ArrayList<Double> getPpmErrors(ArrayList<Pair<Double, Double>> mzMatches) {
    ArrayList<Double> ppmErrors = new ArrayList<>(mzMatches.size());
    for (int i = 0; i < mzMatches.size(); i++) {
      ppmErrors.set(i, (mzMatches.get(i).getLeft() - mzMatches.get(i).getRight()) / mzMatches.get(i).getRight() * 1_000_000);
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
    ArrayList<Pair<Double, Double>> matches = new ArrayList<>();

//    Range<Double> rtSecRange = Range < Double >.
//    closed(retentionTimeSec - retentionTimeSecTolerance, retentionTimeSec + retentionTimeSecTolerance);

    Range<Double> rtSecRange = Range.closed(retentionTimeSec - retentionTimeSecTolerance,
            retentionTimeSec + retentionTimeSecTolerance);

    for (DataPoint dataPoint : massList) {
      double mz = dataPoint.getMZ();
//      Range<Double> mzRange = Range < Double >.closed(mz - mzRatioTolerance, mz + mzRatioTolerance);
      Range<Double> mzRange = Range.closed(mz - mzRatioTolerance, mz + mzRatioTolerance);

      List<StandardsListItem> dataPointMatches = standardsList.getInRanges(mzRange, rtSecRange);

      if (dataPointMatches.size() != 1) {
        continue;
      }

      StandardsListItem matchedItem = dataPointMatches.get(0);
      double matchedMz = matchedItem.getMzRatio();

      matches.add(Pair.of(mz, matchedMz));
    }

    return matches;
  }
}
