/*
 * Copyright 2006-2010 The MZmine 2 Development Team
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
package net.sf.mzmine.modules.peaklistmethods.alignment.ransac;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Logger;
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
import net.sf.mzmine.util.PeakUtils;
import net.sf.mzmine.util.Range;
import org.apache.commons.math.ArgumentOutsideDomainException;
import org.apache.commons.math.analysis.interpolation.LoessInterpolator;
import org.apache.commons.math.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math.stat.regression.SimpleRegression;

class RansacAlignerTask implements Task {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	private PeakList peakLists[], alignedPeakList;
	private TaskStatus status = TaskStatus.WAITING;
	private String errorMessage;

	// Processed rows counter
	private int processedRows, totalRows;

	// Parameters
	private String peakListName;
	private double mzTolerance;
	private double rtTolerance;
	private RansacAlignerParameters parameters;
	private double rtToleranceValueAbs;
	private boolean sameChargeRequired;

	// ID counter for the new peaklist
	private int newRowID = 1;

	public RansacAlignerTask(PeakList[] peakLists,
			RansacAlignerParameters parameters) {

		this.peakLists = peakLists;
		this.parameters = parameters;

		// Get parameter values for easier use
		peakListName = (String) parameters
				.getParameterValue(RansacAlignerParameters.peakListName);

		mzTolerance = (Double) parameters
				.getParameterValue(RansacAlignerParameters.MZTolerance);

		rtTolerance = (Double) parameters
				.getParameterValue(RansacAlignerParameters.RTTolerance);

		rtToleranceValueAbs = (Double) parameters
				.getParameterValue(RansacAlignerParameters.RTToleranceValueAbs);

		sameChargeRequired = (Boolean) parameters
				.getParameterValue(RansacAlignerParameters.SameChargeRequired);

	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
	 */
	public String getTaskDescription() {
		return "Ransac aligner, " + peakListName + " (" + peakLists.length
				+ " peak lists)";
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

	public void run() {

		status = TaskStatus.PROCESSING;
		logger.info("Running Ransac aligner");

		// Remember how many rows we need to process. Each row will be processed
		// twice, first for score calculation, second for actual alignment.
		for (int i = 0; i < peakLists.length; i++) {
			totalRows += peakLists[i].getNumberOfRows() * 2;
		}

		// Collect all data files
		Vector<RawDataFile> allDataFiles = new Vector<RawDataFile>();

		for (PeakList peakList : peakLists) {

			for (RawDataFile dataFile : peakList.getRawDataFiles()) {

				// Each data file can only have one column in aligned peak list
				if (allDataFiles.contains(dataFile)) {
					status = TaskStatus.ERROR;
					errorMessage = "Cannot run alignment, because file "
							+ dataFile + " is present in multiple peak lists";
					return;
				}

				allDataFiles.add(dataFile);
			}
		}

		// Create a new aligned peak list
		alignedPeakList = new SimplePeakList(peakListName, allDataFiles
				.toArray(new RawDataFile[0]));

		// Iterate source peak lists
		for (PeakList peakList : peakLists) {

			Hashtable<PeakListRow, PeakListRow> alignmentMapping = this
					.getAlignmentMap(peakList);

			PeakListRow allRows[] = peakList.getRows();

			// Align all rows using mapping
			for (PeakListRow row : allRows) {
				PeakListRow targetRow = alignmentMapping.get(row);

				// If we have no mapping for this row, add a new one
				if (targetRow == null) {
					targetRow = new SimplePeakListRow(newRowID);
					newRowID++;
					alignedPeakList.addRow(targetRow);
				}

				// Add all peaks from the original row to the aligned row
				for (RawDataFile file : row.getRawDataFiles()) {
					targetRow.addPeak(file, row.getPeak(file));
				}

				// Add all non-existing identities from the original row to the
				// aligned row
				PeakUtils.copyPeakListRowProperties(row, targetRow);

				processedRows++;

			}

		} // Next peak list

		// Add new aligned peak list to the project
		MZmineProject currentProject = MZmineCore.getCurrentProject();
		currentProject.addPeakList(alignedPeakList);

		// Add task description to peakList
		alignedPeakList
				.addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod(
						"Ransac aligner", parameters));

		logger.info("Finished RANSAC aligner");
		status = TaskStatus.FINISHED;

	}

	/**
	 * 
	 * @param peakList
	 * @return
	 */
	private Hashtable<PeakListRow, PeakListRow> getAlignmentMap(
			PeakList peakList) {

		// Create a table of mappings for best scores
		Hashtable<PeakListRow, PeakListRow> alignmentMapping = new Hashtable<PeakListRow, PeakListRow>();

		if (alignedPeakList.getNumberOfRows() < 1) {
			return alignmentMapping;
		}

		// Create a sorted set of scores matching
		TreeSet<RowVsRowScore> scoreSet = new TreeSet<RowVsRowScore>();

		// RANSAC algorithm
		Vector<AlignStructMol> list = ransacPeakLists(alignedPeakList, peakList);
		PolynomialSplineFunction function = this.getPolynomialFunction(list, alignedPeakList.getRowsRTRange());

		PeakListRow allRows[] = peakList.getRows();

		for (PeakListRow row : allRows) {
			// Calculate limits for a row with which the row can be aligned
			double mzMin = row.getAverageMZ() - mzTolerance;
			double mzMax = row.getAverageMZ() + mzTolerance;
			double rtMin, rtMax;

			double rt;
			try {
				rt = function.value(row.getAverageRT());
			} catch (ArgumentOutsideDomainException ex) {
				rt = -1;
			}
			if ( Double.isNaN(rt) || rt == -1) {
				rt = row.getAverageRT();
			}

			double rtToleranceValue = 0.0f;
			rtToleranceValue = rtToleranceValueAbs;
			rtMin = rt - rtToleranceValue;
			rtMax = rt + rtToleranceValue;

			// Get all rows of the aligned peaklist within parameter limits
			PeakListRow candidateRows[] = alignedPeakList
					.getRowsInsideScanAndMZRange(new Range(rtMin, rtMax),
							new Range(mzMin, mzMax));

			for (PeakListRow candidate : candidateRows) {
				RowVsRowScore score;
				if (sameChargeRequired
						&& (!PeakUtils.compareChargeState(row, candidate)))
					continue;

				try {
					score = new RowVsRowScore(row, candidate, mzTolerance,
							rtToleranceValue, rt);

					scoreSet.add(score);
					errorMessage = score.getErrorMessage();

				} catch (Exception e) {
					e.printStackTrace();
					status = TaskStatus.ERROR;
					return null;
				}
			}
			processedRows++;
		}

		// Iterate scores by descending order
		Iterator<RowVsRowScore> scoreIterator = scoreSet.iterator();
		while (scoreIterator.hasNext()) {

			RowVsRowScore score = scoreIterator.next();

			// Check if the row is already mapped
			if (alignmentMapping.containsKey(score.getPeakListRow())) {
				continue;
			}

			// Check if the aligned row is already filled
			if (alignmentMapping.containsValue(score.getAlignedRow())) {
				continue;
			}

			alignmentMapping.put(score.getPeakListRow(), score.getAlignedRow());

		}

		return alignmentMapping;
	}

	/**
	 * RANSAC
	 * 
	 * @param alignedPeakList
	 * @param peakList
	 * @return
	 */
	private Vector<AlignStructMol> ransacPeakLists(PeakList alignedPeakList,
			PeakList peakList) {
		Vector<AlignStructMol> list = this.getVectorAlignment(alignedPeakList,
				peakList);
		RANSAC ransac = new RANSAC(parameters);
		ransac.alignment(list);
		return list;
	}

	/**
	 * Return the corrected RT of the row
	 * 
	 * @param row
	 * @param list
	 * @return
	 */
	private PolynomialSplineFunction getPolynomialFunction(Vector<AlignStructMol> list, Range RTrange) {
		List<RTs> data = new ArrayList<RTs>();
		for (AlignStructMol m : list) {
			if (m.Aligned) {
				data.add(new RTs(m.RT2, m.RT));
			}
		}

		data = this.smooth(data, RTrange);
		Collections.sort(data, new RTs());

		double[] xval = new double[data.size()];
		double[] yval = new double[data.size()];
		int i = 0;

		for (RTs rt : data) {
			xval[i] = rt.RT;
			yval[i++] = rt.RT2;
		}

		try {
			LoessInterpolator loess = new LoessInterpolator();
			return loess.interpolate(xval, yval);
		} catch (Exception ex) {
			return null;
		}
	}

	private class RTs implements Comparator {

		double RT;
		double RT2;
		int map;

		public RTs() {
		}

		public RTs(double RT, double RT2) {
			this.RT = RT + 0.001 / Math.random();
			this.RT2 = RT2 + 0.001 / Math.random();
		}

		public int compare(Object arg0, Object arg1) {
			if (((RTs) arg0).RT < ((RTs) arg1).RT) {
				return -1;
			} else {
				return 1;
			}

		}
	}

	private List<RTs> smooth(List<RTs> list) {
		Collections.sort(list, new RTs());
		for (int i = 0; i < list.size() - 1; i++) {
			RTs point1 = list.get(i);
			RTs point2 = list.get(i + 1);
			if (point1.RT < point2.RT - 5) {
				SimpleRegression regression = new SimpleRegression();
				regression.addData(point1.RT, point1.RT2);
				regression.addData(point2.RT, point2.RT2);
				double rt = point1.RT + 1;
				while (rt < point2.RT) {
					RTs newPoint = new RTs(rt, regression.predict(rt));
					list.add(newPoint);
					rt++;
				}

			}
		}

		return list;
	}

     private List<RTs> smooth(List<RTs> list, Range RTrange) {

        // Add one point at the begining and another at the end of the list to
        // ampliate the RT limits to cover the RT range completly
        try {
            Collections.sort(list, new RTs());

            RTs firstPoint = list.get(0);
            RTs lastPoint = list.get(list.size() - 1);

            double min = Math.abs(firstPoint.RT - RTrange.getMin());

            double RTx = firstPoint.RT - min;
            double RTy = firstPoint.RT2 - min;

            RTs newPoint = new RTs(RTx, RTy);
            list.add(newPoint);

            double max = Math.abs(RTrange.getMin() - lastPoint.RT);
            RTx = lastPoint.RT + max;
            RTy = lastPoint.RT2 + max;

            newPoint = new RTs(RTx, RTy);
            list.add(newPoint);
        } catch (Exception exception) {
        }

        // Add points to the model in between of the real points to smooth the regression model
        Collections.sort(list, new RTs());

        for (int i = 0; i < list.size() - 1; i++) {
            RTs point1 = list.get(i);
            RTs point2 = list.get(i + 1);
            if (point1.RT < point2.RT - 2) {
                SimpleRegression regression = new SimpleRegression();
                regression.addData(point1.RT, point1.RT2);
                regression.addData(point2.RT, point2.RT2);
                double rt = point1.RT + 1;
                while (rt < point2.RT) {
                    RTs newPoint = new RTs(rt, regression.predict(rt));
                    list.add(newPoint);
                    rt++;
                }

            }
        }

        return list;
    }


	/**
	 * Create the vector which contains all the possible aligned peaks.
	 * 
	 * @param peakListX
	 * @param peakListY
	 * @return vector which contains all the possible aligned peaks.
	 */
	private Vector<AlignStructMol> getVectorAlignment(PeakList peakListX,
			PeakList peakListY) {

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
			Range mzRange = new Range(mzMin, mzMax);
			Range rtRange = new Range(rtMin, rtMax);

			// Get all rows of the aligned peaklist within parameter limits
			PeakListRow candidateRows[] = peakListY
					.getRowsInsideScanAndMZRange(rtRange, mzRange);

			for (PeakListRow candidateRow : candidateRows) {
				alignMol.addElement(new AlignStructMol(row, candidateRow));
			}
		}

		return alignMol;
	}

	public Object[] getCreatedObjects() {
		return new Object[] { alignedPeakList };
	}
}
