/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.modules.datapointprocessing.isotopes.deisotoper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.IsotopePattern.IsotopePatternStatus;
import net.sf.mzmine.datamodel.PolarityType;
import net.sf.mzmine.datamodel.impl.SimpleIsotopePattern;
import net.sf.mzmine.modules.datapointprocessing.DataPointProcessingController;
import net.sf.mzmine.modules.datapointprocessing.DataPointProcessingTask;
import net.sf.mzmine.modules.datapointprocessing.datamodel.ProcessedDataPoint;
import net.sf.mzmine.modules.datapointprocessing.datamodel.results.DPPIsotopePatternResult;
import net.sf.mzmine.modules.datapointprocessing.datamodel.results.DPPResult;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopeprediction.IsotopePatternCalculator;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.taskcontrol.TaskStatusListener;
import net.sf.mzmine.util.FormulaUtils;

/**
 * 
 * This is basically copy-pasted from
 * net.sf.mzmine.modules.peaklistmethods.isotopes.deisotoper.IsotopeGrouperTask
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
  private String element;
  private boolean autoRemove;

  public DPPIsotopeGrouperTask(DataPoint[] dataPoints, SpectraPlot plot, ParameterSet parameterSet,
      DataPointProcessingController controller, TaskStatusListener listener) {
    super(dataPoints, plot, parameterSet, controller, listener);

    // Get parameter values for easier use
    mzTolerance = parameterSet.getParameter(DPPIsotopeGrouperParameters.mzTolerance).getValue();
    monotonicShape =
        parameterSet.getParameter(DPPIsotopeGrouperParameters.monotonicShape).getValue();
    maximumCharge = parameterSet.getParameter(DPPIsotopeGrouperParameters.maximumCharge).getValue();
    element = parameterSet.getParameter(DPPIsotopeGrouperParameters.element).getValue();
    autoRemove = parameterSet.getParameter(DPPIsotopeGrouperParameters.autoRemove).getValue();
  }



  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    if (!(getDataPoints() instanceof ProcessedDataPoint[])) {
      logger.warning(
          "The data points passed to Isotope Grouper were not an instance of processed data points. Make sure to run mass detection first.");
      setStatus(TaskStatus.ERROR);
      return;
    }

    if (!FormulaUtils.checkMolecularFormula(element)) {
      setStatus(TaskStatus.ERROR);
      logger.warning("Invalid element parameter in " + this.getClass().getName());
    }

    ProcessedDataPoint[] dataPoints = (ProcessedDataPoint[]) getDataPoints();

    int charges[] = new int[maximumCharge];
    for (int i = 0; i < maximumCharge; i++)
      charges[i] = i + 1;

    IsotopePattern pattern =
        IsotopePatternCalculator.calculateIsotopePattern(element, 0.01, 1, PolarityType.POSITIVE);
    double isotopeDistance =
        pattern.getDataPoints()[1].getMZ() - pattern.getDataPoints()[0].getMZ();

    ProcessedDataPoint[] sortedDataPoints = dataPoints.clone();
    Arrays.sort(sortedDataPoints, (d1, d2) -> {
      return -1*Double.compare(d1.getIntensity(), d2.getIntensity()); // *-1 to sort descending
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
      // isotope, we skip this left the original peak in the peak list.
      if (bestFitPeaks.size() == 1) {
        if (!autoRemove)
          deisotopedDataPoints.add(sortedDataPoints[i]);
        processedPeaks++;
        continue;
      }

      DataPoint[] originalPeaks = bestFitPeaks.toArray(new DataPoint[0]);
      SimpleIsotopePattern newPattern =
          new SimpleIsotopePattern(originalPeaks, IsotopePatternStatus.DETECTED, aPeak.toString());

      sortedDataPoints[i].addResult(new DPPIsotopePatternResult(newPattern));
      deisotopedDataPoints.add(sortedDataPoints[i]);

      // logger.info("Found isotope pattern for m/z " + dataPoints[i].getMZ() + " size: "
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
        double isotopeMZ = candidatePeakMZ - isotopeDistance * direction * n / (double) charge;

        if (mzTolerance.checkWithinTolerance(isotopeMZ, mainMZ)
            // && rtTolerance.checkWithinTolerance(candidatePeakRT, mainRT)
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


  @Override
  public String getTaskDescription() {
    return "Deisotoping of " + getTargetPlot().getName();
  }

  @Override
  public double getFinishedPercentage() {
    if (totalPeaks == 0)
      return 0.0f;
    return (double) processedPeaks / (double) totalPeaks;
  }

}
