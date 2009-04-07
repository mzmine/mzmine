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

package net.sf.mzmine.modules.peaklist.rowsfilter;

import java.util.logging.Logger;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListAppliedMethod;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.data.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.main.mzmineclient.MZmineCore;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskStatus;

class RowsFilterTask implements Task {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private PeakList peakList;

    private TaskStatus status = TaskStatus.WAITING;
    private String errorMessage;

    // Processed rows counter
    private int processedRows, totalRows;

    // Method parameters
    private int minPresent, minIsotopePatternSize;
    private String suffix;
    private double minMZ, maxMZ, minRT, maxRT;
    private boolean identified, removeOriginal;
    private RowsFilterParameters parameters;

    public RowsFilterTask(PeakList peakList, RowsFilterParameters parameters) {

        this.peakList = peakList;
        this.parameters = parameters;

        suffix = (String) parameters.getParameterValue(RowsFilterParameters.suffix);
        minPresent = (Integer) parameters.getParameterValue(RowsFilterParameters.minPeaks);
        minIsotopePatternSize = (Integer) parameters.getParameterValue(RowsFilterParameters.minIsotopePatternSize);
        minMZ = (Double) parameters.getParameterValue(RowsFilterParameters.minMZ);
        maxMZ = (Double) parameters.getParameterValue(RowsFilterParameters.maxMZ);
        minRT = (Double) parameters.getParameterValue(RowsFilterParameters.minRT);
        maxRT = (Double) parameters.getParameterValue(RowsFilterParameters.maxRT);
        identified = (Boolean) parameters.getParameterValue(RowsFilterParameters.identified);
        removeOriginal = (Boolean) parameters.getParameterValue(RowsFilterParameters.autoRemove);

    }

    public void cancel() {
        status = TaskStatus.CANCELED;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public double getFinishedPercentage() {
        if (totalRows == 0)
            return 0.0f;
        return (double) processedRows / (double) totalRows;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public String getTaskDescription() {
        return "Filtering peak list rows";
    }

    public void run() {

        status = TaskStatus.PROCESSING;
        logger.info("Running peak list rows filter");

        totalRows = peakList.getNumberOfRows();
        processedRows = 0;

        // Create new peaklist
        SimplePeakList filteredPeakList = new SimplePeakList(
                peakList.toString() + " " + suffix, peakList.getRawDataFiles());

        // Copy rows with enough peaks to new alignment result
        for (PeakListRow row : peakList.getRows()) {

            if (status == TaskStatus.CANCELED)
                return;

            boolean rowIsGood = true;

            if (row.getNumberOfPeaks() < minPresent)
                rowIsGood = false;
            if ((identified) && (row.getPreferredPeakIdentity() == null))
                rowIsGood = false;
            if ((row.getAverageMZ() > maxMZ) || (row.getAverageMZ() < minMZ))
                rowIsGood = false;
            if ((row.getAverageRT() > maxRT) || (row.getAverageRT() < minRT))
                rowIsGood = false;

            int maxIsotopePatternSizeOnRow = 1;
            for (ChromatographicPeak p : row.getPeaks()) {
            	if (p instanceof IsotopePattern) {
            		IsotopePattern i = (IsotopePattern)p;
            		ChromatographicPeak[] originalPeaks = i.getOriginalPeaks();
            		if (originalPeaks != null) {
            			if (maxIsotopePatternSizeOnRow < originalPeaks.length)
            				maxIsotopePatternSizeOnRow = originalPeaks.length;
            		}
            	}
            }
            if (maxIsotopePatternSizeOnRow < minIsotopePatternSize)
            	rowIsGood = false;
            
            if (rowIsGood)
                filteredPeakList.addRow(row);

            processedRows++;

        }

        // Add new peaklist to the project
        MZmineProject currentProject = MZmineCore.getCurrentProject();
        currentProject.addPeakList(filteredPeakList);
        
		// Load previous applied methods
		for (PeakListAppliedMethod proc: peakList.getAppliedMethods()){
			filteredPeakList.addDescriptionOfAppliedTask(proc);
		}
        
        // Add task description to peakList
        filteredPeakList.addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod(getTaskDescription(), parameters));


        // Remove the original peaklist if requested
        if (removeOriginal)
            currentProject.removePeakList(peakList);

        logger.info("Finished peak list rows filter");
        status = TaskStatus.FINISHED;

    }

}
