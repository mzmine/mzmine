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

package io.github.mzmine.modules.dataprocessing.filter_isotopegrouper;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IsotopePattern.IsotopePatternStatus;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimpleIsotopePattern;
import io.github.mzmine.modules.tools.isotopeprediction.IsotopePatternCalculator;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.DataPointSorter;
import io.github.mzmine.util.FeatureListRowSorter;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
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
  private static final Logger logger = Logger.getLogger(IsotopeGrouperTask.class.getName());
  private static final double isotopeDistance = IsotopePatternCalculator.THIRTHEEN_C_DISTANCE;
  private final MZmineProject project;
  private final ModularFeatureList featureList;
  // parameter values
  private final String suffix;
  private final MZTolerance mzTolerance;
  private final RTTolerance rtTolerance;
  private final boolean useMobilityTolerance;
  private final MobilityTolerance mobilityTolerance;
  private final boolean monotonicShape;
  private final boolean chooseMostIntense;
  private final boolean keepAllMS2;
  private final int maximumCharge;
  private final ParameterSet parameters;
  private final OriginalFeatureListOption handleOriginal;
  // peaks counter
  private int processedRows, totalRows;

  /**
   *
   */
  IsotopeGrouperTask(MZmineProject project, ModularFeatureList featureList, ParameterSet parameters,
      @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);

    this.project = project;
    this.featureList = featureList;
    this.parameters = parameters;

    // Get parameter values for easier use
    suffix = parameters.getParameter(IsotopeGrouperParameters.suffix).getValue();
    mzTolerance = parameters.getParameter(IsotopeGrouperParameters.mzTolerance).getValue();
    rtTolerance = parameters.getParameter(IsotopeGrouperParameters.rtTolerance).getValue();
    monotonicShape = parameters.getParameter(IsotopeGrouperParameters.monotonicShape).getValue();
    maximumCharge = parameters.getParameter(IsotopeGrouperParameters.maximumCharge).getValue();
    keepAllMS2 = parameters.getParameter(IsotopeGrouperParameters.keepAllMS2).getValue();
    chooseMostIntense = (Objects.equals(
        parameters.getParameter(IsotopeGrouperParameters.representativeIsotope).getValue(),
        IsotopeGrouperParameters.ChooseTopIntensity));
    handleOriginal = parameters.getValue(IsotopeGrouperParameters.handleOriginal);
    useMobilityTolerance = parameters.getParameter(IsotopeGrouperParameters.mobilityTolerace)
        .getValue();
    mobilityTolerance = parameters.getParameter(IsotopeGrouperParameters.mobilityTolerace)
        .getEmbeddedParameter().getValue();
  }

  @Override
  public String getTaskDescription() {
    return "Isotopic peaks grouper on " + featureList;
  }

  @Override
  public double getFinishedPercentage() {
    if (totalRows == 0) {
      return 0.0f;
    }
    return (double) processedRows / (double) totalRows;
  }

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

    // create copy or work on same list
    ModularFeatureList deisotopedFeatureList = switch (handleOriginal) {
      case KEEP, REMOVE -> featureList.createCopy(featureList.getName() + " " + suffix,
          getMemoryMapStorage(), false);
      case PROCESS_IN_PLACE -> featureList;
    };
    //    DataTypeUtils.copyTypes(featureList, deisotopedFeatureList, true, true);

    // Collect all selected charge states
    int[] charges = new int[maximumCharge];
    for (int i = 0; i < maximumCharge; i++) {
      charges[i] = i + 1;
    }

    final FeatureListRowSorter rowsHeightSorter = new FeatureListRowSorter(SortingProperty.Height,
        SortingDirection.Descending);
    final FeatureListRowSorter rowsMzSorter = new FeatureListRowSorter(SortingProperty.MZ,
        SortingDirection.Ascending);

    // Sort peaks by descending height
    List<FeatureListRow> rowsSortedByHeight = new ArrayList<>(deisotopedFeatureList.getRows());
    rowsSortedByHeight.sort(rowsHeightSorter);

    // use a second sorted list to limit the number of comparisons
    List<FeatureListRow> rowsSortedByMz = new ArrayList<>(deisotopedFeatureList.getRows());
    rowsSortedByMz.sort(rowsMzSorter);

    // Loop through all peaks
    totalRows = rowsSortedByHeight.size();

    // list of final rows (size is usually similar)
    List<FeatureListRow> finalRows = new ArrayList<>((int) (totalRows * 0.9));

    while (!rowsSortedByHeight.isEmpty()) {

      if (isCanceled()) {
        return;
      }

      ModularFeatureListRow mostIntenseRow = (ModularFeatureListRow) rowsSortedByHeight.remove(0);

      // Check if peak was already deleted
      if (mostIntenseRow == null) {
        processedRows++;
        continue;
      }
      // find index in mz sorted list
      int indexMzSorted = Collections.binarySearch(rowsSortedByMz, mostIntenseRow, rowsMzSorter);
      rowsSortedByMz.remove(indexMzSorted);

      // Check which charge state fits best around this peak
      int bestFitCharge = 0;
      int bestFitScore = -1;
      List<FeatureListRow> bestFitRows = null;
      for (int charge : charges) {

        List<FeatureListRow> fittedRows = new ArrayList<>();
        fittedRows.add(mostIntenseRow);
        // use rows sorted by mz
        fitPattern(fittedRows, mostIntenseRow, charge, rowsSortedByMz, indexMzSorted);

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
        finalRows.add(mostIntenseRow);
        processedRows++;
        continue;
      }

      // Convert the peak pattern to array
      final DataPoint[] isotopes = bestFitRows.stream()
          .map(r -> new SimpleDataPoint(r.getAverageMZ(), r.getMaxHeight()))
          .sorted(new DataPointSorter(SortingProperty.MZ, SortingDirection.Ascending))
          .toArray(DataPoint[]::new);
      SimpleIsotopePattern newPattern = new SimpleIsotopePattern(isotopes, bestFitCharge,
          IsotopePatternStatus.DETECTED, mostIntenseRow.toString());

      // Depending on user's choice, we leave either the most intense, or
      // the lowest m/z peak
      if (chooseMostIntense) {
        bestFitRows.sort(rowsHeightSorter);
      } else {
        bestFitRows.sort(rowsMzSorter);
      }

      // add to final rows
      final FeatureListRow mainRow = bestFitRows.get(0);
      finalRows.add(mainRow);
      // set isotope pattern
      Feature feature = mainRow.getFeatures().get(0);

      // do not set isotope pattern if feature already has an isotope pattern
      // this means the isotope finder (or another module already ran) keep the old pattern
      // we trust the isotope finder more on detecting all isotope signals
      if (feature.getIsotopePattern() == null) {
        feature.setIsotopePattern(newPattern);
        feature.setCharge(bestFitCharge);
      }

      // Remove all peaks already assigned to isotope pattern
      // first is already removed
      bestFitRows.remove(0);
      rowsSortedByHeight.removeAll(bestFitRows);
      rowsSortedByMz.removeAll(bestFitRows);

      // in case user wants to keep all features with MS2 - eventhough they were flagged as isotopes
      // this can be useful for complex datasets
      // in general, when an MS2 is triggered we might want to retain this feauture in any case
      if (keepAllMS2) {
        for (var isotopeWithMS2 : bestFitRows) {
          if (isotopeWithMS2.hasMs2Fragmentation()) {
            finalRows.add(isotopeWithMS2);
          }
        }
      }

      // Update completion rate
      processedRows += bestFitRows.size();
    }

    // Add task description to peakList
    deisotopedFeatureList.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod(IsotopeGrouperModule.MODULE_NAME,
            IsotopeGrouperModule.class, parameters, getModuleCallDate()));

    // sort by RT
    finalRows.sort(FeatureListRowSorter.DEFAULT_RT);

    // replace rows in list
    deisotopedFeatureList.setRows(finalRows);

    // Remove the original peakList if requested, or add, or work in place
    handleOriginal.reflectNewFeatureListToProject(suffix, project, deisotopedFeatureList,
        featureList);

    logger.info("Finished isotopic peak grouper on " + featureList);
    setStatus(TaskStatus.FINISHED);
  }

  /**
   * Fits isotope pattern around one peak.
   *
   * @param row            Pattern is fitted around this peak
   * @param charge         Charge state of the fitted pattern
   * @param rowsSortedByMz rows sorted by mz
   * @param startRowIndex  index to start with in mz sorted list
   */
  private void fitPattern(List<FeatureListRow> fittedRows, ModularFeatureListRow row, int charge,
      List<FeatureListRow> rowsSortedByMz, int startRowIndex) {

    if (charge == 0) {
      return;
    }

    // Search for peaks before the start peak
    if (!monotonicShape) {
      fitHalfPattern(row, charge, -1, fittedRows, rowsSortedByMz, startRowIndex);
    }

    // Search for peaks after the start peak
    fitHalfPattern(row, charge, 1, fittedRows, rowsSortedByMz, startRowIndex);
  }

  /**
   * Helper method for fitPattern. Fits only one half of the pattern.
   *
   * @param row            Pattern is fitted around this peak
   * @param charge         Charge state of the fitted pattern
   * @param direction      Defines which half to fit: -1=fit to peaks before start M/Z, +1=fit to
   *                       peaks after start M/Z
   * @param fittedRows     All matching peaks will be added to this set
   * @param rowsSortedByMz rows sorted by mz
   * @param startRowIndex  index to start with in mz sorted list
   */
  private void fitHalfPattern(ModularFeatureListRow row, int charge, int direction,
      List<FeatureListRow> fittedRows, List<FeatureListRow> rowsSortedByMz, int startRowIndex) {

    // Use M/Z and RT of the strongest peak of the pattern (row)
    double mainMZ = row.getAverageMZ();
    float mainRT = row.getAverageRT();
    Float mainMobility = row.getAverageMobility();

    final double absoluteMzTolerance = mzTolerance.getMzToleranceForMass(mainMZ);

    // Variable n is the number of peak we are currently searching. 1=first
    // peak before/after start peak, 2=peak before/after previous, 3=...
    boolean followingPeakFound;
    int n = 1;
    do {
      // start at start row index in mz sorted list
      int ind = startRowIndex;

      // Assume we don't find match for n:th peak in the pattern (which
      // will end the loop)
      followingPeakFound = false;

      // Loop through all peaks, and collect candidates for the n:th peak
      // in the pattern
      List<FeatureListRow> goodCandidates = new ArrayList<>();
      for (; ind < rowsSortedByMz.size() && ind >= 0; ind += direction) {

        ModularFeatureListRow candidatePeak = (ModularFeatureListRow) rowsSortedByMz.get(ind);

        if (candidatePeak == null) {
          continue;
        }

        // Get properties of the candidate peak
        double candidatePeakMZ = candidatePeak.getAverageMZ();

        // Does this peak fill all requirements of a candidate?
        // - within tolerances from the expected location (M/Z and RT)
        // - not already a fitted peak (only necessary to avoid
        // conflicts when parameters are set too wide)
        double isotopeMZ = candidatePeakMZ - isotopeDistance * direction * n / charge;
        double deltaMZ = isotopeMZ - mainMZ;

        // break the loop if deltaMZ reaches out of the maximum allowed mz tolerance (one sided check)
        if (deltaMZ * direction > absoluteMzTolerance) {
          break;
        }

        // check if in range
        if (Math.abs(deltaMZ) <= absoluteMzTolerance && rtTolerance.checkWithinTolerance(
            candidatePeak.getAverageRT(), mainRT)) {
          if (!useMobilityTolerance || mainMobility == null || checkCandidateMobility(mainMobility,
              candidatePeak)) {
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

  private boolean checkCandidateMobility(Float mainMobility, FeatureListRow row) {
    Float candidateMobility = row.getAverageMobility();
    return candidateMobility == null || mobilityTolerance.checkWithinTolerance(mainMobility,
        candidateMobility);
  }

}
