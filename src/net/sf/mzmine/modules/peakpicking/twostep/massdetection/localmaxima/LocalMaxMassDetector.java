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

package net.sf.mzmine.modules.peakpicking.twostep.massdetection.localmaxima;

import java.util.Iterator;
import java.util.Vector;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.modules.peakpicking.twostep.massdetection.MassDetector;
import net.sf.mzmine.modules.peakpicking.twostep.massdetection.MzPeak;

/**
 * This class detects all local maximum in a given scan. 
 *
 */
public class LocalMaxMassDetector implements MassDetector {

	// parameter values
	private float noiseLevel;
	
	public LocalMaxMassDetector(LocalMaxMassDetectorParameters parameters) {
		noiseLevel = (Float) parameters
				.getParameterValue(LocalMaxMassDetectorParameters.noiseLevel);
	}

	/* (non-Javadoc)
	 * @see net.sf.mzmine.modules.peakpicking.twostep.massdetection.MassDetector#getMassValues(net.sf.mzmine.data.Scan)
	 */
	public MzPeak[] getMassValues(Scan scan) {

		DataPoint[] scanDataPoints = scan.getDataPoints();
		Vector<MzPeak> mzPeaks = new Vector<MzPeak>();
		int length = scanDataPoints.length - 1;
		Vector<DataPoint> rawDataPoints = new Vector<DataPoint>();
		boolean top = true;
		int peakMax = 0;

		float[] intensityValues = new float[scanDataPoints.length];
		for (int i = 0; i < scanDataPoints.length; i++) {
			intensityValues[i] = scanDataPoints[i].getIntensity();
		}

		for (int ind = 1; ind <= length; ind++) {

			while ((ind < length) && (intensityValues[ind] == 0)) {
				ind++;
			}
			if (ind >= length) {
				break;
			}

			// While peak is on
			//ind--;
			while ((ind < length - 1) && (intensityValues[ind] > 0)) {
				// Check for all local maximum and minimum in this peak
				rawDataPoints.add(scanDataPoints[ind]);
				if (top) {
					if ((intensityValues[ind - 1] < intensityValues[ind])
							&& (intensityValues[ind] >= intensityValues[ind + 1])) {
						peakMax = ind;
						top = false;
					}
				} else {
					if ((intensityValues[ind - 1] > intensityValues[ind])
							&& (intensityValues[ind] < intensityValues[ind + 1])) {
						mzPeaks.add(new MzPeak(scanDataPoints[peakMax], rawDataPoints
						              						.toArray(new DataPoint[0])));
						rawDataPoints.clear();
						peakMax = 0;
						top= true;
					}
				}
				ind++;
			}
			mzPeaks.add(new MzPeak(scanDataPoints[peakMax], rawDataPoints
				              						.toArray(new DataPoint[0])));
			rawDataPoints.clear();
			peakMax = 0;

			mzPeaks = removeNoise(mzPeaks);

			top = true;
		}
		return mzPeaks.toArray(new MzPeak[0]);
	}

	/**
	 * This function sets the m/z peaks defined by all local maximum
	 * and  minimum. Also applies a filter for peaks with intensity 
	 * below of noise level parameter. 
	 * 
	 * @param localMaximum
	 * @param localMinimum
	 * @param start
	 * @param end
	 */
	private Vector<MzPeak> removeNoise(Vector<MzPeak> mzPeaks) {

		Iterator<MzPeak> mzPeaksIterator = mzPeaks.iterator();
		while (mzPeaksIterator.hasNext()) {
			MzPeak currentMzPeak = mzPeaksIterator.next(); 
			if (currentMzPeak.getIntensity() < noiseLevel) 
				mzPeaksIterator.remove();
		}
		return mzPeaks;
	}

}
