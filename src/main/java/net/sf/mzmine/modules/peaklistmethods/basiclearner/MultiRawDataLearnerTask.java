/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.peaklistmethods.basiclearner;

import java.util.Arrays;
import java.util.logging.Logger;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakList.PeakListAppliedMethod;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.PeakListRowSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

class MultiRawDataLearnerTask extends AbstractTask {
  private Logger logger = Logger.getLogger(this.getClass().getName());

  private final MZmineProject project;
  private PeakList peakList;
  private PeakList resultPeakList;

  // peaks counter
  private int processedRows;
  private int totalRows;

  // parameter values
  private String suffix;
  private MZTolerance mzTolerance;
  private RTTolerance rtTolerance;
  private boolean removeOriginal;
  private ParameterSet parameters;

  /**
   * Constructor to set all parameters and the project
   * 
   * @param rawDataFile
   * @param parameters
   */
  public MultiRawDataLearnerTask(MZmineProject project, PeakList peakList,
      ParameterSet parameters) {
    this.project = project;
    this.peakList = peakList;
    this.parameters = parameters;
    // Get parameter values for easier use
    suffix = parameters.getParameter(LearnerParameters.suffix).getValue();
    mzTolerance = parameters.getParameter(LearnerParameters.mzTolerance).getValue();
    rtTolerance = parameters.getParameter(LearnerParameters.rtTolerance).getValue();
    removeOriginal = parameters.getParameter(LearnerParameters.autoRemove).getValue();
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
   */
  @Override
  public String getTaskDescription() {
    return "Learner task on " + peakList;
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    if (totalRows == 0)
      return 0;
    return (double) processedRows / (double) totalRows;
  }

  /**
   * @see Runnable#run()
   */
  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    logger.info("Running learner task on " + peakList);

    // Create a new results peakList which is added at the end
    resultPeakList = new SimplePeakList(peakList + " " + suffix, peakList.getRawDataFiles());

    /**
     * - A PeakList is a list of Features (peak in retention time dimension with accurate m/z)<br>
     * ---- contains one or multiple RawDataFiles <br>
     * ---- access mean retention time, mean m/z, maximum intensity, ...<br>
     * - A RawDataFile holds a full chromatographic run with all ms scans<br>
     * ---- Each Scan and the underlying raw data can be accessed <br>
     * ---- Scans can be filtered by MS level, polarity, ...<br>
     */
    // get all rows and sort by m/z
    PeakListRow[] rows = peakList.getRows();
    Arrays.sort(rows, new PeakListRowSorter(SortingProperty.MZ, SortingDirection.Ascending));

    // number of rawFiles is 1 prior to peaklist alignment
    RawDataFile[] rawFiles = peakList.getRawDataFiles();
    boolean isAlignedPeakList = rawFiles.length > 1;

    totalRows = rows.length;
    // loop through all rows
    for (PeakListRow row : rows) {
      // loop through all raw data files
      for (RawDataFile raw : rawFiles) {
        // check for cancelled state and stop
        if (isCanceled())
          return;

        // current peak
        Feature peak = row.getPeak(raw);
        // check for peak in row for specific raw file
        if (peak != null) {
          double mz = peak.getMZ();
          double intensity = peak.getHeight();
          double rt = peak.getRT();

          // do stuff
          // ...

        }
      }
      // Update completion rate
      processedRows++;
    }

    // add to project
    addResultToProject();

    logger.info("Finished on " + peakList);
    setStatus(TaskStatus.FINISHED);
  }

  /**
   * Add peaklist to project, delete old if requested, add description to result
   */
  public void addResultToProject() {
    // Add new peakList to the project
    project.addPeakList(resultPeakList);

    // Load previous applied methods
    for (PeakListAppliedMethod proc : peakList.getAppliedMethods()) {
      resultPeakList.addDescriptionOfAppliedTask(proc);
    }

    // Add task description to peakList
    resultPeakList
        .addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod("Learner task", parameters));

    // Remove the original peakList if requested
    if (removeOriginal)
      project.removePeakList(peakList);
  }

}
