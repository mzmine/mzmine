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

package net.sf.mzmine.modules.peaklistmethods.filtering.duplicatefilter;

import java.util.Arrays;
import java.util.logging.Logger;

import net.sf.mzmine.data.PeakIdentity;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListAppliedMethod;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.data.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.PeakListRowSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

/**
 * 
 */
class DuplicateFilterTask implements Task {

    private Logger logger = Logger.getLogger(this.getClass().getName());

	private PeakList peakList, filteredPeakList;

    private TaskStatus status = TaskStatus.WAITING;
    private String errorMessage;

    // peaks counter
    private int processedRows, totalRows;

    // parameter values
    private String suffix;
    private double mzDifferenceMax, rtDifferenceMax;
    private boolean requireSameIdentification, removeOriginal;
    private DuplicateFilterParameters parameters;

    /**
     * @param rawDataFile
     * @param parameters
     */
    DuplicateFilterTask(PeakList peakList, DuplicateFilterParameters parameters) {

        this.peakList = peakList;
        this.parameters = parameters;

        // Get parameter values for easier use
        suffix = (String) parameters.getParameterValue(DuplicateFilterParameters.suffix);
        mzDifferenceMax = (Double) parameters.getParameterValue(DuplicateFilterParameters.mzDifferenceMax);
        rtDifferenceMax = (Double) parameters.getParameterValue(DuplicateFilterParameters.rtDifferenceMax);
        requireSameIdentification = (Boolean) parameters.getParameterValue(DuplicateFilterParameters.requireSameIdentification);
        removeOriginal = (Boolean) parameters.getParameterValue(DuplicateFilterParameters.autoRemove);

    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return "Duplicate peak filter on " + peakList;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public double getFinishedPercentage() {
        if (totalRows == 0)
            return 0.0f;
        return (double) processedRows / (double) totalRows;
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

    public PeakList getPeakList() {
        return peakList;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#cancel()
     */
    public void cancel() {
        status = TaskStatus.CANCELED;
    }

    /**
     * @see Runnable#run()
     */
    public void run() {

        status = TaskStatus.PROCESSING;
        logger.info("Running duplicate peaka filter on " + peakList);

        filteredPeakList = new SimplePeakList(peakList + " "
				+ suffix, peakList.getRawDataFiles());
       
        PeakListRow[] peakListRows = peakList.getRows();
        
        totalRows = peakListRows.length;
        
        Arrays.sort(peakListRows, new PeakListRowSorter(SortingProperty.Area,
				SortingDirection.Descending));

        // Loop through all peak list rows
		for (int firstRowIndex = 0; firstRowIndex < peakListRows.length; firstRowIndex++) {
        	        	       
        	if (peakListRows[firstRowIndex] == null) {
                processedRows++;
                continue;
            }
        	        	
        	// Search for duplicate rows with smaller peak area
			for (int secondRowIndex = (firstRowIndex + 1); secondRowIndex < peakListRows.length; secondRowIndex++) {

        		if (status == TaskStatus.CANCELED) return;
        		
        		if (peakListRows[secondRowIndex] == null)
					continue;
        		
        		// Compare identifications
        		boolean sameID = true;
        		if (requireSameIdentification) {
        			// TODO
        			// Use preferred identifications if available instead of all identifications
        			// Use CompoundIDs if available instead of CompoundNames
        			
       				PeakIdentity[] firstIdentities = peakListRows[firstRowIndex]
							.getPeakIdentities();
					PeakIdentity[] secondIdentities = peakListRows[secondRowIndex]
							.getPeakIdentities();
       				
       				if ( (firstIdentities.length==0) && (secondIdentities.length==0) )
       					sameID = true;
       				
       				for (PeakIdentity firstIdentity : firstIdentities) {
       					sameID = false;
       					for (PeakIdentity secondIdentity : secondIdentities) {
       						if (firstIdentity.getName().equals(secondIdentity.getName())) {
       							sameID = true;
       							break;
       						}
       					}
       					if (!sameID)
       						break;
       				}
       				
        		}
      				
        		// Compare m/z
        		boolean sameMZ = false;
        		double firstMZ = peakListRows[firstRowIndex].getAverageMZ();
				double secondMZ = peakListRows[secondRowIndex].getAverageMZ();
        		if (Math.abs(firstMZ-secondMZ) < mzDifferenceMax) {
        			sameMZ = true;
        		}
        		
        		// Compare rt
        		boolean sameRT = false;
        		double firstRT = peakListRows[firstRowIndex].getAverageRT();
				double secondRT = peakListRows[secondRowIndex].getAverageRT();
        		if (Math.abs(firstRT-secondRT) < rtDifferenceMax) {
        			sameRT = true;
        		}
        		
        		if (sameID && sameMZ && sameRT) {
        			// System.out.println("Duplicate for ID " +
					// peakListRows[firstRowIndex].getID() + " is ID " +
					// peakListRows[secondRowIndex].getID());
					peakListRows[secondRowIndex] = null;
        		}

        		
        	}
        	
        	processedRows++;
        	
        }
        
        // Add all remaining rows to a new peak list
		for (PeakListRow peakListRow : peakListRows) {
			if (peakListRow != null)
				filteredPeakList.addRow(peakListRow);
        }


        // Add new peakList to the project
        MZmineProject currentProject = MZmineCore.getCurrentProject();
        currentProject.addPeakList(filteredPeakList);
        
		// Load previous applied methods
		for (PeakListAppliedMethod proc : peakList.getAppliedMethods()) {
			filteredPeakList.addDescriptionOfAppliedTask(proc);
		}
        
        // Add task description to peakList
        filteredPeakList.addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod("Duplicate peak filter", parameters));


        // Remove the original peakList if requested
        if (removeOriginal)
            currentProject.removePeakList(peakList);

        logger.info("Finished duplicate peak filter on " + peakList);
        status = TaskStatus.FINISHED;

    }

	public Object[] getCreatedObjects() {
		return new Object[] { filteredPeakList };
	}

}
