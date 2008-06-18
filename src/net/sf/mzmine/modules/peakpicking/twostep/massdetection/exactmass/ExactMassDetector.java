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

import java.lang.reflect.Constructor;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peakpicking.twostep.massdetection.MassDetector;
import net.sf.mzmine.modules.peakpicking.twostep.massdetection.MzPeak;
import net.sf.mzmine.modules.peakpicking.twostep.massdetection.exactmass.peakmodel.PeakModel;
import net.sf.mzmine.util.MzPeaksSorterByIntensity;
import net.sf.mzmine.util.MzPeaksSorterByMZ;
import net.sf.mzmine.util.Range;

public class ExactMassDetector implements MassDetector {

	// parameter values
	private float noiseLevel;
	private int resolution;
	private String peakModelname;

	// Desktop
	private Desktop desktop = MZmineCore.getDesktop();

	public ExactMassDetector(ExactMassDetectorParameters parameters) {
		noiseLevel = (Float) parameters
				.getParameterValue(ExactMassDetectorParameters.noiseLevel);
		resolution = (Integer) parameters
				.getParameterValue(ExactMassDetectorParameters.resolution);
		peakModelname = (String) parameters
				.getParameterValue(ExactMassDetectorParameters.peakModel);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sf.mzmine.modules.peakpicking.twostep.massdetection.MassDetector#getMassValues(net.sf.mzmine.data.Scan)
	 */
	public MzPeak[] getMassValues(Scan scan) {

		// Create a tree set of detected mzPeaks sorted by MZ in ascending order
		TreeSet<MzPeak> mzPeaks = new TreeSet<MzPeak>(new MzPeaksSorterByMZ());

		// First get all candidate peaks (local maximum)
		TreeSet<MzPeak> candidates = getLocalMaxima(scan);

		// We calculate the exact mass for each peak and remove lateral peaks,
		// starting with biggest intensity peak and so on
		while (candidates.size() > 0) {

			// Always take the biggest (intensity) peak
			MzPeak currentCandidate = candidates.first();

			// Calculate the exact mass and update value in current candidate
			// (MzPeak)
			float exactMz = calculateExactMass(currentCandidate);
			currentCandidate.setMZ(exactMz);

			// Add this candidate to the final tree set sorted by MZ and remove
			// from tree set sorted by intensity
			mzPeaks.add(currentCandidate);
			candidates.remove(currentCandidate);

			// Remove from tree set sorted by intensity all FTMS shoulder peaks,
			// taking as a main peak the current candidate
			removeLateralPeaks(currentCandidate, candidates);

		}

		// Return an array of detected MzPeaks sorted by MZ
		return mzPeaks.toArray(new MzPeak[0]);

	}

	/**
	 * This method gets all possible MzPeaks using local maximum criteria from
	 * the current scan and return a tree set of MzPeaks sorted by intensity in
	 * descending order.
	 * 
	 * @param scan
	 * @return
	 */
	private TreeSet<MzPeak> getLocalMaxima(Scan scan) {

		DataPoint[] scanDataPoints = scan.getDataPoints();
		DataPoint localMaximum = scanDataPoints[0];
		Vector<DataPoint> rangeDataPoints = new Vector<DataPoint>();

		// Create a tree set of candidate mzPeaks sorted by intensity in
		// descending order.
		TreeSet<MzPeak> candidatePeaks = new TreeSet<MzPeak>(
				new MzPeaksSorterByIntensity());

		boolean ascending = true;

		// Iterate through all data points
		for (int i = 0; i < scanDataPoints.length - 1; i++) {

			boolean nextIsBigger = scanDataPoints[i + 1].getIntensity() > scanDataPoints[i]
					.getIntensity();
			boolean nextIsZero = scanDataPoints[i + 1].getIntensity() == 0;
			boolean currentIsZero = scanDataPoints[i].getIntensity() == 0;

			// Ignore zero intensity regions
			if (currentIsZero) {
				continue;
			}

			// Add current (non-zero) data point to the current m/z peak
			rangeDataPoints.add(scanDataPoints[i]);

			// Check for local maximum
			if (ascending && (!nextIsBigger)) {
				localMaximum = scanDataPoints[i];
				rangeDataPoints.remove(scanDataPoints[i]);
				ascending = false;
				continue;
			}

			// Check for the end of the peak
			if ((!ascending) && (nextIsBigger || nextIsZero)) {

				// Add the m/z peak if it is above the noise level
				if (localMaximum.getIntensity() > noiseLevel) {
					DataPoint[] rawDataPoints = rangeDataPoints
							.toArray(new DataPoint[0]);
					candidatePeaks.add(new MzPeak(localMaximum, rawDataPoints));
				}

				// Reset and start with new peak
				ascending = true;
				rangeDataPoints.clear();
			}

		}
		return candidatePeaks;
	}

	/**
	 * This method calculates the exact mass of
	 * 
	 * 
	 * @param currentCandidate
	 * @return
	 */
	private float calculateExactMass(MzPeak currentCandidate) {

		float xRight = -1, xLeft = -1;
		DataPoint[] rangeDataPoints = currentCandidate.getRawDataPoints();

		for (int i = 0; i < rangeDataPoints.length; i++) {
			if ((rangeDataPoints[i].getIntensity() <= currentCandidate
					.getIntensity() / 2)
					&& (rangeDataPoints[i].getMZ() < currentCandidate.getMZ())
					&& (i + 1 < rangeDataPoints.length)) {

				float leftY1 = rangeDataPoints[i].getIntensity();
				float leftX1 = rangeDataPoints[i].getMZ();
				float leftY2 = rangeDataPoints[i + 1].getIntensity();
				float leftX2 = rangeDataPoints[i + 1].getMZ();

				float mLeft = (leftY1 - leftY2) / (leftX1 - leftX2);
				xLeft = leftX1
						+ (((currentCandidate.getIntensity() / 2) - leftY1) / mLeft);
				continue;
			}
			if ((rangeDataPoints[i].getIntensity() >= currentCandidate
					.getIntensity() / 2)
					&& (rangeDataPoints[i].getMZ() > currentCandidate.getMZ())
					&& (i + 1 < rangeDataPoints.length)) {

				float rightY1 = rangeDataPoints[i].getIntensity();
				float rightX1 = rangeDataPoints[i].getMZ();
				float rightY2 = rangeDataPoints[i + 1].getIntensity();
				float rightX2 = rangeDataPoints[i + 1].getMZ();

				float mRight = (rightY1 - rightY2) / (rightX1 - rightX2);
				xRight = rightX1
						+ (((currentCandidate.getIntensity() / 2) - rightY1) / mRight);
				break;
			}
		}

		if ((xRight == -1) || (xLeft == -1))
			return currentCandidate.getMZ();

		float FWHM = xRight - xLeft;

		float exactMass = xLeft + FWHM / 2;

		return exactMass;
	}

	/**
	 * This function remove FTMS shoulder peaks encountered in the lateral of a
	 * main peak (currentCandidate). First calculates a peak model (Gauss,
	 * Lorenzian, etc) defined by peakModelName parameter, with the same
	 * position (m/z) and height (intensity) of the currentCandidate, and the
	 * defined resolution (resolution parameter). Second search and remove all
	 * the lateral peaks that are under the curve of the modeled peak.
	 * 
	 * @param mzPeaks
	 * @param percentageHeight
	 * @param percentageResolution
	 */
	private void removeLateralPeaks(MzPeak currentCandidate,
			TreeSet<MzPeak> candidates) {

		Constructor peakModelConstruct;
		Class peakModelClass;
		PeakModel peakModel;

		int peakModelindex = 0;

		// Peak Model used to remove FTMS shoulder peaks
		for (String model : ExactMassDetectorParameters.peakModelNames) {
			if (model.equals(peakModelname))
				break;
			peakModelindex++;
		}

		String peakModelClassName = ExactMassDetectorParameters.peakModelClasses[peakModelindex];

		try {
			peakModelClass = Class.forName(peakModelClassName);
			peakModelConstruct = peakModelClass.getConstructors()[0];
			peakModel = (PeakModel) peakModelConstruct.newInstance();

		} catch (Exception e) {
			desktop
					.displayErrorMessage("Error trying to make an instance of peak model "
							+ peakModelClassName);
			return;
		}

		peakModel.setParameters(currentCandidate.getMZ(), currentCandidate
				.getIntensity(), resolution);
		Range rangePeak = peakModel.getWidth(noiseLevel);

		Iterator<MzPeak> candidatesIterator = candidates.iterator();
		while (candidatesIterator.hasNext()) {

			MzPeak lateralCandidate = candidatesIterator.next();

			if ((lateralCandidate.getMZ() >= rangePeak.getMin())
					&& (lateralCandidate.getMZ() <= rangePeak.getMax())
					&& (lateralCandidate.getIntensity() < peakModel
							.getIntensity(lateralCandidate.getMZ()))) {
				candidatesIterator.remove();
			}
		}

		return;

	}
}
