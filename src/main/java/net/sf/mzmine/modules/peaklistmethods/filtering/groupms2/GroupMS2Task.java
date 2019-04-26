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

package net.sf.mzmine.modules.peaklistmethods.filtering.groupms2;

import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.common.collect.Range;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

/**
 * Filters out peak list rows.
 */
public class GroupMS2Task extends AbstractTask {

  // Logger.
  private static final Logger LOG = Logger.getLogger(GroupMS2Task.class.getName());
  // Peak lists.
  private final MZmineProject project;
  // Processed rows counter
  private int processedRows, totalRows;
  // Parameters.
  private final ParameterSet parameters;
  private PeakList list;
  private RTTolerance rtTol;
  private MZTolerance mzTol;
  private boolean limitRTByFeature;

  /**
   * Create the task.
   *
   * @param list peak list to process.
   * @param parameterSet task parameters.
   */
  public GroupMS2Task(final MZmineProject project, final PeakList list,
      final ParameterSet parameterSet) {

    // Initialize.
    this.project = project;
    parameters = parameterSet;
    rtTol = parameters.getParameter(GroupMS2Parameters.rtTol).getValue();
    mzTol = parameters.getParameter(GroupMS2Parameters.mzTol).getValue();
    limitRTByFeature = parameters.getParameter(GroupMS2Parameters.limitRTByFeature).getValue();
    this.list = list;
    processedRows = 0;
    totalRows = 0;
  }

  @Override
  public double getFinishedPercentage() {

    return totalRows == 0 ? 0.0 : (double) processedRows / (double) totalRows;
  }

  @Override
  public String getTaskDescription() {

    return "Adding all MS2 scans to their features in list " + list.getName();
  }

  @Override
  public void run() {

    try {
      setStatus(TaskStatus.PROCESSING);

      totalRows = list.getNumberOfRows();
      // for all features
      for (PeakListRow row : list.getRows()) {
        for (Feature f : row.getPeaks()) {
          if (getStatus() == TaskStatus.ERROR)
            return;
          if (isCanceled())
            return;

          RawDataFile raw = f.getDataFile();
          IntArrayList scans = new IntArrayList();
          int best = f.getRepresentativeScanNumber();
          double frt = f.getRT();
          double fmz = f.getMZ();
          Range<Double> rtRange = f.getRawDataPointsRTRange();
          int i = best;
          // left
          while (i > 1) {
            try {
              i--;
              Scan scan = raw.getScan(i);
              if ((!limitRTByFeature || rtRange.contains(scan.getRetentionTime()))
                  && rtTol.checkWithinTolerance(frt, scan.getRetentionTime())) {
                if (scan.getPrecursorMZ() != 0
                    && mzTol.checkWithinTolerance(fmz, scan.getPrecursorMZ()))
                  scans.add(i);
              } else {
                // end of loop - out of tolerance
                break;
              }
            } catch (Exception e) {
            }
          }
          int[] scanNumbers = raw.getScanNumbers();
          // right
          while (i < scanNumbers[scanNumbers.length - 1]) {
            try {
              i++;
              Scan scan = raw.getScan(i);
              if ((!limitRTByFeature || rtRange.contains(scan.getRetentionTime()))
                  && rtTol.checkWithinTolerance(frt, scan.getRetentionTime())) {
                if (scan.getPrecursorMZ() != 0
                    && mzTol.checkWithinTolerance(fmz, scan.getPrecursorMZ()))
                  scans.add(i);
              } else {
                // end of loop - out of tolerance
                break;
              }
            } catch (Exception e) {
            }
          }
          // set list to feature
          f.setAllMS2FragmentScanNumbers(scans.toIntArray());
        }
        processedRows++;
      }

      setStatus(TaskStatus.FINISHED);
      LOG.info("Finished adding all MS2 scans to their features in " + list.getName());

    } catch (Throwable t) {
      t.printStackTrace();
      setErrorMessage(t.getMessage());
      setStatus(TaskStatus.ERROR);
      LOG.log(Level.SEVERE, "Error while adding all MS2 scans to their feautres", t);
    }

  }

}


