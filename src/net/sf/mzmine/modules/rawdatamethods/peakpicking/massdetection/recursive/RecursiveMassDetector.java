/*
 * Copyright 2006-2011 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.recursive;

import java.util.TreeSet;
import java.util.Vector;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.chromatogrambuilder.MzPeak;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.MassDetector;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.util.DataPointSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

public class RecursiveMassDetector implements MassDetector {

	private ParameterSet parameters;
	// Parameter values
	private double minimumMZPeakWidth, maximumMZPeakWidth, noiseLevel;
	private TreeSet<MzPeak> mzPeaks;
	private DataPoint[] dataPoints;

	public RecursiveMassDetector() {
		parameters = new RecursiveMassDetectorParameters();
	}

	public MzPeak[] getMassValues(Scan scan) {

		noiseLevel = parameters.getParameter(
				RecursiveMassDetectorParameters.noiseLevel).getDouble();
		minimumMZPeakWidth = parameters.getParameter(
				RecursiveMassDetectorParameters.minimumMZPeakWidth).getDouble();
		maximumMZPeakWidth = parameters.getParameter(
				RecursiveMassDetectorParameters.maximumMZPeakWidth).getDouble();

		dataPoints = scan.getDataPoints();
		mzPeaks = new TreeSet<MzPeak>(new DataPointSorter(SortingProperty.MZ,
				SortingDirection.Ascending));

		// Find MzPeaks
		recursiveThreshold(1, dataPoints.length - 1, noiseLevel, 0);
		return mzPeaks.toArray(new MzPeak[0]);
	}

	/**
	 * This function searches for maxima from given part of a spectrum
	 */
	private int recursiveThreshold(int startInd, int stopInd,
			double curentNoiseLevel, int recuLevel) {

		// logger.finest(" Level of recursion " + recuLevel);

		Vector<DataPoint> RawDataPointsInds = new Vector<DataPoint>();
		int peakStartInd, peakStopInd, peakMaxInd;
		double peakWidthMZ;

		for (int ind = startInd; ind < stopInd; ind++) {

			boolean currentIsBiggerNoise = dataPoints[ind].getIntensity() > curentNoiseLevel;
			double localMinimum = Double.MAX_VALUE;

			// Ignore intensities below curentNoiseLevel
			if (!currentIsBiggerNoise) {
				continue;
			}

			// Add initial point of the peak
			peakStartInd = ind;
			peakMaxInd = peakStartInd;

			// While peak is on
			while ((ind < stopInd)
					&& (dataPoints[ind].getIntensity() > curentNoiseLevel)) {

				boolean isLocalMinimum = (dataPoints[ind - 1].getIntensity() > dataPoints[ind]
						.getIntensity())
						&& (dataPoints[ind].getIntensity() < dataPoints[ind + 1]
								.getIntensity());

				// Check if this is the minimum point of the peak
				if (isLocalMinimum
						&& (dataPoints[ind].getIntensity() < localMinimum))
					localMinimum = dataPoints[ind].getIntensity();

				// Check if this is the maximum point of the peak
				if (dataPoints[ind].getIntensity() > dataPoints[peakMaxInd]
						.getIntensity())
					peakMaxInd = ind;

				// Forming the DataPoint array that defines this peak
				RawDataPointsInds.add(dataPoints[ind]);
				ind++;
			}

			// Add ending point of the peak
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

				if (recuLevel > 0) {
					// return stop index and beginning of the next peak
					return ind;
				}
			}
			RawDataPointsInds.clear();

			// If the peak is still too big applies the same method until find a
			// peak of the right size
			if (peakWidthMZ > maximumMZPeakWidth) {
				if (localMinimum < Double.MAX_VALUE) {
					ind = recursiveThreshold(peakStartInd, peakStopInd,
							localMinimum, recuLevel + 1);
				}

			}

		}

		// return stop index
		return stopInd;

	}

	public String toString() {
		return "Recursive threshold";
	}

	@Override
	public ParameterSet getParameterSet() {
		return parameters;
	}

}
