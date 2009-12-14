/*
 * Copyright 2006-2009 The MZmine 2 Development Team
 *
 * This file is part of MZmine 2.
 *
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */
package net.sf.mzmine.modules.gapfilling.peakfinderRansac;

import java.util.Vector;
import java.util.logging.Logger;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.PeakIdentity;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.data.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.data.impl.SimplePeakListRow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.Range;
import org.apache.commons.math.stat.regression.SimpleRegression;

class PeakFinderTask implements Task {

    private Logger logger = Logger.getLogger(this.getClass().getName());
    private TaskStatus status = TaskStatus.WAITING;
    private String errorMessage;
    private PeakList peakList,  processedPeakList;
    private String suffix;
    private double intTolerance,  mzTolerance;
    private double rtToleranceValueAbs;
    private PeakFinderParameters parameters;
    private int processedScans,  totalScans;

    PeakFinderTask(PeakList peakList, PeakFinderParameters parameters) {

        this.peakList = peakList;
        this.parameters = parameters;

        suffix = (String) parameters.getParameterValue(PeakFinderParameters.suffix);
        intTolerance = (Double) parameters.getParameterValue(PeakFinderParameters.intTolerance);
        mzTolerance = (Double) parameters.getParameterValue(PeakFinderParameters.MZTolerance);
        rtToleranceValueAbs = (Double) parameters.getParameterValue(PeakFinderParameters.RTToleranceValueAbs);
    }

    public void run() {

        status = TaskStatus.PROCESSING;
        logger.info("Running Ransac gap filler on " + peakList);

        // Calculate total number of scans in all files
        for (RawDataFile dataFile : peakList.getRawDataFiles()) {
            totalScans += dataFile.getNumOfScans(1);
        }

        // Create new peak list
        processedPeakList = new SimplePeakList(peakList + " " + suffix,
                peakList.getRawDataFiles());

        // Fill new peak list with empty rows
        for (int row = 0; row < peakList.getNumberOfRows(); row++) {
            PeakListRow sourceRow = peakList.getRow(row);
            PeakListRow newRow = new SimplePeakListRow(sourceRow.getID());
            newRow.setComment(sourceRow.getComment());
            for (PeakIdentity ident : sourceRow.getPeakIdentities()) {
                newRow.addPeakIdentity(ident, false);
            }
            if (sourceRow.getPreferredPeakIdentity() != null) {
                newRow.setPreferredPeakIdentity(sourceRow.getPreferredPeakIdentity());
            }
            processedPeakList.addRow(newRow);
        }


        // Process all raw data files
        for (RawDataFile dataFile : peakList.getRawDataFiles()) {

            RegressionInfo info = new RegressionInfo(dataFile);
            for (PeakListRow row : peakList.getRows()) {
                ChromatographicPeak peaki = row.getPeak(dataFile);
                double peake = row.getAverageRT();
                if (peaki != null) {
                    info.addData(peaki.getRT(), peake);
                }
            }

            // Canceled?
            if (status == TaskStatus.CANCELED) {
                return;
            }

            Vector<Gap> gaps = new Vector<Gap>();

            // Fill each row of this raw data file column, create new empty gaps
            // if necessary
            for (int row = 0; row < peakList.getNumberOfRows(); row++) {
                PeakListRow sourceRow = peakList.getRow(row);
                PeakListRow newRow = processedPeakList.getRow(row);

                ChromatographicPeak sourcePeak = sourceRow.getPeak(dataFile);

                if (sourcePeak == null) {

                    // Create a new gap

                    double mz = sourceRow.getAverageMZ();
                    double rt = this.getRealRT(info, dataFile, sourceRow);

                    if (rt == -1) {
                        continue;
                    }

                    Gap newGap = new Gap(newRow, dataFile, mz, rt,
                            intTolerance, mzTolerance, rtToleranceValueAbs);

                    gaps.add(newGap);

                } else {
                    newRow.addPeak(dataFile, sourcePeak);
                }

            }

            // Stop processing this file if there are no gaps
            if (gaps.size() == 0) {
                processedScans += dataFile.getNumOfScans();
                continue;
            }

            // Get all scans of this data file
            int scanNumbers[] = dataFile.getScanNumbers(1);

            // Process each scan
            for (int scanNumber : scanNumbers) {

                // Canceled?
                if (status == TaskStatus.CANCELED) {
                    return;
                }

                // Get the scan
                Scan scan = dataFile.getScan(scanNumber);

                // Feed this scan to all gaps
                for (Gap gap : gaps) {
                    gap.offerNextScan(scan);
                }

                processedScans++;
            }

            // Finalize gaps
            for (Gap gap : gaps) {
                gap.noMoreOffers();
            }

        }

        // Append processed peak list to the project
        MZmineProject currentProject = MZmineCore.getCurrentProject();
        currentProject.addPeakList(processedPeakList);

        // Add task description to peakList
        processedPeakList.addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod(
                "Ransac Gap filling ", parameters));

        logger.info("Finished Ransac gap-filling on " + peakList);
        status = TaskStatus.FINISHED;

    }

    /**
     * Return the retention time where the peak must be based on the ransac
     * alignment of all the samples.
     */
    public double getRealRT(RegressionInfo info, RawDataFile rawDataFile, PeakListRow row) {

        try {
            double RTX = row.getAverageRT();
            double minRT = RTX - 60;
            if (minRT < 0) {
                minRT = 0;
            }
            SimpleRegression regression = info.getSimpleRegression(new Range(minRT, RTX + 60), this.rtToleranceValueAbs * 10);
            return regression.predict(RTX);


        } catch (Exception e) {
            return -1;
        }
    }

    public void cancel() {
        status = TaskStatus.CANCELED;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public double getFinishedPercentage() {
        if (totalScans == 0) {
            return 0;
        }
        return (double) processedScans / (double) totalScans;

    }

    public TaskStatus getStatus() {
        return status;
    }

    public String getTaskDescription() {
        return "Ransac Gap filling " + peakList;
    }

    PeakList getPeakList() {
        return peakList;
    }

    public Object[] getCreatedObjects() {
        return new Object[]{processedPeakList};
    }
}
