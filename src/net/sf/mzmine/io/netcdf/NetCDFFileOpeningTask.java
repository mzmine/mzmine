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

package net.sf.mzmine.io.netcdf;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.mzmine.data.Scan;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.io.IOController.PreloadLevel;
import net.sf.mzmine.io.impl.OpenedRawDataFileImpl;
import net.sf.mzmine.taskcontrol.DistributableTask;

/**
 * 
 */
public class NetCDFFileOpeningTask implements DistributableTask {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private File originalFile;
    private TaskStatus status;
    private String errorMessage;

    private int parsedScans;
    private int totalScans;

    private NetCDFFile buildingFile;
    private OpenedRawDataFile newMZmineFile;
    private Scan buildingScan;

    /**
     * 
     */
    public NetCDFFileOpeningTask(File fileToOpen, PreloadLevel preloadLevel) {
        originalFile = fileToOpen;
        status = TaskStatus.WAITING;

        buildingFile = new NetCDFFile(fileToOpen, preloadLevel);
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return "Opening file " + originalFile;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public float getFinishedPercentage() {
        return totalScans == 0 ? 0 : (float) parsedScans / totalScans;
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
    public OpenedRawDataFile getResult() {
        return newMZmineFile;
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {

        // Update task status
        status = TaskStatus.PROCESSING;
        logger.info("Started parsing file " + originalFile);

        try {

            // Initialize parser
            NetCDFFileParser cdfParser = new NetCDFFileParser(originalFile);
            buildingFile.addParser(cdfParser);

            // Open netCDF file and read general information
            cdfParser.openFile();
            cdfParser.readGeneralInformation();

            // Parse scans
            totalScans = cdfParser.getTotalScans();
            for (int i = 0; i < totalScans; i++) {
                buildingScan = cdfParser.parseScan(i);
                buildingFile.addScan(buildingScan);
                parsedScans++;

                // Check if cancel is requested
                if (status == TaskStatus.CANCELED) {
                    // Close netCDF file
                    cdfParser.closeFile();
                    return;
                }
            }

            // Close netCDF file
            cdfParser.closeFile();

            newMZmineFile = new OpenedRawDataFileImpl(buildingFile,
                    buildingFile.getDataDescription());

        } catch (Throwable e) {
            logger.log(Level.SEVERE, "Could not open file "
                    + originalFile.getPath(), e);
            errorMessage = e.toString();
            status = TaskStatus.ERROR;
            return;
        }

        logger.info("Finished parsing " + originalFile + ", parsed "
                + parsedScans + " scans");
        
        // Update task status
        status = TaskStatus.FINISHED;

    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#cancel()
     */
    public void cancel() {
        logger.info("Cancelling opening of NETCDF file " + originalFile);
        status = TaskStatus.CANCELED;
    }

}
