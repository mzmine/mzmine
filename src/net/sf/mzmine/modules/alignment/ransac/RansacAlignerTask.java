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
package net.sf.mzmine.modules.alignment.ransac;

import java.util.Vector;
import java.util.logging.Logger;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.data.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.data.impl.SimplePeakListRow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.Range;

/**
 * 
 */
class RansacAlignerTask implements Task {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	private PeakList peakLists[],  alignedPeakList;
	private TaskStatus status = TaskStatus.WAITING;
	private String errorMessage;

	// Processed rows counter
	private int processedRows,  totalRows;
	private String peakListName;
	private double mzTolerance;
	private double rtToleranceValueAbs;
	private RansacAlignerParameters parameters;
	private Vector<RawDataFile> allDataFiles;
	private int contID = 1;

	/**
	 * @param rawDataFile
	 * @param parameters
	 */
	RansacAlignerTask(PeakList[] peakLists, RansacAlignerParameters parameters) {

		this.peakLists = peakLists;
		this.parameters = parameters;

		// Get parameter values for easier use
		peakListName = (String) parameters.getParameterValue(RansacAlignerParameters.peakListName);

		mzTolerance = (Double) parameters.getParameterValue(RansacAlignerParameters.MZTolerance);

		rtToleranceValueAbs = (Double) parameters.getParameterValue(RansacAlignerParameters.RTToleranceValueAbs);

	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
	 */
	public String getTaskDescription() {
		return "Ransac aligner, " + peakListName + " (" + peakLists.length + " peak lists)";
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
	 */
	public double getFinishedPercentage() {
		if (totalRows == 0) {
			return 0f;
		}
		return (double) processedRows / (double) totalRows;
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
		logger.info("Running RANSAC aligner");

		// Remember how many rows we need to process.
		for (int i = 0; i < peakLists.length; i++) {
			for (int e = i + 1; e < peakLists.length; e++) {
				totalRows += peakLists[i].getNumberOfRows() * 2;
			}
		}

		// Collect all data files
		allDataFiles = new Vector<RawDataFile>();
		for (PeakList peakList : peakLists) {

			for (RawDataFile dataFile : peakList.getRawDataFiles()) {

				// Each data file can only have one column in aligned peak list
				if (allDataFiles.contains(dataFile)) {
					status = TaskStatus.ERROR;
					errorMessage = "Cannot run alignment, because file " + dataFile + " is present in multiple peak lists";
					return;
				}

				allDataFiles.add(dataFile);
			}
		}

		// Create a new aligned peak list
		alignedPeakList = new SimplePeakList(peakListName, allDataFiles.toArray(new RawDataFile[0]));


		// Create a vector with the alignment model for the combination of some samples
		Vector<Vector<AlignStructMol>> lists = new Vector<Vector<AlignStructMol>>();

		for (int i = 0; i < peakLists.length; i++) {
			for (int e = i + 1; e < peakLists.length; e++) {
				Vector<AlignStructMol> list = this.getVectorAlignment(peakLists[i], peakLists[e]);
				RANSAC ransac = new RANSAC(parameters, peakLists[i].getName());
				ransac.alignment(list);
				lists.addElement(list);
			}
		}


		// write results
		alignedPeakList = this.writeResults(lists);

		// write isolate peaks
		for (PeakList peakList : peakLists) {
			for (PeakListRow row : peakList.getRows()) {
				if (!isRow(row, alignedPeakList)) {
					alignedPeakList.addRow(row);
				}
			}
			alignedPeakList.setName(peakListName);
		}


		// Add new aligned peak list to the project
		MZmineProject currentProject = MZmineCore.getCurrentProject();

		currentProject.addPeakList(alignedPeakList);

		// Add task description to peakList
		alignedPeakList.addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod("RANSAC aligner", parameters));

		logger.info(
				"Finished RANSAC aligner");
		status = TaskStatus.FINISHED;

	}

	private PeakList writeResults(Vector<Vector<AlignStructMol>> lists) {

		PeakList alignedPeakList2 = new SimplePeakList(peakListName,
				allDataFiles.toArray(new RawDataFile[0]));
		for (Vector<AlignStructMol> list : lists) {
			for (AlignStructMol mol : list) {
				if (mol.Aligned) {
					PeakListRow row1 = alignedPeakList2.getPeakRow(mol.row1.getPeaks()[0]);
					PeakListRow row2 = alignedPeakList2.getPeakRow(mol.row2.getPeaks()[0]);
					if (row1 == null && row2 == null) {
						PeakListRow row3 = new SimplePeakListRow(contID++);

						row3.addPeak(mol.row1.getPeaks()[0].getDataFile(), mol.row1.getPeaks()[0]);
						row3.addPeak(mol.row2.getPeaks()[0].getDataFile(), mol.row2.getPeaks()[0]);

						alignedPeakList2.addRow(row3);

					} else {
						if (row1 != null) {
							row1.addPeak(mol.row2.getPeaks()[0].getDataFile(), mol.row2.getPeaks()[0]);
						}
						if (row2 != null) {
							row2.addPeak(mol.row1.getPeaks()[0].getDataFile(), mol.row1.getPeaks()[0]);
						}

					}
				} else {
					PeakListRow row1 = alignedPeakList2.getPeakRow(mol.row1.getPeaks()[0]);
					PeakListRow row2 = alignedPeakList2.getPeakRow(mol.row2.getPeaks()[0]);
					if (row1 == null) {
						PeakListRow row3 = new SimplePeakListRow(contID++);
						row3.addPeak(mol.row1.getPeaks()[0].getDataFile(), mol.row1.getPeaks()[0]);
						alignedPeakList2.addRow(row3);
					}
					if (row2 == null) {
						PeakListRow row3 = new SimplePeakListRow(contID++);
						row3.addPeak(mol.row2.getPeaks()[0].getDataFile(), mol.row2.getPeaks()[0]);
						alignedPeakList2.addRow(row3);
					}
				}
				processedRows++;
			}
		}

		fixAlignment(alignedPeakList2);
		return alignedPeakList2;

	}

	public void fixAlignment(PeakList peakList) {
		for (PeakListRow row : peakList.getRows()) {
			for (ChromatographicPeak peak : row.getPeaks()) {
				PeakListRow row2 = getPeakRow(peak, peakList, row);
				if (row2 != null) {
					repair(row, row2, peak, peakList);
				}
			}
		}
	}

	public void repair(PeakListRow row, PeakListRow row2, ChromatographicPeak p, PeakList peakList) {

		//System.out.println(row.getAverageMZ() + ": " + row.getID() + " - " + row2.getAverageMZ() + ": " + row2.getID());
		PeakListRow bigRow;
		PeakListRow smallRow;
		//System.out.println(row.getNumberOfPeaks() + " - " + row2.getNumberOfPeaks());
		if (row.getNumberOfPeaks() > row2.getNumberOfPeaks()) {
			bigRow = row;
			smallRow = row2;
		} else {
			bigRow = row2;
			smallRow = row;
		}


		for (ChromatographicPeak peak : smallRow.getPeaks()) {
			ChromatographicPeak peak2 = bigRow.getPeak(peak.getDataFile());
			if (peak2 == null && peak != p) {
				bigRow.addPeak(peak.getDataFile(), peak);
			} else {
			}

		}

		peakList.removeRow(smallRow);
	}

	private boolean isRow(PeakListRow row, PeakList peakList) {
		for (PeakListRow row2 : peakList.getRows()) {
			for (ChromatographicPeak p : row.getPeaks()) {
				if (row2.hasPeak(p)) {
					return true;
				}
			}
		}
		return false;
	}

	private PeakListRow getPeakRow(ChromatographicPeak p, PeakList peakList, PeakListRow row) {
		for (PeakListRow row2 : peakList.getRows()) {
			if (row2.hasPeak(p) && row != row2) {
				return row2;
			}
		}
		return null;
	}

	public Vector<AlignStructMol> getVectorAlignment(PeakList peakList, PeakList peakList2) {

		Vector<AlignStructMol> alignMol = new Vector<AlignStructMol>();

		for (PeakListRow row : peakList.getRows()) {

			if (status == TaskStatus.CANCELED) {
				return null;
			}

			// Calculate limits for a row with which the row can be aligned
			double mzMin = row.getAverageMZ() - mzTolerance;
			double mzMax = row.getAverageMZ() + mzTolerance;
			double rtMin, rtMax;
			double rtToleranceValue = rtToleranceValueAbs;
			rtMin = row.getAverageRT() - rtToleranceValue;
			rtMax = row.getAverageRT() + rtToleranceValue;

			// Get all rows of the aligned peaklist within parameter limits
			PeakListRow candidateRows[] = peakList2.getRowsInsideScanAndMZRange(
					new Range(rtMin, rtMax), new Range(mzMin, mzMax));

			for (PeakListRow candidateRow : candidateRows) {
				alignMol.addElement(new AlignStructMol(row, candidateRow));
			}
			processedRows++;
		}
		return alignMol;
	}

	public Object[] getCreatedObjects() {
		return new Object[]{alignedPeakList};
	}
}
