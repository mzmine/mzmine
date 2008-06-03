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

package net.sf.mzmine.modules.peakpicking.twostep.massdetection.exactmass;

import java.util.Iterator;
import java.util.Vector;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.modules.peakpicking.twostep.massdetection.MassDetector;
import net.sf.mzmine.modules.peakpicking.twostep.massdetection.MzPeak;
import net.sf.mzmine.util.Range;

public class ExactMassDetector implements MassDetector {

	// parameter values
	private Float noiseLevel;
	private float resolution;

	private DataPoint scanDataPoints[];

	public ExactMassDetector(ExactMassDetectorParameters parameters) {
		noiseLevel = (Float) parameters
				.getParameterValue(ExactMassDetectorParameters.noiseLevel);
		resolution = (Float) parameters
				.getParameterValue(ExactMassDetectorParameters.resolution);
	}

	public MzPeak[] getMassValues(Scan scan) {
		scanDataPoints = scan.getDataPoints();
		int startCurrentPeak = 0, endCurrentPeak = 0;
		Vector<MzPeak> mzPeaks = new Vector<MzPeak>();
		int length = scanDataPoints.length - 1;
		Vector<Integer> localMinimum = new Vector<Integer>();
		Vector<Integer> localMaximum = new Vector<Integer>();
		boolean top = true;

		float[] intensityValues = new float[scanDataPoints.length];
		for (int i = 0; i < scanDataPoints.length; i++) {
			intensityValues[i] = scanDataPoints[i].getIntensity();
		}

		for (int ind = 1; ind <= length; ind++) {

			while ((ind <= length) && (intensityValues[ind] == 0)) {
				ind++;
			}
			if (ind >= length) {
				break;
			}

			// While peak is on
			startCurrentPeak = ind;
			while ((ind < length - 1) && (intensityValues[ind + 1] > 0)) {
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
			endCurrentPeak = ind + 1;

			DataPoint[] exactMassDataPoint = calculateExactMass(localMaximum,
					localMinimum, startCurrentPeak, endCurrentPeak);

			for (DataPoint dp : exactMassDataPoint) {
				mzPeaks.add(new MzPeak(scan.getScanNumber(), dp.getMZ(), dp
						.getIntensity()));
			}

			top = true;
			localMaximum.clear();
			localMinimum.clear();
		}
		removeLateralPeaks(mzPeaks);
		return mzPeaks.toArray(new MzPeak[0]);
	}

	/**
	 * 
	 * This function calculates the mass (m/z) giving weight to each data point
	 * of the peak
	 * 
	 * @param localMaximum
	 * @param localMinimum
	 * @param start
	 * @param end
	 * @return
	 */
	private DataPoint[] calculateExactMass(Vector<Integer> localMaximum,
			Vector<Integer> localMinimum, int start, int end) {
		Vector<DataPoint> dataPoints = new Vector<DataPoint>();
		float sumMz = 0;
		float sumIntensities = 0;
		if ((localMinimum.isEmpty()) && (localMaximum.size() == 1)) {
			for (int i = start; i < end; i++) {
				sumMz += scanDataPoints[i].getMZ()
						* scanDataPoints[i].getIntensity();
				sumIntensities += scanDataPoints[i].getIntensity();
			}
			float exactMz = sumMz / sumIntensities;
			float intensity = scanDataPoints[localMaximum.firstElement()]
					.getIntensity();
			dataPoints.add(new SimpleDataPoint(exactMz, intensity));
		} else {
			Iterator<Integer> maximum = localMaximum.iterator();
			Iterator<Integer> minimum = localMinimum.iterator();
			if (!localMinimum.isEmpty()) {
				int tempStart = start;
				int tempEnd = minimum.next();
				while (maximum.hasNext()) {
					int index = maximum.next();
					sumMz = 0;
					sumIntensities = 0;
					for (int i = tempStart; i <= tempEnd; i++) {
						sumMz += scanDataPoints[i].getMZ()
								* scanDataPoints[i].getIntensity();
						sumIntensities += scanDataPoints[i].getIntensity();
					}
					float exactMz = sumMz / sumIntensities;
					float intensity = scanDataPoints[index].getIntensity();
					dataPoints.add(new SimpleDataPoint(exactMz, intensity));
					tempStart = tempEnd;
					if (minimum.hasNext())
						tempEnd = minimum.next();
					else
						tempEnd = end;

				}
			}
		}

		return dataPoints.toArray(new DataPoint[0]);
	}

	/**
	 * 
	 * This function calculates the base peak width (central peak), and
	 * eliminates the lateral peaks in this range with a height less than 5% of
	 * the central peak.
	 * 
	 * @param mzPeaks
	 */
	private void removeLateralPeaks(Vector<MzPeak> mzPeaks) {

		Vector<MzPeak> removeMzPeaks = new Vector<MzPeak>();
		MzPeak[] arrayMzPeak = mzPeaks.toArray(new MzPeak[0]);
		for (MzPeak currentMzPeak : arrayMzPeak) {

			// FWFM (Full Width at Half Maximum)
			float FWHM = currentMzPeak.getMZ() / resolution;

			// Using the Gaussian function we calculate the base peak width,
			// at 0.1% of peak's height
			double partA = 2 * FWHM * FWHM;
			float peakHeight = currentMzPeak.getIntensity();
			float heightPercentage = peakHeight * 0.001f;
			double ln = Math.abs(Math.log(heightPercentage / peakHeight));
			float sideRange = (float) Math.sqrt(partA * ln);
			Range rangePeak = new Range(currentMzPeak.getMZ() - sideRange,
					currentMzPeak.getMZ() + sideRange);

			if (currentMzPeak.getIntensity() < noiseLevel)
				removeMzPeaks.add(currentMzPeak);

			Iterator<MzPeak> anotherIteratorMzPeak = mzPeaks.iterator();
			while (anotherIteratorMzPeak.hasNext()) {
				MzPeak comparedMzPeak = anotherIteratorMzPeak.next();
				if (((comparedMzPeak.getMZ() >= rangePeak.getMin()) || (comparedMzPeak
						.getMZ() >= rangePeak.getMax()))
						&& (comparedMzPeak.getIntensity() / peakHeight < 0.05)) {
					removeMzPeaks.add(comparedMzPeak);
				}
				if (comparedMzPeak.getMZ() > rangePeak.getMax())
					break;
			}
		}

		for (MzPeak clean : removeMzPeaks) {
			mzPeaks.remove(clean);
		}

	}

}
