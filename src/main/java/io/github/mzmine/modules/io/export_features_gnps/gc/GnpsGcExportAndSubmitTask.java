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

package io.github.mzmine.modules.io.export_features_gnps.gc;

import com.google.common.util.concurrent.AtomicDouble;
import io.github.msdk.MSDKRuntimeException;
import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.export_features_csv.CSVExportModularTask;
import io.github.mzmine.modules.io.export_features_csv_legacy.LegacyCSVExportTask;
import io.github.mzmine.modules.io.export_features_csv_legacy.LegacyExportRowCommonElement;
import io.github.mzmine.modules.io.export_features_csv_legacy.LegacyExportRowDataFileElement;
import io.github.mzmine.modules.io.export_features_gnps.GNPSUtils;
import io.github.mzmine.modules.io.export_features_gnps.fbmn.FeatureListRowsFilter;
import io.github.mzmine.modules.io.export_features_gnps.fbmn.GnpsFbmnExportAndSubmitParameters;
import io.github.mzmine.modules.io.export_features_mgf.AdapMgfExportModule;
import io.github.mzmine.modules.io.export_features_mgf.AdapMgfExportParameters;
import io.github.mzmine.modules.io.export_features_mgf.AdapMgfExportParameters.MzMode;
import io.github.mzmine.modules.io.export_features_mgf.AdapMgfExportTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.AllTasksFinishedListener;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.awt.Desktop;
import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Exports all files needed for GNPS GC-MS workflow
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class GnpsGcExportAndSubmitTask extends AbstractTask {

  // Logger.
  private final Logger logger = Logger.getLogger(getClass().getName());

  private final ParameterSet parameters;
  private final AtomicDouble progress = new AtomicDouble(0);

  private final FeatureList featureList;
  private final MzMode representativeMZ;
  private final AbundanceMeasure featureMeasure;

  private File file;
  private final boolean openFolder;

  GnpsGcExportAndSubmitTask(ParameterSet parameters, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null
    this.parameters = parameters;

    this.featureList = parameters.getParameter(GnpsGcExportAndSubmitParameters.FEATURE_LISTS)
        .getValue().getMatchingFeatureLists()[0];
    this.representativeMZ = parameters.getParameter(
        GnpsGcExportAndSubmitParameters.REPRESENTATIVE_MZ).getValue();
    this.featureMeasure = parameters.getParameter(GnpsGcExportAndSubmitParameters.FEATURE_INTENSITY)
        .getValue();
    openFolder = parameters.getParameter(GnpsGcExportAndSubmitParameters.OPEN_FOLDER).getValue();
    file = parameters.getParameter(GnpsGcExportAndSubmitParameters.FILENAME).getValue();
    file = FileAndPathUtil.eraseFormat(file);
    parameters.getParameter(GnpsGcExportAndSubmitParameters.FILENAME).setValue(file);
    // submit =
    // parameters.getParameter(GnpsGcExportAndSubmitParameters.SUBMIT).getValue();
//    submit = false;
  }

  @Override
  public TaskPriority getTaskPriority() {
    // to not block mzmine with single process (1 thread)
    return TaskPriority.HIGH;
  }

  @Override
  public String getTaskDescription() {
    return "Exporting files GNPS feature based molecular networking job";
  }

  @Override
  public double getFinishedPercentage() {
    return progress.get();
  }

  @Override
  public void run() {
    final AbstractTask thistask = this;
    setStatus(TaskStatus.PROCESSING);

    List<AbstractTask> list = new ArrayList<>(3);
    // add mgf export task
    list.add(addAdapMgfTask(parameters));

    // add csv quant table
    list.add(addLegacyQuantTableTask(parameters, null));
    // add full quant table
//    list.add(addFullQuantTableTask(parameters, null));

    // finish listener to submit
    final File fileName = file;
    final File folder = file.getParentFile();
    new AllTasksFinishedListener(list, true,
        // succeed
        l -> {
          try {
            logger.info("succeed" + thistask.getStatus().toString());
//            if (submit) {
//              GnpsGcSubmitParameters param = parameters.getParameter(
//                  GnpsGcExportAndSubmitParameters.SUBMIT).getEmbeddedParameters();
//              submit(fileName, param);
//            }

            // open folder
            try {
              if (openFolder && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(folder);
              }
            } catch (Exception ex) {
            }
          } finally {
            // finish task
            if (thistask.getStatus() == TaskStatus.PROCESSING) {
              thistask.setStatus(TaskStatus.FINISHED);
            }
          }
        }, lerror -> {
      setErrorMessage("GNPS-GC submit was not started due too errors while file export");
      thistask.setStatus(TaskStatus.ERROR);
      throw new MSDKRuntimeException(
          "GNPS-GC submit was not started due too errors while file export");
    },
        // cancel if one was cancelled
        listCancelled -> cancel()) {
      @Override
      public void taskStatusChanged(Task task, TaskStatus newStatus, TaskStatus oldStatus) {
        super.taskStatusChanged(task, newStatus, oldStatus);
        // show progress
        progress.getAndSet(getProgress());
      }
    };

    MZmineCore.getTaskController().addTasks(list.toArray(new AbstractTask[list.size()]));

    // wait till finish
    while (!(isCanceled() || isFinished())) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        logger.log(Level.SEVERE, "Error in GNPS-GC export/submit task", e);
      }
    }
  }

  /**
   * Export mgf (adap mgf export) of clustered spectra
   *
   * @param parameters
   * @return
   */
  private AbstractTask addAdapMgfTask(ParameterSet parameters) {
    File full = parameters.getParameter(GnpsGcExportAndSubmitParameters.FILENAME).getValue();
    String name = FilenameUtils.removeExtension(full.getName());
    full = FileAndPathUtil.getRealFilePath(full.getParentFile(), name, "mgf");

    ParameterSet mgfParam = MZmineCore.getConfiguration()
        .getModuleParameters(AdapMgfExportModule.class);
    mgfParam.getParameter(AdapMgfExportParameters.FILENAME).setValue(full);
    mgfParam.getParameter(AdapMgfExportParameters.FRACTIONAL_MZ).setValue(true);
    mgfParam.getParameter(AdapMgfExportParameters.REPRESENTATIVE_MZ).setValue(representativeMZ);
    return new AdapMgfExportTask(mgfParam, new FeatureList[]{featureList},
        getModuleCallDate()); // todo: this will be inconsistent for batch modes, because the task will add it's own applied method.
  }

  /**
   * Submit GNPS job
   *
   * @param fileName
   * @param param
   */
  private void submit(File fileName, GnpsGcSubmitParameters param) {
    try {
      String url = GNPSUtils.submitGcJob(fileName, param);
      if (url == null || url.isEmpty()) {
        logger.log(Level.WARNING, "GNPS-GC submit failed (url empty)");
      }
    } catch (Exception e) {
      logger.log(Level.WARNING, "GNPS-GC submit failed", e);
    }
  }


  /**
   * Export the whole quant table in the new format
   *
   * @param parameters export parameters {@link GnpsFbmnExportAndSubmitParameters}
   * @param tasks      new task is added to this list of tasks
   */
  private AbstractTask addFullQuantTableTask(ParameterSet parameters, Collection<Task> tasks) {
    File full = parameters.getParameter(GnpsFbmnExportAndSubmitParameters.FILENAME).getValue();
    final String name = FilenameUtils.removeExtension(full.getName());
    full = new File(full.getParentFile(), name + "_quant_full.csv");

    CSVExportModularTask quanExportModular = new CSVExportModularTask(new ModularFeatureList[]{
        (ModularFeatureList) featureList}, full, ",", ";", FeatureListRowsFilter.ALL,
        true, getModuleCallDate());

    if (tasks != null) {
      tasks.add(quanExportModular);
    }
    return quanExportModular;
  }

  /**
   * Export quant table
   *
   * @param parameters
   * @param tasks
   */
  private AbstractTask addLegacyQuantTableTask(ParameterSet parameters, Collection<Task> tasks) {
    File full = parameters.getParameter(GnpsGcExportAndSubmitParameters.FILENAME).getValue();
    String name = FilenameUtils.removeExtension(full.getName());
    full = FileAndPathUtil.getRealFilePath(full.getParentFile(), name + "_quant", "csv");

    LegacyExportRowCommonElement[] common = new LegacyExportRowCommonElement[]{
        LegacyExportRowCommonElement.ROW_ID, LegacyExportRowCommonElement.ROW_MZ,
        LegacyExportRowCommonElement.ROW_RT};

    // height or area?
    LegacyExportRowDataFileElement[] rawdata = new LegacyExportRowDataFileElement[]{
        featureMeasure.equals(AbundanceMeasure.Area) ? LegacyExportRowDataFileElement.FEATURE_AREA
            : LegacyExportRowDataFileElement.FEATURE_HEIGHT};

    LegacyCSVExportTask quanExport = new LegacyCSVExportTask(new FeatureList[]{featureList}, full,
        ",", common, rawdata, false, ";", FeatureListRowsFilter.ALL, getModuleCallDate());
    if (tasks != null) {
      tasks.add(quanExport);
    }
    return quanExport;
  }

}
