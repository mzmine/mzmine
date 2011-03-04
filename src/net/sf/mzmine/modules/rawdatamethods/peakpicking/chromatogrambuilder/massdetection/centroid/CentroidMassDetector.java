/*
 * Copyright 2006-2011 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.rawdatamethods.peakpicking.chromatogrambuilder.massdetection.centroid;

import java.util.ArrayList;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.chromatogrambuilder.MzPeak;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.chromatogrambuilder.massdetection.MassDetector;
import net.sf.mzmine.parameters.ParameterSet;

public class CentroidMassDetector implements MassDetector {

	private ParameterSet parameters;

	public CentroidMassDetector() {
		parameters = new CentroidMassDetectorParameters(this);
	}

	public MzPeak[] getMassValues(Scan scan) {

		double noiseLevel = parameters.getParameter(
				CentroidMassDetectorParameters.noiseLevel).getDouble();

		ArrayList<MzPeak> mzPeaks = new ArrayList<MzPeak>();

		DataPoint dataPoints[] = scan.getDataPoints();

		// Find possible mzPeaks
		for (int j = 0; j < dataPoints.length; j++) {

			// Is intensity above the noise level?
			if (dataPoints[j].getIntensity() >= noiseLevel) {
				// Yes, then mark this index as mzPeak
				mzPeaks.add(new MzPeak(dataPoints[j]));
			}
		}
		return mzPeaks.toArray(new MzPeak[0]);
	}

	public ParameterSet getParameters() {
		return parameters;
	}

	public String toString() {
		return "Centroid";
	}

	@Override
	public ParameterSet getParameterSet() {
		return parameters;
	}

}
