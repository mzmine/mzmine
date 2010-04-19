/*
 * Copyright 2006-2010 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.rawdata.zoomscan;

import java.io.IOException;
import java.util.logging.Logger;

import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.RawDataFileWriter;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskStatus;

/**
 * 
 */
class ZoomScanFilterTask implements Task {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private RawDataFile dataFile, filteredRawDataFile;

    private TaskStatus status = TaskStatus.WAITING;
    private String errorMessage;

    // scan counter
    private int filteredScans, totalScans;

    // parameter values
    private String suffix;
    private double minMZRange;
    private boolean removeOriginal;

    /**
     * @param rawDataFile
     * @param parameters
     */
    ZoomScanFilterTask(RawDataFile dataFile, ZoomScanFilterParameters parameters) {
        this.dataFile = dataFile;
        suffix = (String) parameters.getParameterValue(ZoomScanFilterParameters.suffix);
        minMZRange = (Double) parameters.getParameterValue(ZoomScanFilterParameters.minMZRange);
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
        logger.info("Running zoom scan filter on " + dataFile);
        
        try {

            // Create new temporary file
            String newName = dataFile.getName() + " " + suffix;
            RawDataFileWriter rawDataFileWriter = MZmineCore.createNewFile(newName);

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
			filteredRawDataFile = rawDataFileWriter.finishWriting();
            MZmineCore.getCurrentProject().addFile(filteredRawDataFile);

            // Remove the original file if requested
            if (removeOriginal)
                MZmineCore.getCurrentProject().removeFile(dataFile);

            status = TaskStatus.FINISHED;
            logger.info("Finished zoom scan filter on " + dataFile);

        } catch (IOException e) {
            status = TaskStatus.ERROR;
            errorMessage = e.toString();
            return;
        }

    }

	public Object[] getCreatedObjects() {
		return new Object[] { filteredRawDataFile };
	}
	
}
