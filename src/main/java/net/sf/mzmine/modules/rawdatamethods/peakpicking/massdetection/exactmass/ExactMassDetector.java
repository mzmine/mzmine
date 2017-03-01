/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.exactmass;

import java.util.ArrayList;
import java.util.TreeSet;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.MassDetector;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.util.DataPointSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

public class ExactMassDetector implements MassDetector {

    /**
     * @see net.sf.mzmine.modules.peakpicking.threestep.massdetection.MassDetector#getMassValues(net.sf.mzmine.datamodel.Scan)
     */
    public DataPoint[] getMassValues(Scan scan, ParameterSet parameters) {

	double noiseLevel = parameters.getParameter(
		ExactMassDetectorParameters.noiseLevel).getValue();

	// Create a tree set of detected mzPeaks sorted by MZ in ascending order
	TreeSet<ExactMzDataPoint> mzPeaks = new TreeSet<ExactMzDataPoint>(
		new DataPointSorter(SortingProperty.MZ,
			SortingDirection.Ascending));

	// Create a tree set of candidate mzPeaks sorted by intensity in
	// descending order.
	TreeSet<ExactMzDataPoint> candidatePeaks = new TreeSet<ExactMzDataPoint>(
		new DataPointSorter(SortingProperty.Intensity,
			SortingDirection.Descending));

	// First get all candidate peaks (local maximum)
	getLocalMaxima(scan, candidatePeaks, noiseLevel);

	// We calculate the exact mass for each peak,
	// starting with biggest intensity peak and so on
	while (candidatePeaks.size() > 0) {

	    // Always take the biggest (intensity) peak
	    ExactMzDataPoint currentCandidate = candidatePeaks.first();

	    // Calculate the exact mass and update value in current candidate
	    // (MzPeak)
	    double exactMz = calculateExactMass(currentCandidate);
	    currentCandidate.setMZ(exactMz);

	    // Add this candidate to the final tree set sorted by MZ and remove
	    // from tree set sorted by intensity
	    mzPeaks.add(currentCandidate);
	    candidatePeaks.remove(currentCandidate);

	}

	// Return an array of detected MzPeaks sorted by MZ
	return mzPeaks.toArray(new ExactMzDataPoint[0]);

    }

    /**
     * This method gets all possible MzPeaks using local maximum criteria from
     * the current scan and return a tree set of MzPeaks sorted by intensity in
     * descending order.
     * 
     * @param scan
     * @return
     */
    private void getLocalMaxima(Scan scan,
	    TreeSet<ExactMzDataPoint> candidatePeaks, double noiseLevel) {

	DataPoint[] scanDataPoints = scan.getDataPoints();
	if (scanDataPoints.length == 0)
	    return;
	DataPoint localMaximum = scanDataPoints[0];
	ArrayList<DataPoint> rangeDataPoints = new ArrayList<DataPoint>();

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

		    DataPoint[] rawDataPoints = rangeDataPoints
			    .toArray(new DataPoint[0]);
		    candidatePeaks.add(new ExactMzDataPoint(localMaximum,
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
     * @param ExactMassDataPoint
     * @return double
     */
    private double calculateExactMass(ExactMzDataPoint currentCandidate) {

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
	DataPoint[] rangeDataPoints = currentCandidate.getRawDataPoints();

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

    public @Nonnull String getName() {
	return "Exact mass";
    }

    @Override
    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
	return ExactMassDetectorParameters.class;
    }

}
