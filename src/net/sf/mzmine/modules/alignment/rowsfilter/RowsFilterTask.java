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

package net.sf.mzmine.modules.alignment.rowsfilter;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.Task;

class RowsFilterTask implements Task {

    private PeakList peakList;

    private TaskStatus status = TaskStatus.WAITING;
    private String errorMessage;

    // Processed rows counter
    private int processedRows, totalRows;

    // Method parameters
    private int minPresent;
    private String newName;
    private float minMZ, maxMZ, minRT, maxRT;
    private boolean identified, removeOriginal;

    public RowsFilterTask(PeakList peakList, SimpleParameterSet parameters) {

        this.peakList = peakList;

        newName = (String) parameters.getParameterValue(RowsFilterParameters.peakListName);
        minPresent = (Integer) parameters.getParameterValue(RowsFilterParameters.minPeaks);
        minMZ = (Float) parameters.getParameterValue(RowsFilterParameters.minMZ);
        maxMZ = (Float) parameters.getParameterValue(RowsFilterParameters.maxMZ);
        minRT = (Float) parameters.getParameterValue(RowsFilterParameters.minRT);
        maxRT = (Float) parameters.getParameterValue(RowsFilterParameters.maxRT);
        identified = (Boolean) parameters.getParameterValue(RowsFilterParameters.identified);
        removeOriginal = (Boolean) parameters.getParameterValue(RowsFilterParameters.autoRemove);

    }

    public void cancel() {
        status = TaskStatus.CANCELED;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public float getFinishedPercentage() {
        if (totalRows == 0)
            return 0.0f;
        return (float) processedRows / (float) totalRows;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public String getTaskDescription() {
        return "Filtering peak list rows";
    }

    public void run() {

        status = TaskStatus.PROCESSING;

        totalRows = peakList.getNumberOfRows();
        processedRows = 0;

        // Create new alignment result and add opened raw data files to it
        SimplePeakList filteredPeakList = new SimplePeakList(newName);

        for (RawDataFile rawData : peakList.getRawDataFiles()) {
            filteredPeakList.addRawDataFile(rawData);
        }

        // Copy rows with enough peaks to new alignment result
        for (PeakListRow row : peakList.getRows()) {

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
                filteredPeakList.addRow(row);

            processedRows++;

        }

        // Add new peaklist to the project
        MZmineProject currentProject = MZmineCore.getCurrentProject();
        currentProject.addPeakList(filteredPeakList);

        // Remove the original peaklist if requested
        if (removeOriginal)
            MZmineCore.getCurrentProject().removePeakList(peakList);

        status = TaskStatus.FINISHED;

    }

}
