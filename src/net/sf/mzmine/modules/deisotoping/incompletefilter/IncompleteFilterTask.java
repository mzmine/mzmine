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

package net.sf.mzmine.modules.deisotoping.incompletefilter;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.taskcontrol.Task;

/**
 * 
 */
class IncompleteFilterTask implements Task {

    private OpenedRawDataFile dataFile;

    private TaskStatus status;
    private String errorMessage;

    private int processedPeaks;
    private int totalPeaks;

    private PeakList currentPeakList;
    private SimplePeakList processedPeakList;

    private ParameterSet parameters;
    private int minimumNumberOfPeaks;

    /**
     * @param rawDataFile
     * @param parameters
     */
    IncompleteFilterTask(OpenedRawDataFile dataFile, PeakList currentPeakList,
            ParameterSet parameters) {
        status = TaskStatus.WAITING;
        this.dataFile = dataFile;

        this.currentPeakList = currentPeakList;

        processedPeakList = new SimplePeakList();

        this.parameters = parameters;
        minimumNumberOfPeaks = (Integer) parameters.getParameterValue(IncompleteFilter.minimumNumberOfPeaks);

    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return "Simple incomplete isotopic pattern filter on " + dataFile;
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
        Object[] results = new Object[3];
        results[0] = dataFile;
        results[1] = processedPeakList;
        results[2] = parameters;
        return results;
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

        // TODO: Implement this method!

        status = TaskStatus.FINISHED;

    }

}
