/*
 * Copyright 2006-2008 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peakpicking.gapfiller;

import java.util.Vector;

import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.data.impl.SimplePeakListRow;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.Task;

class GapFillerTask implements Task {

    private TaskStatus status = TaskStatus.WAITING;
    private String errorMessage;

    private PeakList peakList;

    private String suffix;
    private float intTolerance, mzTolerance;
    private boolean rtToleranceUseAbs;
    private float rtToleranceValueAbs, rtToleranceValuePercent;

    private int processedScans, totalScans;

    GapFillerTask(PeakList peakList, GapFillerParameters parameters) {

        this.peakList = peakList;

        suffix = (String) parameters.getParameterValue(GapFillerParameters.suffix);
        intTolerance = (Float) parameters.getParameterValue(GapFillerParameters.intTolerance);
        mzTolerance = (Float) parameters.getParameterValue(GapFillerParameters.MZTolerance);
        if (parameters.getParameterValue(GapFillerParameters.RTToleranceType) == GapFillerParameters.RTToleranceTypeAbsolute)
            rtToleranceUseAbs = true;
        rtToleranceValueAbs = (Float) parameters.getParameterValue(GapFillerParameters.RTToleranceValueAbs);
        rtToleranceValuePercent = (Float) parameters.getParameterValue(GapFillerParameters.RTToleranceValuePercent);
    }

    public void run() {

        status = TaskStatus.PROCESSING;

        // Calculate total number of scans in all files
        for (RawDataFile dataFile : peakList.getRawDataFiles()) {
            totalScans += dataFile.getNumOfScans();
        }

        // Create new peak list
        SimplePeakList processedPeakList = new SimplePeakList(peakList + " "
                + suffix, peakList.getRawDataFiles());

        // Fill new peak list with empty rows
        for (int row = 0; row < peakList.getNumberOfRows(); row++) {
            PeakListRow sourceRow = peakList.getRow(row);
            PeakListRow newRow = new SimplePeakListRow(sourceRow.getID());
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

                Peak sourcePeak = sourceRow.getPeak(dataFile);
                Peak sourceOriginalPeak = sourceRow.getOriginalPeakListEntry(dataFile);

                if (sourcePeak == null) {

                    // Create a new gap

                    float mz = sourceRow.getAverageMZ();
                    float rt = sourceRow.getAverageRT();
                    float rtTolerance;
                    if (rtToleranceUseAbs)
                        rtTolerance = rtToleranceValueAbs;
                    else
                        rtTolerance = rt * rtToleranceValuePercent;

                    Gap newGap = new Gap(newRow, dataFile, mz, rt, intTolerance,
                            mzTolerance, rtTolerance);

                    gaps.add(newGap);

                } else {
                    newRow.addPeak(dataFile, sourceOriginalPeak, sourcePeak);
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

    public float getFinishedPercentage() {
        if (totalScans == 0)
            return 0;
        return (float) processedScans / (float) totalScans;

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
