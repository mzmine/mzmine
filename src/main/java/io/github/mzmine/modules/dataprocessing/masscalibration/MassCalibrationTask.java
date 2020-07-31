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
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.SimpleMassList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.masscalibration.MassCalibrationParameters.BiasEstimationChoice;
import io.github.mzmine.modules.dataprocessing.masscalibration.MassCalibrationParameters.RangeExtractionChoice;
import io.github.mzmine.modules.dataprocessing.masscalibration.charts.OLSRegressionTrend;
import io.github.mzmine.modules.dataprocessing.masscalibration.charts.Trend2D;
import io.github.mzmine.modules.dataprocessing.masscalibration.charts.WeightedKnnTrend;
import io.github.mzmine.modules.dataprocessing.masscalibration.errormodeling.DistributionRange;
import io.github.mzmine.modules.dataprocessing.masscalibration.standardslist.StandardsList;
import io.github.mzmine.modules.dataprocessing.masscalibration.standardslist.StandardsListExtractor;
import io.github.mzmine.modules.dataprocessing.masscalibration.standardslist.StandardsListExtractorFactory;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.combonested.NestedCombo;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import org.jfree.data.xy.XYSeries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 *
 */
public class MassCalibrationTask extends AbstractTask {

  private final Logger logger = Logger.getLogger(this.getClass().getName());
  private final RawDataFile dataFile;
  // User parameters
  private final String massListName;
  private final String suffix;
  private final boolean autoRemove;
  private final ParameterSet parameters;
  // scan counter
  protected int processedScans = 0, totalScans;
  protected int[] scanNumbers;
  private StandardsListExtractor standardsListExtractor;
  protected ArrayList<MassPeakMatch> massPeakMatches = new ArrayList<>();
  protected ArrayList<Double> errors = new ArrayList<>();
  protected HashMap<String, DistributionRange> errorRanges = new HashMap<>();
  protected double biasEstimate;
  protected boolean previewRun = false;

  protected MassCalibrator massCalibrator;

  /**
   * @param dataFile
   * @param parameters
   */
  public MassCalibrationTask(RawDataFile dataFile, ParameterSet parameters, boolean previewRun) {

    this.dataFile = dataFile;
    this.parameters = parameters;
    this.previewRun = previewRun;

    this.massListName = parameters.getParameter(MassCalibrationParameters.massList).getValue();

    this.suffix = parameters.getParameter(MassCalibrationParameters.suffix).getValue();
    this.autoRemove = parameters.getParameter(MassCalibrationParameters.autoRemove).getValue();

  }

  public MassCalibrationTask(RawDataFile dataFile, ParameterSet parameters) {
    this(dataFile, parameters, false);
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getTaskDescription()
   */
  public String getTaskDescription() {
    return "Calibrating mass in " + dataFile;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  public double getFinishedPercentage() {
    if (totalScans == 0)
      return 0;
    else
      // processed scans are added twice, when errors are obtain and when mass lists are shifted
      // so to get finished percentage of the task, divide processed scans by double total scans
      return (double) processedScans / totalScans / 2;
  }

  public RawDataFile getDataFile() {
    return dataFile;
  }

  /**
   * @see Runnable#run()
   */
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    logger.info("Started mass calibration on " + dataFile);

    String standardsListFilename = null;
    StandardsList standardsList;
    try {
      standardsListFilename = parameters.getParameter(MassCalibrationParameters.standardsList).getValue()
              .getAbsolutePath();
      standardsListExtractor = StandardsListExtractorFactory.createFromFilename(standardsListFilename);
      standardsList = standardsListExtractor.extractStandardsList();

      if (standardsList.getStandardMolecules().size() == 0) {
        throw new RuntimeException("Empty standards list extracted, make sure the file adheres to the expected" +
                " format, first column is retention time given in minutes, second column is ion formula string.");
      }

    } catch (Exception e) {
      logger.warning("Exception when extracting standards list from " + standardsListFilename);
      logger.warning(e.toString());
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Exception when extracting standards list from " + standardsListFilename + "\n" + e.toString());
      return;
    }

    Boolean filterDuplicates = parameters.getParameter(MassCalibrationParameters.filterDuplicates).getValue();
    MZTolerance mzRatioTolerance = parameters.getParameter(MassCalibrationParameters.mzRatioTolerance).getValue();
    RTTolerance rtTolerance = parameters.getParameter(MassCalibrationParameters.retentionTimeTolerance).getValue();

    massCalibrator = null;
    ParameterSet rangeParameterSet;
    NestedCombo rangeExtractionMethod = parameters.
            getParameter(MassCalibrationParameters.rangeExtractionMethod).getValue();

    ParameterSet trendParameterSet;
    NestedCombo trendMethod = parameters.getParameter(MassCalibrationParameters.biasEstimationMethod).getValue();
    Trend2D errorTrend = null;

    if (trendMethod.getCurrentChoice().equals(BiasEstimationChoice.KNN_REGRESSION.toString())) {
      trendParameterSet = trendMethod.getChoices().get(BiasEstimationChoice.KNN_REGRESSION.toString());
      Double percentageNeighbors =
              trendParameterSet.getParameter(MassCalibrationParameters.nearestNeighborsPercentage).getValue();
      errorTrend = new WeightedKnnTrend(percentageNeighbors / 100.0);
    }
    else if (trendMethod.getCurrentChoice().equals(BiasEstimationChoice.OLS_REGRESSION.toString())) {
      trendParameterSet = trendMethod.getChoices().get(BiasEstimationChoice.OLS_REGRESSION.toString());
      Integer polynomialDegree = trendParameterSet.getParameter(MassCalibrationParameters.polynomialDegree).getValue();
      Boolean exponentialFeature = trendParameterSet.getParameter(MassCalibrationParameters.exponentialFeature)
              .getValue();
      Boolean logarithmicFeature = trendParameterSet.getParameter(MassCalibrationParameters.logarithmicFeature)
              .getValue();
      errorTrend = new OLSRegressionTrend(polynomialDegree, exponentialFeature, logarithmicFeature);
    }



    if (rangeExtractionMethod.getCurrentChoice().equals(RangeExtractionChoice.RANGE_METHOD.toString())) {
      rangeParameterSet = rangeExtractionMethod.getChoices().get(RangeExtractionChoice.RANGE_METHOD.toString());
      Double tolerance = rangeParameterSet.getParameter(MassCalibrationParameters.tolerance).getValue();
      Double rangeSize = rangeParameterSet.getParameter(MassCalibrationParameters.rangeSize).getValue();
      massCalibrator = new MassCalibrator(rtTolerance, mzRatioTolerance, tolerance, rangeSize, standardsList, errorTrend);
    } else if (rangeExtractionMethod.getCurrentChoice().equalsIgnoreCase(RangeExtractionChoice.PERCENTILE_RANGE.toString())) {
      rangeParameterSet = rangeExtractionMethod.getChoices().get(RangeExtractionChoice.PERCENTILE_RANGE.toString());
      Range<Double> percentileRange = rangeParameterSet.getParameter(MassCalibrationParameters.percentileRange).getValue();
      massCalibrator = new MassCalibrator(rtTolerance, mzRatioTolerance, percentileRange, standardsList, errorTrend);
    }


    scanNumbers = dataFile.getScanNumbers();
    totalScans = scanNumbers.length;

    // Check if we have at least one scan with a mass list of given name
    boolean haveMassList = false;
    for (int i = 0; i < totalScans; i++) {
      Scan scan = dataFile.getScan(scanNumbers[i]);
      MassList massList = scan.getMassList(massListName);
      if (massList != null) {
        haveMassList = true;
        break;
      }
    }
    if (!haveMassList) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage(dataFile.getName() + " has no mass list called '" + massListName + "'");
      return;
    }

    // obtain errors from all scans
    for (int i = 0; i < totalScans; i++) {

      if (isCanceled())
        return;

      Scan scan = dataFile.getScan(scanNumbers[i]);

      MassList massList = scan.getMassList(massListName);

      // Skip those scans which do not have a mass list of given name
      if (massList == null) {
        processedScans++;
        continue;
      }

      DataPoint[] mzPeaks = massList.getDataPoints();

      /*List<Double> massListErrors = massCalibrator.findMassListErrors(mzPeaks, scan.getRetentionTime(),
              massPeakMatches);
      errors.addAll(massListErrors);*/

      massCalibrator.addMassList(mzPeaks, scan.getRetentionTime());

      processedScans++;
    }

    massPeakMatches = massCalibrator.getAllMassPeakMatches();
    errors = massCalibrator.getAllMzErrors();
    Collections.sort(errors);

    if (errors.size() == 0) {
      String warningMessage = "No matches were made between the extracted standards list and the mass lists" +
              " in the selected raw datafile. The module will continue to calibrate mass lists using" +
              " no matches, the bias estimate is zero, so the mass peaks will be shifted by zero.";
      logger.warning("Mass calibration warning: " + warningMessage);
      if (previewRun == false) {
        MZmineCore.getDesktop().displayMessage("Mass calibration warning", warningMessage);
      }
    }

//    Collections.sort(errors);
//    biasEstimate = massCalibrator.estimateBiasFromErrors(errors, filterDuplicates, errorRanges);
    biasEstimate = massCalibrator.estimateBias(filterDuplicates);
    errorRanges = massCalibrator.getErrorRanges();

    // mass calibrate all mass lists
    for (int i = 0; i < totalScans; i++) {

      if (isCanceled())
        return;

      Scan scan = dataFile.getScan(scanNumbers[i]);

      MassList massList = scan.getMassList(massListName);

      // Skip those scans which do not have a mass list of given name
      if (massList == null) {
        processedScans++;
        continue;
      }

      DataPoint[] mzPeaks = massList.getDataPoints();

//      DataPoint[] newMzPeaks = massCalibrator.calibrateMassList(mzPeaks, biasEstimate);
      DataPoint[] newMzPeaks = massCalibrator.calibrateMassList(mzPeaks);

      SimpleMassList newMassList =
              new SimpleMassList(massListName + " " + suffix, scan, newMzPeaks);

      scan.addMassList(newMassList);

      // Remove old mass list
      if (autoRemove && previewRun == false)
        scan.removeMassList(massList);

      processedScans++;
    }

    setStatus(TaskStatus.FINISHED);

    logger.info("Finished mass calibration on " + dataFile);

  }

  public ArrayList<MassPeakMatch> getMassPeakMatches() {
    return massPeakMatches;
  }

  public ArrayList<Double> getErrors() {
    return errors;
  }

  public HashMap<String, DistributionRange> getErrorRanges() {
    return errorRanges;
  }

  public double getBiasEstimate() {
    return biasEstimate;
  }

  public Trend2D getErrorVsMzTrend() {
    return massCalibrator.errorVsMzTrend;
  }

  public XYSeries getErrorVsMzSeries() {
    return massCalibrator.errorVsMzSeries;
  }

}
