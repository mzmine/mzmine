package net.sf.mzmine.methods.normalization.linear;

import java.util.Hashtable;

import net.sf.mzmine.data.AlignmentResult;
import net.sf.mzmine.data.AlignmentResultRow;
import net.sf.mzmine.data.ParameterValue;
import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.impl.SimpleAlignmentResult;
import net.sf.mzmine.data.impl.SimpleAlignmentResultRow;
import net.sf.mzmine.data.impl.SimplePeak;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.taskcontrol.Task;

public class LinearNormalizerTask implements Task {

	private final double maximumOverallPeakHeightAfterNormalization = 100000.0;
	
	private AlignmentResult originalAlignmentResult;
	private LinearNormalizerParameters parameters;
	private String normalizationTypeString;
	private ParameterValue normalizationTypeParameterValue;
	
	private TaskStatus status;
	private String errorMessage;
	
	private int processedDataFiles;
	private int totalDataFiles;
	
	private SimpleAlignmentResult normalizedAlignmentResult;
	
	
	public LinearNormalizerTask(AlignmentResult alignmentResult, LinearNormalizerParameters parameters) {
		this.originalAlignmentResult = alignmentResult;
		this.parameters = parameters;
		
		normalizationTypeParameterValue = parameters.getParameterValue(LinearNormalizerParameters.NormalizationType);
		normalizationTypeString = normalizationTypeParameterValue.getStringValue(); 
		
	}
	
	public void cancel() {
		status = TaskStatus.CANCELED;

	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public float getFinishedPercentage() {
		return (float)processedDataFiles / (float)totalDataFiles;
	}

	public Object getResult() {
		// TODO Auto-generated method stub
		Object[] result = new Object[3];
		result[0] = originalAlignmentResult;
		result[1] = normalizedAlignmentResult;
		result[2] = parameters;
		return result;
	}

	public TaskStatus getStatus() {
		return status;
	}

	public String getTaskDescription() {
		return "Linear normalization of " + originalAlignmentResult.toString() + " by " + normalizationTypeString; 
	}

	public void run() {
		
		status = TaskStatus.PROCESSING;

		totalDataFiles = originalAlignmentResult.getNumberOfRawDataFiles();
		
		// This hashtable maps rows from original alignment result to rows of the normalized alignment
		Hashtable<AlignmentResultRow, SimpleAlignmentResultRow> rowMap = new Hashtable<AlignmentResultRow, SimpleAlignmentResultRow>(); 

		// Initialize new alignment result for to normalized result
		normalizedAlignmentResult = new SimpleAlignmentResult("Result from Linear Normalization by " + normalizationTypeString);
		
		// Copy raw data files from original alignment result to new alignment result
		for (OpenedRawDataFile ord : originalAlignmentResult.getRawDataFiles()) 
			normalizedAlignmentResult.addOpenedRawDataFile(ord);
		
		// Loop through all raw data files, and find the peak with biggest height
		double maxOriginalHeight = 0.0;
		for (OpenedRawDataFile ord : originalAlignmentResult.getRawDataFiles()) {
			for (AlignmentResultRow originalAlignmentRow : originalAlignmentResult.getRows()) {
				Peak p = originalAlignmentRow.getPeak(ord);
				if (p!=null) 
					if (maxOriginalHeight<=p.getNormalizedHeight())
						maxOriginalHeight = p.getNormalizedHeight(); 
			}
		}
		
		// Loop through all raw data files, and normalize peak values
		for (OpenedRawDataFile ord : originalAlignmentResult.getRawDataFiles()) {
			
			if (status == TaskStatus.CANCELED) {
				normalizedAlignmentResult = null;
				rowMap.clear();
				rowMap = null;
				return;
			}
			
			// Determine normalization type and calculate normalization factor accordingly
			double normalizationFactor = 1.0;
			
			// - normalization by average squared peak intensity
			if (normalizationTypeParameterValue==LinearNormalizerParameters.NormalizationTypeAverageIntensity) {
				double intensitySum = 0.0;
				int intensityCount = 0;
				for (AlignmentResultRow alignmentRow : originalAlignmentResult.getRows()) {
					Peak p = alignmentRow.getPeak(ord);
					if (p!=null) {
						// TODO: Use global parameter to determine whether to use height or area
						intensitySum += p.getNormalizedHeight();
						intensityCount++;
					}
				}
				normalizationFactor = intensitySum / (double)intensityCount;
			}

			// - normalization by average squared peak intensity
			if (normalizationTypeParameterValue==LinearNormalizerParameters.NormalizationTypeAverageSquaredIntensity) {
				double intensitySum = 0.0;
				int intensityCount = 0;
				for (AlignmentResultRow alignmentRow : originalAlignmentResult.getRows()) {			
					Peak p = alignmentRow.getPeak(ord);
					if (p!=null) {
						// TODO: Use global parameter to determine whether to use height or area
						intensitySum += (p.getNormalizedHeight() * p.getNormalizedHeight());
						intensityCount++;
					}
				}
				normalizationFactor = intensitySum / (double)intensityCount;
			}
			
			// - normalization by maximum peak intensity
			if (normalizationTypeParameterValue==LinearNormalizerParameters.NormalizationTypeMaximumPeakHeight) {
				double maximumIntensity = 0.0;
				for (AlignmentResultRow alignmentRow : originalAlignmentResult.getRows()) {			
					Peak p = alignmentRow.getPeak(ord);
					if (p!=null) {
						// TODO: Use global parameter to determine whether to use height or area
						if (maximumIntensity<p.getNormalizedHeight())
							maximumIntensity = p.getNormalizedHeight();
					}
				}
				normalizationFactor = maximumIntensity;
			}		

			// - normalization by total raw signal
			if (normalizationTypeParameterValue==LinearNormalizerParameters.NormalizationTypeTotalRawSignal) {
				// TODO: Add a method for calculating total raw signal to RawDataFile interface, and use that method here.
				normalizationFactor = 1.0;
			}			
			
			// Find peak with maximum height and calculate scaling the brings height of this peak to
			
			// Readjust normalization factor so that maximum height will be equal to maximumOverallPeakHeightAfterNormalization after normalization
			double maxNormalizedHeight = maxOriginalHeight / normalizationFactor;
			normalizationFactor = normalizationFactor * maxNormalizedHeight /  maximumOverallPeakHeightAfterNormalization;
							
				
			// Normalize all peak intenisities using the normalization factor
			for (AlignmentResultRow originalAlignmentRow : originalAlignmentResult.getRows()) {
				Peak originalPeak = originalAlignmentRow.getPeak(ord);
				if (originalPeak!=null) {
					SimplePeak normalizedPeak = new SimplePeak(originalPeak);
					double normalizedHeight = originalPeak.getNormalizedHeight() / normalizationFactor;
					double normalizedArea = originalPeak.getNormalizedArea() / normalizationFactor;
					normalizedPeak.setNormalizedHeight(normalizedHeight);
					normalizedPeak.setNormalizedArea(normalizedArea);
				
					SimpleAlignmentResultRow normalizedRow = rowMap.get(originalAlignmentRow);
					if (normalizedRow==null) {
						normalizedRow = new SimpleAlignmentResultRow();
						normalizedRow.setIsotopePattern(originalAlignmentRow.getIsotopePattern());
						rowMap.put(originalAlignmentRow, normalizedRow);
					}
				
					normalizedRow.addPeak(ord, normalizedPeak);
					
				}
			}
			
			
			// Progress
			processedDataFiles++;
		}
		
		// Finally add all normalized rows to normalized alignment result
		for (AlignmentResultRow originalAlignmentRow : originalAlignmentResult.getRows()) {
			SimpleAlignmentResultRow normalizedRow = rowMap.get(originalAlignmentRow);
			normalizedAlignmentResult.addRow(normalizedRow);
		}
		
        status = TaskStatus.FINISHED;
    }
	

}
