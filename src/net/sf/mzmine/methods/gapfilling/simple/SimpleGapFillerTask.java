package net.sf.mzmine.methods.gapfilling.simple;

import java.io.IOException;
import java.util.Hashtable;

import net.sf.mzmine.data.AlignmentResult;
import net.sf.mzmine.data.AlignmentResultRow;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimpleAlignmentResult;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.methods.MethodParameters;
import net.sf.mzmine.methods.alignment.join.JoinAlignerParameters;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.taskcontrol.Task.TaskStatus;

class SimpleGapFillerTask implements Task {

	private OpenedRawDataFile openedRawDataFile;
	
	private EmptyGap[] emptyGaps;
	
	private TaskStatus status;
	private String errorMessage;
	
	private SimpleGapFillerParameters parameters;
	private double intTolerance;
	private double mzTolerance;
	private boolean rtToleranceUseAbs;
	private double rtToleranceValueAbs;
	private double rtToleranceValuePercent;
	
	private int processedRawDataFiles;
	private int totalRawDataFiles;
	
	public SimpleGapFillerTask(OpenedRawDataFile openedRawDataFile, EmptyGap[] emptyGaps,
			SimpleGapFillerParameters parameters) {
		
		status = TaskStatus.WAITING;
		
		this.openedRawDataFile = openedRawDataFile;
		this.emptyGaps = emptyGaps;

        this.parameters = parameters;
        intTolerance = parameters.getParameterValue(SimpleGapFillerParameters.IntTolerance).getDoubleValue(); 
        mzTolerance = parameters.getParameterValue(SimpleGapFillerParameters.MZTolerance).getDoubleValue();
        if (parameters.getParameterValue(SimpleGapFillerParameters.RTToleranceType)==SimpleGapFillerParameters.RTToleranceTypeAbsolute) rtToleranceUseAbs = true;
        rtToleranceValueAbs = parameters.getParameterValue(SimpleGapFillerParameters.RTToleranceValueAbs).getDoubleValue();
        rtToleranceValuePercent = parameters.getParameterValue(SimpleGapFillerParameters.RTToleranceValuePercent).getDoubleValue();

	}
	
	public void run() {
		
		status = TaskStatus.PROCESSING;
		
		RawDataFile rawDataFile = openedRawDataFile.getCurrentFile();
		int[] scanNumbers = rawDataFile.getScanNumbers(1);

		for (int scanNumber : scanNumbers) {
			if (status == TaskStatus.CANCELED) return;
			
			// Get next scan
			Scan s=null;
			try {
				s = rawDataFile.getScan(scanNumber);
			} catch (IOException e) {
				errorMessage = "Error while reading raw data file " + rawDataFile.getFile();
				status = TaskStatus.ERROR;
				return;
			}
			
			// Feed this scan to all empty gaps
			for (EmptyGap emptyGap : emptyGaps) {
				emptyGap.offerNextScan(s);
			}
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
		if (totalRawDataFiles==0) return 0;
		return (float)processedRawDataFiles / (float)totalRawDataFiles;

	}

	public Object getResult() {
		Object[] result = new Object[3];
		result[0] = openedRawDataFile;
		result[1] = emptyGaps;
		result[2] = parameters;
		return result;
	}

	public TaskStatus getStatus() {
		return status;
	}

	public String getTaskDescription() {
		return "Simple gap filler";
	}


}
