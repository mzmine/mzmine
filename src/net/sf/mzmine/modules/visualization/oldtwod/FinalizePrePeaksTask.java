package net.sf.mzmine.modules.visualization.oldtwod;

import java.io.IOException;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.data.impl.SimplePeakListRow;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.Task.TaskStatus;

public class FinalizePrePeaksTask implements Task {

	private RawDataFile dataFile;
	
	private PreConstructionPeak[] prePeaks;
	
    private int processedScans;
    private int totalScans;	
	
	private TaskStatus status;
	
	private String errorMessage;
	
	public FinalizePrePeaksTask(RawDataFile dataFile, PreConstructionPeak[] prePeaks) {
		this.dataFile = dataFile;
		this.prePeaks = prePeaks;
		
		status = TaskStatus.WAITING;
	}
	
	
	public void cancel() {
		status = TaskStatus.CANCELED;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public float getFinishedPercentage() {
        if (totalScans == 0)
            return 0.0f;
        return (float) processedScans / (1.0f * totalScans);

	}

	public Object getResult() {
        Object[] results = new Object[3];
        results[0] = dataFile;
        results[1] = prePeaks;
        results[2] = null; //TODO: parameters;
        return results;
	}

	public TaskStatus getStatus() {
		return status;
	}

	public String getTaskDescription() {
        return "Finalize defined pre-peaks of " + dataFile;
	}

	public void run() {
		
		status = TaskStatus.PROCESSING;

		// Loop through scans
        int[] scanNumbers = dataFile.getScanNumbers();
        totalScans = scanNumbers.length;

        for (int i = 0; i < scanNumbers.length; i++) {

            if (status == TaskStatus.CANCELED)
                return;

            Scan s = dataFile.getScan(scanNumbers[i]);
            
            // Offer this scan to all pre-peaks
            for (PreConstructionPeak prePeak : prePeaks) {
            	prePeak.addScan(s, i, totalScans);
            }

            processedScans++;

        }
        

		// TODO: Get user selected peak list for the file 
		/*
		MZmineProject currentProject = MZmineCore.getCurrentProject();
		PeakList peakList = currentProject.getFilePeakList(resultDataFile);
		*/
		PeakList peakList = null;
		
        // If there is no existing peak list, then create a new one
        if (peakList==null) {
			// TODO: Name for the new peak list should be provided by the user as a parameter
        	peakList = new SimplePeakList("new empty peak list");
			// TODO: Add peak list to project
        	// currentProject.setFilePeakList(resultDataFile, peakList);
        }
        
		// Find highest ID of existing peak list rows
        int highestID = Integer.MIN_VALUE;
        for (PeakListRow row : peakList.getRows()) {
        	if (row.getID()>highestID) 
        		highestID = row.getID();
        }
        
		// Append pre-peaks as new rows to the peak list
        highestID++;
        for (PreConstructionPeak prePeak : prePeaks) {
        	SimplePeakListRow newRow = new SimplePeakListRow(highestID);
        	newRow.addPeak(dataFile, prePeak, prePeak);
        	peakList.addRow(newRow);
        	highestID++;
        }
        
		
		status = TaskStatus.FINISHED;

	}

}
