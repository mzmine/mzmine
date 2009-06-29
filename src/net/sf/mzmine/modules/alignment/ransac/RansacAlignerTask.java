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
	private AlignmentChart chart;
	private Boolean showChart;

	
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

		showChart = (Boolean) parameters.getParameterValue(RansacAlignerParameters.chart);

		if (showChart) {
			chart = new AlignmentChart("Alignment");
			chart.setVisible(true);
			MZmineCore.getDesktop().addInternalFrame(chart);
		}
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
		
		alignedPeakList = this.getPeakList(peakLists);

		// Add new aligned peak list to the project
		MZmineProject currentProject = MZmineCore.getCurrentProject();
		currentProject.addPeakList(alignedPeakList);

		// Add task description to peakList
		alignedPeakList.addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod("RANSAC aligner", parameters));

		logger.info(
				"Finished RANSAC aligner");
		status = TaskStatus.FINISHED;

	}

	/**
	 * 
	 * @param peakListss
	 * @return
	 */
	private PeakList getPeakList(PeakList[] peakListss) {
		allDataFiles = new Vector<RawDataFile>();
		for (PeakList peakList : peakListss) {
			if (peakList != null) {
				for (RawDataFile dataFile : peakList.getRawDataFiles()) {
					allDataFiles.add(dataFile);
				}
			}
		}

		// Create a vector with the alignment model for the combination of some samples
		Vector<Vector<AlignStructMol>> lists = new Vector<Vector<AlignStructMol>>();

		// do the aligment combining all the samples
		for (int i = 0; i < peakListss.length; i++) {
			for (int e = i + 1; e < peakListss.length; e++) {
				if (peakListss[i] != null && peakListss[e] != null) {
					Vector<AlignStructMol> list = this.getVectorAlignment(peakListss[i], peakListss[e]);
					RANSAC ransac = new RANSAC(parameters, peakListss[i].getName());
					ransac.alignment(list);
					lists.addElement(list);
					// Visualizantion of the new selected model
					if (showChart) {
						chart.removeSeries();
						chart.addSeries(list, peakListss[i].getName() + " vs " + peakListss[e].getName());
					//	chart.printAlignmentChart();
					}
				}
			}
		}


		// write results
		PeakList PeakList = this.getNewPeakList(lists);

		// write isolate peaks
		for (PeakList peakList : peakListss) {
			if (peakList != null) {
				for (PeakListRow row : peakList.getRows()) {
					if (!isRow(row, PeakList)) {
						PeakList.addRow(row);
					}
				}
			}
		}

		return PeakList;
	}

	/**
	 * Create the vector which contains all the possible aligned peaks.
	 * @param peakList
	 * @param peakList2
	 * @return vector which contains all the possible aligned peaks.
	 */
	private Vector<AlignStructMol> getVectorAlignment(PeakList peakList, PeakList peakList2) {

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

	/**
	 * Write the result of all the alignments
	 * @param lists vector which contains the result of all the alignments between all the samples
	 * @return peak list
	 */
	private PeakList getNewPeakList(Vector<Vector<AlignStructMol>> lists) {

		try {

			// create a new peak list
			PeakList alignedPeakList2 = new SimplePeakList(peakListName,
					allDataFiles.toArray(new RawDataFile[0]));

			// for each possible aligned pair of peaks in all the samples
			for (Vector<AlignStructMol> list : lists) {
				for (AlignStructMol mol : list) {

					// check if the rows are already in the new peak list
					PeakListRow row1 = alignedPeakList2.getPeakRow(mol.row1.getPeaks()[0]);
					PeakListRow row2 = alignedPeakList2.getPeakRow(mol.row2.getPeaks()[0]);

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
							alignedPeakList2.addRow(row3);

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
							alignedPeakList2.addRow(row3);
						}
						if (row2 == null) {
							PeakListRow row3 = new SimplePeakListRow(contID++);
							for (ChromatographicPeak peak : mol.row2.getPeaks()) {
								row3.addPeak(peak.getDataFile(), peak);
							}
							alignedPeakList2.addRow(row3);
						}
					}
					processedRows++;
				}
			}

			fixAlignment(alignedPeakList2);
			return alignedPeakList2;
		} catch (Exception e) {
			return null;

		}
	}

	/**
	 * Find the rows where are repeats peaks and call the function "removeRepeatedRows()"
	 * @param peakList
	 */
	private void fixAlignment(PeakList peakList) {
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
		for (PeakListRow row2 : peakList.getRows()) {
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
		for (PeakListRow row2 : peakList.getRows()) {
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
