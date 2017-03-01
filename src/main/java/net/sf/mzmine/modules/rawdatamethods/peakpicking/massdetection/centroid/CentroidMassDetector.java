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

package net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.centroid;

import java.util.ArrayList;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.MassDetector;
import net.sf.mzmine.parameters.ParameterSet;

public class CentroidMassDetector implements MassDetector {

    public DataPoint[] getMassValues(Scan scan, ParameterSet parameters) {

	double noiseLevel = parameters.getParameter(
		CentroidMassDetectorParameters.noiseLevel).getValue();

	ArrayList<DataPoint> mzPeaks = new ArrayList<DataPoint>();

	DataPoint dataPoints[] = scan.getDataPoints();

	// Find possible mzPeaks
	for (int j = 0; j < dataPoints.length; j++) {

	    // Is intensity above the noise level?
	    if (dataPoints[j].getIntensity() >= noiseLevel) {
		// Yes, then mark this index as mzPeak
		mzPeaks.add(dataPoints[j]);
	    }
	}
	return mzPeaks.toArray(new DataPoint[0]);
    }

    public @Nonnull String getName() {
	return "Centroid";
    }

    @Override
    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
	return CentroidMassDetectorParameters.class;
    }

}
