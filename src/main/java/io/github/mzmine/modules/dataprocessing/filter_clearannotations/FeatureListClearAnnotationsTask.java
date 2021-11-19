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

package io.github.mzmine.modules.dataprocessing.filter_clearannotations;

import io.github.mzmine.datamodel.FeatureIdentity;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Filters out feature list rows.
 */
public class FeatureListClearAnnotationsTask extends AbstractTask {

  // Logger.
  private static final Logger logger =
      Logger.getLogger(FeatureListClearAnnotationsTask.class.getName());
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
  public FeatureListClearAnnotationsTask(final MZmineProject project, final FeatureList list,
      final ParameterSet parameterSet, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null
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

    return "Clearing annotation from peaklist";
  }

  @Override
  public void run() {

    try {
      setStatus(TaskStatus.PROCESSING);
      logger.info("Filtering feature list rows");

      totalRows = origPeakList.getRows().size();
      // Filter the feature list.
      for (FeatureListRow row : origPeakList.getRows()) {

        if (parameters.getParameter(FeatureListClearAnnotationsParameters.CLEAR_IDENTITY)
            .getValue()) {
          for (FeatureIdentity identity : row.getPeakIdentities())
            row.removeFeatureIdentity(identity);
        }

        if (parameters.getParameter(FeatureListClearAnnotationsParameters.CLEAR_COMMENT)
            .getValue()) {
          row.setComment("");
        }
        processedRows += 1;

      }

      if (getStatus() == TaskStatus.ERROR)
        return;

      if (isCanceled())
        return;

      // Add new peaklist to the project
//      project.addFeatureList(filteredPeakList); // the origList is processed, this doesnt make sense
      origPeakList.getAppliedMethods().add(new SimpleFeatureListAppliedMethod(
          FeatureListClearAnnotationsModule.class, parameters, getModuleCallDate()));

      // Remove the original peaklist if requested
      /*
       * if (parameters .getParameter(PeaklistClearAnnotationsParameters.AUTO_REMOVE) .getValue()) {
       * project.removePeakList(origPeakList); }
       */

      setStatus(TaskStatus.FINISHED);
      logger.info("Finished peak comparison rows filter");

    } catch (Throwable t) {
      t.printStackTrace();
      setErrorMessage(t.getMessage());
      setStatus(TaskStatus.ERROR);
      logger.log(Level.SEVERE, "Peak comparison row filter error", t);
    }

  }

}
