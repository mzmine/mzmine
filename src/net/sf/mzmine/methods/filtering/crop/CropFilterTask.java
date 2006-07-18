/*
 * Copyright 2006 The MZmine Development Team
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

package net.sf.mzmine.methods.filtering.crop;

import java.io.IOException;

import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimpleScan;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.io.RawDataFileWriter;
import net.sf.mzmine.taskcontrol.Task;

/**
 * 
 */
class CropFilterTask implements Task {

    private OpenedRawDataFile dataFile;
    private RawDataFile rawDataFile;
    private CropFilterParameters parameters;
    private TaskStatus status;
    private String errorMessage;

    private int filteredScans;
    private int totalScans;

    private RawDataFile filteredRawDataFile;

    /**
     * @param rawDataFile
     * @param parameters
     */
    CropFilterTask(OpenedRawDataFile dataFile, CropFilterParameters parameters) {
        status = TaskStatus.WAITING;
        this.dataFile = dataFile;
        this.rawDataFile = dataFile.getCurrentFile();
        this.parameters = parameters;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return "Crop filtering " + rawDataFile;
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
        Object[] results = new Object[3];
        results[0] = dataFile;
        results[1] = filteredRawDataFile;
        results[2] = parameters;

        return results;
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

        Scan sc;

        // Get all MS^1 scans
        int[] scanNumbers = rawDataFile.getScanNumbers(1);
        totalScans = scanNumbers.length;

        // Loop through all scans
        for (int scani = 0; scani < totalScans; scani++) {

            // Get scan
            try {
                sc = rawDataFile.getScan(scanNumbers[scani]);
            } catch (IOException e) {
                status = TaskStatus.ERROR;
                errorMessage = e.toString();
                try {
                    filteredRawDataFile = rawDataFileWriter.finishWriting();
                } catch (IOException e2) {
                }
                return;
            }

            // Is this scan within the RT range?
            if ((sc.getRetentionTime() >= parameters.minRT)
                    && (sc.getRetentionTime() <= parameters.maxRT)) {

                // Pickup datapoints inside the M/Z range
                double originalMassValues[] = sc.getMZValues();
                double originalIntensityValues[] = sc.getIntensityValues();

                int numSmallerThanMin = 0;
                for (int ind = 0; ind < originalMassValues.length; ind++) {
                    if (originalMassValues[ind] >= parameters.minMZ) {
                        break;
                    }
                    numSmallerThanMin++;
                }

                int numBiggerThanMax = 0;
                for (int ind = (originalMassValues.length - 1); ind >= 0; ind--) {
                    if (originalMassValues[ind] <= parameters.maxMZ) {
                        break;
                    }
                    numBiggerThanMax++;
                }

                double newMassValues[] = new double[originalMassValues.length
                        - numSmallerThanMin - numBiggerThanMax];
                double newIntensityValues[] = new double[originalMassValues.length
                        - numSmallerThanMin - numBiggerThanMax];

                int newInd = 0;
                for (int ind = numSmallerThanMin; ind < (originalMassValues.length - numBiggerThanMax); ind++) {
                    newMassValues[newInd] = originalMassValues[ind];
                    newIntensityValues[newInd] = originalIntensityValues[ind];
                    newInd++;
                }

                // Write the modified scan to file
                try {

                    SimpleScan newScan = new SimpleScan(sc);
                    newScan.setData(newMassValues, newIntensityValues);
                    rawDataFileWriter.addScan(newScan);

                } catch (IOException e) {
                    status = TaskStatus.ERROR;
                    errorMessage = e.toString();
                    try {
                        filteredRawDataFile = rawDataFileWriter.finishWriting();
                    } catch (IOException e2) {
                    }
                    return;
                }

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
