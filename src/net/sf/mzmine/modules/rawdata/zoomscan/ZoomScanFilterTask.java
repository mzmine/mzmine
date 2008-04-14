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

package net.sf.mzmine.modules.rawdata.zoomscan;

import java.io.IOException;

import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.RawDataFileWriter;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.taskcontrol.Task;

/**
 * 
 */
class ZoomScanFilterTask implements Task {

    private RawDataFile dataFile;

    private TaskStatus status = TaskStatus.WAITING;
    private String errorMessage;

    // scan counter
    private int filteredScans, totalScans;

    // parameter values
    private String suffix;
    private float minMZRange;
    private boolean removeOriginal;

    /**
     * @param rawDataFile
     * @param parameters
     */
    ZoomScanFilterTask(RawDataFile dataFile, ZoomScanFilterParameters parameters) {
        this.dataFile = dataFile;
        suffix = (String) parameters.getParameterValue(ZoomScanFilterParameters.suffix);
        minMZRange = (Float) parameters.getParameterValue(ZoomScanFilterParameters.minMZRange);
        removeOriginal = (Boolean) parameters.getParameterValue(ZoomScanFilterParameters.autoRemove);
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
     * @see java.lang.Runnable#run()
     */
    public void run() {

        status = TaskStatus.PROCESSING;

        try {

            // Create new temporary file
            String newName = dataFile.getFileName() + " " + suffix;
            RawDataFileWriter rawDataFileWriter = MZmineCore.getIOController().createNewFile(
                    newName, suffix, dataFile.getPreloadLevel());

            // Get all scans
            int[] scanNumbers = dataFile.getScanNumbers();
            totalScans = scanNumbers.length;

            // Loop through all scans
            for (int scani = 0; scani < totalScans; scani++) {

                if (status == TaskStatus.CANCELED)
                    return;

                // Get next scan
                Scan oldScan = dataFile.getScan(scanNumbers[scani]);

                // Leave all scans of MS levels >1 and appropriate m/z range
                if ((oldScan.getMSLevel() != 1)
                        || (oldScan.getMZRange().getSize() >= minMZRange)) {
                    rawDataFileWriter.addScan(oldScan);
                }

                filteredScans++;

            }

            // Finalize writing
            RawDataFile filteredRawDataFile = rawDataFileWriter.finishWriting();
            MZmineCore.getCurrentProject().addFile(filteredRawDataFile);

            // Remove the original file if requested
            if (removeOriginal)
                MZmineCore.getCurrentProject().removeFile(dataFile);

            status = TaskStatus.FINISHED;

        } catch (IOException e) {
            status = TaskStatus.ERROR;
            errorMessage = e.toString();
            return;
        }

    }

}
