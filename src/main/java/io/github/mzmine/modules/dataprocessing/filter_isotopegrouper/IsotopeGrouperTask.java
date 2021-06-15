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

package io.github.mzmine.modules.dataprocessing.filter_isotopegrouper;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IsotopePattern.IsotopePatternStatus;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimpleIsotopePattern;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureListRowSorter;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import org.jetbrains.annotations.Nullable;

/**
 *
 */
class IsotopeGrouperTask extends AbstractTask {

  /**
   * The isotopeDistance constant defines expected distance between isotopes. Actual weight of 1
   * neutron is 1.008665 Da, but part of this mass is consumed as binding energy to other
   * protons/neutrons. Actual mass increase of isotopes depends on chemical formula of the molecule.
   * Since we don't know the formula, we can assume the distance to be ~1.0033 Da, with user-defined
   * tolerance.
   */
  private static final double isotopeDistance = 1.0033;
  private final Logger logger = Logger.getLogger(this.getClass().getName());
  private final MZmineProject project;
  private final ModularFeatureList featureList;
  // parameter values
  private final String suffix;
  private final MZTolerance mzTolerance;
  private final RTTolerance rtTolerance;
  private final boolean useMobilityTolerance;
  private final MobilityTolerance mobilityTolerance;
  private final boolean monotonicShape;
  private final boolean removeOriginal;
  private final boolean chooseMostIntense;
  private final int maximumCharge;
  private final ParameterSet parameters;
  // peaks counter
  private int processedRows, totalRows;

  /**
   *
   */
  IsotopeGrouperTask(MZmineProject project, ModularFeatureList featureList, ParameterSet parameters,
      @Nullable MemoryMapStorage storage) {
    super(storage);

    this.project = project;
    this.featureList = featureList;
    this.parameters = parameters;

    // Get parameter values for easier use
    suffix = parameters.getParameter(IsotopeGrouperParameters.suffix).getValue();
    mzTolerance = parameters.getParameter(IsotopeGrouperParameters.mzTolerance).getValue();
    rtTolerance = parameters.getParameter(IsotopeGrouperParameters.rtTolerance).getValue();
    monotonicShape = parameters.getParameter(IsotopeGrouperParameters.monotonicShape).getValue();
    maximumCharge = parameters.getParameter(IsotopeGrouperParameters.maximumCharge).getValue();
    chooseMostIntense = (Objects
        .equals(parameters.getParameter(IsotopeGrouperParameters.representativeIsotope)
            .getValue(), IsotopeGrouperParameters.ChooseTopIntensity));
    removeOriginal = parameters.getParameter(IsotopeGrouperParameters.autoRemove).getValue();
    useMobilityTolerance = parameters.getParameter(IsotopeGrouperParameters.mobilityTolerace)
        .getValue();
    mobilityTolerance = parameters.getParameter(IsotopeGrouperParameters.mobilityTolerace)
        .getEmbeddedParameter()
        .getValue();
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getTaskDescription()
   */
  @Override
  public String getTaskDescription() {
    return "Isotopic peaks grouper on " + featureList;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    if (totalRows == 0) {
      return 0.0f;
    }
    return (double) processedRows / (double) totalRows;
  }

  /**
   * @see Runnable#run()
   */
  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);
    logger.info("Running isotopic peak grouper on " + featureList);

    // We assume source peakList contains one datafile
    if (featureList.getRawDataFiles().size() > 1) {
      setErrorMessage("Cannot perform deisotoping on aligned feature list.");
      setStatus(TaskStatus.ERROR);
      return;
    }

    // Create a new deisotoped peakList
    ModularFeatureList deisotopedFeatureList = new ModularFeatureList(featureList + " " + suffix,
        getMemoryMapStorage(), featureList.getRawDataFiles());
    deisotopedFeatureList
        .setSelectedScans(featureList.getRawDataFile(0), featureList.getSeletedScans(
            featureList.getRawDataFile(0)));

    // Collect all selected charge states
    int[] charges = new int[maximumCharge];
    for (int i = 0; i < maximumCharge; i++) {
      charges[i] = i + 1;
    }

    // Sort peaks by descending height
    List<FeatureListRow> sortedRows = new ArrayList<>(featureList.getRows());
    sortedRows.sort(new FeatureListRowSorter(SortingProperty.Height, SortingDirection.Descending));

    // Loop through all peaks
    totalRows = sortedRows.size();

    while (!sortedRows.isEmpty()) {

      if (isCanceled()) {
        return;
      }

      ModularFeatureListRow row = (ModularFeatureListRow) sortedRows.get(0);

      // Check if peak was already deleted
      if (row == null) {
        processedRows++;
        continue;
      }

      // Check which charge state fits best around this peak
      int bestFitCharge = 0;
      int bestFitScore = -1;
      List<FeatureListRow> bestFitRows = null;
      for (int charge : charges) {

        List<FeatureListRow> fittedRows = new ArrayList<>();
        fittedRows.add(row);
        fitPattern(fittedRows, row, charge, sortedRows);

        int score = fittedRows.size();
        if ((score > bestFitScore) || ((score == bestFitScore) && (bestFitCharge > charge))) {
          bestFitScore = score;
          bestFitCharge = charge;
          bestFitRows = fittedRows;
        }

      }

      assert bestFitRows != null;

      // Verify the number of detected isotopes. If there is only one
      // isotope, we skip this left the original peak in the feature list.
      if (bestFitRows.size() == 1) {
        deisotopedFeatureList
            .addRow(new ModularFeatureListRow(deisotopedFeatureList, row.getID(), row, true));
        sortedRows.remove(bestFitRows.get(0));
        processedRows++;
        continue;
      }

      // Convert the peak pattern to array
      FeatureListRow[] originalRows = bestFitRows.toArray(new FeatureListRow[0]);

      // Create a new SimpleIsotopePattern
      DataPoint[] isotopes = new DataPoint[bestFitRows.size()];
      for (int i = 0; i < isotopes.length; i++) {
        FeatureListRow p = originalRows[i];
        isotopes[i] = new SimpleDataPoint(p.getAverageMZ(), p.getAverageHeight());
      }
      SimpleIsotopePattern newPattern =
          new SimpleIsotopePattern(isotopes, IsotopePatternStatus.DETECTED, row.toString());

      // Depending on user's choice, we leave either the most intense, or
      // the lowest m/z peak
      if (chooseMostIntense) {
        Arrays.sort(originalRows,
            new FeatureListRowSorter(SortingProperty.Height, SortingDirection.Descending));
      } else {
        Arrays
            .sort(originalRows,
                new FeatureListRowSorter(SortingProperty.MZ, SortingDirection.Ascending));
      }

      // copy row
      FeatureListRow newRow = new ModularFeatureListRow(deisotopedFeatureList,
          originalRows[0].getID(), (ModularFeatureListRow) originalRows[0], true);
      deisotopedFeatureList.addRow(newRow);
      // set isotope pattern
      Feature feature = newRow.getFeatures().get(0);
      feature.setIsotopePattern(newPattern);
      feature.setCharge(bestFitCharge);

      // Remove all peaks already assigned to isotope pattern
      for (FeatureListRow fit : bestFitRows) {
        sortedRows.remove(fit);
      }

      // Update completion rate
      processedRows += bestFitRows.size();
    }

    // Add new feature list to the project
    project.addFeatureList(deisotopedFeatureList);

    // Load previous applied methods
    for (FeatureListAppliedMethod proc : featureList.getAppliedMethods()) {
      deisotopedFeatureList.addDescriptionOfAppliedTask(proc);
    }

    // Add task description to peakList
    deisotopedFeatureList.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod("Isotopic peaks grouper",
            IsotopeGrouperModule.class, parameters));

    // Remove the original peakList if requested
    if (removeOriginal) {
      project.removeFeatureList(featureList);
    }

    logger.info("Finished isotopic peak grouper on " + featureList);
    setStatus(TaskStatus.FINISHED);
  }

  /**
   * Fits isotope pattern around one peak.
   *
   * @param row    Pattern is fitted around this peak
   * @param charge Charge state of the fitted pattern
   */
  private void fitPattern(List<FeatureListRow> fittedRows, ModularFeatureListRow row, int charge,
      List<FeatureListRow> sortedRows) {

    if (charge == 0) {
      return;
    }

    // Search for peaks before the start peak
    if (!monotonicShape) {
      fitHalfPattern(row, charge, -1, fittedRows, sortedRows);
    }

    // Search for peaks after the start peak
    fitHalfPattern(row, charge, 1, fittedRows, sortedRows);
  }

  /**
   * Helper method for fitPattern. Fits only one half of the pattern.
   *
   * @param row        Pattern is fitted around this peak
   * @param charge     Charge state of the fitted pattern
   * @param direction  Defines which half to fit: -1=fit to peaks before start M/Z, +1=fit to peaks
   *                   after start M/Z
   * @param fittedRows All matching peaks will be added to this set
   */
  private void fitHalfPattern(ModularFeatureListRow row, int charge, int direction,
      List<FeatureListRow> fittedRows,
      List<FeatureListRow> sortedRows) {

    // Use M/Z and RT of the strongest peak of the pattern (row)
    double mainMZ = row.getAverageMZ();
    float mainRT = row.getAverageRT();
    Float mainMobility = row.getAverageMobility();

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
      List<FeatureListRow> goodCandidates = new ArrayList<>();
      for (int ind = 0; ind < sortedRows.size(); ind++) {

        ModularFeatureListRow candidatePeak = (ModularFeatureListRow) sortedRows.get(ind);

        if (candidatePeak == null) {
          continue;
        }

        // Get properties of the candidate peak
        double candidatePeakMZ = candidatePeak.getAverageMZ();
        float candidatePeakRT = candidatePeak.getAverageRT();
        Float candidateMobility = candidatePeak.getAverageMobility();

        // Does this peak fill all requirements of a candidate?
        // - within tolerances from the expected location (M/Z and RT)
        // - not already a fitted peak (only necessary to avoid
        // conflicts when parameters are set too wide)
        double isotopeMZ = candidatePeakMZ - isotopeDistance * direction * n / charge;

        if (mzTolerance.checkWithinTolerance(isotopeMZ, mainMZ)
            && rtTolerance.checkWithinTolerance(candidatePeakRT, mainRT)) {

          if (useMobilityTolerance && mainMobility != null && candidateMobility != null) {
            if (mobilityTolerance.checkWithinTolerance(mainMobility, candidateMobility)) {
              goodCandidates.add(candidatePeak);
            }
          } else {
            goodCandidates.add(candidatePeak);
          }

        }

      }

      // Add all good candidates to the isotope pattern (note: in MZmine
      // 2.3 and older, only the highest candidate was added)
      if (!goodCandidates.isEmpty()) {

        fittedRows.addAll(goodCandidates);

        // n:th peak was found, so let's move on to n+1
        n++;
        followingPeakFound = true;
      }

    } while (followingPeakFound);
  }

}
