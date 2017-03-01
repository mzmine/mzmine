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

package net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.localmaxima;

import java.util.ArrayList;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.MassDetector;
import net.sf.mzmine.parameters.ParameterSet;

/**
 * This class detects all local maxima in a given scan.
 */
public class LocalMaxMassDetector implements MassDetector {

    public DataPoint[] getMassValues(Scan scan, ParameterSet parameters) {

	double noiseLevel = parameters.getParameter(
		LocalMaxMassDetectorParameters.noiseLevel).getValue();

	// List of found mz peaks
	ArrayList<DataPoint> mzPeaks = new ArrayList<DataPoint>();

	DataPoint dataPoints[] = scan.getDataPoints();

	// All data points of current m/z peak

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

	    // Check for local maximum
	    if (ascending && (!nextIsBigger)) {
		currentMzPeakTop = dataPoints[i];
		ascending = false;
		continue;
	    }

	    assert currentMzPeakTop != null;

	    // Check for the end of the peak
	    if ((!ascending) && (nextIsBigger || nextIsZero)) {

		// Add the m/z peak if it is above the noise level
		if (currentMzPeakTop.getIntensity() > noiseLevel) {
		    mzPeaks.add(currentMzPeakTop);
		}

		// Reset and start with new peak
		ascending = true;

	    }

	}
	return mzPeaks.toArray(new DataPoint[0]);
    }

    @Override
    public @Nonnull String getName() {
	return "Local maxima";
    }

    @Override
    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
	return LocalMaxMassDetectorParameters.class;
    }

}
