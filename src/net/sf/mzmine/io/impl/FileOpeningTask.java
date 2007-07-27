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

package net.sf.mzmine.io.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.mzmine.data.Scan;
import net.sf.mzmine.io.IOController;
import net.sf.mzmine.io.PreloadLevel;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.io.RawDataFileReader;
import net.sf.mzmine.io.RawDataFileWriter;
import net.sf.mzmine.io.readers.MzDatav1_0_5Reader;
import net.sf.mzmine.io.readers.MzXMLv1_1_1Reader;
import net.sf.mzmine.io.readers.MzXMLv2_0Reader;
import net.sf.mzmine.io.readers.MzXMLv2_1Reader;
import net.sf.mzmine.io.readers.NetCDFFileReader;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.taskcontrol.DistributableTask;

/**
 * 
 */
public class FileOpeningTask implements DistributableTask {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private File originalFile;
    private TaskStatus status;
    private String errorMessage;

    private int parsedScans;

    private RawDataFileWriter buildingFile;
    private RawDataFileReader reader;
    private RawDataFile resultFile;
    private PreloadLevel preloadLevel;

    /**
     * 
     */
    public FileOpeningTask(File fileToOpen, PreloadLevel preloadLevel) {

        originalFile = fileToOpen;
        this.preloadLevel = preloadLevel;
        status = TaskStatus.WAITING;

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
        if (reader == null)
            return 0;
        int totalScans = reader.getNumberOfScans();
        return totalScans <= 0 ? 0 : (float) parsedScans / totalScans;
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
    public RawDataFile getResult() {
        return resultFile;
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {

        // Update task status
        logger.info("Started parsing file " + originalFile);
        status = TaskStatus.PROCESSING;

        try {

            String fileName = originalFile.getName();
            IOController ioController = MZmineCore.getIOController();

            // Create new RawDataFile instance
            resultFile = ioController.createNewFile(fileName, preloadLevel);
            buildingFile = resultFile.updateFile();
                
            // Determine parser
            String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();

            if (extension.endsWith("xml") || extension.endsWith("mzdata")) {

                FileReader fileReader = new FileReader(originalFile);
                BufferedReader lineReader = new BufferedReader(fileReader);

                final String MzXMLv1_1_1Regex = new org.proteomecommons.io.mzxml.v1_1_1.MzXMLPeakListReaderFactory().getSchemaURIRegex();
                final String MzXMLv2_0Regex = new org.proteomecommons.io.mzxml.v2_0.MzXMLPeakListReaderFactory().getSchemaURIRegex();
                final String MzXMLv2_1Regex = new org.proteomecommons.io.mzxml.v2_1.MzXMLPeakListReaderFactory().getSchemaURIRegex();
                final String MzDatav1_0_5Regex = new org.proteomecommons.io.mzdata.v1_05.MzDataPeakListReaderFactory().getSchemaURIRegex();

                // read first 20 lines
                for (int i = 0; i < 20; i++) {

                    String line = lineReader.readLine();
                    if (line == null)
                        break;

                    if (line.matches(MzXMLv1_1_1Regex))
                        reader = new MzXMLv1_1_1Reader(originalFile);

                    if (line.matches(MzXMLv2_0Regex))
                        reader = new MzXMLv2_0Reader(originalFile);

                    if (line.matches(MzXMLv2_1Regex))
                        reader = new MzXMLv2_1Reader(originalFile);

                    if (line.matches(MzDatav1_0_5Regex))
                        reader = new MzDatav1_0_5Reader(originalFile);

                }

            }

            if (extension.endsWith("cdf")) {
                reader = new NetCDFFileReader(originalFile);
            }

            if (reader == null) {
                throw (new IOException("Cannot determine file type of file "
                        + originalFile));
            }

            // Open file
            reader.startReading();

            // Parse scans
            Scan scan;
            while ((scan = reader.readNextScan()) != null) {

                // Check if cancel is requested
                if (status == TaskStatus.CANCELED) {
                    return;
                }

                buildingFile.addScan(scan);
                parsedScans++;

            }

            // Close file
            reader.finishReading();
            buildingFile.finishWriting();

        } catch (Throwable e) {
            logger.log(Level.SEVERE, "Could not open file "
                    + originalFile.getPath(), e);
            errorMessage = e.toString();
            status = TaskStatus.ERROR;
            return;
        }

        logger.info("Finished parsing " + originalFile + ", parsed "
                + parsedScans + " scans");

        status = TaskStatus.FINISHED;

    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#cancel()
     */
    public void cancel() {
        logger.info("Cancelling opening of file " + originalFile);
        status = TaskStatus.CANCELED;
    }

}
