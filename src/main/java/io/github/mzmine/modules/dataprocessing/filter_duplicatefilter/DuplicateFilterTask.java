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

package io.github.mzmine.modules.dataprocessing.filter_duplicatefilter;

import static io.github.mzmine.datamodel.FeatureStatus.DETECTED;
import static io.github.mzmine.datamodel.FeatureStatus.ESTIMATED;
import static io.github.mzmine.datamodel.FeatureStatus.UNKNOWN;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.modules.dataprocessing.filter_duplicatefilter.DuplicateFilterParameters.FilterMode;
import io.github.mzmine.modules.dataprocessing.filter_rowsfilter.RowsFilterParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
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
import java.util.List;
import java.util.Objects;
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
  // Parameters.
  private final ParameterSet parameters;
  private FeatureList filteredPeakList;
  // Counters.
  private int processedRows;
  private int totalRows;

  public DuplicateFilterTask(final MZmineProject project, final FeatureList list,
      final ParameterSet params, @Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate) {
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

        final OriginalFeatureListOption originalFeatureListOption = parameters.getValue(
            DuplicateFilterParameters.handleOriginal);

        switch (originalFeatureListOption) {
          case KEEP -> logger.finer("Create new feature List");
          case REMOVE -> logger.finer("Remove original feature list");
          case PROCESS_IN_PLACE -> logger.finer("Process in place");
        }

        // Filter out duplicates..
        filteredPeakList = filterDuplicatePeakListRows(peakList,
            parameters.getParameter(DuplicateFilterParameters.suffix).getValue(),
            parameters.getParameter(DuplicateFilterParameters.mzDifferenceMax).getValue(),
            parameters.getParameter(DuplicateFilterParameters.rtDifferenceMax).getValue(),
            parameters.getParameter(DuplicateFilterParameters.requireSameIdentification).getValue(),
            parameters.getParameter(DuplicateFilterParameters.filterMode).getValue(),
            originalFeatureListOption == OriginalFeatureListOption.PROCESS_IN_PLACE);

        if (!isCanceled()) {
          final String suffix = parameters.getValue(RowsFilterParameters.SUFFIX);
          originalFeatureListOption.reflectNewFeatureListToProject(suffix, project,
              filteredPeakList, peakList);

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
   * @param origPeakList        the original feature list.
   * @param suffix              the suffix to apply to the new feature list name.
   * @param mzTolerance         m/z tolerance.
   * @param rtTolerance         RT tolerance.
   * @param requireSameId       must duplicate peaks have the same identities?
   * @param processOriginalList
   * @return the filtered feature list.
   */
  private FeatureList filterDuplicatePeakListRows(final FeatureList origPeakList,
      final String suffix, final MZTolerance mzTolerance, final RTTolerance rtTolerance,
      final boolean requireSameId, FilterMode mode, Boolean processOriginalList) {
    // Create the new feature list.
    final ModularFeatureList newPeakList;
    if (processOriginalList) {
      newPeakList = (ModularFeatureList) origPeakList;
    } else {
      newPeakList = ((ModularFeatureList) origPeakList).createCopy(origPeakList + " " + suffix,
          getMemoryMapStorage(), false);
    }
    final ModularFeatureListRow[] peakListRows = newPeakList.getRows()
        .toArray(ModularFeatureListRow[]::new);
    final int rowCount = peakListRows.length;
    RawDataFile[] rawFiles = newPeakList.getRawDataFiles().toArray(RawDataFile[]::new);

    // filter by average mz and rt
    totalRows = rowCount;
    processedRows = 0;
    // sort rows
    final int removedDuplicates = switch (mode) {
      case OLD_AVERAGE -> applyOldAverageFilter(mzTolerance, rtTolerance, requireSameId,
          peakListRows, rowCount);
      case NEW_AVERAGE -> applyNewMergingFilter(mzTolerance, rtTolerance, requireSameId,
          newPeakList, peakListRows, rowCount, rawFiles);
      case SINGLE_FEATURE -> applySingleFeatureMergingFilter(mzTolerance, rtTolerance,
          requireSameId, newPeakList, peakListRows, rowCount, rawFiles);
    };

    // finalize
    if (!isCanceled()) {
      // remove all null rows
      removeDuplicatesFromList(newPeakList, peakListRows);

      // Add task description to peakList
      newPeakList.addDescriptionOfAppliedTask(
          new SimpleFeatureListAppliedMethod("Duplicate feature list rows filter",
              DuplicateFilterModule.class, parameters, getModuleCallDate()));
      logger.info("Removed " + removedDuplicates + " duplicate rows");
    }

    return newPeakList;
  }

  /**
   * set filtered rows
   *
   * @param flist
   * @param duplicatesNullArray
   */
  private void removeDuplicatesFromList(ModularFeatureList flist,
      ModularFeatureListRow[] duplicatesNullArray) {
    final var filteredRows = Arrays.stream(duplicatesNullArray).filter(Objects::nonNull)
        .toArray(ModularFeatureListRow[]::new);
    flist.setRows(filteredRows);
  }

  private int applyOldAverageFilter(MZTolerance mzTolerance, RTTolerance rtTolerance,
      boolean requireSameId, ModularFeatureListRow[] peakListRows, int rowCount) {
    Arrays.sort(peakListRows,
        new FeatureListRowSorter(SortingProperty.Area, SortingDirection.Descending));

    // Loop through all feature list rows
    int removedDuplicates = 0;
    for (int firstRowIndex = 0; firstRowIndex < rowCount; firstRowIndex++) {
      if (isCanceled()) {
        return -1;
      }

      final ModularFeatureListRow firstRow = peakListRows[firstRowIndex];

      if (firstRow != null) {
        for (int secondRowIndex = firstRowIndex + 1; secondRowIndex < rowCount; secondRowIndex++) {
          if (isCanceled()) {
            return -1;
          }

          final FeatureListRow secondRow = peakListRows[secondRowIndex];
          if (secondRow != null) {
            // Compare identifications
            final boolean sameID =
                !requireSameId || FeatureUtils.compareIdentities(firstRow, secondRow);

            boolean sameMZRT = checkSameAverageRTMZ(firstRow, secondRow, mzTolerance, rtTolerance);

            // Duplicate peaks?
            if (sameID && sameMZRT) {
              // second row deleted
              removedDuplicates++;
              peakListRows[secondRowIndex] = null;
            }
          }
        }
      }
      processedRows++;
    }
    return removedDuplicates;
  }


  private int applyNewMergingFilter(MZTolerance mzTolerance, RTTolerance rtTolerance,
      boolean requireSameId, ModularFeatureList newPeakList, ModularFeatureListRow[] peakListRows,
      int rowCount, RawDataFile[] rawFiles) {
    // sort by mz to limit number of iterations
    Arrays.sort(peakListRows,
        new FeatureListRowSorter(SortingProperty.MZ, SortingDirection.Ascending));

    // Loop through all feature list rows
    int n = 0;
    for (int firstRowIndex = 0; firstRowIndex < rowCount; firstRowIndex++) {
      if (isCanceled()) {
        return -1;
      }

      final ModularFeatureListRow firstRow = peakListRows[firstRowIndex];

      if (firstRow != null) {

        final double averageMZ1 = firstRow.getAverageMZ();
        final Range<Double> mzRange = mzTolerance.getToleranceRange(averageMZ1);
        double lowerMZ = mzRange.lowerEndpoint();
        double upperMZ = mzRange.upperEndpoint();

        for (int secondRowIndex = firstRowIndex + 1; secondRowIndex < rowCount; secondRowIndex++) {
          if (isCanceled()) {
            return -1;
          }

          final FeatureListRow secondRow = peakListRows[secondRowIndex];
          if (secondRow != null) {
            // check mz first to stop loop
            final double averageMZ2 = secondRow.getAverageMZ();
            if (averageMZ2 < lowerMZ) {
              continue;
            }
            if (averageMZ2 > upperMZ) {
              break;
            }

            // Compare identifications
            final boolean sameID =
                !requireSameId || FeatureUtils.compareIdentities(firstRow, secondRow);

            boolean sameRT = rtTolerance.checkWithinTolerance(firstRow.getAverageRT(),
                secondRow.getAverageRT());

            // Duplicate peaks?
            if (sameID && sameRT) {
              // create consensus row in new filter
              // copy all detected features of row2 into row1
              // to exchange gap-filled against detected
              // features
              createConsensusFirstRow(newPeakList, rawFiles, firstRow, secondRow);
              // second row deleted
              n++;
              peakListRows[secondRowIndex] = null;
            }
          }
        }
      }
      processedRows++;
    }
    return n;
  }

  /**
   * Removes duplicates when one feature in two rows match.
   *
   * @return number of duplicates
   */
  private int applySingleFeatureMergingFilter(MZTolerance mzTolerance, RTTolerance rtTolerance,
      boolean requireSameId, ModularFeatureList newPeakList, ModularFeatureListRow[] peakListRows,
      int rowCount, RawDataFile[] rawFiles) {
    // sort by mz to limit number of iterations
    Arrays.sort(peakListRows,
        new FeatureListRowSorter(SortingProperty.MZ, SortingDirection.Ascending));

    // Loop through all feature list rows
    int n = 0;
    for (int firstRowIndex = 0; firstRowIndex < rowCount; firstRowIndex++) {
      if (isCanceled()) {
        return -1;
      }

      final ModularFeatureListRow firstRow = peakListRows[firstRowIndex];

      if (firstRow != null) {

        final List<ModularFeature> firstFeatures = firstRow.getFeatures();
        double minMZ = Double.MAX_VALUE;
        double maxMZ = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < firstFeatures.size(); i++) {
          Double mz = firstFeatures.get(i).getMZ();
          if (mz == null) {
            continue;
          }
          if (mz < minMZ) {
            minMZ = mz;
          }
          if (mz > maxMZ) {
            maxMZ = mz;
          }
        }
        double lowerMZ = mzTolerance.getToleranceRange(minMZ).lowerEndpoint();
        double upperMZ = mzTolerance.getToleranceRange(maxMZ).upperEndpoint();

        for (int secondRowIndex = firstRowIndex + 1; secondRowIndex < rowCount; secondRowIndex++) {
          if (isCanceled()) {
            return -1;
          }

          final FeatureListRow secondRow = peakListRows[secondRowIndex];
          if (secondRow != null) {
            // check mz first to stop loop
            final double averageMZ2 = secondRow.getAverageMZ();
            if (averageMZ2 < lowerMZ) {
              continue;
            }
            if (averageMZ2 > upperMZ) {
              break;
            }

            // Compare identifications
            final boolean sameID =
                !requireSameId || FeatureUtils.compareIdentities(firstRow, secondRow);

            boolean sameRT = checkSameSingleFeatureRTMZ(rawFiles, firstRow, secondRow, mzTolerance,
                rtTolerance);

            // Duplicate peaks?
            if (sameID && sameRT) {
              // create consensus row in new filter
              // copy all detected features of row2 into row1
              // to exchange gap-filled against detected
              // features
              createConsensusFirstRow(newPeakList, rawFiles, firstRow, secondRow);
              // second row deleted
              n++;
              peakListRows[secondRowIndex] = null;
            }
          }
        }
      }
      processedRows++;
    }
    return n;
  }

  /**
   * Turns firstRow to consensus row. With all features with highest FeatureStatus:
   * DETECTED>ESTIMATED>UNKNOWN Or the highest feature when comparing two ESTIMATED features
   *
   * @param rawFiles
   * @param firstRow
   * @param secondRow
   */
  private void createConsensusFirstRow(ModularFeatureList flist, RawDataFile[] rawFiles,
      FeatureListRow firstRow, FeatureListRow secondRow) {
    for (RawDataFile raw : rawFiles) {
      Feature f2 = secondRow.getFeature(raw);
      if (f2 == null) {
        continue;
      }

      Feature f1 = firstRow.getFeature(raw);
      FeatureStatus status1 = f1 != null ? f1.getFeatureStatus() : UNKNOWN;
      switch (f2.getFeatureStatus()) {
        case DETECTED:
          // DETECTED over all - both detected use heighest feature
          if (status1 != DETECTED || f1.getHeight() < f2.getHeight()) {
            firstRow.addFeature(raw, new ModularFeature(flist, f2));
          }
          break;
        case ESTIMATED:
          // ESTIMATED over UNKNOWN or
          // BOTH ESTIMATED? take the highest
          if (status1 == UNKNOWN || (status1 == ESTIMATED && f1.getHeight() < f2.getHeight())) {
            firstRow.addFeature(raw, new ModularFeature(flist, f2));
          }
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
          && rtTolerance.checkWithinTolerance(f1.getRT(), f2.getRT())) {
        return true;
      }
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
