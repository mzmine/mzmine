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
package net.sf.mzmine.modules.identification.relatedpeaks;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.logging.Logger;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.data.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.main.mzmineclient.MZmineCore;
import net.sf.mzmine.project.ProjectEvent;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskStatus;

public class RelatedPeaksSearchTask implements Task {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private TaskStatus status = TaskStatus.WAITING;
	private String errorMessage;

	private int finishedRows, totalRows;
	private PeakList peakList;
	private RawDataFile dataFile;

	private double shapeTolerance, rtTolerance, sharingPoints;
	private RelatedPeaksSearchParameters parameters;

	/**
	 * @param parameters
	 * @param peakList
	 */
	public RelatedPeaksSearchTask(RelatedPeaksSearchParameters parameters,
			PeakList peakList) {

		this.peakList = peakList;
		this.parameters = parameters;
		this.dataFile = peakList.getRawDataFile(0);

		shapeTolerance = (Double) parameters
				.getParameterValue(RelatedPeaksSearchParameters.shapeTolerance);
		rtTolerance = (Double) parameters
				.getParameterValue(RelatedPeaksSearchParameters.rtTolerance);
		sharingPoints = (Double) parameters
				.getParameterValue(RelatedPeaksSearchParameters.sharingPoints);

	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#cancel()
	 */
	public void cancel() {
		status = TaskStatus.CANCELED;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getErrorMessage()
	 */
	public String getErrorMessage() {
		return errorMessage;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
	 */
	public double getFinishedPercentage() {
		if (totalRows == 0)
			return 0;
		return ((double) finishedRows) / totalRows;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getStatus()
	 */
	public TaskStatus getStatus() {
		return status;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
	 */
	public String getTaskDescription() {
		return "Identification of related peaks in " + peakList.toString();
	}

	/**
	 * @see java.lang.Runnable#run()
	 */
	public void run() {

		status = TaskStatus.PROCESSING;

		logger.info("Running related peaks search in " + peakList);

		PeakListRow rows[] = peakList.getRows();
		totalRows = rows.length;

		// Compare each two rows against each other
		for (int i = 0; i < totalRows; i++) {

			ChromatographicPeak peak1 = rows[i].getPeak(dataFile);

			for (int j = i + 1; j < rows.length; j++) {

				if (status == TaskStatus.CANCELED) {
					return;
				}

				ChromatographicPeak peak2 = rows[j].getPeak(dataFile);

				if (areRelatedPeaks(peak1, peak2)) {

					RelatedPeakIdentity identity1 = new RelatedPeakIdentity(
							rows[i], rows[j]);
					rows[j].addPeakIdentity(identity1, false);

					RelatedPeakIdentity identity2 = new RelatedPeakIdentity(
							rows[j], rows[i]);
					rows[i].addPeakIdentity(identity2, false);

				}

			}

			finishedRows++;

		}

		// Add task description to peakList
		((SimplePeakList) peakList)
				.addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod(
						"Identification of related peaks", parameters));

		// Notify the project manager that peaklist contents have changed
		MZmineCore.getProjectManager().fireProjectListeners(
				ProjectEvent.PEAKLIST_CONTENTS_CHANGED);
		
        // Notify the project manager that peaklist contents have changed
		MZmineCore.getProjectManager().fireProjectListeners(
				ProjectEvent.PEAKLIST_CONTENTS_CHANGED);

		status = TaskStatus.FINISHED;

		logger.info("Finished related peaks search in " + peakList);

	}

	/**
	 * This method defines if the two peaks received as parameters are related
	 * each other according the tolerance parameters. Return a boolean value.
	 * 
	 * @param peak1
	 * @param peak2
	 * @param shapeTolerance
	 * @param rtTolerance
	 * @param sharingPoints
	 * @return boolean
	 */
	private boolean areRelatedPeaks(ChromatographicPeak p1,
			ChromatographicPeak p2) {

		// Verify proximity in retention time axis
		double diffRT = Math.abs(p1.getRT() - p2.getRT());
		if (diffRT > rtTolerance) {
			return false;
		}

		// Verify % of sharing points
		if (!hasSharedPoints(p1, p2)) {
			return false;
		}

		// Verify proximity in term of shape form
		Hashtable<Integer, Double> shapePoints1 = getNormalizedShapePoints(p1);
		Hashtable<Integer, Double> shapePoints2 = getNormalizedShapePoints(p2);

		int key, comparedPoints = 0;
		double intPoint1, intPoint2, totalDiff = 0;

		Iterator itr = shapePoints1.keySet().iterator();
		while (itr.hasNext()) {
			key = (Integer) itr.next();
			intPoint1 = shapePoints1.get(key);
			try {
				intPoint2 = shapePoints2.get(key);
				totalDiff += Math.abs(intPoint1 - intPoint2);
			} catch (Exception e) {
				totalDiff += intPoint1;
			}
			comparedPoints++;
		}

		totalDiff /= comparedPoints;

		if (totalDiff > shapeTolerance) {
			return false;
		}

		return true;
	}

	/**
	 * Verify the percentage of scans in common between two peaks. Returns a
	 * boolean value depending on tolerance parameter
	 * 
	 * @param p1
	 * @param p2
	 * @param sharingPoints
	 * @return boolean
	 */
	private boolean hasSharedPoints(ChromatographicPeak p1,
			ChromatographicPeak p2) {

		int numOfHits = 0;
		double sharedPercentage = 0;
		int[] scansNumbersPeak1 = p1.getScanNumbers();
		int[] scansNumbersPeak2 = p2.getScanNumbers();
		int length1 = scansNumbersPeak1.length;
		int length2 = scansNumbersPeak2.length;

		// Count the number of scans in comon
		for (int scanP1 : scansNumbersPeak1) {
			for (int scanP2 : scansNumbersPeak2) {
				if (scanP1 == scanP2) {
					numOfHits++;
					break;
				}
			}
		}

		// Always use the peak with less number of scans to set the percentage
		// of shared points
		if (length1 > length2) {
			sharedPercentage = numOfHits / (double) length2;
		} else {
			sharedPercentage = numOfHits / (double) length1;
		}

		if (sharedPercentage >= sharingPoints) {
			return true;
		}

		return false;
	}

	/**
	 * This method returns a Hastable that contains the number of scan as key
	 * and the normalized intensity as value. The intensity is normalized
	 * according the biggest intensity in the peak.
	 * 
	 * @param peak
	 * @return Hashtable<Integer, Double>
	 */
	private Hashtable<Integer, Double> getNormalizedShapePoints(
			ChromatographicPeak peak) {

		double biggestIntensity = Double.MIN_VALUE;
		int scanNumbers[] = peak.getScanNumbers();
		Hashtable<Integer, Double> shapePoints = new Hashtable<Integer, Double>();

		double[] intensities = new double[scanNumbers.length];

		// Get the shape in terms of intensity
		for (int i = 0; i < scanNumbers.length; i++) {
			DataPoint dataPoint = peak.getDataPoint(scanNumbers[i]);
			if (dataPoint == null) {
				intensities[i] = 0;
			} else {
				intensities[i] = dataPoint.getIntensity();
			}

			if (intensities[i] > biggestIntensity) {
				biggestIntensity = intensities[i];
			}
		}

		// Normalize all intensities according to biggest intensity point
		for (int i = 0; i < scanNumbers.length; i++) {
			intensities[i] /= biggestIntensity;
			shapePoints.put(scanNumbers[i], intensities[i]);
		}

		return shapePoints;

	}
}
