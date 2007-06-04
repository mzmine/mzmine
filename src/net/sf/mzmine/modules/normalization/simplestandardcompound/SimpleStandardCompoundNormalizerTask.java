package net.sf.mzmine.modules.normalization.simplestandardcompound;

import java.util.logging.Logger;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.taskcontrol.Task;

public class SimpleStandardCompoundNormalizerTask implements Task {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	
	private PeakList originalPeakList;
	private SimpleStandardCompoundNormalizerParameterSet parameters;
	
	private TaskStatus taskStatus;
	private String errorMessage;
	
	public SimpleStandardCompoundNormalizerTask(PeakList peakList, SimpleStandardCompoundNormalizerParameterSet parameters) {
		this.originalPeakList = peakList;
		this.parameters = parameters;
	}
	
	public void cancel() {
		taskStatus = TaskStatus.CANCELED;

	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public float getFinishedPercentage() {
		// TODO Auto-generated method stub
		return 0;
	}

	public Object getResult() {
		// TODO Auto-generated method stub
		return null;
	}

	public TaskStatus getStatus() {
		return taskStatus;
	}

	public String getTaskDescription() {
		return "Simple standard compound normalization of " + originalPeakList.toString();
	}

	public void run() {
		taskStatus = TaskStatus.PROCESSING;
		

		logger.fine("Starting Simple standard compound normlization of " + originalPeakList.toString() + " using " + parameters.getParameters().getParameterValue(SimpleStandardCompoundNormalizerParameterSet.normalizationType) + " (total " + parameters.getSelectedPeaks().length + " standard peaks)");
		
		
		taskStatus = TaskStatus.ERROR;
		errorMessage = "Method not yet implemented.";

	}

}
