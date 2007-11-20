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

package net.sf.mzmine.modules.alignment.gapfiller;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.taskcontrol.Task;

class SimpleGapFillerTask implements Task {

    private RawDataFile rawDataFile;

    private EmptyGap[] emptyGaps;

    private TaskStatus status;
    private String errorMessage;

    private ParameterSet parameters;
    private int processedScans;;
    private int totalScans;

    public SimpleGapFillerTask(RawDataFile rawDataFile,
            EmptyGap[] emptyGaps, ParameterSet parameters) {

        status = TaskStatus.WAITING;

        this.rawDataFile = rawDataFile;
        this.emptyGaps = emptyGaps;

        this.parameters = parameters;

    }

    public void run() {

        status = TaskStatus.PROCESSING;

        int[] scanNumbers = rawDataFile.getScanNumbers(1);
        totalScans = scanNumbers.length;

        for (int scanNumber : scanNumbers) {

            if (status == TaskStatus.CANCELED)
                return;

            // Get next scan
            Scan s = rawDataFile.getScan(scanNumber);

            // Feed this scan to all empty gaps
            for (EmptyGap emptyGap : emptyGaps) {
                emptyGap.offerNextScan(s);
            }

            processedScans++;
        }

        status = TaskStatus.FINISHED;

    }

    public void cancel() {
        status = TaskStatus.CANCELED;

    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public float getFinishedPercentage() {
        if (totalScans == 0)
            return 0;
        return (float) processedScans / (float) totalScans;

    }

    public Object getResult() {
        Object[] result = new Object[3];
        result[0] = rawDataFile;
        result[1] = emptyGaps;
        result[2] = parameters;
        return result;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public String getTaskDescription() {
        return "Simple gap filler " + rawDataFile.toString();
    }

}
