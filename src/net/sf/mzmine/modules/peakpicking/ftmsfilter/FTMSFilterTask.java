/*
 * Copyright 2006-2007 The MZmine Development Team
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

package net.sf.mzmine.modules.peakpicking.ftmsfilter;

import java.util.logging.Logger;

import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.Task;

/**
 * 
 */
class FTMSFilterTask implements Task {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private RawDataFile dataFile;

    private TaskStatus status;
    private String errorMessage;

    private int processedPeaks, totalPeaks;

    private PeakList peakList;
    private float mzDifferenceMin, mzDifferenceMax, rtDifferenceMax, heightMax;

    /**
     * @param rawDataFile
     * @param parameters
     */
    FTMSFilterTask(RawDataFile dataFile, SimpleParameterSet parameters) {

        status = TaskStatus.WAITING;

        this.dataFile = dataFile;

        MZmineProject currentProject = MZmineCore.getCurrentProject();
        this.peakList = currentProject.getFilePeakList(dataFile);

        mzDifferenceMin = (Float) parameters.getParameterValue(FTMSFilter.mzDifferenceMin);
        mzDifferenceMax = (Float) parameters.getParameterValue(FTMSFilter.mzDifferenceMax);
        rtDifferenceMax = (Float) parameters.getParameterValue(FTMSFilter.rtDifferenceMax);
        heightMax = (Float) parameters.getParameterValue(FTMSFilter.heightMax);

    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return "FTMS shoulder peak filter on " + dataFile;
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

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getResult()
     */
    public Object getResult() {
        return null;
    }

    public RawDataFile getDataFile() {
        return dataFile;
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

        Peak[] allPeaks = peakList.getPeaks(dataFile);

        // Loop through all peaks
        totalPeaks = allPeaks.length;

        for (Peak mainPeak : allPeaks) {

            for (Peak candidateShoulderPeak : allPeaks) {

                // check if we're not canceled
                if (status == TaskStatus.CANCELED)
                    return;

                // do not compare peak to itself
                if (mainPeak == candidateShoulderPeak)
                    continue;

                float mzDifference = Math.abs(candidateShoulderPeak.getMZ()
                        - mainPeak.getMZ());
                float rtDifference = Math.abs(candidateShoulderPeak.getRT()
                        - mainPeak.getRT());

                if ((mzDifference >= mzDifferenceMin)
                        && (mzDifference <= mzDifferenceMax)
                        && (rtDifference <= rtDifferenceMax)
                        && (candidateShoulderPeak.getHeight() <= (mainPeak.getHeight() * heightMax))) {
                    // found a shoulder peak, remove it
                    int shoulderPeakRow = peakList.getPeakRow(candidateShoulderPeak);
                    if (shoulderPeakRow < 0) continue;
                    logger.finest("Found shoulder peak: main peak " + mainPeak
                            + ", shoulder peak " + candidateShoulderPeak);
                    peakList.removeRow(shoulderPeakRow);

                }

            }

            // Update completion rate
            processedPeaks++;

        }

        status = TaskStatus.FINISHED;

    }

}
