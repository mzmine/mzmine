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

package net.sf.mzmine.io.util;

import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.mzmine.data.Scan;
import net.sf.mzmine.io.RawDataAcceptor;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.taskcontrol.Task;

/**
 * Utility task to retrieve selected scans from raw data file.
 */
public class RawDataRetrievalTask implements Task {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private RawDataFile rawDataFile;
    private int scanNumbers[];
    private int retrievedScans;
    private RawDataAcceptor acceptor;
    private TaskStatus status;
    private String errorMessage, taskDescription;

    public RawDataRetrievalTask(RawDataFile rawDataFile, int scanNumbers[],
            String taskDescription, RawDataAcceptor acceptor) {
        status = TaskStatus.WAITING;
        this.rawDataFile = rawDataFile;
        this.taskDescription = taskDescription;
        this.acceptor = acceptor;
        this.scanNumbers = scanNumbers;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return taskDescription;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public float getFinishedPercentage() {
        return (float) retrievedScans / scanNumbers.length;
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
        // this task has no result
        return null;
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

        logger.finest("Starting new raw data retrieval task");

        status = TaskStatus.PROCESSING;
        Scan scan;

        for (int i = 0; i < scanNumbers.length; i++) {

            if (status == TaskStatus.CANCELED)
                return;

            try {

                scan = rawDataFile.getScan(scanNumbers[i]);
                acceptor.addScan(scan, i, scanNumbers.length);

            } catch (Throwable e) {

                logger.log(Level.WARNING,
                        "Raw data retrieval task caught an exception", e);
                status = TaskStatus.ERROR;
                errorMessage = e.toString();
                return;
            }

            retrievedScans++;

        }

        status = TaskStatus.FINISHED;

        logger.finest("Raw data retrieval task finished");

    }

}
