package net.sf.mzmine.modules.normalization.simplestandardcompound;

import java.util.Hashtable;
import java.util.logging.Logger;

import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.impl.SimplePeak;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.data.impl.SimplePeakListRow;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.modules.normalization.linear.LinearNormalizer;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.Task.TaskStatus;

public class SimpleStandardCompoundNormalizerTask implements Task {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	
	private PeakList originalPeakList;
	private SimpleStandardCompoundNormalizerParameterSet parameters;
	
	private TaskStatus taskStatus;
	private String errorMessage;
	
    private int processedDataFiles;
    private int totalDataFiles;

    private SimplePeakList normalizedPeakList;	
	
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
		return (float)processedDataFiles / (float)totalDataFiles;
	}

	public Object getResult() {
		return normalizedPeakList;
	}

	public TaskStatus getStatus() {
		return taskStatus;
	}

	public String getTaskDescription() {
		return "Simple standard compound normalization of " + originalPeakList.toString();
	}

	public void run() {
		
		taskStatus = TaskStatus.PROCESSING;
	
		Object normalizationType = parameters.getParameters().getParameterValue(SimpleStandardCompoundNormalizerParameterSet.StandardUsageType);
		double MZvsRTBalance = (Double)parameters.getParameters().getParameterValue(SimpleStandardCompoundNormalizerParameterSet.MZvsRTBalance);
		
		logger.fine("Starting Simple standard compound normlization of " + originalPeakList.toString() + " using " + normalizationType.toString() + " (total " + parameters.getSelectedStandardPeakListRows().length +  " standard peaks)");
		
        // This hashtable maps rows from original alignment result to rows of
        // the normalized alignment
        Hashtable<PeakListRow, SimplePeakListRow> rowMap = new Hashtable<PeakListRow, SimplePeakListRow>();

        // Initialize new alignment result for the normalized result
        normalizedPeakList = new SimplePeakList(
                "Result from Simple standard compound normalization using "
                        + normalizationType.toString());

        // Copy raw data files from original alignment result to new alignment
        // result
        totalDataFiles = originalPeakList.getRawDataFiles().length;
        for (OpenedRawDataFile ord : originalPeakList.getRawDataFiles()) 
            normalizedPeakList.addOpenedRawDataFile(ord);


        // Loop through all rows
        for (PeakListRow row : originalPeakList.getRows()) {
        	
        	// Cancel ?
            if (taskStatus == TaskStatus.CANCELED) {
                normalizedPeakList = null;
                rowMap.clear();
                rowMap = null;
                return;
            }    	

            // Get m/z and RT of the current row
        	double mz = row.getAverageMZ();
        	double rt = row.getAverageRT();
        	      	

        	// Loop through all raw data files
        	for (OpenedRawDataFile ord : originalPeakList.getRawDataFiles()) {        	
        	
        		double normalizationFactors[] = null;
        		double normalizationFactorWeights[] = null;
        		
	        	if (normalizationType == SimpleStandardCompoundNormalizerParameterSet.StandardUsageTypeNearest) {
	        		
	        		// Search for nearest standard
	        		PeakListRow nearestStandardRow = null;
	        		double nearestStandardRowDistance = Double.MAX_VALUE;
	        		PeakListRow[] standardRows = parameters.getSelectedStandardPeakListRows();
	        		for (int standardRowIndex=0; standardRowIndex<standardRows.length; standardRowIndex++) { 
	        			PeakListRow standardRow = standardRows[standardRowIndex]; 
	        			            
	        			double stdMZ = standardRow.getAverageMZ();
	        			double stdRT = standardRow.getAverageRT();
	        			double distance = MZvsRTBalance * java.lang.Math.abs(mz-stdMZ) + java.lang.Math.abs(rt-stdRT);
	        			if (distance<=nearestStandardRowDistance) {
	        				nearestStandardRow = standardRow;
	        				nearestStandardRowDistance = distance; 
	        			}
	        			        			
	        		}
	        		
	        		// Calc and store a single normalization factor
	        		normalizationFactors = new double[1];
	        		normalizationFactorWeights = new double[1];
	        		// TODO: Determine normalization factor using data on the nearestStandardRow	        		
	        		normalizationFactors[0] = 1.0;
	        		normalizationFactorWeights[0] = 1.0;
	        		
	        	}
	
	        	if (normalizationType == SimpleStandardCompoundNormalizerParameterSet.StandardUsageTypeWeighted) {
	        		
	        		// Add all standards as factors, and use distance as weight
	        		PeakListRow[] standardRows = parameters.getSelectedStandardPeakListRows();
	        		normalizationFactors = new double[standardRows.length];
	        		normalizationFactorWeights = new double[standardRows.length];
	        		
	        		for (int standardRowIndex=0; standardRowIndex<standardRows.length; standardRowIndex++) { 
	        			PeakListRow standardRow = standardRows[standardRowIndex]; 
	        			            
	        			double stdMZ = standardRow.getAverageMZ();
	        			double stdRT = standardRow.getAverageRT();
	        			double distance = MZvsRTBalance * java.lang.Math.abs(mz-stdMZ) + java.lang.Math.abs(rt-stdRT);

	        			// TODO: Determine normalization factor using data on the current standardRow and weight using distance
	        			normalizationFactors[standardRowIndex] = 1.0;
	        			normalizationFactorWeights[standardRowIndex] = 1.0 / (double)standardRows.length;
	        			// normalizationFactorWeights[standardRowIndex] = distance;
	        		}
	        		
	        	}
	        	
	        	// Calculate a single normalization factor as weighted average of all factors
	        	double weightedSum = 0.0;
	        	double sumOfWeights = 0.0;
	        	for (int factorIndex=0; factorIndex<normalizationFactors.length; factorIndex++) {
	        		weightedSum += normalizationFactors[factorIndex] * normalizationFactorWeights[factorIndex];
	        		sumOfWeights += normalizationFactorWeights[factorIndex];
	        	}
	        	double normalizationFactor = weightedSum / sumOfWeights;
	        	
	        	// TODO: How to handle zero normalization factor?
	        	if (normalizationFactor==0.0) normalizationFactor = Double.MIN_VALUE;
	        	
	        	
	        	// Normalize peak 	        	
                Peak originalPeak = row.getPeak(ord);
                if (originalPeak != null) {
                    SimplePeak normalizedPeak = new SimplePeak(originalPeak);
                    double normalizedHeight = originalPeak.getHeight()
                            / normalizationFactor;
                    double normalizedArea = originalPeak.getArea()
                            / normalizationFactor;
                    normalizedPeak.setHeight(normalizedHeight);
                    normalizedPeak.setArea(normalizedArea);

                    SimplePeakListRow normalizedRow = rowMap.get(row);
                    if (normalizedRow == null) {
                        normalizedRow = new SimplePeakListRow(row.getID());
                        rowMap.put(row, normalizedRow);
                    }

                    normalizedRow.addPeak(ord, originalPeak, normalizedPeak);
                }

        	}   	
        	
        }
        
        // TODO: Scale all normalized values
        
        // Finally add all normalized rows to normalized alignment result
        for (PeakListRow originalAlignmentRow : originalPeakList.getRows()) {
            SimplePeakListRow normalizedRow = rowMap.get(originalAlignmentRow);
            normalizedPeakList.addRow(normalizedRow);
        }

        taskStatus = TaskStatus.FINISHED;
		
	}

}
