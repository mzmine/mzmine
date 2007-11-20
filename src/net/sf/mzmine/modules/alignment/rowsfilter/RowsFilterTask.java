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

package net.sf.mzmine.modules.alignment.rowsfilter;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.taskcontrol.Task;

class RowsFilterTask implements Task {

    private PeakList originalPeakList;
    private SimplePeakList processedPeakList;
    private TaskStatus status;
    private String errorMessage;

    private float processedRows;
    private float totalRows;

    private int minPresent;
    private String newName;
    private float minMZ, maxMZ, minRT, maxRT;
    private boolean identified;

    public RowsFilterTask(PeakList peakList,
            SimpleParameterSet parameters) {
        
        status = TaskStatus.WAITING;
        
        originalPeakList = peakList;
        minPresent = (Integer) parameters.getParameterValue(RowsFilter.minPeaksParam);
        newName = (String) parameters.getParameterValue(RowsFilter.nameParam);
        minMZ = (Float) parameters.getParameterValue(RowsFilter.minMZParam);
        maxMZ = (Float) parameters.getParameterValue(RowsFilter.maxMZParam);
        minRT = (Float) parameters.getParameterValue(RowsFilter.minRTParam);
        maxRT = (Float) parameters.getParameterValue(RowsFilter.maxRTParam);
        identified = (Boolean) parameters.getParameterValue(RowsFilter.identifiedParam);

    }

    public void cancel() {
        status = TaskStatus.CANCELED;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public float getFinishedPercentage() {
        return processedRows / totalRows;
    }

    public Object getResult() {
        return processedPeakList;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public String getTaskDescription() {
        return "Filtering peak list rows";
    }

    public void run() {

        status = TaskStatus.PROCESSING;

        totalRows = originalPeakList.getNumberOfRows();
        processedRows = 0;

        // Create new alignment result and add opened raw data files to it
        processedPeakList = new SimplePeakList(newName);

        for (RawDataFile rawData : originalPeakList.getRawDataFiles()) {
            processedPeakList.addRawDataFile(rawData);
        }

        // Copy rows with enough peaks to new alignment result
        for (PeakListRow row : originalPeakList.getRows()) {

            if (status == TaskStatus.CANCELED)
                return;

            boolean rowIsGood = true;

            if (row.getNumberOfPeaks() < minPresent)
                rowIsGood = false;
            if ((identified) && (row.getPreferredCompoundIdentity() == null))
                rowIsGood = false;
            if ((row.getAverageMZ() > maxMZ) || (row.getAverageMZ() < minMZ))
                rowIsGood = false;
            if ((row.getAverageRT() > maxRT) || (row.getAverageRT() < minRT))
                rowIsGood = false;

            if (rowIsGood)
                processedPeakList.addRow(row);

            processedRows++;

        }

        status = TaskStatus.FINISHED;

    }

}
