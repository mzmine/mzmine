/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.impl.masslist.SimpleMassList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_masscalibration.MassCalibrationParameters.BiasEstimationChoice;
import io.github.mzmine.modules.dataprocessing.featdet_masscalibration.MassCalibrationParameters.MassPeakMatchingChoice;
import io.github.mzmine.modules.dataprocessing.featdet_masscalibration.MassCalibrationParameters.RangeExtractionChoice;
import io.github.mzmine.modules.dataprocessing.featdet_masscalibration.charts.ArithmeticMeanKnnTrend;
import io.github.mzmine.modules.dataprocessing.featdet_masscalibration.charts.OLSRegressionTrend;
import io.github.mzmine.modules.dataprocessing.featdet_masscalibration.charts.Trend2D;
import io.github.mzmine.modules.dataprocessing.featdet_masscalibration.errormodeling.DistributionRange;
import io.github.mzmine.modules.dataprocessing.featdet_masscalibration.standardslist.StandardsList;
import io.github.mzmine.modules.dataprocessing.featdet_masscalibration.standardslist.StandardsListExtractor;
import io.github.mzmine.modules.dataprocessing.featdet_masscalibration.standardslist.StandardsListExtractorFactory;
import io.github.mzmine.modules.dataprocessing.featdet_masscalibration.standardslist.UniversalCalibrantsListCsvExtractor;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.combonested.NestedCombo;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.logging.Logger;
import javafx.collections.ObservableList;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.data.xy.XYSeries;

/**
 * Mass calibration task with preview run flag
 */
public class MassCalibrationTask extends AbstractTask {

  protected static boolean runCalibrationOnPreview = false;

  private final Logger logger = Logger.getLogger(this.getClass().getName());
  private final ParameterSet parameters;
  private final RawDataFile dataFile;

  // scan counter
  protected int processedScans = 0, totalScans;
  protected ObservableList<Scan> scanNumbers;

  // task timer
  protected Long startMillis;
  protected Long endMillis;

  // method parameters
  // standards list
  protected String standardsListFilename = null;
  protected StandardsListExtractor standardsListExtractor;
  protected StandardsList standardsList;
  // tolerances
  protected MZTolerance mzRatioTolerance = null;
  protected RTTolerance rtTolerance = null;
  // error trend
  protected Trend2D errorTrend = null;

  // mass calibrator and data passed between it
  protected MassCalibrator massCalibrator;
  protected ArrayList<MassPeakMatch> massPeakMatches = new ArrayList<>();
  protected ArrayList<Double> errors = new ArrayList<>();
  protected HashMap<String, DistributionRange> errorRanges = new HashMap<>();
  protected double biasEstimate;

  private final MemoryMapStorage storageMemoryMap;
  protected boolean previewRun = false;
  protected Runnable afterHook = null;

  /**
   * @param dataFile
   * @param parameters
   * @param storageMemoryMap
   * @param previewRun
   */
  public MassCalibrationTask(RawDataFile dataFile, ParameterSet parameters,
      @Nullable MemoryMapStorage storageMemoryMap, boolean previewRun, @NotNull Instant moduleCallDate) {
    super(storageMemoryMap, moduleCallDate);
    this.dataFile = dataFile;
    this.parameters = parameters;
    this.storageMemoryMap = storageMemoryMap;
    this.previewRun = previewRun;

  }

  public MassCalibrationTask(RawDataFile dataFile, ParameterSet parameters,
      MemoryMapStorage storageMemoryMap, @NotNull Instant moduleCallDate) {
    this(dataFile, parameters, storageMemoryMap, false, moduleCallDate);
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

  @Override
  public TaskPriority getTaskPriority() {
    if (previewRun) {
      return TaskPriority.HIGH;
    }
    return TaskPriority.NORMAL;
  }

  public Runnable getAfterHook() {
    return afterHook;
  }

  public void setAfterHook(Runnable afterHook) {
    this.afterHook = afterHook;
  }

  /**
   * @see Runnable#run()
   */
  public void run() {
    runTask();
    if (afterHook != null && isCanceled() == false) {
      afterHook.run();
    }
    if(!previewRun) {
      dataFile.getAppliedMethods().add(
          new SimpleFeatureListAppliedMethod(MassCalibrationModule.class, parameters,
              getModuleCallDate()));
    }
  }

  /**
   * @see Runnable#run()
   */
  public void runTask() {
    setStatus(TaskStatus.PROCESSING);
    logger.info("Started mass calibration on " + dataFile);
    startMillis = System.currentTimeMillis();

    boolean extractionOk = extractStandardsList();
    if (extractionOk == false) {
      endMillis = System.currentTimeMillis();
      return;
    }

    Double intensityThreshold =
        parameters.getParameter(MassCalibrationParameters.intensityThreshold).getValue();
    Boolean filterDuplicates =
        parameters.getParameter(MassCalibrationParameters.duplicateErrorFilter).getValue();
    extractToleranceParameters();

    extractErrorTrend();

    massCalibrator = null;
    NestedCombo rangeExtractionMethod =
        parameters.getParameter(MassCalibrationParameters.rangeExtractionMethod).getValue();
    ParameterSet rangeParameterSet = rangeExtractionMethod.getCurrentChoiceParameterSet();

    if (rangeExtractionMethod.isCurrentChoice(RangeExtractionChoice.RANGE_METHOD)) {
      Double tolerance =
          rangeParameterSet.getParameter(MassCalibrationParameters.errorRangeTolerance).getValue();
      Double rangeSize =
          rangeParameterSet.getParameter(MassCalibrationParameters.errorRangeSize).getValue();
      massCalibrator = new MassCalibrator(rtTolerance, mzRatioTolerance, tolerance, rangeSize,
          standardsList, errorTrend);
    } else if (rangeExtractionMethod.isCurrentChoice(RangeExtractionChoice.PERCENTILE_RANGE)) {
      Range<Double> percentileRange =
          rangeParameterSet.getParameter(MassCalibrationParameters.percentileRange).getValue();
      massCalibrator = new MassCalibrator(rtTolerance, mzRatioTolerance, percentileRange,
          standardsList, errorTrend);
    }


    scanNumbers = dataFile.getScans();
    totalScans = scanNumbers.size();

    // Check if we have at least one scan with a mass list
    boolean haveMassList = false;
    for (int i = 0; i < totalScans; i++) {
      Scan scan = scanNumbers.get(i);
      MassList massList = scan.getMassList();
      if (massList != null) {
        haveMassList = true;
        break;
      }
    }
    if (!haveMassList) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage(dataFile.getName() + " has no mass list");
      endMillis = System.currentTimeMillis();
      return;
    }

    // obtain errors from all scans
    for (int i = 0; i < totalScans; i++) {

      if (isCanceled()) {
        endMillis = System.currentTimeMillis();
        return;
      }

      Scan scan = scanNumbers.get(i);

      MassList massList = scan.getMassList();

      // Skip those scans which do not have a mass list of given name
      if (massList == null) {
        processedScans++;
        continue;
      }

      DataPoint[] mzPeaks = massList.getDataPoints();

      /*
       * List<Double> massListErrors = massCalibrator.findMassListErrors(mzPeaks,
       * scan.getRetentionTime(), massPeakMatches); errors.addAll(massListErrors);
       */

      massCalibrator.addMassList(mzPeaks, scan.getRetentionTime(), scanNumbers.get(i),
          intensityThreshold);

      processedScans++;
    }

    massPeakMatches = massCalibrator.getAllMassPeakMatches();
    Collections.sort(massPeakMatches, MassPeakMatch.measuredMzComparator);
    errors = massCalibrator.getAllMzErrors();
    Collections.sort(errors);

    if (errors.size() == 0) {
      String warningMessage =
          "No matches were made between the extracted standards list and the mass lists"
              + " in the selected raw datafile. The module will continue to calibrate mass lists using"
              + " no matches, the bias estimate is zero, so the mass peaks will be shifted by zero.";
      logger.warning("Mass calibration warning: " + warningMessage);
      if (previewRun == false) {
        MZmineCore.getDesktop().displayMessage("Mass calibration warning", warningMessage);
      }
    }

    // Collections.sort(errors);
    // biasEstimate = massCalibrator.estimateBiasFromErrors(errors, filterDuplicates, errorRanges);
    biasEstimate = massCalibrator.estimateBias(filterDuplicates);
    errorRanges = massCalibrator.getErrorRanges();

    if (runCalibrationOnPreview == false && previewRun) {
      endMillis = System.currentTimeMillis();
      setStatus(TaskStatus.FINISHED);
      logger.info(
          "Finished mass calibration on " + dataFile + ", running time: " + getRunningTimeString());
      return;
    }

    // mass calibrate all mass lists
    for (int i = 0; i < totalScans; i++) {

      if (isCanceled()) {
        endMillis = System.currentTimeMillis();
        return;
      }

      Scan scan = scanNumbers.get(i);
      MassList massList = scan.getMassList();

      // Skip those scans which do not have a mass list of given name
      if (massList == null) {
        processedScans++;
        continue;
      }

      DataPoint[] mzPeaks = massList.getDataPoints();

      // DataPoint[] newMzPeaks = massCalibrator.calibrateMassList(mzPeaks, biasEstimate);
      DataPoint[] newMzPeaks = massCalibrator.calibrateMassList(mzPeaks);

      MassList newMassList =
          SimpleMassList.create(storageMemoryMap, newMzPeaks);

      scan.addMassList(newMassList);

      processedScans++;
    }

    endMillis = System.currentTimeMillis();
    setStatus(TaskStatus.FINISHED);
    logger.info(
        "Finished mass calibration on " + dataFile + ", running time: " + getRunningTimeString());

  }

  protected boolean extractStandardsList() {
    NestedCombo massPeakMatchingMethod =
        parameters.getParameter(MassCalibrationParameters.referenceLibrary).getValue();
    try {
      if (massPeakMatchingMethod.isCurrentChoice(MassPeakMatchingChoice.UNIVERSAL_CALIBRANTS)) {
        String universalCalibrantsIonizationMode =
            massPeakMatchingMethod.getCurrentChoiceParameterSet()
                .getParameter(MassCalibrationParameters.ionizationMode).getValue();
        String universalCalibrantsFilename =
            MassCalibrationParameters.ionizationModeChoices.get(universalCalibrantsIonizationMode);
        try (InputStream is = getClass().getClassLoader()
            .getResourceAsStream(universalCalibrantsFilename)) {
          UniversalCalibrantsListCsvExtractor extractor = new UniversalCalibrantsListCsvExtractor(
              universalCalibrantsFilename, is);
          standardsListExtractor = extractor;
          standardsList = standardsListExtractor.extractStandardsList();
        }
      } else {
        standardsListFilename = massPeakMatchingMethod.getChoices()
            .get(MassPeakMatchingChoice.STANDARDS_LIST.toString())
            .getParameter(MassCalibrationParameters.standardsList).getValue().getAbsolutePath();
        standardsListExtractor =
            StandardsListExtractorFactory.createFromFilename(standardsListFilename, false);
        standardsList = standardsListExtractor.extractStandardsList();
      }

      if (standardsList.getStandardMolecules().size() == 0) {
        throw new RuntimeException(
            "Empty standards list extracted, make sure the file adheres to the expected"
                + " format, first column is retention time given in minutes, second column is ion formula string.");
      }

    } catch (Exception e) {
      error("Exception when extracting standards list from " + standardsListFilename, e);
      return false;
    }
    return true;
  }

  protected void extractToleranceParameters() {
    NestedCombo massPeakMatchingMethod =
        parameters.getParameter(MassCalibrationParameters.referenceLibrary).getValue();
    ParameterSet massPeakMatchingParameterSet =
        massPeakMatchingMethod.getCurrentChoiceParameterSet();
    if (massPeakMatchingMethod.isCurrentChoice(MassPeakMatchingChoice.STANDARDS_LIST)) {
      mzRatioTolerance = massPeakMatchingParameterSet
          .getParameter(MassCalibrationParameters.mzToleranceSCL).getValue();
      rtTolerance = massPeakMatchingParameterSet
          .getParameter(MassCalibrationParameters.retentionTimeTolerance).getValue();
    } else if (massPeakMatchingMethod
        .isCurrentChoice(MassPeakMatchingChoice.UNIVERSAL_CALIBRANTS)) {
      mzRatioTolerance = massPeakMatchingParameterSet
          .getParameter(MassCalibrationParameters.mzToleranceUCL).getValue();
      rtTolerance = null;
    }
  }

  protected void extractErrorTrend() {
    NestedCombo trendMethod =
        parameters.getParameter(MassCalibrationParameters.biasEstimationMethod).getValue();
    ParameterSet trendParameterSet = trendMethod.getCurrentChoiceParameterSet();

    if (trendMethod.isCurrentChoice(BiasEstimationChoice.KNN_REGRESSION)) {
      Double percentageNeighbors = trendParameterSet
          .getParameter(MassCalibrationParameters.nearestNeighborsPercentage).getValue();
      errorTrend = new ArithmeticMeanKnnTrend(percentageNeighbors / 100.0);
    } else if (trendMethod.isCurrentChoice(BiasEstimationChoice.OLS_REGRESSION)) {
      Integer polynomialDegree =
          trendParameterSet.getParameter(MassCalibrationParameters.polynomialDegree).getValue();
      Boolean exponentialFeature =
          trendParameterSet.getParameter(MassCalibrationParameters.exponentialFeature).getValue();
      Boolean logarithmicFeature =
          trendParameterSet.getParameter(MassCalibrationParameters.logarithmicFeature).getValue();
      errorTrend = new OLSRegressionTrend(polynomialDegree, exponentialFeature, logarithmicFeature);
    }
  }

  /**
   * Get running time of this task in milliseconds if it was not started and then finished, then
   * returns null
   *
   * @return
   */
  public Long getRunningTimeMillis() {
    if (startMillis != null && endMillis != null) {
      return endMillis - startMillis;
    }
    return null;
  }


  /**
   * Get running time of this task formatted as a string if it was not started and then finished,
   * then returns null
   *
   * @return
   */
  public String getRunningTimeString() {
    Long runningTimeMillis = getRunningTimeMillis();
    if (runningTimeMillis == null) {
      return null;
    }
    return DurationFormatUtils.formatDuration(runningTimeMillis, "ss.S's'");
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
