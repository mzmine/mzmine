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

package net.sf.mzmine.modules.peaklist.ftmsfilter;

import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.data.impl.SimplePeakListRow;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.Task;

/**
 * 
 */
class FTMSFilterTask implements Task {

    private PeakList peaklist;

    private TaskStatus status = TaskStatus.WAITING;
    private String errorMessage;

    // peaks counter
    private int processedPeaks, totalPeaks;

    // parameter values
    private String suffix;
    private float mzDifferenceMin, mzDifferenceMax, rtDifferenceMax, heightMax;
    private boolean removeOriginal;

    /**
     * @param rawDataFile
     * @param parameters
     */
    FTMSFilterTask(PeakList peaklist, FTMSFilterParameters parameters) {

        this.peaklist = peaklist;

        // Get parameter values for easier use
        suffix = (String) parameters.getParameterValue(FTMSFilterParameters.suffix);
        mzDifferenceMin = (Float) parameters.getParameterValue(FTMSFilterParameters.mzDifferenceMin);
        mzDifferenceMax = (Float) parameters.getParameterValue(FTMSFilterParameters.mzDifferenceMax);
        rtDifferenceMax = (Float) parameters.getParameterValue(FTMSFilterParameters.rtDifferenceMax);
        heightMax = (Float) parameters.getParameterValue(FTMSFilterParameters.heightMax);
        removeOriginal = (Boolean) parameters.getParameterValue(FTMSFilterParameters.autoRemove);

    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return "FTMS shoulder peak filter on " + peaklist;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public float getFinishedPercentage() {
        if (totalPeaks == 0)
            return 0.0f;
        return (float) processedPeaks / (float) totalPeaks;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getStatus()
     */
    public TaskStatus getStatus() {
        return status;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getErrorMessage()
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    public PeakList getPeakList() {
        return peaklist;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#cancel()
     */
    public void cancel() {
        status = TaskStatus.CANCELED;
    }

    /**
     * @see Runnable#run()
     */
    public void run() {

        status = TaskStatus.PROCESSING;

        // We assume source peaklist contains one datafile
        RawDataFile dataFile = peaklist.getRawDataFile(0);

        // Create new deisotoped peaklist
        SimplePeakList filteredPeakList = new SimplePeakList(peaklist + " "
                + suffix, peaklist.getRawDataFiles());

        Peak[] allPeaks = peaklist.getPeaks(dataFile);

        totalPeaks = allPeaks.length;

        // Loop through all peaks
        allPeaksLoop: for (int candidatePeakIndex = 0; candidatePeakIndex < allPeaks.length; candidatePeakIndex++) {

            Peak candidatePeak = allPeaks[candidatePeakIndex];
            float candidatePeakMZ = candidatePeak.getMZ();
            float candidatePeakRT = candidatePeak.getRT();
            float candidatePeakHeight = candidatePeak.getHeight();

            for (int comparedPeakIndex = candidatePeakIndex + 1; comparedPeakIndex < allPeaks.length; comparedPeakIndex++) {

                // check if we're not canceled
                if (status == TaskStatus.CANCELED)
                    return;

                Peak comparedPeak = allPeaks[comparedPeakIndex];

                float comparedPeakMZ = comparedPeak.getMZ();
                float comparedPeakRT = comparedPeak.getRT();
                float comparedPeakHeight = comparedPeak.getHeight();

                float mzDifference = Math.abs(comparedPeakMZ - candidatePeakMZ);
                float rtDifference = Math.abs(comparedPeakRT - candidatePeakRT);

                if ((mzDifference >= mzDifferenceMin)
                        && (mzDifference <= mzDifferenceMax)
                        && (rtDifference <= rtDifferenceMax)
                        && (candidatePeakHeight <= (comparedPeakHeight * heightMax))) {
                    // Found a shoulder peak, skip it
                    processedPeaks++;
                    continue allPeaksLoop;
                }

            }

            // Add new row to the filtered peak list
            int oldRowID = peaklist.getPeakRow(candidatePeak).getID();
            SimplePeakListRow newRow = new SimplePeakListRow(oldRowID);
            newRow.addPeak(dataFile, candidatePeak, candidatePeak);
            filteredPeakList.addRow(newRow);

            // Update completion rate
            processedPeaks++;

        }

        // Add new peaklist to the project
        MZmineProject currentProject = MZmineCore.getCurrentProject();
        currentProject.addPeakList(filteredPeakList);

        // Remove the original peaklist if requested
        if (removeOriginal)
            currentProject.removePeakList(peaklist);

        status = TaskStatus.FINISHED;

    }

}
