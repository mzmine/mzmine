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
package net.sf.mzmine.modules.gapfilling.peakfinderRansac;


import java.util.Vector;
import java.util.logging.Logger;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.PeakIdentity;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.data.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.data.impl.SimplePeakListRow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.alignment.ransac.AlignStructMol;
import net.sf.mzmine.modules.alignment.ransac.RANSAC;
import net.sf.mzmine.modules.alignment.ransac.RansacAlignerParameters;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.Range;
import org.apache.commons.math.stat.regression.SimpleRegression;

class PeakFinderTask implements Task {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	private TaskStatus status = TaskStatus.WAITING;
	private String errorMessage;
	private PeakList peakList,  processedPeakList;
	private String suffix;
	private double intTolerance,  mzTolerance;
	private boolean rtToleranceUseAbs;
	private double rtToleranceValueAbs,  rtToleranceValuePercent;
	private PeakFinderParameters parameters;
	private int processedScans,  totalScans;	
	

	PeakFinderTask(PeakList peakList, PeakFinderParameters parameters) {

		this.peakList = peakList;
		this.parameters = parameters;

		suffix = (String) parameters.getParameterValue(PeakFinderParameters.suffix);
		intTolerance = (Double) parameters.getParameterValue(PeakFinderParameters.intTolerance);
		mzTolerance = (Double) parameters.getParameterValue(PeakFinderParameters.MZTolerance);
		if (parameters.getParameterValue(PeakFinderParameters.RTToleranceType) == PeakFinderParameters.RTToleranceTypeAbsolute) {
			rtToleranceUseAbs = true;
		}
		rtToleranceValueAbs = (Double) parameters.getParameterValue(PeakFinderParameters.RTToleranceValueAbs);
		rtToleranceValuePercent = (Double) parameters.getParameterValue(PeakFinderParameters.RTToleranceValuePercent);
	}

	

	public void run() {

		status = TaskStatus.PROCESSING;
		logger.info("Running Ransac gap filler on " + peakList);

		// Calculate total number of scans in all files
		for (RawDataFile dataFile : peakList.getRawDataFiles()) {
			totalScans += dataFile.getNumOfScans(1);
		}

		// Create new peak list
		processedPeakList = new SimplePeakList(peakList + " " + suffix,
				peakList.getRawDataFiles());

		// Fill new peak list with empty rows
		for (int row = 0; row < peakList.getNumberOfRows(); row++) {
			PeakListRow sourceRow = peakList.getRow(row);
			PeakListRow newRow = new SimplePeakListRow(sourceRow.getID());
			newRow.setComment(sourceRow.getComment());
			for (PeakIdentity ident : sourceRow.getPeakIdentities()) {
				newRow.addPeakIdentity(ident, false);
			}
			if (sourceRow.getPreferredPeakIdentity() != null) {
				newRow.setPreferredPeakIdentity(sourceRow.getPreferredPeakIdentity());
			}
			processedPeakList.addRow(newRow);
		}
		

		//RANSAC parameters
		RansacAlignerParameters parametersRansac = new RansacAlignerParameters();
		parametersRansac.setParameterValue(RansacAlignerParameters.Iterations, parameters.getParameterValue(PeakFinderParameters.Iterations));
		parametersRansac.setParameterValue(RansacAlignerParameters.Margin, parameters.getParameterValue(PeakFinderParameters.Margin));
		parametersRansac.setParameterValue(RansacAlignerParameters.NMinPoints, parameters.getParameterValue(PeakFinderParameters.NMinPoints));
		parametersRansac.setParameterValue(RansacAlignerParameters.curve, parameters.getParameterValue(PeakFinderParameters.curve));

		//RANSAC algorithm for all the samples
		Vector<RegressionInfo> regressionInfo = new Vector<RegressionInfo>();
		RawDataFile[] datafiles = peakList.getRawDataFiles();

		for (int i = 0; i < datafiles.length; i++) {
			for (int e = i + 1; e < datafiles.length; e++) {
				Vector<AlignStructMol>list = this.getVectorAlignment(peakList.getPeaks(datafiles[i]), peakList.getPeaks(datafiles[e]));
				RANSAC ransac = new RANSAC(parametersRansac);
				ransac.alignment(list);

				SimpleRegression regression = new SimpleRegression();				
				for (AlignStructMol mols : list) {
					if (mols.Aligned) {
						regression.addData(mols.RT, mols.RT2);
					}
				}		
				
				regressionInfo.add(new RegressionInfo(regression.getSlope(), regression.getIntercept(), datafiles[i], datafiles[e]));
			}
		}

		// Process all raw data files
		for (RawDataFile dataFile : peakList.getRawDataFiles()) {

			// Canceled?
			if (status == TaskStatus.CANCELED) {
				return;
			}

			Vector<Gap> gaps = new Vector<Gap>();

			// Fill each row of this raw data file column, create new empty gaps
			// if necessary
			for (int row = 0; row < peakList.getNumberOfRows(); row++) {
				PeakListRow sourceRow = peakList.getRow(row);
				PeakListRow newRow = processedPeakList.getRow(row);

				ChromatographicPeak sourcePeak = sourceRow.getPeak(dataFile);

				if (sourcePeak == null) {

					// Create a new gap

					double mz = sourceRow.getAverageMZ();
					double rt = this.getRealRT(regressionInfo, dataFile, sourceRow);
					double rtTolerance = 0;
					if (rtToleranceUseAbs) {
						rtTolerance = rtToleranceValueAbs;
					} else {
						rtTolerance = rt * rtToleranceValuePercent;
					}



					Gap newGap = new Gap(newRow, dataFile, mz, rt,
							intTolerance, mzTolerance, rtTolerance);

					gaps.add(newGap);

				} else {
					newRow.addPeak(dataFile, sourcePeak);
				}

			}

			// Stop processing this file if there are no gaps
			if (gaps.size() == 0) {
				processedScans += dataFile.getNumOfScans();
				continue;
			}

			// Get all scans of this data file
			int scanNumbers[] = dataFile.getScanNumbers(1);

			// Process each scan
			for (int scanNumber : scanNumbers) {

				// Canceled?
				if (status == TaskStatus.CANCELED) {
					return;
				}

				// Get the scan
				Scan scan = dataFile.getScan(scanNumber);

				// Feed this scan to all gaps
				for (Gap gap : gaps) {
					gap.offerNextScan(scan);
				}

				processedScans++;
			}

			// Finalize gaps
			for (Gap gap : gaps) {
				gap.noMoreOffers();
			}

		}

		// Append processed peak list to the project
		MZmineProject currentProject = MZmineCore.getCurrentProject();
		currentProject.addPeakList(processedPeakList);

		// Add task description to peakList
		processedPeakList.addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod(
				"Ransac Gap filling ", parameters));

		logger.info("Finished Ransac gap-filling on " + peakList);
		status = TaskStatus.FINISHED;

	}

	/**
	 * Create the vector which contains all the possible aligned peaks.
	 * @return vector which contains all the possible aligned peaks.
	 */
	private Vector<AlignStructMol> getVectorAlignment(ChromatographicPeak[] peaksX, ChromatographicPeak[] peaksY) {

		Vector<AlignStructMol> alignMol = new Vector<AlignStructMol>();

		for (ChromatographicPeak row : peaksX) {

			if (status == TaskStatus.CANCELED) {
				return null;
			}

			// Calculate limits for a row with which the row can be aligned
			double mzMin = row.getMZ() - mzTolerance;
			double mzMax = row.getMZ() + mzTolerance;
			double rtMin, rtMax;
			double rtToleranceValue = rtToleranceValueAbs;
			rtMin = row.getRT() - rtToleranceValue;
			rtMax = row.getRT() + rtToleranceValue;
			Range mzRange = new Range(mzMin, mzMax);
			Range rtRange = new Range(rtMin, rtMax);
			for (ChromatographicPeak candidateRow : peaksY) {
				if (mzRange.contains(candidateRow.getMZ()) && rtRange.contains(candidateRow.getRT())) {
					alignMol.addElement(new AlignStructMol(row, candidateRow));
				}
			}

		}
		return alignMol;
	}

	/**
	 * Return the retention time where the peak must be based on the ransac 
	 * alignment of all the samples.	
	 */
	public double getRealRT(Vector<RegressionInfo> regressionInfo, RawDataFile rawDataFile, PeakListRow row) {
		double bestY = 0;
		int cont = 0;
		for (RegressionInfo rinfo : regressionInfo) {
			if (rinfo.getRawDataFile1() == rawDataFile) {
				try {

					double RTX = row.getPeak(rinfo.getRawDataFile2()).getRT();
					double y = (RTX - rinfo.getIntercept())/rinfo.getSlope();
					if (y > 0 &&  rinfo.getSlope() > 0) {
						bestY += y;
						cont++;
					}

				} catch (Exception exception) {
				}
			}
			if (rinfo.getRawDataFile2() == rawDataFile) {

				try {

					double RTX = row.getPeak(rinfo.getRawDataFile1()).getRT();
					double y = rinfo.getIntercept() + (RTX * rinfo.getSlope());
					if (y > 0) {
						bestY += y;
						cont++;
					}
				} catch (Exception exception) {
				}
			}

		}

		return bestY / cont;

	}

	public void cancel() {
		status = TaskStatus.CANCELED;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public double getFinishedPercentage() {
		if (totalScans == 0) {
			return 0;
		}
		return (double) processedScans / (double) totalScans;

	}

	public TaskStatus getStatus() {
		return status;
	}

	public String getTaskDescription() {
		return "Ransac Gap filling " + peakList;
	}

	PeakList getPeakList() {
		return peakList;
	}

	public Object[] getCreatedObjects() {
		return new Object[]{processedPeakList};
	}	
}
