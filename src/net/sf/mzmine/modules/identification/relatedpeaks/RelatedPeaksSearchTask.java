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

package net.sf.mzmine.modules.identification.relatedpeaks;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.CompoundIdentity;
import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RelatedPeaksIdentity;
import net.sf.mzmine.data.impl.SimpleCompoundIdentity;
import net.sf.mzmine.data.impl.SimpleRelatedPeaksIdentity;
import net.sf.mzmine.taskcontrol.Task;

public class RelatedPeaksSearchTask implements Task {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	private TaskStatus status = TaskStatus.WAITING;
	private String errorMessage;
	private int finishedRows = 0, numRows;
	private PeakList peakList;
	private int numOfGroups;
	private double shapeTolerance, rtTolerance, sharingPoints;

	/**
	 * @param parameters
	 * @param peakList
	 */
	public RelatedPeaksSearchTask(RelatedPeaksSearchParameters parameters,
			PeakList peakList) {

		this.peakList = peakList;
		numRows = peakList.getNumberOfRows();
		numOfGroups = 0;
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
		return ((double) finishedRows) / numRows;
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
		return "Identification of related peaks throw " + peakList.toString();
	}

	/**
	 * @see java.lang.Runnable#run()
	 */
	public void run() {

		status = TaskStatus.PROCESSING;

		Iterator itr;
		PeakListRow comparedRow;
		ChromatographicPeak currentPeak, comparedPeak;
		CompoundIdentity identity;
		boolean alreadyRelated = false, goodCandidate = false;
		RelatedPeaksIdentity currentGroup = null;
		Vector<RelatedPeaksIdentity> relatedPeaksGroups = new Vector<RelatedPeaksIdentity>();
		HashSet<PeakListRow> comparissonPeaks = new HashSet<PeakListRow>(Arrays
				.asList(peakList.getRows()));

		for (PeakListRow currentRow : peakList.getRows()) {

			if (status == TaskStatus.CANCELED)
				return;

			// Verify if the current row already belongs to one group
			for (RelatedPeaksIdentity group : relatedPeaksGroups) {
				if (group.containsRow(currentRow)) {
					alreadyRelated = true;
					break;
				}
			}

			if (alreadyRelated) {
				alreadyRelated = false;
				continue;
			}

			// Get the biggest peak in the row for comparison
			currentPeak = getBiggestPeak(currentRow);

			currentGroup = null;
			itr = comparissonPeaks.iterator();

			// Compare the current row against the complete peak list over the
			// same raw data file
			while (itr.hasNext()) {

				comparedRow = (PeakListRow) itr.next();
				// Always the comparison is against peaks from the same raw data
				comparedPeak = comparedRow.getPeak(currentPeak.getDataFile());

				// Verify if there is not peak in the same retention time in
				// this row
				if (comparedPeak == null) {
					continue;
				}

				// Avoid compare the same peak
				if (currentPeak == comparedPeak) {
					continue;
				}

				// Verify if the compared peak is related to the current peak
				goodCandidate = areRelatedPeaks(currentPeak, comparedPeak,
						shapeTolerance, rtTolerance, sharingPoints);

				if (goodCandidate) {

					goodCandidate = false;
					alreadyRelated = false;

					// If the current peak already belongs to one group, add the
					// compared peak to that group
					if (currentGroup != null) {
						currentGroup.addRow(comparedRow);
						identity = new SimpleCompoundIdentity(null,
								currentGroup.getGroupName(), null, null, null,
								"Related peak search", null);
						comparedRow.addCompoundIdentity(identity, true);
						alreadyRelated = true;
						continue;
					}

					// If the compared peak belongs to any group, add the
					// current peak to that group
					for (RelatedPeaksIdentity group : relatedPeaksGroups) {
						if (group.containsRow(comparedRow)) {
							group.addRow(currentRow);
							identity = new SimpleCompoundIdentity(null, group
									.getGroupName(), null, null, null,
									"Related peak search", null);
							currentRow.addCompoundIdentity(identity, true);
							currentGroup = group;
							alreadyRelated = true;
							break;
						}
					}

					// If the current peak doesn't belong to any group neither
					// the compared, then initializes a new group
					if (alreadyRelated) {
						alreadyRelated = false;
					} else {
						String name = "Group" + numOfGroups;
						identity = new SimpleCompoundIdentity(null, name, null,
								null, null, "Related peak search", null);
						comparedRow.addCompoundIdentity(identity, true);
						currentRow.addCompoundIdentity(identity, true);
						currentGroup = new SimpleRelatedPeaksIdentity(name,
								currentRow, comparedRow);
						relatedPeaksGroups.add(currentGroup);
						numOfGroups++;

					}

				}

			}

			finishedRows++;

		}

		status = TaskStatus.FINISHED;

	}

	/**
	 * Retrieve the biggest peak of the peak list row parameter
	 * 
	 * @param row
	 * @return
	 */
	private static ChromatographicPeak getBiggestPeak(PeakListRow row) {
		ChromatographicPeak peak = null;

		double intensity = Double.MIN_NORMAL;

		for (ChromatographicPeak p : row.getPeaks()) {
			if (p.getHeight() > intensity) {
				peak = p;
				intensity = p.getHeight();
			}
		}

		return peak;
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
	private static boolean areRelatedPeaks(ChromatographicPeak p1,
			ChromatographicPeak p2, double shapeTolerance, double rtTolerance,
			double sharingPoints) {

		// Verify proximity in retention time axis
		double diffRT = Math.abs(p1.getRT() - p2.getRT());
		if (diffRT > rtTolerance)
			return false;

		// Verify % of sharing points
		if (!hasSharedPoints(p1, p2, sharingPoints))
			return false;

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
	private static boolean hasSharedPoints(ChromatographicPeak p1,
			ChromatographicPeak p2, double sharingPoints) {

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
	private static Hashtable<Integer, Double> getNormalizedShapePoints(
			ChromatographicPeak peak) {

		double biggestIntensity = Double.MIN_VALUE;
		int scanNumbers[] = peak.getScanNumbers();
		Hashtable<Integer, Double> shapePoints = new Hashtable<Integer, Double>();

		double[] intensities = new double[scanNumbers.length];

		// Get the shape in terms of intensity
		for (int i = 0; i < scanNumbers.length; i++) {
			DataPoint dataPoint = peak.getMzPeak(scanNumbers[i]);
			if (dataPoint == null)
				intensities[i] = 0;
			else
				intensities[i] = dataPoint.getIntensity();

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
