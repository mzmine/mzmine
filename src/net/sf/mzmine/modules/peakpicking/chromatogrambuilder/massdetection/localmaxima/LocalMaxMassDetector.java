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

package net.sf.mzmine.modules.peakpicking.chromatogrambuilder.massdetection.localmaxima;

import java.util.ArrayList;
import java.util.Vector;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.modules.peakpicking.chromatogrambuilder.MzPeak;
import net.sf.mzmine.modules.peakpicking.chromatogrambuilder.massdetection.MassDetector;

/**
 * This class detects all local maxima in a given scan.
 */
public class LocalMaxMassDetector implements MassDetector {

	// Parameter value
	private double noiseLevel;

	public LocalMaxMassDetector(LocalMaxMassDetectorParameters parameters) {
		noiseLevel = (Double) parameters
				.getParameterValue(LocalMaxMassDetectorParameters.noiseLevel);
	}

	public MzPeak[] getMassValues(Scan scan) {

		// List of found mz peaks
		ArrayList<MzPeak> mzPeaks = new ArrayList<MzPeak>();

		DataPoint dataPoints[] = scan.getDataPoints();

		// All data points of current m/z peak
		Vector<DataPoint> currentMzPeakDataPoints = new Vector<DataPoint>();

		// Top data point of current m/z peak
		DataPoint currentMzPeakTop = null;

		// True if we haven't reached the current local maximum yet
		boolean ascending = true;

		// Iterate through all data points
		for (int i = 0; i < dataPoints.length - 1; i++) {

			boolean nextIsBigger = dataPoints[i + 1].getIntensity() > dataPoints[i]
					.getIntensity();
			boolean nextIsZero = dataPoints[i + 1].getIntensity() == 0;
			boolean currentIsZero = dataPoints[i].getIntensity() == 0;

			// Ignore zero intensity regions
			if (currentIsZero)
				continue;

			// Add current (non-zero) data point to the current m/z peak
			currentMzPeakDataPoints.add(dataPoints[i]);

			// Check for local maximum
			if (ascending && (!nextIsBigger)) {
				currentMzPeakTop = dataPoints[i];
				ascending = false;
				continue;
			}

			// Check for the end of the peak
			if ((!ascending) && (nextIsBigger || nextIsZero)) {

				// Add the m/z peak if it is above the noise level
				if (currentMzPeakTop.getIntensity() > noiseLevel) {
					MzPeak newMzPeak = new MzPeak(currentMzPeakTop,
							currentMzPeakDataPoints.toArray(new DataPoint[0]));
					mzPeaks.add(newMzPeak);
				}

				// Reset and start with new peak
				ascending = true;
				currentMzPeakDataPoints.clear();

			}

		}
		return mzPeaks.toArray(new MzPeak[0]);
	}

}
