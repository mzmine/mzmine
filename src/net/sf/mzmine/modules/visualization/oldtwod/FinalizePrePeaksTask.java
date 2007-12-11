package net.sf.mzmine.modules.visualization.oldtwod;

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
		
		// TODO
		errorMessage = "Unfinished";
		status = TaskStatus.ERROR;

	}

}
