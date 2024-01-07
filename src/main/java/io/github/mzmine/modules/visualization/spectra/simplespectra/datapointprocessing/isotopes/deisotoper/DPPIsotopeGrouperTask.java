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

package io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.isotopes.deisotoper;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.IsotopePattern.IsotopePatternStatus;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.impl.SimpleIsotopePattern;
import io.github.mzmine.modules.tools.isotopeprediction.IsotopePatternCalculator;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingController;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingTask;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.ProcessedDataPoint;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.results.DPPIsotopePatternResult;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.results.DPPResultsDataSet;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.taskcontrol.TaskStatusListener;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.javafx.FxColorUtil;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

/**
 *
 * This is basically copy-pasted from
 * io.github.mzmine.modules.peaklistmethods.isotopes.deisotoper.IsotopeGrouperTask
 *
 */
public class DPPIsotopeGrouperTask extends DataPointProcessingTask {

  private static Logger logger = Logger.getLogger(DPPIsotopeGrouperTask.class.getName());

  // peaks counter
  private int processedPeaks, totalPeaks;

  // parameter values
  private MZTolerance mzTolerance;
  private boolean monotonicShape;
  private int maximumCharge;
  private String element = "C";
  private boolean autoRemove;

  public DPPIsotopeGrouperTask(MassSpectrum spectrum, SpectraPlot plot, ParameterSet parameterSet,
      DataPointProcessingController controller, TaskStatusListener listener) {
    super(spectrum, plot, parameterSet, controller, listener);

    // Get parameter values for easier use
    mzTolerance = parameterSet.getParameter(DPPIsotopeGrouperParameters.mzTolerance).getValue();
    monotonicShape =
        parameterSet.getParameter(DPPIsotopeGrouperParameters.monotonicShape).getValue();
    maximumCharge = parameterSet.getParameter(DPPIsotopeGrouperParameters.maximumCharge).getValue();
    // element =
    // parameterSet.getParameter(DPPIsotopeGrouperParameters.element).getValue();
    autoRemove = parameterSet.getParameter(DPPIsotopeGrouperParameters.autoRemove).getValue();
    setDisplayResults(
        parameterSet.getParameter(DPPIsotopeGrouperParameters.displayResults).getValue());

    Color c = FxColorUtil.fxColorToAWT(
        parameterSet.getParameter(DPPIsotopeGrouperParameters.datasetColor).getValue());
    setColor(c);

  }

  @Override
  public void run() {
    if (!checkParameterSet() || !checkValues()) {
      setStatus(TaskStatus.ERROR);
      return;
    }

    if (!FormulaUtils.checkMolecularFormula(element)) {
      setStatus(TaskStatus.ERROR);
      logger.warning(
          "Data point/Spectra processing: Invalid element parameter in " + getTaskDescription());
    }

    if (getDataPoints().getNumberOfDataPoints() == 0) {
      logger.info("Data point/Spectra processing: 0 data points were passed to "
          + getTaskDescription() + " Please check the parameters.");
      setStatus(TaskStatus.CANCELED);
      return;
    }

    /*
     * if (!(getDataPoints() instanceof ProcessedDataPoint[])) { logger.warning(
     * "Data point/Spectra processing: The data points passed to Isotope Grouper were not an instance of processed data points."
     * + " Make sure to run mass detection first."); setStatus(TaskStatus.CANCELED); return; }
     */

    setStatus(TaskStatus.PROCESSING);

    ProcessedDataPoint[] dataPoints = {}; // (ProcessedDataPoint[]) getDataPoints();

    int charges[] = new int[maximumCharge];
    for (int i = 0; i < maximumCharge; i++)
      charges[i] = i + 1;

    IsotopePattern pattern =
        IsotopePatternCalculator.calculateIsotopePattern(element, 0.01, 1, PolarityType.POSITIVE);
    double isotopeDistance = pattern.getMzValue(1) - pattern.getMzValue(0);

    ProcessedDataPoint[] sortedDataPoints = dataPoints.clone();
    Arrays.sort(sortedDataPoints, (d1, d2) -> {
      return -1 * Double.compare(d1.getIntensity(), d2.getIntensity()); // *-1
                                                                        // to
                                                                        // sort
                                                                        // descending
    });

    List<ProcessedDataPoint> deisotopedDataPoints = new ArrayList<>();

    for (int i = 0; i < sortedDataPoints.length; i++) {
      if (isCanceled())
        return;

      DataPoint aPeak = sortedDataPoints[i];

      if (aPeak == null) {
        processedPeaks++;
        continue;
      }

      // Check which charge state fits best around this peak
      int bestFitCharge = 0;
      int bestFitScore = -1;
      Vector<DataPoint> bestFitPeaks = null;
      for (int charge : charges) {

        Vector<DataPoint> fittedPeaks = new Vector<DataPoint>();
        fittedPeaks.add(aPeak);
        fitPattern(fittedPeaks, aPeak, charge, sortedDataPoints, isotopeDistance);

        int score = fittedPeaks.size();
        if ((score > bestFitScore) || ((score == bestFitScore) && (bestFitCharge > charge))) {
          bestFitScore = score;
          bestFitCharge = charge;
          bestFitPeaks = fittedPeaks;
        }
      }

      assert bestFitPeaks != null;

      // Verify the number of detected isotopes. If there is only one
      // isotope, we skip this left the original peak in the feature list.
      if (bestFitPeaks.size() == 1) {
        if (!autoRemove)
          deisotopedDataPoints.add(sortedDataPoints[i]);
        processedPeaks++;
        continue;
      }

      DataPoint[] originalPeaks = bestFitPeaks.toArray(new DataPoint[0]);
      SimpleIsotopePattern newPattern = new SimpleIsotopePattern(originalPeaks, bestFitCharge,
          IsotopePatternStatus.DETECTED, aPeak.toString());

      sortedDataPoints[i].addResult(new DPPIsotopePatternResult(newPattern, bestFitCharge));
      deisotopedDataPoints.add(sortedDataPoints[i]);

      // logger.info("Found isotope pattern for m/z " +
      // dataPoints[i].getMZ() + " size: "
      // + newPattern.getNumberOfDataPoints());

      for (int j = 0; j < sortedDataPoints.length; j++) {
        if (bestFitPeaks.contains(sortedDataPoints[j]))
          sortedDataPoints[j] = null;
      }

      // Update completion rate
      processedPeaks++;
    }

    deisotopedDataPoints.sort((d1, d2) -> {
      return Double.compare(d1.getMZ(), d2.getMZ());
    });

    setResults(deisotopedDataPoints.toArray(new ProcessedDataPoint[0]));
    setStatus(TaskStatus.FINISHED);
  }

  /**
   * Fits isotope pattern around one peak.
   *
   * @param p Pattern is fitted around this peak
   * @param charge Charge state of the fitted pattern
   */
  private void fitPattern(Vector<DataPoint> fittedPeaks, DataPoint p, int charge,
      DataPoint[] sortedPeaks, double isotopeDistance) {

    if (charge == 0) {
      return;
    }

    // Search for peaks before the start peak
    if (!monotonicShape) {
      fitHalfPattern(p, charge, -1, fittedPeaks, sortedPeaks, isotopeDistance);
    }

    // Search for peaks after the start peak
    fitHalfPattern(p, charge, 1, fittedPeaks, sortedPeaks, isotopeDistance);

  }

  /**
   * Helper method for fitPattern. Fits only one half of the pattern.
   *
   * @param p Pattern is fitted around this peak
   * @param charge Charge state of the fitted pattern
   * @param direction Defines which half to fit: -1=fit to peaks before start M/Z, +1=fit to peaks
   *        after start M/Z
   * @param fittedPeaks All matching peaks will be added to this set
   */
  private void fitHalfPattern(DataPoint p, int charge, int direction, Vector<DataPoint> fittedPeaks,
      DataPoint[] sortedPeaks, double isotopeDistance) {

    // Use M/Z and RT of the strongest peak of the pattern (peak 'p')
    double mainMZ = p.getMZ();
    // double mainRT = p.getRT();

    // Variable n is the number of peak we are currently searching. 1=first
    // peak before/after start peak, 2=peak before/after previous, 3=...
    boolean followingPeakFound;
    int n = 1;
    do {

      // Assume we don't find match for n:th peak in the pattern (which
      // will end the loop)
      followingPeakFound = false;

      // Loop through all peaks, and collect candidates for the n:th peak
      // in the pattern
      Vector<DataPoint> goodCandidates = new Vector<DataPoint>();
      for (int ind = 0; ind < sortedPeaks.length; ind++) {

        DataPoint candidatePeak = sortedPeaks[ind];

        if (candidatePeak == null)
          continue;

        // Get properties of the candidate peak
        double candidatePeakMZ = candidatePeak.getMZ();
        // double candidatePeakRT = candidatePeak.getRT();

        // Does this peak fill all requirements of a candidate?
        // - within tolerances from the expected location (M/Z and RT)
        // - not already a fitted peak (only necessary to avoid
        // conflicts when parameters are set too wide)
        double isotopeMZ = candidatePeakMZ - isotopeDistance * direction * n / charge;

        if (mzTolerance.checkWithinTolerance(isotopeMZ, mainMZ)
            // && rtTolerance.checkWithinTolerance(candidatePeakRT,
            // mainRT)
            && (!fittedPeaks.contains(candidatePeak))) {
          goodCandidates.add(candidatePeak);

        }

      }

      // Add all good candidates to the isotope pattern (note: in MZmine
      // 2.3 and older, only the highest candidate was added)
      if (!goodCandidates.isEmpty()) {

        fittedPeaks.addAll(goodCandidates);

        // n:th peak was found, so let's move on to n+1
        n++;
        followingPeakFound = true;
      }

    } while (followingPeakFound);

  }

  /*
   * @Override public String getTaskDescription() { return "Deisotoping of Scan #" +
   * getTargetPlot().getMainScanDataSet().getScan().getScanNumber(); }
   */

  @Override
  public double getFinishedPercentage() {
    if (getDataPoints().getNumberOfDataPoints() == 0)
      return 0.0f;
    return processedPeaks / (double) getDataPoints().getNumberOfDataPoints();
  }

  @Override
  public void displayResults() {
    if (isDisplayResults() || getController().isLastTaskRunning()) {
      getTargetPlot().addDataSet(
          new DPPResultsDataSet("Isotopes (" + getResults().length + ")", getResults()), getColor(),
          false, true);
    }
  }

}
