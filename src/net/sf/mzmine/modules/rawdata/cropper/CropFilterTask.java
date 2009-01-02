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

package net.sf.mzmine.modules.rawdata.cropper;

import java.io.IOException;

import net.sf.mzmine.data.MzDataPoint;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.RawDataFileWriter;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimpleScan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.Range;

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
    private Range mzRange, rtRange;
    private boolean removeOriginal;

    /**
     * @param rawDataFile
     * @param parameters
     */
    CropFilterTask(RawDataFile dataFile, CropFilterParameters parameters) {
        this.dataFile = dataFile;
        suffix = (String) parameters.getParameterValue(CropFilterParameters.suffix);
        mzRange = (Range) parameters.getParameterValue(CropFilterParameters.mzRange);
        rtRange = (Range) parameters.getParameterValue(CropFilterParameters.retentionTimeRange);
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
    public double getFinishedPercentage() {
        if (totalScans == 0)
            return 0.0f;
        return (double) filteredScans / totalScans;
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
            RawDataFileWriter rawDataFileWriter = MZmineCore.createNewFile(newName);

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
                if (rtRange.contains(oldScan.getRetentionTime())) {

                    // Check if whole m/z range is within cropping region or
                    // scan is a fragmentation scan. In such case we copy the
                    // scan unmodified.
                    if ((oldScan.getMSLevel() > 1)
                            || (oldScan.getMZRange().containsRange(mzRange))) {
                        rawDataFileWriter.addScan(oldScan);
                        filteredScans++;
                        continue;
                    }

                    // Pickup datapoints inside the m/z range
                    MzDataPoint croppedDataPoints[] = oldScan.getDataPointsByMass(mzRange);

                    // Create updated scan
                    SimpleScan newScan = new SimpleScan(oldScan);
                    newScan.setDataPoints(croppedDataPoints);

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
