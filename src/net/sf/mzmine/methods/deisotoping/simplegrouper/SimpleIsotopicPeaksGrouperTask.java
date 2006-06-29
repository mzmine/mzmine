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

package net.sf.mzmine.methods.deisotoping.simplegrouper;

import java.io.IOException;
import java.util.Vector;
import java.util.TreeSet;
import java.util.Hashtable;
import java.util.Iterator;


import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.interfaces.Scan;
import net.sf.mzmine.interfaces.PeakList;
import net.sf.mzmine.methods.peakpicking.SimplePeakList;
import net.sf.mzmine.methods.peakpicking.SimplePeak;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.util.MyMath;


/**
 *
 */
public class SimpleIsotopicPeaksGrouperTask implements Task {

    private RawDataFile rawDataFile;
    private SimpleIsotopicPeaksGrouperParameters parameters;
    private TaskStatus status;
    private String errorMessage;

    private int processedPeaks;
    private int totalPeaks;

	private PeakList processedPeakList;

    /**
     * @param rawDataFile
     * @param parameters
     */
    SimpleIsotopicPeaksGrouperTask(RawDataFile rawDataFile, SimpleIsotopicPeaksGrouperParameters parameters) {
        status = TaskStatus.WAITING;
        this.rawDataFile = rawDataFile;
        this.parameters = parameters;

        processedPeakList = new SimplePeakList();

    }


    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return "Simple isotopic peaks grouper on " + rawDataFile;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public float getFinishedPercentage() {
		if (totalPeaks == 0) return 0.0f;
        return (float) processedPeaks / (float)totalPeaks;
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
		results[0] = rawDataFile;
		results[1] = processedPeakList;
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
     * @see net.sf.mzmine.taskcontrol.Task#getPriority()
     */
    public TaskPriority getPriority() {
        return TaskPriority.NORMAL;
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {

		status = TaskStatus.PROCESSING;


		status = TaskStatus.FINISHED;

    }



}
