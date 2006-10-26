package net.sf.mzmine.methods.gapfilling.simple;

import java.util.Hashtable;
import java.util.Vector;

import net.sf.mzmine.data.AlignmentResult;
import net.sf.mzmine.data.AlignmentResultRow;
import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.impl.SimpleAlignmentResult;
import net.sf.mzmine.data.impl.SimpleAlignmentResultRow;
import net.sf.mzmine.data.impl.SimpleIsotopePattern;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.taskcontrol.Task.TaskStatus;

class SimpleGapFillerMain implements TaskListener {

	private AlignmentResult originalAlignmentResult;
	private SimpleAlignmentResult processedAlignmentResult;
	
	private SimpleGapFillerParameters parameters;
	
	// Maps raw data files to an array of gaps which must be filled from the raw data. Used when distributing tasks.
	private Hashtable<OpenedRawDataFile, Vector<EmptyGap>> gapsForRawData;
	
	// Maps an empty gap to a opened raw data file. Used when constructing a peak from an empty gap and placing it on alignment row. 
	private Hashtable<EmptyGap, OpenedRawDataFile> rawDataForGap;
	
	// Maps an alignment row to an array of all empty gaps on that row. Used when constructing new alignment result
	private Hashtable<AlignmentResultRow, Vector<EmptyGap>> gapsForRow;

	// Maps raw data file to results of processing task (array of empty gaps)
	private Hashtable<OpenedRawDataFile, EmptyGap[]> resultsForRawData;
	
	private Vector<Task> startedTasks;
	private Vector<Task> completedTasks;
	
	
	private TaskController taskController;
	
	private TaskStatus overallStatus;
	
	
	public SimpleGapFillerMain(TaskController taskController, AlignmentResult alignmentResult, SimpleGapFillerParameters parameters) {
		
		this.taskController = taskController; 
		this.originalAlignmentResult = alignmentResult;
		this.parameters = parameters;
		
		gapsForRawData = new Hashtable<OpenedRawDataFile, Vector<EmptyGap>>();
		rawDataForGap = new Hashtable<EmptyGap, OpenedRawDataFile>();
		gapsForRow = new Hashtable<AlignmentResultRow, Vector<EmptyGap>>();
		resultsForRawData = new Hashtable<OpenedRawDataFile, EmptyGap[]>();
		
	}
	
	public void doTasks() {

		
		/*
		 * Loop rows of original alignment result
		 * For each row with some missing peaks, generate "a working row" containing EmptyGap objects for each missing peak
		 */
		OpenedRawDataFile[] rawDataFiles = originalAlignmentResult.getRawDataFiles();
		int i=0;
		for (AlignmentResultRow alignmentRow : originalAlignmentResult.getRows()) {

			Vector<EmptyGap> gapsOfTheCurrentRow = gapsForRow.get(alignmentRow);
			if (gapsOfTheCurrentRow==null) { gapsOfTheCurrentRow = new Vector<EmptyGap>(); gapsForRow.put(alignmentRow, gapsOfTheCurrentRow); }   
			
			double mz = alignmentRow.getAverageMZ();
			double rt = alignmentRow.getAverageRT();
			for (OpenedRawDataFile openedRawDataFile : rawDataFiles) {
				if (alignmentRow.getPeak(openedRawDataFile)==null) {
					EmptyGap emptyGap = new EmptyGap(mz,rt,parameters);
					
					Vector<EmptyGap> emptyGaps = gapsForRawData.get(openedRawDataFile);
					if (emptyGaps==null) { emptyGaps = new Vector<EmptyGap>(); gapsForRawData.put(openedRawDataFile, emptyGaps); }
					emptyGaps.add(emptyGap);

					rawDataForGap.put(emptyGap, openedRawDataFile);
					gapsOfTheCurrentRow.add(emptyGap);
					
				}
			}
			i++;
		}
		
		// Start a task for filling gaps in each raw data file
		overallStatus = TaskStatus.PROCESSING;

		startedTasks = new Vector<Task>();
		completedTasks = new Vector<Task>();
		for (OpenedRawDataFile openedRawDataFile : rawDataFiles) {
			
			Vector<EmptyGap> emptyGapsV = gapsForRawData.get(openedRawDataFile);
			if (emptyGapsV==null) continue;
			if (emptyGapsV.size()==0) continue;
			EmptyGap[] emptyGaps = emptyGapsV.toArray(new EmptyGap[0]);
			
			Task gapFillingTask = new SimpleGapFillerTask(openedRawDataFile, emptyGaps, parameters);
			startedTasks.add(gapFillingTask);
			taskController.addTask(gapFillingTask, this);
			
		}
		
	}
	
	public void taskFinished(Task task) {
		
		if (overallStatus == TaskStatus.ERROR) return;

		// Did the task fail?
		if (task.getStatus()== TaskStatus.ERROR) {
			overallStatus = TaskStatus.ERROR;
			// Cancel all remaining tasks
			for (Task t : startedTasks) 
				if ( 	(t.getStatus()!=TaskStatus.FINISHED) ||
						(t.getStatus()!=TaskStatus.ERROR) )
					t.cancel();
		}
		
		
		
		// Pickup results
		Object[] results = (Object[]) task.getResult();
		OpenedRawDataFile openedRawDataFile = (OpenedRawDataFile)results[0];
		EmptyGap[] emptyGaps = (EmptyGap[])results[1];
		resultsForRawData.put(openedRawDataFile, emptyGaps);
		
		completedTasks.add(task);
		
		// All results received already?
		if (completedTasks.size()==startedTasks.size()) {
			
			// Yes, then construct new alignment result & copy opened raw data files from original alignment result to the new one
			processedAlignmentResult = new SimpleAlignmentResult("Result from gap-filling");
			for (OpenedRawDataFile loopOpenedRawDataFile : originalAlignmentResult.getRawDataFiles()) {
				processedAlignmentResult.addOpenedRawDataFile(loopOpenedRawDataFile);
			}
			
			// Add rows to the new alignment result
			for (AlignmentResultRow alignmentRow : originalAlignmentResult.getRows()) {
				SimpleAlignmentResultRow processedAlignmentRow = new SimpleAlignmentResultRow();	
				processedAlignmentRow.setIsotopePattern(alignmentRow.getIsotopePattern());
				
				// Copy old peaks to new row
				for (OpenedRawDataFile loopOpenedRawDataFile : alignmentRow.getOpenedRawDataFiles()) {
					Peak p = alignmentRow.getPeak(loopOpenedRawDataFile);
					processedAlignmentRow.addPeak(loopOpenedRawDataFile, p);
				}

				// Construct new peaks from empty gaps and put them on same row
				Vector<EmptyGap> filledGaps = gapsForRow.get(alignmentRow);
				for (EmptyGap filledGap : filledGaps) {
					Peak p = filledGap.getEstimatedPeak();
					p.addData(IsotopePattern.class, alignmentRow.getIsotopePattern());
					OpenedRawDataFile peakRawData = rawDataForGap.get(filledGap);
					processedAlignmentRow.addPeak(peakRawData, p);
				}
				
				// Add row to the new alignment result
				processedAlignmentResult.addRow(processedAlignmentRow);
				
				
			}
			
			// TODO: Add method and parameters to history of an alignment result
			
			// Add new alignment result to the project			
			MZmineProject.getCurrentProject().addAlignmentResult(processedAlignmentResult);
			
		}

	}

	public void taskStarted(Task task) {
		// TODO Auto-generated method stub

	}

}
