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
	private DataPoint scanDataPoints[];
	private Vector<MzPeak> mzPeaks;

	public LocalMaxMassDetector(LocalMaxMassDetectorParameters parameters) {
		noiseLevel = (Float) parameters
				.getParameterValue(LocalMaxMassDetectorParameters.noiseLevel);
	}

	/* (non-Javadoc)
	 * @see net.sf.mzmine.modules.peakpicking.twostep.massdetection.MassDetector#getMassValues(net.sf.mzmine.data.Scan)
	 */
	public MzPeak[] getMassValues(Scan scan) {

		scanDataPoints = scan.getDataPoints();
		int startCurrentPeak = 0, endCurrentPeak = 0;
		mzPeaks = new Vector<MzPeak>();
		int length = scanDataPoints.length - 1;
		Vector<Integer> localMinimum = new Vector<Integer>();
		Vector<Integer> localMaximum = new Vector<Integer>();
		boolean top = true;

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
			startCurrentPeak = ind - 1;
			while ((ind < length - 1) && (intensityValues[ind] > 0)) {
				// Check for all local maximum and minimum in this peak
				if (top) {
					if ((intensityValues[ind - 1] < intensityValues[ind])
							&& (intensityValues[ind] > intensityValues[ind + 1])) {
						localMaximum.add(ind);
						top = false;
					}
				} else {
					if ((intensityValues[ind - 1] > intensityValues[ind])
							&& (intensityValues[ind] < intensityValues[ind + 1])) {
						localMinimum.add(ind);
						top = true;
					}
				}
				ind++;
			}
			endCurrentPeak = ind;

			// Using all local maximum and minimum, we define m/z peaks.
			definePeaks(localMaximum, localMinimum, startCurrentPeak,
					endCurrentPeak);

			top = true;
			localMaximum.clear();
			localMinimum.clear();
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
	private void definePeaks(Vector<Integer> localMaximum,
			Vector<Integer> localMinimum, int start, int end) {

		Vector<DataPoint> rangeDataPoints = new Vector<DataPoint>();

		if ((localMinimum.isEmpty()) && (localMaximum.size() == 1)) {
			// Filter of noise level
			if (scanDataPoints[localMaximum.firstElement()].getIntensity() > noiseLevel) {
				for (int i = start; i < end; i++) {
					rangeDataPoints.add(scanDataPoints[i]);
				}
				mzPeaks.add(new MzPeak(scanDataPoints[localMaximum
						.firstElement()].getMZ(), scanDataPoints[localMaximum
						.firstElement()].getIntensity(), rangeDataPoints
						.toArray(new DataPoint[0])));
			}
		} else {
			Iterator<Integer> maximum = localMaximum.iterator();
			Iterator<Integer> minimum = localMinimum.iterator();
			if (!localMinimum.isEmpty()) {
				int tempStart = start;
				int tempEnd = minimum.next();
				while (maximum.hasNext()) {
					int index = maximum.next();
					// Filter of noise level
					if (scanDataPoints[index].getIntensity() > noiseLevel) {
						for (int i = tempStart; i < tempEnd; i++) {
							rangeDataPoints.add(scanDataPoints[i]);
						}
						mzPeaks.add(new MzPeak(scanDataPoints[index].getMZ(),
								scanDataPoints[index].getIntensity(),
								rangeDataPoints.toArray(new DataPoint[0])));
					}
					tempStart = tempEnd;
					if (minimum.hasNext())
						tempEnd = minimum.next();
					else
						tempEnd = end;
				}
			}
		}
	}

}
