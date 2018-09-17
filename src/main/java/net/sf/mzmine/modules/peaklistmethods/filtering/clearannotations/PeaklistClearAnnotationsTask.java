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

package net.sf.mzmine.modules.peaklistmethods.filtering.clearannotations;

import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakList.PeakListAppliedMethod;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.impl.SimpleFeature;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.datamodel.impl.SimplePeakListRow;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.PeakUtils;

import com.google.common.collect.Range;

/**
 * Filters out peak list rows.
 */
public class PeaklistClearAnnotationsTask extends AbstractTask {

  // Logger.
  private static final Logger LOG = Logger.getLogger(PeaklistClearAnnotationsTask.class.getName());
  // Peak lists.
  private final MZmineProject project;
  private final PeakList origPeakList;
  private PeakList filteredPeakList;
  // Processed rows counter
  private int processedRows, totalRows;
  // Parameters.
  private final ParameterSet parameters;

  /**
   * Create the task.
   *
   * @param list peak list to process.
   * @param parameterSet task parameters.
   */
  public PeaklistClearAnnotationsTask(final MZmineProject project, final PeakList list,
      final ParameterSet parameterSet) {

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
      LOG.info("Filtering peak list rows");

      totalRows = origPeakList.getRows().length;
      // Filter the peak list.
      for (PeakListRow row : origPeakList.getRows()) {

        if (parameters.getParameter(PeaklistClearAnnotationsParameters.CLEAR_IDENTITY).getValue()) {
          for (PeakIdentity identity : row.getPeakIdentities())
            row.removePeakIdentity(identity);
        }

        if (parameters.getParameter(PeaklistClearAnnotationsParameters.CLEAR_COMMENT).getValue()) {
          row.setComment("");
        }
        processedRows += 1;

      }

      if (getStatus() == TaskStatus.ERROR)
        return;

      if (isCanceled())
        return;

      // Add new peaklist to the project
      project.addPeakList(filteredPeakList);

      // Remove the original peaklist if requested
      /*
       * if (parameters .getParameter(PeaklistClearAnnotationsParameters.AUTO_REMOVE) .getValue()) {
       * project.removePeakList(origPeakList); }
       */

      setStatus(TaskStatus.FINISHED);
      LOG.info("Finished peak comparison rows filter");

    } catch (Throwable t) {
      t.printStackTrace();
      setErrorMessage(t.getMessage());
      setStatus(TaskStatus.ERROR);
      LOG.log(Level.SEVERE, "Peak comparison row filter error", t);
    }

  }

}


