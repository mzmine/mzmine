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

package net.sf.mzmine.modules.peakpicking.peakextender;

import java.util.Arrays;
import java.util.logging.Logger;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListAppliedMethod;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.data.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.data.impl.SimplePeakListRow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peakpicking.chromatogrambuilder.MzPeak;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.PeakSorter;
import net.sf.mzmine.util.PeakUtils;
import net.sf.mzmine.util.Range;
import net.sf.mzmine.util.ScanUtils;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

public class PeakExtenderTask implements Task {
	
	private Logger logger = Logger.getLogger(this.getClass().getName());

	private TaskStatus status = TaskStatus.WAITING;
	private String errorMessage;

	private PeakList peakList, extendedPeakList;
	
	// peaks counter
	private int processedPeaks, totalPeaks;

	//Parameters
	private double mzTolerance;
	private double minimumHeight;
	private String suffix;
	private boolean removeOriginal;
		
	
	private PeakExtenderParameters parameters;

	public PeakExtenderTask(PeakList peakList, PeakExtenderParameters parameters) {

		this.peakList = peakList;
		this.parameters = parameters;
		
		suffix = (String) parameters
		.getParameterValue(PeakExtenderParameters.suffix);
		mzTolerance = (Double) parameters
		.getParameterValue(PeakExtenderParameters.mzTolerance);
		minimumHeight = (Double) parameters
		.getParameterValue(PeakExtenderParameters.minimumHeight);
		removeOriginal = (Boolean) parameters
		.getParameterValue(PeakExtenderParameters.autoRemove);
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
	 */
	public String getTaskDescription() {
		return "Extending peaks on " + peakList;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
	 */
	public double getFinishedPercentage() {
		if (totalPeaks == 0)
			return 0.0f;
		return (double) processedPeaks / (double) totalPeaks;
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
		logger.info("Running peak extender on " + peakList);

		// We assume source peakList contains one datafile
		RawDataFile dataFile = peakList.getRawDataFile(0);

		// Create a new deisotoped peakList
		extendedPeakList = new SimplePeakList(peakList + " " + suffix,
				peakList.getRawDataFiles());

		// Sort peaks by descending height
		ChromatographicPeak[] sortedPeaks = peakList.getPeaks(dataFile);
		Arrays.sort(sortedPeaks, new PeakSorter(SortingProperty.Height,
				SortingDirection.Descending));

		// Loop through all peaks
		totalPeaks = sortedPeaks.length;
		ChromatographicPeak oldPeak;
		
		for (int ind = 0; ind < totalPeaks; ind++) {

			if (status == TaskStatus.CANCELED)
				return;

			oldPeak = sortedPeaks[ind];

			if (oldPeak.getHeight() >= minimumHeight){
				ChromatographicPeak newPeak =  this.getExtendedPeak(oldPeak);
				//Get previous pekaListRow
				PeakListRow oldRow = peakList.getPeakRow(oldPeak);

				// keep old ID
				int oldID = oldRow.getID();
				SimplePeakListRow newRow = new SimplePeakListRow(oldID);
				PeakUtils.copyPeakListRowProperties(oldRow, newRow);
				newRow.addPeak(dataFile, newPeak);
				extendedPeakList.addRow(newRow);
			}

			// Update completion rate
			processedPeaks++;

		}

		// Add new peakList to the project
		MZmineProject currentProject = MZmineCore.getCurrentProject();
		currentProject.addPeakList(extendedPeakList);

		// Load previous applied methods
		for (PeakListAppliedMethod proc : peakList.getAppliedMethods()) {
			extendedPeakList.addDescriptionOfAppliedTask(proc);
		}

		// Add task description to peakList
		extendedPeakList
				.addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod(
						"Peak extended", parameters));

		// Remove the original peakList if requested
		if (removeOriginal)
			currentProject.removePeakList(peakList);

		logger.info("Finished peak extender on " + peakList);
		status = TaskStatus.FINISHED;

	}



	private ChromatographicPeak getExtendedPeak(ChromatographicPeak oldPeak) {

		double maxHeight = oldPeak.getHeight();
		int originScanNumber = oldPeak.getRepresentativeScanNumber();
		RawDataFile rawFile = oldPeak.getDataFile();
		ExtendedPeak newPeak = new ExtendedPeak(rawFile);
		int totalScanNumber = rawFile.getNumOfScans();
		Range mzRange = new Range(oldPeak.getMZ() - (mzTolerance / 2.0f), 
				oldPeak.getMZ()	+ (mzTolerance / 2.0f));
		Scan scan;
		DataPoint dataPoint;
		
		
		//Look for dataPoint related to this peak to the left
		int scanNumber = originScanNumber;
		scanNumber--;
		while (scanNumber > 0){

			scan = rawFile.getScan(scanNumber);

			if (scan == null){
				scanNumber--;
				continue;
			}

			if (scan.getMSLevel() != 1){
				scanNumber--;
				continue;
			}
			
			dataPoint = ScanUtils.findBasePeak(scan, mzRange);

			if (dataPoint == null)
				break;
			if (dataPoint.getIntensity() < minimumHeight)
				break;

			newPeak.addMzPeak(scanNumber, new MzPeak(dataPoint));
			if (dataPoint.getIntensity() > maxHeight)
				maxHeight = dataPoint.getIntensity();
			
			scanNumber--;
			
		}

		//Add original dataPoint
		newPeak.addMzPeak(originScanNumber, new MzPeak(oldPeak.getDataPoint(originScanNumber)));
		
		//Look to the right
		scanNumber = originScanNumber;
		scanNumber++;
		while (scanNumber <= totalScanNumber){
			
			scan = rawFile.getScan(scanNumber);
			
			if (scan == null){
				scanNumber++;
				continue;
			}
			
			if (scan.getMSLevel() != 1){
				scanNumber++;
				continue;
			}
			
			dataPoint = ScanUtils.findBasePeak(scan, mzRange);
			
			if (dataPoint == null)
				break;
			if (dataPoint.getIntensity() < minimumHeight)
				break;

			newPeak.addMzPeak(scanNumber, new MzPeak(dataPoint));
			if (dataPoint.getIntensity() > maxHeight)
				maxHeight = dataPoint.getIntensity();
			
			scanNumber++;
			
		}

		newPeak.finishExtendedPeak();
		
		int[] scanNumbers = newPeak.getScanNumbers();
		logger.info("Extended peak original "+originScanNumber+" from "+scanNumbers[0]+" - "+
				scanNumbers[scanNumbers.length-1]+" height " + maxHeight);
		
		return newPeak;
		
	}

	public Object[] getCreatedObjects() {
		return new Object[] { extendedPeakList };
	}


}
