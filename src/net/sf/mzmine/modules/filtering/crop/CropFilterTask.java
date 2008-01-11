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

package net.sf.mzmine.modules.filtering.crop;

import java.io.IOException;
import java.util.Arrays;

import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimpleScan;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.io.RawDataFileWriter;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.taskcontrol.Task;

/**
 * 
 */
class CropFilterTask implements Task {

    private RawDataFile dataFile;

    private TaskStatus status = TaskStatus.WAITING;
    private String errorMessage;

    // scan counter
    private int filteredScans, totalScans;

    // parameter values
    private String suffix;
    private float minMZ, maxMZ, minRT, maxRT;
    private boolean removeOriginal;

    /**
     * @param rawDataFile
     * @param parameters
     */
    CropFilterTask(RawDataFile dataFile, CropFilterParameters parameters) {
        this.dataFile = dataFile;
        suffix = (String) parameters.getParameterValue(CropFilterParameters.suffix);
        minMZ = (Float) parameters.getParameterValue(CropFilterParameters.minMZ);
        minRT = (Float) parameters.getParameterValue(CropFilterParameters.minRT);
        maxMZ = (Float) parameters.getParameterValue(CropFilterParameters.maxMZ);
        maxRT = (Float) parameters.getParameterValue(CropFilterParameters.maxRT);
        removeOriginal = (Boolean) parameters.getParameterValue(CropFilterParameters.autoRemove);
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return "Crop filtering " + dataFile;
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
            String newName = dataFile.getFileName() + " " +  suffix;            
            RawDataFileWriter rawDataFileWriter = 
            	MZmineCore.getIOController().createNewFile(
            		newName,suffix,dataFile.getPreloadLevel());

            // Get all scans
            int[] scanNumbers = dataFile.getScanNumbers();
            totalScans = scanNumbers.length;

            // Loop through all scans
            for (int scanIndex = 0; scanIndex < totalScans; scanIndex++) {

                // Check if we are not canceled
                if (status == TaskStatus.CANCELED)
                    return;

                // Get scan
                Scan oldScan = dataFile.getScan(scanNumbers[scanIndex]);

                // Is this scan within the RT range?
                if ((oldScan.getRetentionTime() >= minRT)
                        && (oldScan.getRetentionTime() <= maxRT)) {

                    // Check if whole m/z range is within cropping region or
                    // scan is a fragmentation scan. In such case we copy the
                    // scan unmodified.
                    if ((oldScan.getMSLevel() > 1)
                            || ((oldScan.getMZRangeMin() >= minMZ) && (oldScan.getMZRangeMax() <= maxMZ))) {
                        rawDataFileWriter.addScan(oldScan);
                        filteredScans++;
                        continue;
                    }

                    // Pickup datapoints inside the m/z range
                    float originalMassValues[] = oldScan.getMZValues();
                    float originalIntensityValues[] = oldScan.getIntensityValues();

                    // Find minimum index within m/z range
                    int minIndex = Arrays.binarySearch(originalMassValues,
                            minMZ);
                    if (minIndex < 0)
                        minIndex = (minIndex * -1) - 1;

                    // Find maximum index within m/z range
                    int maxIndex = Arrays.binarySearch(originalMassValues,
                            maxMZ);
                    if (maxIndex < 0)
                        maxIndex = (maxIndex * -1) - 2;

                    // Skip this scan if there are no m/z values in range
                    if (maxIndex < minIndex)
                        continue;

                    // Create cropped m/z and intensity arrays
                    float newMassValues[] = new float[maxIndex - minIndex + 1];
                    float newIntValues[] = new float[maxIndex - minIndex + 1];

                    // Fill cropped m/z and intensity arrays
                    for (int ind = minIndex; ind <= maxIndex; ind++) {
                        newMassValues[ind - minIndex] = originalMassValues[ind];
                        newIntValues[ind - minIndex] = originalIntensityValues[ind];
                    }

                    // Create updated scan
                    SimpleScan newScan = new SimpleScan(oldScan);
                    newScan.setData(newMassValues, newIntValues);

                    // Write the updated scan to new file
                    rawDataFileWriter.addScan(newScan);

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
