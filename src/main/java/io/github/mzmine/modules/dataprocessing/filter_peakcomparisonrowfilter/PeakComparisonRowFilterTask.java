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

package io.github.mzmine.modules.dataprocessing.filter_peakcomparisonrowfilter;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Filters out feature list rows.
 */
public class PeakComparisonRowFilterTask extends AbstractTask {

  // Logger.
  private static final Logger logger =
      Logger.getLogger(PeakComparisonRowFilterTask.class.getName());
  // Feature lists.
  private final MZmineProject project;
  private final FeatureList origPeakList;
  private FeatureList filteredPeakList;
  // Processed rows counter
  private int processedRows, totalRows;
  // Parameters.
  private final ParameterSet parameters;

  /**
   * Create the task.
   *
   * @param list feature list to process.
   * @param parameterSet task parameters.
   */
  public PeakComparisonRowFilterTask(final MZmineProject project, final FeatureList list,
      final ParameterSet parameterSet, @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);

    // Initialize.
    this.project = project;
    parameters = parameterSet;
    origPeakList = list;
    filteredPeakList = null;
    processedRows = 0;
    totalRows = 0;
  }

  @Override
  public double getFinishedPercentage() {

    return totalRows == 0 ? 0.0 : (double) processedRows / (double) totalRows;
  }

  @Override
  public String getTaskDescription() {

    return "Filtering feature list rows based on peak comparisons";
  }

  @Override
  public void run() {

    try {
      setStatus(TaskStatus.PROCESSING);
      logger.info("Filtering feature list rows");

      // Filter the feature list.
      filteredPeakList = filterPeakListRows(origPeakList);

      if (getStatus() == TaskStatus.ERROR)
        return;

      if (isCanceled())
        return;

      // Add new peaklist to the project
      project.addFeatureList(filteredPeakList);

      // Remove the original peaklist if requested
      if (parameters.getParameter(PeakComparisonRowFilterParameters.AUTO_REMOVE).getValue()) {
        project.removeFeatureList(origPeakList);
      }

      setStatus(TaskStatus.FINISHED);
      logger.info("Finished peak comparison rows filter");

    } catch (Throwable t) {
      t.printStackTrace();
      setErrorMessage(t.getMessage());
      setStatus(TaskStatus.ERROR);
      logger.log(Level.SEVERE, "Peak comparison row filter error", t);
    }

  }

  /**
   * Filter the feature list rows by comparing peaks within a row.
   *
   * @param peakList feature list to filter.
   * @return a new feature list with rows of the original feature list that pass the filtering.
   */
  private FeatureList filterPeakListRows(final FeatureList peakList) {

    // Create new feature list.
    final ModularFeatureList newPeakList = new ModularFeatureList(
        peakList.getName() + ' '
            + parameters.getParameter(PeakComparisonRowFilterParameters.SUFFIX).getValue(),
        getMemoryMapStorage(), peakList.getRawDataFiles());

    // Copy previous applied methods.
    for (final FeatureListAppliedMethod method : peakList.getAppliedMethods()) {

      newPeakList.addDescriptionOfAppliedTask(method);
    }

    // Add task description to peakList.
    newPeakList.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod(getTaskDescription(),
            PeakComparisonRowFilterModule.class, parameters, getModuleCallDate()));

    // Get parameters.
    final boolean evalutateFoldChange =
        parameters.getParameter(PeakComparisonRowFilterParameters.FOLD_CHANGE).getValue();
    final boolean evalutatePPMdiff =
        parameters.getParameter(PeakComparisonRowFilterParameters.MZ_PPM_DIFF).getValue();
    final boolean evalutateRTdiff =
        parameters.getParameter(PeakComparisonRowFilterParameters.RT_DIFF).getValue();
    final int columnIndex1 =
        parameters.getParameter(PeakComparisonRowFilterParameters.COLUMN_INDEX_1).getValue();
    final int columnIndex2 =
        parameters.getParameter(PeakComparisonRowFilterParameters.COLUMN_INDEX_2).getValue();
    final Range<Double> foldChangeRange =
        parameters.getParameter(PeakComparisonRowFilterParameters.FOLD_CHANGE)
            .getEmbeddedParameter().getValue();
    final Range<Double> ppmDiffRange =
        parameters.getParameter(PeakComparisonRowFilterParameters.FOLD_CHANGE)
            .getEmbeddedParameter().getValue();
    final Range<Double> rtDiffRange =
        parameters.getParameter(PeakComparisonRowFilterParameters.FOLD_CHANGE)
            .getEmbeddedParameter().getValue();

    // Setup variables
    final ModularFeatureListRow[] rows = peakList.getRows().toArray(ModularFeatureListRow[]::new);
    RawDataFile rawDataFile1;
    RawDataFile rawDataFile2;
    Feature peak1;
    Feature peak2;
    totalRows = rows.length;
    final RawDataFile[] rawDataFiles = peakList.getRawDataFiles().toArray(RawDataFile[]::new);

    boolean allCriteriaMatched = true;

    // Error handling. User tried to select a column from the peaklist that
    // doesn't exist.
    if (columnIndex1 >= rawDataFiles.length) {
      setErrorMessage("Column 1 set too large.");
      setStatus(TaskStatus.ERROR);
      return null;
    }
    if (columnIndex2 >= rawDataFiles.length) {
      setErrorMessage("Column 2 set too large.");
      setStatus(TaskStatus.ERROR);
      return null;
    }

    // Loop over the rows & filter
    for (processedRows = 0; !isCanceled() && processedRows < totalRows; processedRows++) {

      if (isCanceled())
        return null;

      allCriteriaMatched = true;

      double peak1Area = 1.0; // Default value in case of null peak
      double peak2Area = 1.0;
      double peak1MZ = -1.0;
      double peak2MZ = -1.0;
      double peak1RT = -1.0;
      double peak2RT = -1.0;
      double foldChange = 0.0;
      double ppmDiff = 0.0;
      double rtDiff = 0.0;
      final ModularFeatureListRow row = rows[processedRows];
      rawDataFile1 = rawDataFiles[columnIndex1];
      rawDataFile2 = rawDataFiles[columnIndex2];

      peak1 = row.getFeature(rawDataFile1);
      peak2 = row.getFeature(rawDataFile2);

      if (peak1 != null) {
        peak1Area = peak1.getArea();
        peak1MZ = peak1.getMZ();
        peak1RT = peak1.getRT();
      }

      if (peak2 != null) {
        peak2Area = peak2.getArea();
        peak2MZ = peak2.getMZ();
        peak2RT = peak2.getRT();
      }

      // Fold change criteria checking.
      if (evalutateFoldChange) {
        foldChange = Math.log(peak1Area / peak2Area) / Math.log(2);
        if (!foldChangeRange.contains(foldChange))
          allCriteriaMatched = false;

        // PPM difference evaluation
        if (evalutatePPMdiff) {
          ppmDiff = (peak1MZ - peak2MZ) / peak1MZ * 1E6;
          if (!ppmDiffRange.contains(ppmDiff))
            allCriteriaMatched = false;
        }

        // RT difference evaluation
        if (evalutateRTdiff) {
          rtDiff = peak1RT - peak2RT;
          if (!rtDiffRange.contains(rtDiff))
            allCriteriaMatched = false;
        }

      }

      // Good row?
      if (allCriteriaMatched)
        newPeakList.addRow(new ModularFeatureListRow(newPeakList, row.getID(), row, true));

    }

    return newPeakList;
  }

}
