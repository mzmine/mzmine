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

package net.sf.mzmine.modules.peakpicking.gapfiller;

import java.util.Vector;

import net.sf.mzmine.data.PeakIdentity;
import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.data.impl.SimplePeakListRow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.Task;

class GapFillerTask implements Task {

    private TaskStatus status = TaskStatus.WAITING;
    private String errorMessage;

    private PeakList peakList;

    private String suffix;
    private double intTolerance, mzTolerance;
    private boolean rtToleranceUseAbs;
    private double rtToleranceValueAbs, rtToleranceValuePercent;

    private int processedScans, totalScans;

    GapFillerTask(PeakList peakList, GapFillerParameters parameters) {

        this.peakList = peakList;

        suffix = (String) parameters.getParameterValue(GapFillerParameters.suffix);
        intTolerance = (Double) parameters.getParameterValue(GapFillerParameters.intTolerance);
        mzTolerance = (Double) parameters.getParameterValue(GapFillerParameters.MZTolerance);
        if (parameters.getParameterValue(GapFillerParameters.RTToleranceType) == GapFillerParameters.RTToleranceTypeAbsolute)
            rtToleranceUseAbs = true;
        rtToleranceValueAbs = (Double) parameters.getParameterValue(GapFillerParameters.RTToleranceValueAbs);
        rtToleranceValuePercent = (Double) parameters.getParameterValue(GapFillerParameters.RTToleranceValuePercent);
    }

    public void run() {

        status = TaskStatus.PROCESSING;

        // Calculate total number of scans in all files
        for (RawDataFile dataFile : peakList.getRawDataFiles()) {
            totalScans += dataFile.getNumOfScans(1);
        }

        // Create new peak list
        SimplePeakList processedPeakList = new SimplePeakList(peakList + " "
                + suffix, peakList.getRawDataFiles());

        // Fill new peak list with empty rows
        for (int row = 0; row < peakList.getNumberOfRows(); row++) {
            PeakListRow sourceRow = peakList.getRow(row);
            PeakListRow newRow = new SimplePeakListRow(sourceRow.getID());
            newRow.setComment(sourceRow.getComment());
            for (PeakIdentity ident : sourceRow.getCompoundIdentities())
                newRow.addCompoundIdentity(ident, false);
            if (sourceRow.getPreferredCompoundIdentity() != null)
                newRow.setPreferredCompoundIdentity(sourceRow.getPreferredCompoundIdentity());
            processedPeakList.addRow(newRow);
        }

        // Process all raw data files
        for (RawDataFile dataFile : peakList.getRawDataFiles()) {

            // Canceled?
            if (status == TaskStatus.CANCELED)
                return;

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
                    double rt = sourceRow.getAverageRT();
                    double rtTolerance;
                    if (rtToleranceUseAbs)
                        rtTolerance = rtToleranceValueAbs;
                    else
                        rtTolerance = rt * rtToleranceValuePercent;

                    Gap newGap = new Gap(newRow, dataFile, mz, rt,
                            intTolerance, mzTolerance, rtTolerance);

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
                if (status == TaskStatus.CANCELED)
                    return;

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

        status = TaskStatus.FINISHED;

    }

    public void cancel() {
        status = TaskStatus.CANCELED;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public double getFinishedPercentage() {
        if (totalScans == 0)
            return 0;
        return (double) processedScans / (double) totalScans;

    }

    public TaskStatus getStatus() {
        return status;
    }

    public String getTaskDescription() {
        return "Gap filling " + peakList;
    }

    PeakList getPeakList() {
        return peakList;
    }

}
