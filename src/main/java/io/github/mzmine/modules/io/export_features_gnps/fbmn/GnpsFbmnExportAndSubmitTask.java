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
/*
 * This module was prepared by Abi Sarvepalli, Christopher Jensen, and Zheng Zhang at the Dorrestein
 * Lab (University of California, San Diego).
 *
 * It is freely available under the GNU GPL licence of MZmine2.
 *
 * For any questions or concerns, please refer to:
 * https://groups.google.com/forum/#!forum/molecular_networking_bug_reports
 *
 * Credit to the Du-Lab development team for the initial commitment to the MGF export module.
 */

package io.github.mzmine.modules.io.export_features_gnps.fbmn;

import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.export.ExportCorrAnnotationTask;
import io.github.mzmine.modules.io.export_features_csv.CSVExportModularTask;
import io.github.mzmine.modules.io.export_features_csv_legacy.LegacyCSVExportTask;
import io.github.mzmine.modules.io.export_features_csv_legacy.LegacyExportRowCommonElement;
import io.github.mzmine.modules.io.export_features_csv_legacy.LegacyExportRowDataFileElement;
import io.github.mzmine.modules.io.export_features_gnps.GNPSUtils;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.ProcessedItemsCounter;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureMeasurementType;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.awt.Desktop;
import java.io.File;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Exports all files needed for GNPS
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class GnpsFbmnExportAndSubmitTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(
      GnpsFbmnExportAndSubmitTask.class.getName());
  private final ParameterSet parameters;
  private final FeatureMeasurementType featureMeasure;
  private final File baseFile;
  private final ModularFeatureList[] featureLists;
  private final int totalSteps = 4;
  private final FeatureTableExportType csvType;
  private int currentStep = 0;
  private Task currentTask;
  private String currentDescription = "Export to GNPS FBMN and IIMN";

  GnpsFbmnExportAndSubmitTask(ParameterSet parameters, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null
    this.parameters = parameters;

    featureLists = parameters.getParameter(GnpsFbmnExportAndSubmitParameters.FEATURE_LISTS)
        .getValue().getMatchingFeatureLists();
    featureMeasure = parameters.getValue(GnpsFbmnExportAndSubmitParameters.FEATURE_INTENSITY);
    csvType = parameters.getValue(GnpsFbmnExportAndSubmitParameters.CSV_TYPE);
    baseFile = FileAndPathUtil.eraseFormat(
        parameters.getValue(GnpsFbmnExportAndSubmitParameters.FILENAME));
    parameters.getParameter(GnpsFbmnExportAndSubmitParameters.FILENAME).setValue(baseFile);
  }

  @Override
  public String getTaskDescription() {
    return currentDescription == null ? "" : currentDescription;
  }

  @Override
  public double getFinishedPercentage() {
    if (currentTask != null) {
      synchronized (currentTask) {
        if (currentTask != null) {
          return (currentTask.getFinishedPercentage() + currentStep) / (double) totalSteps;
        }
      }
    }
    return currentStep / (double) totalSteps;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    boolean openFolder = parameters.getParameter(GnpsFbmnExportAndSubmitParameters.OPEN_FOLDER)
        .getValue();
    boolean submit = parameters.getParameter(GnpsFbmnExportAndSubmitParameters.SUBMIT).getValue();
    final FeatureListRowsFilter filter = parameters.getValue(
        GnpsFbmnExportAndSubmitParameters.FILTER);

    this.addTaskStatusListener((task, newStatus, oldStatus) -> {
      if (currentTask != null) {
        synchronized (currentTask) {
          if (TaskStatus.ERROR.equals(newStatus) || TaskStatus.CANCELED.equals(newStatus)) {
            // cancel the subtask
            if (currentTask != null) {
              currentTask.cancel();
            }
          }
        }
      }
    });

    currentDescription = "Exporting GNPS mgf of MS2 spectra";
    currentTask = new GnpsFbmnMgfExportTask(parameters, getModuleCallDate());
    currentTask.run();
    currentStep++;
    int mgfCount = ((ProcessedItemsCounter) currentTask).getProcessedItems();

    if (checkTaskCanceledOrFailed()) {
      return;
    }

    int csvLineCount = 0;
    final boolean allCSV = FeatureTableExportType.ALL.equals(csvType);
    // add old csv quant table for old FBMN support
    if (allCSV || FeatureTableExportType.SIMPLE.equals(csvType)) {
      currentDescription = "Exporting GNPS legacy csv format (simple csv)";
      currentTask = addLegacyQuantTableTask(parameters);
      currentTask.run();
      currentStep++;

      csvLineCount = ((ProcessedItemsCounter) currentTask).getProcessedItems();
      if (checkTaskCanceledOrFailed()) {
        return;
      }
    }

    // add new csv export for whole table
    if (allCSV || FeatureTableExportType.COMPREHENSIVE.equals(csvType)) {
      currentDescription = "Exporting MZmine csv format (complete feature table csv)";
      currentTask = addFullQuantTableTask(parameters);
      currentTask.run();
      currentStep++;

      csvLineCount = ((ProcessedItemsCounter) currentTask).getProcessedItems();
      if (checkTaskCanceledOrFailed()) {
        return;
      }
    }

    // add csv extra edges
    currentDescription = "Exporting extra edges csv format (ion identity networking)";
    currentTask = addExtraEdgesTask(parameters);
    currentTask.run();
    currentStep++;

    if (checkTaskCanceledOrFailed()) {
      return;
    }
    currentTask = null;

    final File folder = baseFile.getParentFile();
    // csv count is always the same
    // MGF and csv only of MS2 is required for export
    if (!filter.requiresMS2() || csvLineCount == mgfCount) {
      logger.log(Level.INFO,
          String.format("GNPS export succeeded. mgf MS2=%d;  csv rows=%d", mgfCount, csvLineCount));
      currentDescription = "All GNPS exports successful";
    } else {
      final String error = String.format(
          "GNPS export resulted in files with different length despite using the same filter. Try to use this module manually after running a batch. mgf MS2=%d;  csv rows=%d",
          mgfCount, csvLineCount);
      currentDescription = "Error during csv export";
      logger.log(Level.WARNING, error);
      setErrorMessage(error);
      setStatus(TaskStatus.ERROR);
      return;
    }
    // submit HTTP request to GNPS FBMN quickstart
    if (submit) {
      currentDescription = "Submitting job to GNPS";
      GnpsFbmnSubmitParameters param = parameters.getParameter(
          GnpsFbmnExportAndSubmitParameters.SUBMIT).getEmbeddedParameters();
      submit(baseFile, param);
    }

    // open folder
    try {
      if (openFolder && Desktop.isDesktopSupported()) {
        Desktop.getDesktop().open(folder);
      }
    } catch (Exception ex) {
      logger.log(Level.WARNING, "Cannot open folder " + ex.getMessage(), ex);
    }

    setStatus(TaskStatus.FINISHED);
  }

  /**
   * Check if the current task finished successfully.
   *
   * @return true if task is cancelled or failed
   */
  private boolean checkTaskCanceledOrFailed() {
    if (isCanceled() || !TaskStatus.FINISHED.equals(currentTask.getStatus())) {
      setStatus(currentTask.getStatus());
      setErrorMessage(currentTask.getErrorMessage());
      return true;
    }
    return false;
  }

  /**
   * Submit GNPS job
   *
   * @param fileName base file name
   * @param param    parameters for the direct submission
   */
  private void submit(File fileName, GnpsFbmnSubmitParameters param) {
    try {
      String url = GNPSUtils.submitFbmnJob(fileName, param);
      if (url == null || url.isEmpty()) {
        logger.log(Level.WARNING, "GNPS submit failed (response url empty)");
      }
    } catch (Exception e) {
      logger.log(Level.WARNING, "GNPS submit failed", e);
    }
  }

  /**
   * Export the whole quant table in the new format
   *
   * @param parameters export parameters {@link GnpsFbmnExportAndSubmitParameters}
   */
  private AbstractTask addFullQuantTableTask(ParameterSet parameters) {
    File full = parameters.getParameter(GnpsFbmnExportAndSubmitParameters.FILENAME).getValue();
    final String name = FilenameUtils.removeExtension(full.getName());
    full = new File(full.getParentFile(), name + "_quant_full.csv");

    FeatureListRowsFilter filter = parameters.getParameter(GnpsFbmnExportAndSubmitParameters.FILTER)
        .getValue();

    return new CSVExportModularTask(featureLists, full, ",", ";", filter, true,
        getModuleCallDate());
  }


  /**
   * Export quant table in new and old format
   *
   * @param parameters export parameters {@link GnpsFbmnExportAndSubmitParameters}
   */
  private AbstractTask addLegacyQuantTableTask(ParameterSet parameters) {
    final String name = FilenameUtils.removeExtension(baseFile.getName());
    File full = new File(baseFile.getParentFile(), name + "_quant.csv");
    // add old CSV export
    LegacyExportRowCommonElement[] common = new LegacyExportRowCommonElement[]{
        LegacyExportRowCommonElement.ROW_ID, LegacyExportRowCommonElement.ROW_MZ,
        LegacyExportRowCommonElement.ROW_RT,
        // ion mobility columns
        LegacyExportRowCommonElement.ROW_ION_MOBILITY,
        LegacyExportRowCommonElement.ROW_ION_MOBILITY_UNIT, LegacyExportRowCommonElement.ROW_CCS,
        // extra for ion identity networking
        LegacyExportRowCommonElement.ROW_CORR_GROUP_ID,
        LegacyExportRowCommonElement.ROW_MOL_NETWORK_ID,
        LegacyExportRowCommonElement.ROW_BEST_ANNOTATION_AND_SUPPORT,
        LegacyExportRowCommonElement.ROW_NEUTRAL_MASS};

    // per raw data file
    LegacyExportRowDataFileElement[] rawdata = new LegacyExportRowDataFileElement[]{
        featureMeasure.equals(FeatureMeasurementType.AREA)
            ? LegacyExportRowDataFileElement.FEATURE_AREA
            : LegacyExportRowDataFileElement.FEATURE_HEIGHT};

    FeatureListRowsFilter filter = parameters.getValue(GnpsFbmnExportAndSubmitParameters.FILTER);

    return new LegacyCSVExportTask(featureLists, full, ",", common, rawdata, false, ";", filter,
        getModuleCallDate());
  }


  /**
   * Export extra edges (wont create files if empty)
   */
  private AbstractTask addExtraEdgesTask(ParameterSet parameters) {
    File full = parameters.getParameter(GnpsFbmnExportAndSubmitParameters.FILENAME).getValue();
    FeatureListRowsFilter filter = parameters.getParameter(GnpsFbmnExportAndSubmitParameters.FILTER)
        .getValue();

    boolean exAnn = true;
    if (parameters.getParameter(GnpsFbmnExportAndSubmitParameters.SUBMIT).getValue()) {
      exAnn = parameters.getParameter(GnpsFbmnExportAndSubmitParameters.SUBMIT)
          .getEmbeddedParameters()
          .getParameter(GnpsFbmnSubmitParameters.EXPORT_ION_IDENTITY_NETWORKS).getValue();
    }

    return new ExportCorrAnnotationTask(featureLists, full, 0, filter, exAnn, false, false, false,
        getModuleCallDate());
  }

}
