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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.MzDataPoint;
import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.PeakIdentity;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.GroupRelatedPeaks;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleGroupRelatedPeaks;
import net.sf.mzmine.taskcontrol.Task;

public class RelatedPeaksSearchTask implements Task {

	private TaskStatus status = TaskStatus.WAITING;
	private String errorMessage;
	private int finishedRows = 0, numRows;
	private PeakList peakList;
	private int numOfGroups;
	private double shapeTolerance, rtTolerance, mzAdductTolerance,
			sharingPoints;
	private SimpleAdduct[] selectedAdducts;

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

		Parameter p = parameters.getParameter("Adducts");
		Object[] objectArray = ((SimpleParameter) p)
				.getMultipleSelectedValues();
		int length = objectArray.length;

		double customMassDifference = (Double) parameters
				.getParameterValue(RelatedPeaksSearchParameters.customAdductValue);
		String customAdductName = (String) parameters
				.getParameterValue(RelatedPeaksSearchParameters.customAdductName);

		selectedAdducts = new SimpleAdduct[length];
		String name;
		double mass = 0;
		for (int i = 0; i < length; i++) {

			name = ((CommonAdducts) objectArray[i]).getName();

			if (name.equals("Custom")) {
				name = customAdductName;
				mass = customMassDifference;
			} else {
				mass = ((CommonAdducts) objectArray[i]).getMassDifference();
			}
			selectedAdducts[i] = new SimpleAdduct(name, mass);
		}

		mzAdductTolerance = (Double) parameters
				.getParameterValue(RelatedPeaksSearchParameters.mzAdductTolerance);

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
		PeakIdentity identity;
		boolean alreadyRelated = false, goodCandidate = false;
		GroupRelatedPeaks currentGroup = null;
		Vector<GroupRelatedPeaks> peaksGroups = new Vector<GroupRelatedPeaks>();
		HashSet<PeakListRow> comparissonPeaks = new HashSet<PeakListRow>(Arrays
				.asList(peakList.getRows()));

		for (PeakListRow currentRow : peakList.getRows()) {

			if (status == TaskStatus.CANCELED) {
				return;
			}

			// Verify if the current row already belongs to one group
			for (GroupRelatedPeaks group : peaksGroups) {
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
				if (comparedRow == null) {
					break;
				}

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

				// set the group of the peak looking the mass differences of
				// each selected adduct
				for (SimpleAdduct adduct : selectedAdducts) {
					// Verify if the compared peak is related to the current
					// peak
					goodCandidate = areRelatedPeaks(currentPeak, comparedPeak,
							shapeTolerance, adduct, rtTolerance,
							mzAdductTolerance, sharingPoints);

					if (goodCandidate) {

						goodCandidate = false;
						alreadyRelated = false;

						// If the current peak already belongs to one group, 
						// add the compared peak to that group
						if (currentGroup != null) {
							currentGroup.addRow(comparedRow);
							identity = new RelatedPeakIdentity(currentRow,
									comparedRow, adduct, currentGroup);
							comparedRow.addCompoundIdentity(identity, true);

							alreadyRelated = true;
							continue;
						}

						// If the compared peak belongs to any group, add the
						// current peak to that group

						for (GroupRelatedPeaks group : peaksGroups) {
							if (group.containsRow(comparedRow)) {
								group.addRow(currentRow);
								identity = new RelatedPeakIdentity(currentRow,
										comparedRow, adduct, group);
								currentRow.addCompoundIdentity(identity, true);
								currentGroup = group;

								alreadyRelated = true;
								break;
							}
						}

						// If the current peak doesn't belong to any group
						// neither
						// the compared, then initializes a new group
						if (alreadyRelated) {
							alreadyRelated = false;
						} else {
							String name = "Group " + numOfGroups;
							currentGroup = new SimpleGroupRelatedPeaks(name,
									currentRow, comparedRow);
							identity = new RelatedPeakIdentity(currentRow,
									comparedRow, adduct, currentGroup);
							comparedRow.addCompoundIdentity(identity, true);
							currentRow.addCompoundIdentity(identity, true);
							peaksGroups.add(currentGroup);
							numOfGroups++;
						}

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

		double intensity = Double.MIN_VALUE;

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
			ChromatographicPeak p2, double shapeTolerance, SimpleAdduct adduct,
			double rtTolerance, double mzAdductTolerance, double sharingPoints) {

		// Verify proximity in retention time axis
		double diffRT = Math.abs(p1.getRT() - p2.getRT());
		if (diffRT > rtTolerance) {
			return false;
		}

		// Verify the distance between peaks in m/z axis. This help to identify
		// false hits for adducts.
		double mzDistance = adduct.getMassDifference();
		double diffMZ = Math.abs(p1.getMZ() - p2.getMZ());
		if (!(adduct.getName().equals(CommonAdducts.ALLRELATED.getName()))
				&& (diffMZ > mzDistance + mzAdductTolerance || diffMZ < mzDistance
						- mzAdductTolerance)) {
			return false;
		}

		// Verify % of sharing points
		if (!hasSharedPoints(p1, p2, sharingPoints)) {
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
			MzDataPoint dataPoint = peak.getMzPeak(scanNumbers[i]);
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
