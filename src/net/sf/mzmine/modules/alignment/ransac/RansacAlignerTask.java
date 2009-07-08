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
	private double rtTolerance;
	private RansacAlignerParameters parameters;
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

		rtTolerance = (Double) parameters.getParameterValue(RansacAlignerParameters.RTTolerance);
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
				totalRows += peakLists[i].getNumberOfRows();
			}
		}
		totalRows += ((peakLists.length) * (peakLists.length - 1)) / 2;


		alignedPeakList = new SimplePeakList(peakListName,
				getRawData(peakLists).toArray(new RawDataFile[0]));

		ransacPeakLists(peakLists, alignedPeakList);


		removeDuplicateRows(alignedPeakList);

		// write isolate peaks
		for (PeakList peakList : peakLists) {
			if (peakList != null) {
				for (PeakListRow row : peakList.getRows()) {
					if (!isRow(row, alignedPeakList)) {
						PeakListRow row3 = new SimplePeakListRow(contID++);
						for (ChromatographicPeak peak : row.getPeaks()) {
							row3.addPeak(peak.getDataFile(), peak);
						}
						alignedPeakList.addRow(row3);
					}
				}
			}
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

	public Vector<RawDataFile> getRawData(PeakList[] peakLists) {
		Vector<RawDataFile> allDataFiles = new Vector<RawDataFile>();
		for (PeakList peakList : peakLists) {
			if (peakList != null) {
				for (RawDataFile dataFile : peakList.getRawDataFiles()) {
					if (!allDataFiles.contains(dataFile)) {
						allDataFiles.add(dataFile);
					}
				}
			}
		}
		return allDataFiles;
	}

	/**
	 * 
	 * @param peakLists
	 *
	 */
	private void ransacPeakLists(PeakList[] peakLists, PeakList finalPeakList) {

		// Do the aligment combining all the samples
		for (int i = 0; i < peakLists.length; i++) {
			for (int e = i + 1; e < peakLists.length; e++) {
				if (peakLists[i] != null && peakLists[e] != null) {
					
					RawDataFile fileX = this.getDataFileWithMorePeaks(peakLists[i]);
					RawDataFile fileY = this.getDataFileWithMorePeaks(peakLists[e]);

					Vector<AlignStructMol> list = this.getVectorAlignment(peakLists[i], peakLists[e], fileX, fileY);
					RANSAC ransac = new RANSAC(parameters);
					ransac.alignment(list);
					this.getNewPeakList(list, finalPeakList);

				}
				processedRows++;
			}
		}
	}
	

	private RawDataFile getDataFileWithMorePeaks(PeakList peackList) {
		int numPeaks = 0;
		RawDataFile file = null;
		for (RawDataFile rfile : peackList.getRawDataFiles()) {
			if (peackList.getPeaks(rfile).length > numPeaks) {
				numPeaks = peackList.getPeaks(rfile).length;
				file = rfile;
			}
		}
		return file;
	}

	/**
	 * Create the vector which contains all the possible aligned peaks.
	 * @param peakList
	 * @param peakList2
	 * @return vector which contains all the possible aligned peaks.
	 */
	private Vector<AlignStructMol> getVectorAlignment(PeakList peakListX, PeakList peakListY, RawDataFile file, RawDataFile file2) {

		Vector<AlignStructMol> alignMol = new Vector<AlignStructMol>();

		for (PeakListRow row : peakListX.getRows()) {

			if (status == TaskStatus.CANCELED) {
				return null;
			}

			// Calculate limits for a row with which the row can be aligned
			double mzMin = row.getAverageMZ() - mzTolerance;
			double mzMax = row.getAverageMZ() + mzTolerance;
			double rtMin, rtMax;
			double rtToleranceValue = rtTolerance;
			rtMin = row.getAverageRT() - rtToleranceValue;
			rtMax = row.getAverageRT() + rtToleranceValue;

			// Get all rows of the aligned peaklist within parameter limits
			PeakListRow candidateRows[] = peakListY.getRowsInsideScanAndMZRange(
					new Range(rtMin, rtMax), new Range(mzMin, mzMax));

			for (PeakListRow candidateRow : candidateRows) {
				if (file == null || file2 == null) {
					alignMol.addElement(new AlignStructMol(row, candidateRow));
				} else {
					if (candidateRow.getPeak(file2) != null) {
						alignMol.addElement(new AlignStructMol(row, candidateRow, file, file2));
					}
				}
			}
			processedRows++;
		}
		return alignMol;
	}

	/**
	 * Write the result of all the alignments
	 * @param lists vector which contains the result of all the alignments between all the samples
	 * @return peak list
	 */
	private void getNewPeakList(Vector<AlignStructMol> list, PeakList peakList) {

		try {
			// for each possible aligned pair of peaks in all the samples			
			for (AlignStructMol mol : list) {

				// check if the rows are already in the new peak list
				PeakListRow row1 = peakList.getPeakRow(mol.row1.getPeaks()[0]);
				PeakListRow row2 = peakList.getPeakRow(mol.row2.getPeaks()[0]);

				if (mol.Aligned) {
					// if they are not add a new row
					if (row1 == null && row2 == null) {
						PeakListRow row3 = new SimplePeakListRow(contID++);
						for (ChromatographicPeak peak : mol.row1.getPeaks()) {
							row3.addPeak(peak.getDataFile(), peak);
						}
						for (ChromatographicPeak peak : mol.row2.getPeaks()) {
							row3.addPeak(peak.getDataFile(), peak);
						}
						peakList.addRow(row3);

					} else {
						// if one of them is already in the new peak list, add the other aligned peak in the same row
						if (row1 != null) {
							for (ChromatographicPeak peak : mol.row2.getPeaks()) {
								row1.addPeak(peak.getDataFile(), peak);
							}
						}
						if (row2 != null) {
							for (ChromatographicPeak peak : mol.row1.getPeaks()) {
								row2.addPeak(peak.getDataFile(), peak);
							}
						}
					}
				} else {
					if (row1 == null) {
						PeakListRow row3 = new SimplePeakListRow(contID++);
						for (ChromatographicPeak peak : mol.row1.getPeaks()) {
							row3.addPeak(peak.getDataFile(), peak);
						}
						peakList.addRow(row3);
					}
					if (row2 == null) {
						PeakListRow row3 = new SimplePeakListRow(contID++);
						for (ChromatographicPeak peak : mol.row2.getPeaks()) {
							row3.addPeak(peak.getDataFile(), peak);
						}
						peakList.addRow(row3);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Find the rows where are repeats peaks and call the function "removeRepeatedRows()"
	 * @param peakList
	 */
	private void removeDuplicateRows(PeakList peakList) {
		for (PeakListRow row : peakList.getRows()) {
			for (ChromatographicPeak peak : row.getPeaks()) {
				PeakListRow row2 = getPeakRow(peak, peakList, row);
				if (row2 != null) {
					removeRepeatedRows(row, row2, peak, peakList);
				}
			}
		}
	}

	/**
	 * Remove the repeated rows.
	 * @param row row where is the repeated peak
	 * @param row2 row where is the repeated peak
	 * @param p peak repeated in both rows
	 * @param peakList
	 */
	private void removeRepeatedRows(PeakListRow row, PeakListRow row2, ChromatographicPeak p, PeakList peakList) {

		PeakListRow bigRow;
		PeakListRow smallRow;
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
			}
		}

		peakList.removeRow(smallRow);
	}

	/**
	 *
	 * @param row
	 * @param peakList
	 * @return return true if the peak list contains one of the peak present in the row
	 */
	private boolean isRow(PeakListRow row, PeakList peakList) {
		for (PeakListRow row2 : peakList.getRowsInsideMZRange(new Range(row.getAverageMZ() - mzTolerance, row.getAverageMZ() + mzTolerance))) {
			for (ChromatographicPeak p : row.getPeaks()) {
				if (row2.hasPeak(p)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 *
	 * @param p
	 * @param peakList
	 * @param row
	 * @return return the row where is the peak "p" into the peak list
	 */
	private PeakListRow getPeakRow(ChromatographicPeak p, PeakList peakList, PeakListRow row) {
		for (PeakListRow row2 : peakList.getRowsInsideMZRange(new Range(row.getAverageMZ() - mzTolerance, row.getAverageMZ() + mzTolerance))) {
			if (row2.hasPeak(p) && row != row2) {
				return row2;
			}
		}
		return null;
	}

	public Object[] getCreatedObjects() {
		return new Object[]{alignedPeakList};
	}
}
