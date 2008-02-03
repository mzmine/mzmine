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

package net.sf.mzmine.modules.peakpicking.anothercentroid;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peakpicking.anothercentroid.DataPointSorter.SortingDirection;
import net.sf.mzmine.modules.peakpicking.anothercentroid.DataPointSorter.SortingProperty;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.Task;

/**
 * 
 */
class AnotherCentroidPickerTask implements Task {

	private static final float isotopeDistance = 1f;

	private RawDataFile dataFile;

	private TaskStatus status = TaskStatus.WAITING;
	private String errorMessage;

	// scan counter
	private int processedScans, totalScans;

	// parameter values
	private String suffix;
	private float noiseLevel;
	private float mzTolerance;
	private float minimumPeakDuration, maximumPeakDuration;
	private int maximumChargeState, minimumNumberOfIsotopicPeaks;

	// peak id counter
	private int newPeakID = 1;

	/**
	 * @param rawDataFile
	 * @param parameters
	 */
	AnotherCentroidPickerTask(RawDataFile dataFile,
			AnotherCentroidPickerParameters parameters) {

		this.dataFile = dataFile;

		// Get parameter values for easier use
		suffix = (String) parameters
				.getParameterValue(AnotherCentroidPickerParameters.suffix);
		noiseLevel = (Float) parameters
				.getParameterValue(AnotherCentroidPickerParameters.noiseLevel);
		minimumPeakDuration = (Float) parameters
				.getParameterValue(AnotherCentroidPickerParameters.minimumPeakDuration);
		maximumPeakDuration = (Float) parameters
				.getParameterValue(AnotherCentroidPickerParameters.maximumPeakDuration);
		mzTolerance = (Float) parameters
				.getParameterValue(AnotherCentroidPickerParameters.mzTolerance);
		maximumChargeState = (Integer) parameters
				.getParameterValue(AnotherCentroidPickerParameters.maximumChargeState);
		minimumNumberOfIsotopicPeaks = (Integer) parameters
				.getParameterValue(AnotherCentroidPickerParameters.minimumNumberOfIsotopicPeaks);

	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
	 */
	public String getTaskDescription() {
		return "Another centroid peak detection on " + dataFile;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
	 */
	public float getFinishedPercentage() {
		if (totalScans == 0)
			return 0.0f;
		return (float) processedScans / (float) (3 * totalScans);
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

	public RawDataFile getDataFile() {
		return dataFile;
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

		// Create new peak list
		SimplePeakList newPeakList = new SimplePeakList(
				dataFile + " " + suffix, dataFile);

		// Get all scans of MS level 1
		int[] scanNumbers = dataFile.getScanNumbers(1);
		totalScans = scanNumbers.length;

		// 1st pass: find isotope patterns in scans
		for (int i = 0; i < totalScans; i++) {

			if (status == TaskStatus.CANCELED)
				return;

			Scan sc = dataFile.getScan(scanNumbers[i]);

			OneDimIsotopePattern[] detectedOneDPatterns = detectPatterns(sc);
			System.out
					.println("Number of detected isotope pattern in scan "
							+ sc.getScanNumber() + " is "
							+ detectedOneDPatterns.length);

			// Store only new patterns

			processedScans++;

		}

		// 2nd pass: calc XIC for each unique pattern
		for (int i = 0; i < totalScans; i++) {

			if (status == TaskStatus.CANCELED)
				return;

			processedScans++;
		}

		// 3rd pass: collect datapoints
		for (int i = 0; i < totalScans; i++) {

			if (status == TaskStatus.CANCELED)
				return;

			processedScans++;
		}

		// Add new peaklist to the project
		MZmineProject currentProject = MZmineCore.getCurrentProject();
		currentProject.addPeakList(newPeakList);

		status = TaskStatus.FINISHED;

	}

	private OneDimIsotopePattern[] detectPatterns(Scan sc) {
		Vector<OneDimIsotopePattern> detectedPatterns = new Vector<OneDimIsotopePattern>();

		// Filter away centroids below noise level
		DataPoint allCentroids[] = sc.getDataPoints();
		Vector<DataPoint> filteredCentroids = new Vector<DataPoint>();
		for (DataPoint centroid : allCentroids) {
			if (centroid.getIntensity() > noiseLevel)
				filteredCentroids.add(centroid);
		}

		// Sort centroids by ascending m/z
		DataPoint sortedCentroids[] = filteredCentroids
				.toArray(new DataPoint[0]);
		Arrays.sort(sortedCentroids, new DataPointSorter(SortingProperty.MZ,
				SortingDirection.ASCENDING));

		// Consider each centroids as monoisotopic centroid of a isotope pattern
		for (int monoCentroidIndex = 0; monoCentroidIndex < sortedCentroids.length; monoCentroidIndex++) {

			DataPoint monoCentroid = sortedCentroids[monoCentroidIndex];

			// If null, then this centroid has been assigned to another pattern
			// already, and should not be considered anymore
			if (monoCentroid == null)
				continue;

			// Test isotope patterns of different charge  starting from the
			// monoisotopic centroid, and select the isotope pattern with most matching centroids
			Hashtable<Integer, DataPoint> bestPattern = null;
			for (int chargeState = 1; chargeState < maximumChargeState; chargeState++) {

				// Collect as many matching centroids as possible
				Hashtable<Integer, DataPoint> collectedCentroids = new Hashtable<Integer, DataPoint>();
				collectedCentroids.put(monoCentroidIndex, monoCentroid);
				for (int otherCentroidIndex = monoCentroidIndex + 1; otherCentroidIndex < sortedCentroids.length; otherCentroidIndex++) {
					DataPoint otherCentroid = sortedCentroids[otherCentroidIndex];
					if (otherCentroid == null)
						continue;

					// Check if m/z difference from this centroid to
					// monoisotopic centroid is within tolerances
					float minMZRange = monoCentroid.getMZ() + isotopeDistance
							* collectedCentroids.size() - mzTolerance;
					float maxMZRange = monoCentroid.getMZ() + isotopeDistance
							* collectedCentroids.size() + mzTolerance;

					if (otherCentroid.getMZ() < minMZRange)
						continue;
					if (otherCentroid.getMZ() > maxMZRange)
						break;

					// TODO: Add other criteria for selecting the centroid
					// (intensity shape of forming pattern, compare possible
					// multiple matches within tolerance, ...)
					collectedCentroids.put(otherCentroidIndex,
							otherCentroid);

				}

				if ( (bestPattern==null) || (bestPattern.size() < collectedCentroids.size()) ) 
					bestPattern = collectedCentroids;

			}
			
			// If best pattern is good enough then keep it and remove all
			// participating centroids from further consideration
			if (bestPattern.size()>=minimumNumberOfIsotopicPeaks) {
				OneDimIsotopePattern pattern = new OneDimIsotopePattern();
				
				Enumeration<Integer> centroidIndices = bestPattern.keys();
				while(centroidIndices.hasMoreElements()) {
					Integer centroidIndex = centroidIndices.nextElement();
					DataPoint centroid = bestPattern.get(centroidIndex);
					pattern.addDataPoint(centroid);
					sortedCentroids[centroidIndex] = null;
				}
				
				detectedPatterns.add(pattern);
				
			}

		}

		return detectedPatterns.toArray(new OneDimIsotopePattern[0]);

	}
	/*
	 * private OneDimIsotopePattern[] detectPatterns(Scan sc) { Vector<OneDimIsotopePattern>
	 * detectedPatterns = new Vector<OneDimIsotopePattern>();
	 * 
	 * DataPoint sortedDataPoints[] = sc.getDataPoints();
	 * 
	 * Arrays.sort(sortedDataPoints, new
	 * DataPointSorterByDescendingIntensity()); // Continue until all possible
	 * isotope patterns have been found int highestIntensityIndex = 0; while
	 * (true) { // Find strongest remaining centroid and start constructing an //
	 * isotope pattern around it while ((highestIntensityIndex <
	 * sortedDataPoints.length) && (sortedDataPoints[highestIntensityIndex] ==
	 * null)) { highestIntensityIndex++; } if (highestIntensityIndex >=
	 * sortedDataPoints.length) break; if
	 * (sortedDataPoints[highestIntensityIndex] == null) break; DataPoint
	 * highestIntensityDataPoint = sortedDataPoints[highestIntensityIndex];
	 * OneDimIsotopePattern trialPattern = new OneDimIsotopePattern(
	 * highestIntensityDataPoint); sortedDataPoints[highestIntensityIndex] =
	 * null; // Search for other centroids in the same pattern for (int
	 * chargeState = 1; chargeState <= maximumChargeState; chargeState++) { //
	 * Find matching centroids with lower m/z int lower = 1; boolean foundLower =
	 * true; while (foundLower) { foundLower = false;
	 * 
	 * for (int i = 0; i < sortedDataPoints.length; i++) { DataPoint
	 * anotherDataPoint = sortedDataPoints[i]; if (anotherDataPoint == null)
	 * continue;
	 * 
	 * double mzDiff = highestIntensityDataPoint.getMZ() -
	 * anotherDataPoint.getMZ(); double expectedMZDiff = (isotopeDistance /
	 * (float) chargeState) (float) lower; if (Math.abs(mzDiff - expectedMZDiff) <
	 * mzTolerance) { trialPattern.addDataPoint(anotherDataPoint);
	 * sortedDataPoints[i] = null; foundLower = true; break; } }
	 * 
	 * lower++; } // Find matching centroids with higher m/z int higher = 1;
	 * boolean foundHigher = true; while (foundHigher) { foundHigher = false;
	 * 
	 * for (int i = 0; i < sortedDataPoints.length; i++) { DataPoint
	 * anotherDataPoint = sortedDataPoints[i]; if (anotherDataPoint == null)
	 * continue;
	 * 
	 * double mzDiff = anotherDataPoint.getMZ() -
	 * highestIntensityDataPoint.getMZ(); double expectedMZDiff =
	 * (isotopeDistance / (float) chargeState) (float) higher; if
	 * (Math.abs(mzDiff - expectedMZDiff) < mzTolerance) {
	 * trialPattern.addDataPoint(anotherDataPoint); sortedDataPoints[i] = null;
	 * foundHigher = true; break; } }
	 * 
	 * higher++; } } // If enough centroids in the pattern, then add this to
	 * detected // patterns if (trialPattern.getNumberOfDataPoints() >=
	 * minimumNumberOfIsotopicPeaks) { detectedPatterns.add(trialPattern); } }
	 * 
	 * return detectedPatterns.toArray(new OneDimIsotopePattern[0]); }
	 */
}
