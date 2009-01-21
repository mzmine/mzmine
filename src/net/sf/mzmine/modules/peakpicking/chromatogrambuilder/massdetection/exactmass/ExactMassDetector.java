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

package net.sf.mzmine.modules.peakpicking.chromatogrambuilder.massdetection.exactmass;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.logging.Logger;

import net.sf.mzmine.data.MzDataPoint;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimpleMzPeak;
import net.sf.mzmine.main.mzmineclient.MZmineCore;
import net.sf.mzmine.modules.peakpicking.chromatogrambuilder.massdetection.MassDetector;
import net.sf.mzmine.util.DataPointSorter;
import net.sf.mzmine.util.Range;

public class ExactMassDetector implements MassDetector {

	// Parameter values
	private double noiseLevel;
	private int resolution;
	private PeakModel peakModel;

	private Logger logger = Logger.getLogger(this.getClass().getName());

	public ExactMassDetector(ExactMassDetectorParameters parameters) {

		noiseLevel = (Double) parameters
				.getParameterValue(ExactMassDetectorParameters.noiseLevel);
		resolution = (Integer) parameters
				.getParameterValue(ExactMassDetectorParameters.resolution);

		String peakModelname = (String) parameters
				.getParameterValue(ExactMassDetectorParameters.peakModel);

		// Create an instance of selected model class
		try {

			String peakModelClassName = null;

			for (int modelIndex = 0; modelIndex < ExactMassDetectorParameters.peakModelNames.length; modelIndex++) {
				if (ExactMassDetectorParameters.peakModelNames[modelIndex]
						.equals(peakModelname))
					peakModelClassName = ExactMassDetectorParameters.peakModelClasses[modelIndex];
				;
			}

			if (peakModelClassName == null)
				throw new ClassNotFoundException();

			Class peakModelClass = Class.forName(peakModelClassName);

			peakModel = (PeakModel) peakModelClass.newInstance();

		} catch (Exception e) {
			logger.severe("Error trying to make an instance of peak model "
					+ peakModelname);
			MZmineCore.getDesktop().displayErrorMessage(
					"Error trying to make an instance of peak model "
							+ peakModelname);
		}

	}

	/**
	 * @see net.sf.mzmine.modules.peakpicking.threestep.massdetection.MassDetector#getMassValues(net.sf.mzmine.data.Scan)
	 */
	public SimpleMzPeak[] getMassValues(Scan scan) {

		// Create a tree set of detected mzPeaks sorted by MZ in ascending order
		TreeSet<SimpleMzPeak> mzPeaks = new TreeSet<SimpleMzPeak>(
				new DataPointSorter(true, true));

		// Create a tree set of candidate mzPeaks sorted by intensity in
		// descending order.
		TreeSet<SimpleMzPeak> candidatePeaks = new TreeSet<SimpleMzPeak>(
				new DataPointSorter(false, false));

		// First get all candidate peaks (local maximum)
		getLocalMaxima(scan, candidatePeaks);

		// We calculate the exact mass for each peak and remove lateral peaks,
		// starting with biggest intensity peak and so on
		while (candidatePeaks.size() > 0) {

			// Always take the biggest (intensity) peak
			SimpleMzPeak currentCandidate = candidatePeaks.first();

			// Calculate the exact mass and update value in current candidate
			// (MzPeak)
			double exactMz = calculateExactMass(currentCandidate);
			currentCandidate.setMZ(exactMz);

			// Add this candidate to the final tree set sorted by MZ and remove
			// from tree set sorted by intensity
			mzPeaks.add(currentCandidate);
			candidatePeaks.remove(currentCandidate);

			// Remove from tree set sorted by intensity all FTMS shoulder peaks,
			// taking as a main peak the current candidate
			removeLateralPeaks(currentCandidate, candidatePeaks);

		}

		// Return an array of detected MzPeaks sorted by MZ
		return mzPeaks.toArray(new SimpleMzPeak[0]);

	}

	/**
	 * This method gets all possible MzPeaks using local maximum criteria from
	 * the current scan and return a tree set of MzPeaks sorted by intensity in
	 * descending order.
	 * 
	 * @param scan
	 * @return
	 */
	private void getLocalMaxima(Scan scan, TreeSet<SimpleMzPeak> candidatePeaks) {

		MzDataPoint[] scanDataPoints = scan.getDataPoints();
		if (scanDataPoints.length == 0)
			return;
		MzDataPoint localMaximum = scanDataPoints[0];
		ArrayList<MzDataPoint> rangeDataPoints = new ArrayList<MzDataPoint>();

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
				ascending = false;
				continue;
			}

			// Check for the end of the peak
			if ((!ascending) && (nextIsBigger || nextIsZero)) {

				// Add the m/z peak if it is above the noise level
				if (localMaximum.getIntensity() > noiseLevel) {

					MzDataPoint[] rawDataPoints = rangeDataPoints
							.toArray(new MzDataPoint[0]);
					candidatePeaks.add(new SimpleMzPeak(localMaximum,
							rawDataPoints));
				}

				// Reset and start with new peak
				ascending = true;
				rangeDataPoints.clear();
			}

		}

	}

	/**
	 * This method calculates the exact mass of a peak using the FWHM concept
	 * and linear equation (y = mx + b).
	 * 
	 * @param SimpleMzPeak
	 * @return double
	 */
	private double calculateExactMass(SimpleMzPeak currentCandidate) {

		/*
		 * According with the FWHM concept, the exact mass of this peak is the
		 * half point of FWHM. In order to get the points in the curve that
		 * define the FWHM, we use the linear equation.
		 * 
		 * First we look for, in left side of the peak, 2 data points together
		 * that have an intensity less (first data point) and bigger (second
		 * data point) than half of total intensity. Then we calculate the slope
		 * of the line defined by this two data points. At least, we calculate
		 * the point in this line that has an intensity equal to the half of
		 * total intensity
		 * 
		 * We repeat the same process in the right side.
		 */

		double xRight = -1, xLeft = -1;
		double halfIntensity = currentCandidate.getIntensity() / 2;
		MzDataPoint[] rangeDataPoints = currentCandidate.getRawDataPoints();

		for (int i = 0; i < rangeDataPoints.length - 1; i++) {

			// Left side of the curve
			if ((rangeDataPoints[i].getIntensity() <= halfIntensity)
					&& (rangeDataPoints[i].getMZ() < currentCandidate.getMZ())
					&& (rangeDataPoints[i + 1].getIntensity() >= halfIntensity)) {

				// First point with intensity just less than half of total
				// intensity
				double leftY1 = rangeDataPoints[i].getIntensity();
				double leftX1 = rangeDataPoints[i].getMZ();

				// Second point with intensity just bigger than half of total
				// intensity
				double leftY2 = rangeDataPoints[i + 1].getIntensity();
				double leftX2 = rangeDataPoints[i + 1].getMZ();

				// We calculate the slope with formula m = Y1 - Y2 / X1 - X2
				double mLeft = (leftY1 - leftY2) / (leftX1 - leftX2);

				// We calculate the desired point (at half intensity) with the
				// linear equation
				// X = X1 + [(Y - Y1) / m ], where Y = half of total intensity
				xLeft = leftX1 + (((halfIntensity) - leftY1) / mLeft);
				continue;
			}

			// Right side of the curve
			if ((rangeDataPoints[i].getIntensity() >= halfIntensity)
					&& (rangeDataPoints[i].getMZ() > currentCandidate.getMZ())
					&& (rangeDataPoints[i + 1].getIntensity() <= halfIntensity)) {

				// First point with intensity just bigger than half of total
				// intensity
				double rightY1 = rangeDataPoints[i].getIntensity();
				double rightX1 = rangeDataPoints[i].getMZ();

				// Second point with intensity just less than half of total
				// intensity
				double rightY2 = rangeDataPoints[i + 1].getIntensity();
				double rightX2 = rangeDataPoints[i + 1].getMZ();

				// We calculate the slope with formula m = Y1 - Y2 / X1 - X2
				double mRight = (rightY1 - rightY2) / (rightX1 - rightX2);

				// We calculate the desired point (at half intensity) with the
				// linear equation
				// X = X1 + [(Y - Y1) / m ], where Y = half of total intensity
				xRight = rightX1 + (((halfIntensity) - rightY1) / mRight);
				break;
			}
		}

		// We verify the values to confirm we find the desired points. If not we
		// return the same mass value.
		if ((xRight == -1) || (xLeft == -1))
			return currentCandidate.getMZ();

		// The center of left and right points is the exact mass of our peak.
		double exactMass = (xLeft + xRight) / 2;

		return exactMass;
	}

	/**
	 * This function remove peaks encountered in the lateral of a main peak
	 * (currentCandidate) that are considered as garbage, for example FTMS
	 * shoulder peaks.
	 * 
	 * First calculates a peak model (Gauss, Lorenzian, etc) defined by
	 * peakModelName parameter, with the same position (m/z) and height
	 * (intensity) of the currentCandidate, and the defined resolution
	 * (resolution parameter). Second search and remove all the lateral peaks
	 * that are under the curve of the modeled peak.
	 * 
	 */
	private void removeLateralPeaks(SimpleMzPeak currentCandidate,
			TreeSet<SimpleMzPeak> candidates) {

		// If there was any problem creating the model
		if (peakModel == null)
			return;

		// We set our peak model with same position(m/z), height(intensity) and
		// resolution of the current peak
		peakModel.setParameters(currentCandidate.getMZ(), currentCandidate
				.getIntensity(), resolution);

		// We use the width of the modeled peak at noise level to set the range
		// of search for lateral peaks.
		Range rangePeak = peakModel.getWidth(noiseLevel);

		// We search over all peak candidates and remove all of them that are
		// under the curve defined by our peak model
		Iterator<SimpleMzPeak> candidatesIterator = candidates.iterator();
		while (candidatesIterator.hasNext()) {

			SimpleMzPeak lateralCandidate = candidatesIterator.next();

			// Condition in x domain (m/z)
			if ((rangePeak.contains(lateralCandidate.getMZ()))
			// Condition in y domain (intensity)
					&& (lateralCandidate.getIntensity() < peakModel
							.getIntensity(lateralCandidate.getMZ()))) {

				candidatesIterator.remove();
			}
		}

	}
}
