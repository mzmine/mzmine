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

package net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.recursive;

import java.util.TreeSet;
import java.util.Vector;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.MassDetector;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.util.DataPointSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

public class RecursiveMassDetector implements MassDetector {

    public DataPoint[] getMassValues(Scan scan, ParameterSet parameters) {

	double noiseLevel = parameters.getParameter(
		RecursiveMassDetectorParameters.noiseLevel).getValue();
	double minimumMZPeakWidth = parameters.getParameter(
		RecursiveMassDetectorParameters.minimumMZPeakWidth).getValue();
	double maximumMZPeakWidth = parameters.getParameter(
		RecursiveMassDetectorParameters.maximumMZPeakWidth).getValue();

	DataPoint dataPoints[] = scan.getDataPoints();
	TreeSet<DataPoint> mzPeaks = new TreeSet<DataPoint>(
		new DataPointSorter(SortingProperty.MZ,
			SortingDirection.Ascending));

	// Find MzPeaks
	recursiveThreshold(mzPeaks, dataPoints, 1, dataPoints.length - 1,
		noiseLevel, minimumMZPeakWidth, maximumMZPeakWidth, 0);
	return mzPeaks.toArray(new DataPoint[0]);
    }

    /**
     * This function searches for maxima from given part of a spectrum
     */
    private int recursiveThreshold(TreeSet<DataPoint> mzPeaks,
	    DataPoint dataPoints[], int startInd, int stopInd,
	    double curentNoiseLevel, double minimumMZPeakWidth,
	    double maximumMZPeakWidth, int recuLevel) {

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
		mzPeaks.add(dataPoints[peakMaxInd]);

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
		    ind = recursiveThreshold(mzPeaks, dataPoints, peakStartInd,
			    peakStopInd, localMinimum, minimumMZPeakWidth,
			    maximumMZPeakWidth, recuLevel + 1);
		}

	    }

	}

	// return stop index
	return stopInd;

    }

    public @Nonnull String getName() {
	return "Recursive threshold";
    }

    @Override
    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
	return RecursiveMassDetectorParameters.class;
    }

}
