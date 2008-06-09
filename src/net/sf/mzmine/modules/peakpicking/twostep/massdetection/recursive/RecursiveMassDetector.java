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

package net.sf.mzmine.modules.peakpicking.twostep.massdetection.recursive;

import java.util.Vector;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.modules.peakpicking.twostep.massdetection.MassDetector;
import net.sf.mzmine.modules.peakpicking.twostep.massdetection.MzPeak;

public class RecursiveMassDetector implements MassDetector {

	// parameter values
	private float minimumMZPeakWidth, maximumMZPeakWidth, noiseLevel;
	private Vector<MzPeak> mzPeaks;
	private DataPoint[] dataPoints;

	public RecursiveMassDetector(RecursiveMassDetectorParameters parameters) {
		noiseLevel = (Float) parameters
				.getParameterValue(RecursiveMassDetectorParameters.noiseLevel);
		minimumMZPeakWidth = (Float) parameters
				.getParameterValue(RecursiveMassDetectorParameters.minimumMZPeakWidth);
		maximumMZPeakWidth = (Float) parameters
				.getParameterValue(RecursiveMassDetectorParameters.maximumMZPeakWidth);
	}

	public MzPeak[] getMassValues(Scan scan) {

		Scan sc = scan;
		dataPoints = sc.getDataPoints();
		mzPeaks = new Vector<MzPeak>();

		// Find MzPeaks
		recursiveThreshold(1, dataPoints.length - 1, noiseLevel, 0);
		return mzPeaks.toArray(new MzPeak[0]);
	}

	/**
	 * This function searches for maximums from given part of a spectrum
	 */
	private int recursiveThreshold(int startInd, int stopInd,
			float currentLevel, int recuLevel) {

		Vector<DataPoint> RawDataPointsInds = new Vector<DataPoint>();
		int peakStartInd, peakStopInd, peakMinInd, peakMaxInd;
		float peakWidthMZ;

		for (int ind = startInd; ind < stopInd; ind++) {

			boolean currentIsBiggerNoise = dataPoints[ind].getIntensity() > currentLevel;
			boolean previousIsZero = dataPoints[ind - 1].getIntensity() == 0;
			boolean previousIsLittler = dataPoints[ind - 1].getIntensity() < dataPoints[ind]
					.getIntensity();
			boolean nextIsZero = dataPoints[ind + 1].getIntensity() == 0;
			boolean nextIsLittler = dataPoints[ind + 1].getIntensity() < dataPoints[ind]
					.getIntensity();

			// Ignore intensities below currentLevel
			if (!currentIsBiggerNoise)
				continue;

			// Add initial point of the peak
			if ((previousIsZero) || (previousIsLittler)) {
				if (ind == 1)
					peakStartInd = ind;
				else {
					peakStartInd = ind - 1;
					RawDataPointsInds.add(dataPoints[peakStartInd]);
				}
			} else {
				peakStartInd = ind;
			}

			peakMinInd = peakStartInd;
			peakMaxInd = peakStartInd;

			// While peak is on
			while ((ind < stopInd)
					&& (dataPoints[ind].getIntensity() > currentLevel)) {

				// Check if this is the minimum point of the peak
				if (dataPoints[ind].getIntensity() < dataPoints[peakMinInd]
						.getIntensity())
					peakMinInd = ind;

				// Check if this is the maximum point of the peak
				if (dataPoints[ind].getIntensity() > dataPoints[peakMaxInd]
						.getIntensity())
					peakMaxInd = ind;

				// Forming the DataPoint array that defines this peak
				RawDataPointsInds.add(dataPoints[ind]);
				ind++;
			}

			// Add ending point of the peak
			if ((nextIsZero) || (nextIsLittler)) {
				peakStopInd = ind + 1;
				RawDataPointsInds.add(dataPoints[peakStopInd]);
			} else
				peakStopInd = ind;

			peakWidthMZ = dataPoints[peakStopInd].getMZ()
					- dataPoints[peakStartInd].getMZ();

			// Verify width of the peak
			if ((peakWidthMZ >= minimumMZPeakWidth)
					&& (peakWidthMZ <= maximumMZPeakWidth)) {

				// Declare a new MzPeak with intensity equal to max intensity
				// data point
				mzPeaks.add(new MzPeak(dataPoints[peakMaxInd],
						RawDataPointsInds.toArray(new DataPoint[0])));
				RawDataPointsInds.clear();

				if (recuLevel > 0) {
					// return stop index and beginning of the next peak
					return ind;
				}
			}

			// If the peak is still too big applies the same method until find a
			// peak of the right size
			if ((peakWidthMZ > maximumMZPeakWidth)
					&& (peakMinInd != peakStartInd)) {
				ind = recursiveThreshold(peakStartInd, peakStopInd,
						dataPoints[peakMinInd].getIntensity(), recuLevel + 1);
			}

		}

		// return stop index
		return stopInd;

	}

}
