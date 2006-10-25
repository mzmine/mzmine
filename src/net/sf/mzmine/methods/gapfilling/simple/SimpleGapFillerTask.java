package net.sf.mzmine.methods.gapfilling.simple;

import net.sf.mzmine.data.AlignmentResult;
import net.sf.mzmine.data.impl.SimpleAlignmentResult;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.methods.MethodParameters;
import net.sf.mzmine.methods.alignment.join.JoinAlignerParameters;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.Task.TaskStatus;

class SimpleGapFillerTask implements Task {

	private AlignmentResult originalAlignmentResult;
	private SimpleAlignmentResult processedAlignmentResult;
	
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
	
	
	public SimpleGapFillerTask(AlignmentResult alignmentResult,
			SimpleGapFillerParameters parameters) {
		
		this.originalAlignmentResult = alignmentResult;

        status = TaskStatus.WAITING;
        this.originalAlignmentResult = alignmentResult;
        this.parameters = parameters;
        intTolerance = parameters.getParameterValue(SimpleGapFillerParameters.IntTolerance).getDoubleValue(); 
        mzTolerance = parameters.getParameterValue(SimpleGapFillerParameters.MZTolerance).getDoubleValue();
        if (parameters.getParameterValue(SimpleGapFillerParameters.RTToleranceType)==SimpleGapFillerParameters.RTToleranceTypeAbsolute) rtToleranceUseAbs = true;
        rtToleranceValueAbs = parameters.getParameterValue(SimpleGapFillerParameters.RTToleranceValueAbs).getDoubleValue();
        rtToleranceValuePercent = parameters.getParameterValue(SimpleGapFillerParameters.RTToleranceValuePercent).getDoubleValue();
        

	}
	
	public void run() {
		// TODO Auto-generated method stub
		
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
		result[0] = originalAlignmentResult;
		result[1] = processedAlignmentResult;
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
