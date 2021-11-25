/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.dataprocessing.filter_duplicatefilter;

import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.modules.dataprocessing.filter_duplicatefilter.DuplicateFilterParameters.FilterMode;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureListRowSorter;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import java.time.Instant;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A task to filter out duplicate feature list rows.
 */
public class DuplicateFilterTask extends AbstractTask {

  // Logger.
  private static final Logger logger = Logger.getLogger(DuplicateFilterTask.class.getName());

  // Original and resultant feature lists.
  private final MZmineProject project;
  private final FeatureList peakList;
  private FeatureList filteredPeakList;

  // Counters.
  private int processedRows;
  private int totalRows;

  // Parameters.
  private final ParameterSet parameters;

  public DuplicateFilterTask(final MZmineProject project, final FeatureList list,
      final ParameterSet params, @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);

    // Initialize.
    this.project = project;
    parameters = params;
    peakList = list;
    filteredPeakList = null;
    totalRows = 0;
    processedRows = 0;
  }

  @Override
  public String getTaskDescription() {

    return "Filtering duplicate feature list rows of " + peakList;
  }

  @Override
  public double getFinishedPercentage() {

    return totalRows == 0 ? 0.0 : (double) processedRows / (double) totalRows;
  }

  @Override
  public void run() {

    if (!isCanceled()) {
      try {

        logger.info("Filtering duplicate peaks list rows of " + peakList);
        setStatus(TaskStatus.PROCESSING);

        // Filter out duplicates..
        filteredPeakList = filterDuplicatePeakListRows(peakList,
            parameters.getParameter(DuplicateFilterParameters.suffix).getValue(),
            parameters.getParameter(DuplicateFilterParameters.mzDifferenceMax).getValue(),
            parameters.getParameter(DuplicateFilterParameters.rtDifferenceMax).getValue(),
            parameters.getParameter(DuplicateFilterParameters.requireSameIdentification).getValue(),
            parameters.getParameter(DuplicateFilterParameters.filterMode).getValue());

        if (!isCanceled()) {

          // Add new peakList to the project.
          project.addFeatureList(filteredPeakList);

          // Remove the original peakList if requested.
          if (parameters.getParameter(DuplicateFilterParameters.autoRemove).getValue()) {

            project.removeFeatureList(peakList);
          }

          // Finished.
          logger.info("Finished filtering duplicate feature list rows on " + peakList);
          setStatus(TaskStatus.FINISHED);
        }
      } catch (Throwable t) {

        logger.log(Level.SEVERE, "Duplicate filter error", t);
        setErrorMessage(t.getMessage());
        setStatus(TaskStatus.ERROR);
      }
    }
  }

  /**
   * Filter our duplicate feature list rows.
   *
   * @param origPeakList the original feature list.
   * @param suffix the suffix to apply to the new feature list name.
   * @param mzTolerance m/z tolerance.
   * @param rtTolerance RT tolerance.
   * @param requireSameId must duplicate peaks have the same identities?
   * @return the filtered feature list.
   */
  private FeatureList filterDuplicatePeakListRows(final FeatureList origPeakList, final String suffix,
      final MZTolerance mzTolerance, final RTTolerance rtTolerance, final boolean requireSameId,
      FilterMode mode) {
    final ModularFeatureListRow[] peakListRows = origPeakList.getRows().toArray(ModularFeatureListRow[]::new);
    final int rowCount = peakListRows.length;
    RawDataFile[] rawFiles = origPeakList.getRawDataFiles().toArray(RawDataFile[]::new);

    // Create the new feature list.
    final ModularFeatureList newPeakList =
        new ModularFeatureList(origPeakList + " " + suffix, getMemoryMapStorage(),
            origPeakList.getRawDataFiles());

    // sort rows
    if (mode.equals(FilterMode.OLD_AVERAGE))
      Arrays.sort(peakListRows,
          new FeatureListRowSorter(SortingProperty.Area, SortingDirection.Descending));
    else
      Arrays.sort(peakListRows,
          new FeatureListRowSorter(SortingProperty.ID, SortingDirection.Ascending));

    // filter by average mz and rt
    boolean filterByAvgRTMZ = !mode.equals(FilterMode.SINGLE_FEATURE);

    // Loop through all feature list rows
    processedRows = 0;
    int n = 0;
    totalRows = rowCount;
    for (int firstRowIndex = 0; !isCanceled() && firstRowIndex < rowCount; firstRowIndex++) {

      final ModularFeatureListRow mainRow = peakListRows[firstRowIndex];

      if (mainRow != null) {
        // copy first row
        ModularFeatureListRow firstRow = new ModularFeatureListRow(newPeakList, mainRow.getID(), mainRow, true);

        for (int secondRowIndex = firstRowIndex + 1; !isCanceled()
            && secondRowIndex < rowCount; secondRowIndex++) {

          final FeatureListRow secondRow = peakListRows[secondRowIndex];
          if (secondRow != null) {
            // Compare identifications
            final boolean sameID =
                !requireSameId || FeatureUtils.compareIdentities(firstRow, secondRow);

            boolean sameMZRT = filterByAvgRTMZ ? // average or
                                                 // single feature
                checkSameAverageRTMZ(firstRow, secondRow, mzTolerance, rtTolerance)
                : checkSameSingleFeatureRTMZ(rawFiles, firstRow, secondRow, mzTolerance,
                    rtTolerance);

            // Duplicate peaks?
            if (sameID && sameMZRT) {
              // create consensus row in new filter
              if (!mode.equals(FilterMode.OLD_AVERAGE)) {
                // copy all detected features of row2 into row1
                // to exchange gap-filled against detected
                // features
                createConsensusFirstRow(newPeakList, rawFiles, firstRow, secondRow);
              }
              // second row deleted
              n++;
              peakListRows[secondRowIndex] = null;
            }
          }
        }
        // add to new list
        newPeakList.addRow(firstRow);
      }
      processedRows++;
    }

    // finalize
    if (!isCanceled()) {
      // Load previous applied methods.
      for (final FeatureListAppliedMethod method : origPeakList.getAppliedMethods()) {
        newPeakList.addDescriptionOfAppliedTask(method);
      }

      // Add task description to peakList
      newPeakList.addDescriptionOfAppliedTask(
          new SimpleFeatureListAppliedMethod("Duplicate feature list rows filter",
              DuplicateFilterModule.class, parameters, getModuleCallDate()));
      logger.info("Removed " + n + " duplicate rows");
    }

    return newPeakList;
  }

  /**
   * Turns firstRow to consensus row. With all features with highest FeatureStatus:
   * DETECTED>ESTIMATED>UNKNOWN Or the highest feature when comparing two ESTIMATED features
   *
   * @param rawFiles
   * @param firstRow
   * @param secondRow
   */
  private void createConsensusFirstRow(ModularFeatureList flist, RawDataFile[] rawFiles, FeatureListRow firstRow,
      FeatureListRow secondRow) {
    for (RawDataFile raw : rawFiles) {
      Feature f2 = secondRow.getFeature(raw);
      if (f2 == null)
        continue;

      switch (f2.getFeatureStatus()) {
        case DETECTED:
          // DETECTED over all
          firstRow.addFeature(raw, new ModularFeature(flist, f2));
          break;
        case ESTIMATED:
          // ESTIMATED over UNKNOWN or
          // BOTH ESTIMATED? take the highest
          Feature f1 = firstRow.getFeature(raw);
          if (f1 != null && (f1.getFeatureStatus().equals(FeatureStatus.UNKNOWN)
              || (f1.getFeatureStatus().equals(FeatureStatus.ESTIMATED)
                  && f1.getHeight() < f2.getHeight())))
            firstRow.addFeature(raw, new ModularFeature(flist, f2));
          break;
      }
    }
  }

  /**
   * Has one feature within RT and mzTolerance in at least one raw data file
   *
   * @param rawFiles
   * @param firstRow
   * @param secondRow
   * @param mzTolerance
   * @param rtTolerance
   * @return
   */
  private boolean checkSameSingleFeatureRTMZ(RawDataFile[] rawFiles, FeatureListRow firstRow,
      FeatureListRow secondRow, MZTolerance mzTolerance, RTTolerance rtTolerance) {
    // at least one similar feature in one raw data file
    for (RawDataFile raw : rawFiles) {
      Feature f1 = firstRow.getFeature(raw);
      Feature f2 = secondRow.getFeature(raw);
      // Compare m/z and rt
      if (f1 != null && f2 != null && mzTolerance.checkWithinTolerance(f1.getMZ(), f2.getMZ())
          && rtTolerance.checkWithinTolerance(f1.getRT(), f2.getRT()))
        return true;
    }
    return false;
  }

  /**
   * Shares the same RT and mz
   *
   * @param firstRow
   * @param secondRow
   * @param mzTolerance
   * @param rtTolerance
   * @return
   */
  private boolean checkSameAverageRTMZ(FeatureListRow firstRow, FeatureListRow secondRow,
      MZTolerance mzTolerance, RTTolerance rtTolerance) {
    // Compare m/z and RT
    return mzTolerance.checkWithinTolerance(firstRow.getAverageMZ(), secondRow.getAverageMZ())
        && rtTolerance.checkWithinTolerance(firstRow.getAverageRT(), secondRow.getAverageRT());
  }

}
