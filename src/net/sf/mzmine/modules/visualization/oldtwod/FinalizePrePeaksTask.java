/*
 * Copyright 2006-2008 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.oldtwod;

import java.io.IOException;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.data.impl.SimplePeakListRow;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.Task.TaskStatus;

public class FinalizePrePeaksTask implements Task {

	private RawDataFile dataFile;

	private PreConstructionPeak[] prePeaks;

	private OldTwoDVisualizerWindow visualizerWindow;

	private int processedScans;
	private int totalScans;

	private TaskStatus status;

	private String errorMessage;

	public FinalizePrePeaksTask(RawDataFile dataFile,
			PreConstructionPeak[] prePeaks,
			OldTwoDVisualizerWindow visualizerWindow) {
		this.dataFile = dataFile;
		this.prePeaks = prePeaks;
		this.visualizerWindow = visualizerWindow;

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

		PeakList peakList = visualizerWindow.getSelectedPeakList();

		// If there is no existing peak list, then create a new one
		if (peakList == null) {
			// TODO: Name for the new peak list should be provided by the user
			// as a parameter
			peakList = new SimplePeakList("new empty peak list",
					new RawDataFile[] { dataFile });
			// Add peak list to project
			MZmineProject currentProject = MZmineCore.getCurrentProject();
			currentProject.addPeakList(peakList);

		}

		// Find highest ID of existing peak list rows
		int highestID = 0;
		for (PeakListRow row : peakList.getRows()) {
			if (row.getID() > highestID)
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

		visualizerWindow.updatePeakListCombo();

		status = TaskStatus.FINISHED;

	}
}
