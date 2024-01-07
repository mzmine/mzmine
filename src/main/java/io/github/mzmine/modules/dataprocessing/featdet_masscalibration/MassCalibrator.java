/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.featdet_masscalibration;

import io.github.mzmine.datamodel.Scan;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import org.jfree.data.xy.XYSeries;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.modules.dataprocessing.featdet_masscalibration.charts.Trend2D;
import io.github.mzmine.modules.dataprocessing.featdet_masscalibration.errormodeling.BiasEstimator;
import io.github.mzmine.modules.dataprocessing.featdet_masscalibration.errormodeling.DistributionExtractor;
import io.github.mzmine.modules.dataprocessing.featdet_masscalibration.errormodeling.DistributionRange;
import io.github.mzmine.modules.dataprocessing.featdet_masscalibration.errormodeling.errortypes.ErrorType;
import io.github.mzmine.modules.dataprocessing.featdet_masscalibration.errormodeling.errortypes.PpmError;
import io.github.mzmine.modules.dataprocessing.featdet_masscalibration.standardslist.StandardsList;
import io.github.mzmine.modules.dataprocessing.featdet_masscalibration.standardslist.StandardsListItem;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;

/**
 * Class for calibrating mass spectra
 */
public class MassCalibrator {

  public static final ErrorType massError = new PpmError();

  protected final RTTolerance retentionTimeTolerance;
  protected final MZTolerance mzRatioTolerance;
  protected final StandardsList standardsList;
  protected MassCalibrationParameters.RangeExtractionChoice rangeExtractionMethod;
  protected double errorDistributionDistance;
  protected double errorMaxRangeLength;
  protected Range<Double> percentileRange;

  protected final Logger logger;
  protected int all, zero, single, multiple = 0;
  protected int massListsCount = 0;

  protected ArrayList<MassPeakMatch> massPeakMatches = new ArrayList<>();
  // protected ArrayList<Double> mzErrors = new ArrayList<>();
  protected HashMap<String, DistributionRange> errorRanges = new HashMap<>();
  protected double biasEstimate;
  protected DistributionRange extractedRange;
  protected Trend2D errorVsMzTrend;
  protected XYSeries errorVsMzSeries;


  /**
   * Create new mass calibrator
   *
   * @param retentionTimeTolerance max difference in RT between standard calibrants and actual mz
   *        peaks
   * @param mzRatioTolerance max difference in mz ratio between standard calibrants and actual mz
   *        peaks
   * @param errorDistributionDistance clustering distance parameter for extracting high density
   *        range of errors that are meant to approximate the set of substantial errors to the bias
   *        estimate
   * @param errorMaxRangeLength max length of the range to be found containing most errors in it
   * @param standardsList list of standard calibrants used for m/z peaks matching and bias
   *        estimation
   * @param errorVsMzTrend trend used for modeling error vs mz ratio for mass peak matches made
   */
  public MassCalibrator(RTTolerance retentionTimeTolerance, MZTolerance mzRatioTolerance,
      double errorDistributionDistance, double errorMaxRangeLength, StandardsList standardsList,
      Trend2D errorVsMzTrend) {
    this.rangeExtractionMethod = MassCalibrationParameters.RangeExtractionChoice.RANGE_METHOD;
    this.errorDistributionDistance = errorDistributionDistance;
    this.errorMaxRangeLength = errorMaxRangeLength;

    this.retentionTimeTolerance = retentionTimeTolerance;
    this.mzRatioTolerance = mzRatioTolerance;
    this.standardsList = standardsList;
    this.errorVsMzTrend = errorVsMzTrend;

    this.logger = Logger.getLogger(this.getClass().getName());
  }

  /**
   * Create new mass calibrator
   *
   * @param retentionTimeTolerance max difference in RT between standard calibrants and actual mz
   *        peaks
   * @param mzRatioTolerance max difference in mz ratio between standard calibrants and actual mz
   *        peaks
   * @param percentileRange percentile range used to extract errors from their distribution
   * @param standardsList list of standard calibrants used for m/z peaks matching and bias
   *        estimation
   * @param errorVsMzTrend trend used for modeling error vs mz ratio for mass peak matches made
   */
  public MassCalibrator(RTTolerance retentionTimeTolerance, MZTolerance mzRatioTolerance,
      Range<Double> percentileRange, StandardsList standardsList, Trend2D errorVsMzTrend) {
    this.rangeExtractionMethod = MassCalibrationParameters.RangeExtractionChoice.PERCENTILE_RANGE;
    this.percentileRange = percentileRange;

    this.retentionTimeTolerance = retentionTimeTolerance;
    this.mzRatioTolerance = mzRatioTolerance;
    this.standardsList = standardsList;
    this.errorVsMzTrend = errorVsMzTrend;

    this.logger = Logger.getLogger(this.getClass().getName());
  }

  public ArrayList<MassPeakMatch> addMassList(DataPoint[] massList, float retentionTime) {
    return addMassList(massList, retentionTime, null, 0.0);
  }

  /**
   * Add mass list to this mass calibrator instance, it performs the mass peak matches and returns a
   * list of them
   *
   * @param massList
   * @param retentionTime
   * @param intensityThreshold
   * @return
   */
  public ArrayList<MassPeakMatch> addMassList(DataPoint[] massList, float retentionTime,
      Scan scanNumber, double intensityThreshold) {
    ArrayList<MassPeakMatch> matches =
        matchPeaksWithCalibrants(massList, retentionTime, scanNumber, intensityThreshold);
    massPeakMatches.addAll(matches);
    return matches;
  }

  /**
   * Find a list of errors from a mass list at certain retention time all the m/z peaks are matched
   * against the list of standard calibrants used and when a match is made, the error is calculated
   * and added to the list currently, ppm errors are used by default, as per massError instantiation
   * above
   *
   * @param massList
   * @param retentionTime
   * @param matchesStore if not null, mass peak matches made in the process will be added to the
   *        list store
   * @return
   */
  public ArrayList<Double> findMassListErrors(DataPoint[] massList, float retentionTime,
      List<MassPeakMatch> matchesStore) {
    List<MassPeakMatch> matches = matchPeaksWithCalibrants(massList, retentionTime);
    if (matchesStore != null) {
      matchesStore.addAll(matches);
    }
    ArrayList<Double> errors = getErrors(matches);
    return errors;
  }

  public ArrayList<Double> findMassListErrors(DataPoint[] massList, float retentionTime) {
    return findMassListErrors(massList, retentionTime, null);
  }

  /**
   * Estimate mass measurement bias using instance mass peak matches
   *
   * @param unique filter out duplicates from the list of errors if unique is set to true
   * @return
   */
  public double estimateBias(boolean unique) {
    Collections.sort(massPeakMatches, MassPeakMatch.mzErrorComparator);
    ArrayList<Double> errors = getAllMzErrors();

    if (errors.size() == 0) {
      biasEstimate = BiasEstimator.arithmeticMean(errors);
      logger.info("Errors zero, bias estimate zero");
      return biasEstimate;
    }

    if (unique) {
      Set<Double> errorsSet = new TreeSet<Double>(errors);
      errors = new ArrayList<Double>(errorsSet);
    }

    extractedRange = extractDistributionErrors(errors);

    if (errorVsMzTrend != null) {
      errorVsMzSeries = buildErrorsVsMzDataset();
      errorVsMzTrend.setDataset(errorVsMzSeries);
    }


    biasEstimate = BiasEstimator.arithmeticMean(extractedRange.getExtractedItems());
    logger.info(String.format("Errors %d, extracted %d, unique %s, bias estimate %f", errors.size(),
        extractedRange.getExtractedItems().size(), unique ? "true" : "false", biasEstimate));

    return biasEstimate;

  }

  protected XYSeries buildErrorsVsMzDataset() {
    XYSeries errorsXY = new XYSeries("PPM errors");
    for (MassPeakMatch match : massPeakMatches) {
      if (extractedRange.getValueRange().contains(match.getMzError())) {
        errorsXY.add(match.getMeasuredMzRatio(), match.getMzError());
      }
    }
    return errorsXY;
  }

  /**
   * Extract error range considered substantial to the mass measurement bias estimation
   *
   * @return
   */
  protected DistributionRange extractDistributionErrors(ArrayList<Double> errors) {
    DistributionRange extractedRange = null;

    if (rangeExtractionMethod == MassCalibrationParameters.RangeExtractionChoice.RANGE_METHOD) {
      if (errorMaxRangeLength != 0) {
        DistributionRange range =
            DistributionExtractor.fixedLengthRange(errors, errorMaxRangeLength);
        DistributionRange stretchedRange =
            DistributionExtractor.fixedToleranceExtensionRange(range, errorDistributionDistance);
        extractedRange = stretchedRange;

        errorRanges.put("High-density range of errors", range);
        if (errorDistributionDistance != 0) {
          errorRanges.put("Tolerance extended range", stretchedRange);
        }

      } else if (errorDistributionDistance != 0) {
        DistributionRange biggestCluster =
            DistributionExtractor.mostPopulatedRangeCluster(errors, errorDistributionDistance);
        extractedRange = biggestCluster;

        errorRanges.put("Biggest range by tolerance", biggestCluster);

      } else {
        DistributionRange wholeRange = DistributionExtractor.wholeRange(errors);
        extractedRange = wholeRange;
      }
    } else if (rangeExtractionMethod == MassCalibrationParameters.RangeExtractionChoice.PERCENTILE_RANGE) {
      DistributionRange range = DistributionExtractor.interpercentileRange(errors,
          percentileRange.lowerEndpoint(), percentileRange.upperEndpoint());
      extractedRange = range;

      errorRanges.put("Percentile range", range);
    }

    return extractedRange;
  }

  /**
   * Estimate measurement bias from errors
   *
   * @param errors list of errors
   * @param unique filter out duplicates from the list of errors if unique is set to true
   * @param errorRangeStore if not null, error ranges extracted in the process will be added to the
   *        map store
   * @return measurement bias estimate
   */
  public double estimateBiasFromErrors(List<Double> errors, boolean unique,
      Map<String, DistributionRange> errorRangeStore) {
    if (errorRangeStore == null) {
      errorRangeStore = new HashMap<>();
    }

    if (errors.size() == 0) {
      double biasEstimate = BiasEstimator.arithmeticMean(errors);
      logger.info("Errors zero, bias estimate zero");
      return biasEstimate;
    }

    if (unique) {
      Set<Double> errorsSet = new TreeSet<Double>(errors);
      errors = new ArrayList<Double>(errorsSet);
    }

    List<Double> extracted = null;
    if (rangeExtractionMethod == MassCalibrationParameters.RangeExtractionChoice.RANGE_METHOD) {
      if (errorMaxRangeLength != 0) {
        DistributionRange range =
            DistributionExtractor.fixedLengthRange(errors, errorMaxRangeLength);
        DistributionRange stretchedRange =
            DistributionExtractor.fixedToleranceExtensionRange(range, errorDistributionDistance);
        extracted = stretchedRange.getExtractedItems();

        errorRangeStore.put("Most populated range", range);
        if (errorDistributionDistance != 0) {
          errorRangeStore.put("Tolerance extended range", stretchedRange);
        }

      } else if (errorDistributionDistance != 0) {
        DistributionRange biggestCluster =
            DistributionExtractor.mostPopulatedRangeCluster(errors, errorDistributionDistance);
        extracted = biggestCluster.getExtractedItems();

        errorRangeStore.put("Biggest range by tolerance", biggestCluster);

      } else {
        DistributionRange wholeRange = DistributionExtractor.wholeRange(errors);
        extracted = errors;

      }
    } else if (rangeExtractionMethod == MassCalibrationParameters.RangeExtractionChoice.PERCENTILE_RANGE) {
      DistributionRange range = DistributionExtractor.interpercentileRange(errors,
          percentileRange.lowerEndpoint(), percentileRange.upperEndpoint());
      extracted = range.getExtractedItems();

      errorRangeStore.put("Percentile range", range);
    }

    double biasEstimate = BiasEstimator.arithmeticMean(extracted);
    logger.info(String.format("Errors %d, extracted %d, unique %s, bias estimate %f", errors.size(),
        extracted.size(), unique ? "true" : "false", biasEstimate));
    return biasEstimate;
  }

  public double estimateBiasFromErrors(List<Double> errors, boolean unique) {
    return estimateBiasFromErrors(errors, unique, null);
  }

  /**
   * Calibrates the mass list shifts all m/z peaks against a bias estimate bias estimate is taken
   * from the instance (global bias estimate or modeled error vs mz trend)
   *
   * @param massList the list of mz peaks to calibrate
   * @return new mass calibrated list of mz peaks
   */
  public DataPoint[] calibrateMassList(DataPoint[] massList) {
    massListsCount++;

    DataPoint[] calibratedMassList = new DataPoint[massList.length];
    for (int i = 0; i < massList.length; i++) {
      DataPoint oldDataPoint = massList[i];
      double oldMz = oldDataPoint.getMZ();
      double calibratedMz;
      if (errorVsMzTrend != null) {
        calibratedMz = massError.calibrateAgainstError(oldMz, errorVsMzTrend.getValue(oldMz));
      } else {
        calibratedMz = massError.calibrateAgainstError(oldMz, biasEstimate);
      }
      calibratedMassList[i] = new SimpleDataPoint(calibratedMz, oldDataPoint.getIntensity());
    }

    return calibratedMassList;
  }

  /**
   * Calibrates the mass list shifts all m/z peaks against a bias estimate bias estimate is
   * currently given by an estimate of an overall ppm error of mass measurement should be obtained
   * by other methods in this class
   *
   * @param massList the list of mz peaks to calibrate
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

  protected ArrayList<MassPeakMatch> matchPeaksWithCalibrants(DataPoint[] massList,
      float retentionTime) {
    return matchPeaksWithCalibrants(massList, retentionTime, null, 0.0);
  }

  /**
   * Match mz peaks with standard calibrants using provided tolerance values when more than single
   * calibrant is within the tolerance no match is made as the peak might correspond to different
   * ions, giving different mz error in later calibration stages for matching purposes consider only
   * mz peaks with intensity equal or above the threshold
   *
   * @param massList
   * @param retentionTime
   * @param intensityThreshold
   * @return list of mass peak matches
   */
  protected ArrayList<MassPeakMatch> matchPeaksWithCalibrants(DataPoint[] massList,
      float retentionTime, Scan scanNumber, double intensityThreshold) {
    ArrayList<MassPeakMatch> matches = new ArrayList<>();

    StandardsList retentionTimeFiltered;
    if (retentionTimeTolerance != null) {
      Range<Float> rtRange = retentionTimeTolerance.getToleranceRange(retentionTime);
      retentionTimeFiltered = standardsList.getInRanges(null, rtRange);
    } else {
      retentionTimeFiltered = standardsList;
    }

    for (DataPoint dataPoint : massList) {
      if (dataPoint.getIntensity() < intensityThreshold) {
        continue;
      }

      double mz = dataPoint.getMZ();
      Range<Double> mzRange = mzRatioTolerance.getToleranceRange(mz);

      List<StandardsListItem> dataPointMatches =
          retentionTimeFiltered.getInRanges(mzRange, null).getStandardMolecules();

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
      double matchedRetentionTime = matchedItem.getRetentionTime();

      // matches.add(new MassPeakMatch(mz, retentionTime, matchedMz, matchedRetentionTime,
      // massError));
      matches.add(new MassPeakMatch(mz, retentionTime, matchedMz, matchedRetentionTime, massError,
          dataPoint, scanNumber, matchedItem));
    }

    return matches;
  }

  public ArrayList<MassPeakMatch> getAllMassPeakMatches() {
    return massPeakMatches;
  }

  public ArrayList<Double> getAllMzErrors() {
    ArrayList<MassPeakMatch> matches = getAllMassPeakMatches();
    ArrayList<Double> mzErrors = new ArrayList<>(matches.size());
    for (int i = 0; i < matches.size(); i++) {
      mzErrors.add(i, matches.get(i).getMzError());
    }
    return mzErrors;
  }

  public HashMap<String, DistributionRange> getErrorRanges() {
    return errorRanges;
  }
}
