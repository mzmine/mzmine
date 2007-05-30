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

package net.sf.mzmine.modules.filtering.zoomscan;

import java.io.IOException;

import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.io.RawDataFileWriter;
import net.sf.mzmine.taskcontrol.Task;

/**
 * 
 */
class ZoomScanFilterTask implements Task {

    private OpenedRawDataFile dataFile;
    private RawDataFile rawDataFile;

    private TaskStatus status = TaskStatus.WAITING;
    private String errorMessage;

    private int filteredScans;
    private int totalScans;

    private RawDataFile filteredRawDataFile;

    private double minMZRange;

    /**
     * @param rawDataFile
     * @param parameters
     */
    ZoomScanFilterTask(OpenedRawDataFile dataFile, SimpleParameterSet parameters) {
        this.dataFile = dataFile;
        this.rawDataFile = dataFile.getCurrentFile();

        minMZRange = (Double) parameters.getParameterValue(ZoomScanFilter.parameterMinMZRange);
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return "Zoom scan filtering " + dataFile;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public float getFinishedPercentage() {
        if (totalScans == 0)
            return 0.0f;
        return (float) filteredScans / totalScans;
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
        return filteredRawDataFile;
    }

    public OpenedRawDataFile getDataFile() {
        return dataFile;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#cancel()
     */
    public void cancel() {
        status = TaskStatus.CANCELED;
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {

        status = TaskStatus.PROCESSING;

        // Create new temporary copy
        RawDataFileWriter rawDataFileWriter;
        try {
            rawDataFileWriter = dataFile.createNewTemporaryFile();
        } catch (IOException e) {
            status = TaskStatus.ERROR;
            errorMessage = e.toString();
            return;
        }

        int[] scanNumbers = rawDataFile.getScanNumbers();
        totalScans = scanNumbers.length;

        // Loop through all scans
        for (int scani = 0; scani < totalScans; scani++) {
            Scan sc;
            try {
                sc = rawDataFile.getScan(scanNumbers[scani]);
            } catch (IOException e) {
                status = TaskStatus.ERROR;
                errorMessage = e.toString();
                return;
            }

            // Check if mz range is wide enough
            double mzMin = sc.getMZRangeMin();
            double mzMax = sc.getMZRangeMax();
            if ((mzMax - mzMin) < minMZRange) {
                continue;
            }

            try {
                rawDataFileWriter.addScan(sc);
            } catch (IOException e) {
                status = TaskStatus.ERROR;
                errorMessage = e.toString();
                return;
            }

            filteredScans++;

        }

        // Finalize writing
        try {
            filteredRawDataFile = rawDataFileWriter.finishWriting();
        } catch (IOException e) {
            status = TaskStatus.ERROR;
            errorMessage = e.toString();
            return;
        }

        status = TaskStatus.FINISHED;

    }

}
