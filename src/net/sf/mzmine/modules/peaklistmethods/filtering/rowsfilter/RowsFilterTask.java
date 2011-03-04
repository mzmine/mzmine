/*
 * Copyright 2006-2011 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.peaklistmethods.filtering.rowsfilter;

import java.util.logging.Logger;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListAppliedMethod;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.data.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.Range;

class RowsFilterTask extends AbstractTask {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private PeakList peakList, filteredPeakList;

	// Processed rows counter
	private int processedRows, totalRows;

	// Method parameters
	private int minPresent, minIsotopePatternSize;
	private String suffix;
	private Range mzRange, rtRange;
	private boolean identified, removeOriginal;
	private RowsFilterParameters parameters;

	public RowsFilterTask(PeakList peakList, RowsFilterParameters parameters) {

		this.peakList = peakList;
		this.parameters = parameters;

		suffix = parameters.getParameter(RowsFilterParameters.suffix)
				.getValue();
		minPresent = parameters.getParameter(RowsFilterParameters.minPeaks)
				.getInt();
		minIsotopePatternSize = parameters.getParameter(
				RowsFilterParameters.minIsotopePatternSize).getInt();
		mzRange = parameters.getParameter(RowsFilterParameters.mzRange)
				.getValue();
		rtRange = parameters.getParameter(RowsFilterParameters.rtRange)
				.getValue();
		identified = parameters.getParameter(RowsFilterParameters.identified)
				.getValue();
		removeOriginal = parameters.getParameter(
				RowsFilterParameters.autoRemove).getValue();

	}

	public double getFinishedPercentage() {
		if (totalRows == 0)
			return 0.0f;
		return (double) processedRows / (double) totalRows;
	}

	public String getTaskDescription() {
		return "Filtering peak list rows";
	}

	public void run() {

		setStatus(TaskStatus.PROCESSING);
		logger.info("Running peak list rows filter");

		totalRows = peakList.getNumberOfRows();
		processedRows = 0;

		// Create new peaklist
		filteredPeakList = new SimplePeakList(peakList.toString() + " "
				+ suffix, peakList.getRawDataFiles());

		// Copy rows with enough peaks to new alignment result
		for (PeakListRow row : peakList.getRows()) {

			if (isCanceled())
				return;

			boolean rowIsGood = true;

			if (row.getNumberOfPeaks() < minPresent)
				rowIsGood = false;
			if ((identified) && (row.getPreferredPeakIdentity() == null))
				rowIsGood = false;
			if (!mzRange.contains(row.getAverageMZ()))
				rowIsGood = false;
			if (!rtRange.contains(row.getAverageRT()))
				rowIsGood = false;

			int maxIsotopePatternSizeOnRow = 1;
			for (ChromatographicPeak p : row.getPeaks()) {
				IsotopePattern i = p.getIsotopePattern();
				if (i != null) {
					if (maxIsotopePatternSizeOnRow < i.getNumberOfIsotopes())
						maxIsotopePatternSizeOnRow = i.getNumberOfIsotopes();
				}

			}
			if (maxIsotopePatternSizeOnRow < minIsotopePatternSize)
				rowIsGood = false;

			if (rowIsGood)
				filteredPeakList.addRow(row);

			processedRows++;

		}

		// Add new peaklist to the project
		MZmineProject currentProject = MZmineCore.getCurrentProject();
		currentProject.addPeakList(filteredPeakList);

		// Load previous applied methods
		for (PeakListAppliedMethod proc : peakList.getAppliedMethods()) {
			filteredPeakList.addDescriptionOfAppliedTask(proc);
		}

		// Add task description to peakList
		filteredPeakList
				.addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod(
						getTaskDescription(), parameters));

		// Remove the original peaklist if requested
		if (removeOriginal)
			currentProject.removePeakList(peakList);

		logger.info("Finished peak list rows filter");
		setStatus(TaskStatus.FINISHED);

	}

	public Object[] getCreatedObjects() {
		return new Object[] { filteredPeakList };
	}

}
