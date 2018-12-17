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

package net.sf.mzmine.modules.peaklistmethods.io.gnpsexport;

import java.awt.Desktop;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.common.util.concurrent.AtomicDouble;
import io.github.msdk.MSDKRuntimeException;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.io.csvexport.CSVExportTask;
import net.sf.mzmine.modules.peaklistmethods.io.csvexport.ExportRowCommonElement;
import net.sf.mzmine.modules.peaklistmethods.io.csvexport.ExportRowDataFileElement;
import net.sf.mzmine.modules.peaklistmethods.io.gnpsexport.GNPSExportAndSubmitParameters.RowFilter;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.AllTasksFinishedListener;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskPriority;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.files.FileAndPathUtil;

/**
 * Exports all files needed for GNPS
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class GNPSExportAndSubmitTask extends AbstractTask {
  // Logger.
  private final Logger LOG = Logger.getLogger(getClass().getName());

  /**
   * This website is still the beta. TODO change later to the final
   */
  public static final String GNPS_WEBSITE =
      "http://mingwangbeta.ucsd.edu:5050/featurebasednetworking";

  private ParameterSet parameters;
  private AtomicDouble progress = new AtomicDouble(0);


  GNPSExportAndSubmitTask(ParameterSet parameters) {
    this.parameters = parameters;
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

    boolean openFolder =
        parameters.getParameter(GNPSExportAndSubmitParameters.OPEN_FOLDER).getValue();
    boolean submit = parameters.getParameter(GNPSExportAndSubmitParameters.SUBMIT).getValue();
    File file = parameters.getParameter(GNPSExportAndSubmitParameters.FILENAME).getValue();
    file = FileAndPathUtil.eraseFormat(file);
    parameters.getParameter(GNPSExportAndSubmitParameters.FILENAME).setValue(file);

    List<AbstractTask> list = new ArrayList<>(3);
    GNPSmgfExportTask task = new GNPSmgfExportTask(parameters);
    list.add(task);

    // add csv quant table
    list.add(addQuantTableTask(parameters, null));

    // finish listener to submit
    final File fileName = file;
    final File folder = file.getParentFile();
    new AllTasksFinishedListener(list, true,
        // succeed
        l -> {
          try {
            LOG.info("succeed" + thistask.getStatus().toString());
            if (submit) {
              GNPSSubmitParameters param = parameters
                  .getParameter(GNPSExportAndSubmitParameters.SUBMIT).getEmbeddedParameters();
              submit(fileName, param);
            }

            // open folder
            try {
              if (openFolder && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(folder);
              }
            } catch (Exception ex) {
            }
          } finally {
            // finish task
            if (thistask.getStatus() == TaskStatus.PROCESSING)
              thistask.setStatus(TaskStatus.FINISHED);
          }
        }, lerror -> {
          setErrorMessage("GNPS submit was not started due too errors while file export");
          thistask.setStatus(TaskStatus.ERROR);
          throw new MSDKRuntimeException(
              "GNPS submit was not started due too errors while file export");
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
        LOG.log(Level.SEVERE, "Error in GNPS export/submit task", e);
      }
    }
  }


  /**
   * Submit GNPS job
   * 
   * @param fileName
   * @param param
   */
  private void submit(File fileName, GNPSSubmitParameters param) {
    try {
      String url = GNPSUtils.submitJob(fileName, param);
      if (url == null || url.isEmpty())
        LOG.log(Level.WARNING, "GNPS submit failed (url empty)");
    } catch (Exception e) {
      LOG.log(Level.WARNING, "GNPS submit failed", e);
    }
  }

  /**
   * Export quant table
   * 
   * @param parameters
   * @param tasks
   */
  private AbstractTask addQuantTableTask(ParameterSet parameters, Collection<Task> tasks) {
    File full = parameters.getParameter(GNPSExportAndSubmitParameters.FILENAME).getValue();
    String name = FileAndPathUtil.eraseFormat(full.getName());
    full = FileAndPathUtil.getRealFilePath(full.getParentFile(), name + "_quant", "csv");

    ExportRowCommonElement[] common = new ExportRowCommonElement[] {ExportRowCommonElement.ROW_ID,
        ExportRowCommonElement.ROW_MZ, ExportRowCommonElement.ROW_RT};

    ExportRowDataFileElement[] rawdata =
        new ExportRowDataFileElement[] {ExportRowDataFileElement.PEAK_AREA};

    RowFilter filter = parameters.getParameter(GNPSExportAndSubmitParameters.FILTER).getValue();

    CSVExportTask quanExport = new CSVExportTask(
        parameters.getParameter(GNPSExportAndSubmitParameters.PEAK_LISTS).getValue()
            .getMatchingPeakLists(), //
        full, ",", common, rawdata, false, ";", filter);
    if (tasks != null)
      tasks.add(quanExport);
    return quanExport;
  }

}
