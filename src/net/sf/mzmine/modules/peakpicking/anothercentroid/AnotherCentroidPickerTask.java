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

import java.util.ArrayList;
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

		// 1st pass: find isotope patterns in each scan, collect unique patterns

		ArrayList<ConstructionIsotopePattern> allCollectedPatterns = new ArrayList<ConstructionIsotopePattern>();
		for (int i = 0; i < totalScans; i++) {

			if (status == TaskStatus.CANCELED)
				return;

			Scan sc = dataFile.getScan(scanNumbers[i]);

			ConstructionIsotopePattern[] patternsForScan = detectPatterns(sc);

			// Store only new patterns
			for (ConstructionIsotopePattern detectedPattern : patternsForScan) {
				boolean foundSimilar = false;
				for (ConstructionIsotopePattern previouslyCollectedPattern : allCollectedPatterns) {
					if (detectedPattern.isSimilar(previouslyCollectedPattern)) {
						foundSimilar = true;
						previouslyCollectedPattern.removeDataPoints();
						previouslyCollectedPattern
								.addDataPoints(detectedPattern.getDataPoints());
						break;
					}
				}

				if (!foundSimilar) {
					allCollectedPatterns.add(detectedPattern);
				}
			}

			processedScans++;

		}

		// 2nd pass: calc XIC for each unique pattern and find elution start and
		// stop times

		// Initialize XICs and get bin borders for each pattern
		ArrayList<Bin> binArray = new ArrayList<Bin>();
		for (ConstructionIsotopePattern pattern : allCollectedPatterns) {
			Bin[] binsForPattern = pattern.initializeXIC(totalScans);
			for (Bin bin : binsForPattern)
				binArray.add(bin);
		}
		Bin[] bins = binArray.toArray(new Bin[0]);

		// Collect XICs
		for (int i = 0; i < totalScans; i++) {

			if (status == TaskStatus.CANCELED)
				return;

			Scan sc = dataFile.getScan(scanNumbers[i]);
			binCentroids(sc, bins);
			for (Bin bin : bins)
				bin.moveToNextScan();
			processedScans++;
		}

		// TODO: Interpret XICs

		// DEBUG begin print out a list of collected patterns
		/*
		 * try { PrintWriter fout = new PrintWriter(new FileWriter(
		 * "D:/temp/detectedPatterns.txt"));
		 * 
		 * fout .println("Monoisotopic m/z\tPattern size\tCharge state\tAll
		 * m/zs"); for (ConstructionIsotopePattern pattern :
		 * allCollectedPatterns) { DataPoint[] dataPoints =
		 * pattern.getDataPoints(); DataPoint monoDataPoint = dataPoints[0];
		 * String allMZs = ""; for (DataPoint dataPoint : dataPoints) { if
		 * (monoDataPoint.getMZ() > dataPoint.getMZ()) { monoDataPoint =
		 * dataPoint; } allMZs += "" + dataPoint.getMZ() + "; "; }
		 * fout.println("" + monoDataPoint.getMZ() + "\t" +
		 * pattern.getNumberOfDataPoints() + "\t" + pattern.getChargeState() +
		 * "\t" + allMZs); } fout.flush(); fout.close(); } catch (IOException
		 * ex) { System.out.println("DEBUG: printing to file failed: " +
		 * ex.toString()); }
		 */
		// DEBUG end
		
		
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

	private ConstructionIsotopePattern[] detectPatterns(Scan sc) {
		Vector<ConstructionIsotopePattern> detectedPatterns = new Vector<ConstructionIsotopePattern>();

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

			// Test isotope patterns of different charge starting from the
			// monoisotopic centroid, and select the isotope pattern with most
			// matching centroids
			Hashtable<Integer, DataPoint> bestPattern = null;
			int bestChargeState = -1;
			for (int chargeState = 1; chargeState <= maximumChargeState; chargeState++) {

				// Initialize collection of centroids in this pattern with the
				// monoisotopic centroid
				Hashtable<Integer, DataPoint> collectedCentroids = new Hashtable<Integer, DataPoint>();
				collectedCentroids.put(monoCentroidIndex, monoCentroid);

				// Start search for other centroids from the next centroid after
				// monoisotopic centroid and continue as long as there are more
				// centroids available and previous centroid in the pattern was
				// found inside the acceptable m/z range
				int otherCentroidIndex = monoCentroidIndex + 1;
				boolean foundNext = true;
				while ((otherCentroidIndex < sortedCentroids.length)
						&& foundNext) {

					// Next peak in the pattern hasn't been found yet
					foundNext = false;

					// Calc expected m/z location and acceptable m/z range of
					// the next peak
					float minMZRange = monoCentroid.getMZ() + isotopeDistance
							* collectedCentroids.size() - mzTolerance;
					float maxMZRange = monoCentroid.getMZ() + isotopeDistance
							* collectedCentroids.size() + mzTolerance;
					float expectedMZ = monoCentroid.getMZ() + isotopeDistance
							* collectedCentroids.size();

					// There are no candidates for the next centroid in the
					// pattern yet
					float bestCandidateDistance = Float.MAX_VALUE;
					DataPoint bestCandidate = null;
					int bestCandidateIndex = -1;

					// Search for the next centroid as long as there are more
					// centroids available
					while ((otherCentroidIndex < sortedCentroids.length)) {

						DataPoint otherCentroid = sortedCentroids[otherCentroidIndex];

						// If this centroid has been already assigned to another
						// pattern, do not attempt to include it in this pattern
						if (otherCentroid == null) {
							otherCentroidIndex++;
							continue;
						}

						// Ignore this centroid if it is not yet inside m/z
						// range, and
						// stop searching if it is already past m/z range
						if (otherCentroid.getMZ() < minMZRange) {
							otherCentroidIndex++;
							continue;
						}
						if (otherCentroid.getMZ() > maxMZRange)
							break;

						// Check if this centroid is closer to the expected m/z
						// location than the previously found candidate
						if (Math.abs(otherCentroid.getMZ() - expectedMZ) < bestCandidateDistance) {
							bestCandidateDistance = Math.abs(otherCentroid
									.getMZ()
									- expectedMZ);

							bestCandidate = otherCentroid;
							bestCandidateIndex = otherCentroidIndex;

						}

						otherCentroidIndex++;
					}

					if (bestCandidate != null) {
						foundNext = true;
						collectedCentroids.put(bestCandidateIndex,
								bestCandidate);
						otherCentroidIndex = bestCandidateIndex + 1;
					}

				}

				if ((bestPattern == null)
						|| (bestPattern.size() < collectedCentroids.size())) {
					bestChargeState = chargeState;
					bestPattern = collectedCentroids;
				}

			}

			// If best pattern is good enough then keep it and remove all
			// participating centroids from further consideration
			if (bestPattern.size() >= minimumNumberOfIsotopicPeaks) {

				ConstructionIsotopePattern pattern = new ConstructionIsotopePattern(
						bestChargeState, mzTolerance);

				Enumeration<Integer> centroidIndices = bestPattern.keys();
				while (centroidIndices.hasMoreElements()) {
					Integer centroidIndex = centroidIndices.nextElement();
					DataPoint centroid = bestPattern.get(centroidIndex);
					pattern.addDataPoint(centroid);
					sortedCentroids[centroidIndex] = null;
				}

				detectedPatterns.add(pattern);

			}

		}

		return detectedPatterns.toArray(new ConstructionIsotopePattern[0]);

	}

	private void binCentroids(Scan sc, Bin[] bins) {
		// TODO !
	}

}
